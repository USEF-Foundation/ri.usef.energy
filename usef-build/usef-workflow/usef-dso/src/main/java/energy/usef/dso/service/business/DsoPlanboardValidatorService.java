/*
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package energy.usef.dso.service.business;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOffer;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PtuFlexRequest;
import energy.usef.core.repository.CongestionPointConnectionGroupRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuFlexRequestRepository;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.dso.exception.DsoBusinessError;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.repository.AggregatorOnConnectionGroupStateRepository;
import org.joda.time.LocalDate;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigInteger;
import java.util.List;

import static energy.usef.dso.exception.DsoBusinessError.FLEX_OFFER_NOT_AS_REQUESTED;

/**
 * This service class implements the business logic related to the DSO part of the d-prognosis reception.
 */
@Stateless
public class DsoPlanboardValidatorService {

    private static final int MINUTES_PER_DAY = 24 * 60;

    private static final int EXTREME_POWER_VALUE = 2000000000;
    private static BigInteger MAX_POWER = BigInteger.valueOf(EXTREME_POWER_VALUE);

    @Inject
    private CongestionPointConnectionGroupRepository congestionPointConnectionGroupRepository;

    @Inject
    private AggregatorOnConnectionGroupStateRepository aggregatorOnConnectionGroupStateRepository;

    @Inject
    private PtuFlexRequestRepository ptuFlexRequestRepository;

    @Inject
    private Config config;

    @Inject
    private PlanboardMessageRepository planboardMessageRepository;

    /**
     * Validate message for individual PTU's.
     *
     * @param ptus
     * @throws BusinessValidationException
     */
    public void validatePtus(List<PTU> ptus) throws BusinessValidationException {
        String ptuDuration = config.getProperty(ConfigParam.PTU_DURATION);
        int numOfPTUs = MINUTES_PER_DAY / Integer.valueOf(ptuDuration);
        BigInteger ptuCount = BigInteger.ZERO;
        for (PTU ptu : ptus) {
            ptuCount = ptuCount.add(ptu.getDuration());
            if (ptu.getPower().compareTo(MAX_POWER) > 0) {
                throw new BusinessValidationException(DsoBusinessError.POWER_VALUE_TOO_BIG);
            }
        }
        if (ptuCount.compareTo(BigInteger.valueOf(numOfPTUs)) != 0) {
            throw new BusinessValidationException(DsoBusinessError.PTUS_INCOMPLETE);
        }

    }

    /**
     * Validate message for individual PTU's.
     *
     * @param entityAddress
     * @throws BusinessValidationException
     */
    public void validateCongestionPoint(String entityAddress) throws BusinessValidationException {
        // Check whether gridPoint is known to DSO at all
        CongestionPointConnectionGroup congestionPoint = congestionPointConnectionGroupRepository.find(entityAddress);
        if (congestionPoint == null) {
            throw new BusinessValidationException(DsoBusinessError.NON_EXISTING_CONGESTION_POINT);
        }

    }

    /**
     * Validate message for being received by a aggregator with appropriate rights.
     *
     * @param sender
     * @param entityAddress
     * @param dateTime
     * @throws BusinessValidationException
     */
    public void validateAggregator(String sender, String entityAddress, LocalDate dateTime) throws BusinessValidationException {
        // Check whether the aggregator has rights to this congestionpoint
        List<Aggregator> aggregators = aggregatorOnConnectionGroupStateRepository.getAggregatorsByCongestionPointAddress(
                entityAddress, dateTime);
        if (aggregators == null) {
            throw new BusinessValidationException(DsoBusinessError.INVALID_SENDER);
        } else {
            for (Aggregator aggregator : aggregators) {
                if (aggregator.getDomain().equals(sender)) {
                    return;
                }
            }
            throw new BusinessValidationException(DsoBusinessError.INVALID_SENDER);
        }
    }

    /**
     * Validate period of message.
     *
     * @param periodDate
     * @throws BusinessValidationException
     */
    public void validatePeriod(LocalDate periodDate) throws BusinessValidationException {
        if (periodDate == null || periodDate.isBefore(DateTimeUtil.getCurrentDate())) {
            throw new BusinessValidationException(DsoBusinessError.INVALID_PERIOD);
        }
    }

    /**
     * Validate the sequence number of a incoming prognosis. If the incoming prognosis is a updated version of an existing one, its
     * sequence number should be greater than the previous one.
     *
     * @param prognosis
     * @throws BusinessValidationException
     */
    public void validatePrognosisSequenceNumber(Prognosis prognosis) throws BusinessValidationException {
        Long maxSequence = planboardMessageRepository.findMaxPlanboardMessageSequence(
                DocumentType.D_PROGNOSIS, prognosis.getMessageMetadata().getSenderDomain(), prognosis.getPeriod(),
                prognosis.getCongestionPoint(), null);
        if (prognosis.getSequence() < maxSequence) {
            throw new BusinessValidationException(DsoBusinessError.DOCUMENT_SEQUENCE_NUMBER_IS_TOO_SMALL);
        }
    }

    public void validateFlexOfferMatchesRequest(FlexOffer flexOffer) throws BusinessValidationException{
        String congestionPoint = flexOffer.getCongestionPoint();
        Long sequenceNumber = flexOffer.getFlexRequestSequence();
        String senderDomain = flexOffer.getMessageMetadata().getSenderDomain();

        List<PtuFlexRequest> ptuFlexRequests = ptuFlexRequestRepository.findPtuFlexRequestWithSequence(congestionPoint, sequenceNumber, senderDomain);
        List<PTU> flexOfferPtus = PtuListConverter.normalize(flexOffer.getPTU());

        boolean unacceptableFlexOffer = true;

        for (PtuFlexRequest ptuFlexRequest : ptuFlexRequests) {
            if (ptuFlexRequest.getDisposition() == DispositionAvailableRequested.REQUESTED) {
                PTU flexOfferPtu = flexOfferPtus.get(ptuFlexRequest.getPtuContainer().getPtuIndex() - 1);
                // Price must be provided and sign of requested and offered power must match
                if (flexOfferPtu.getPrice() != null && flexOfferPtu.getPower().signum() == ptuFlexRequest.getPower().signum()) {
                    unacceptableFlexOffer = false;
                }
            }
        }

        if (unacceptableFlexOffer) {
            throw new BusinessValidationException(FLEX_OFFER_NOT_AS_REQUESTED);
        }
    }
}
