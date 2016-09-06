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

package energy.usef.brp.service.business;

import energy.usef.brp.exception.BrpBusinessError;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.repository.ConnectionGroupStateRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.service.validation.CoreBusinessError;
import energy.usef.core.util.DateTimeUtil;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service class implements the business logic related to the DSO part of the d-prognosis reception.
 */
@Stateless
public class BrpPlanboardValidatorService {
    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final long MAX_POWER = 2000000000L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BrpPlanboardValidatorService.class);

    @Inject
    private Config config;

    @Inject
    private PlanboardMessageRepository planboardMessageRepository;

    @Inject
    private PtuContainerRepository ptuContainerRepository;

    @Inject
    private ConnectionGroupStateRepository connectionGroupStateRepository;

    /**
     * Validate message for individual PTU's.
     *
     * @param ptus
     * @throws BusinessValidationException
     */
    public void validatePtus(List<PTU> ptus) throws BusinessValidationException {
        String ptuDuration = config.getProperty(ConfigParam.PTU_DURATION);
        int numOfPTUs = MINUTES_PER_DAY / Integer.valueOf(ptuDuration);
        for (PTU ptu : ptus) {
            if (ptu.getPower().compareTo(BigInteger.valueOf(MAX_POWER)) > 0) {
                throw new BusinessValidationException(BrpBusinessError.POWER_VALUE_TOO_BIG);
            }
        }
        if (ptus.size() != numOfPTUs) {
            LOGGER.info("Invalid number of PTUs: actual is {}, expected {}", ptus.size(), numOfPTUs);
            throw new BusinessValidationException(BrpBusinessError.PTUS_INCOMPLETE);
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
            throw new BusinessValidationException(BrpBusinessError.INVALID_PERIOD);
        }
    }

    /**
     * Validate the sequence number of a incoming prognosis. If the incoming prognosis is a updated version of an existing one, its
     * sequence number should be greater than the previous one.
     *
     * @param prognosis
     * @throws BusinessValidationException
     */
    public void validateAPlanSequenceNumber(Prognosis prognosis) throws BusinessValidationException {
        Long maxSequence = planboardMessageRepository.findMaxPlanboardMessageSequence(
                DocumentType.A_PLAN, prognosis.getMessageMetadata().getSenderDomain(), prognosis.getPeriod(),
                prognosis.getCongestionPoint(), null);
        if (prognosis.getSequence() < maxSequence) {
            throw new BusinessValidationException(BrpBusinessError.DOCUMENT_SEQUENCE_NUMBER_IS_TOO_SMALL);
        }
    }

    /**
     * Validates if every {@link PTU} is present in the planboard (i.e. has been initialized in a first place).
     *
     * @param period {@link LocalDate} period.
     * @param ptus {@link List} of {@link PTU}.
     * @throws BusinessValidationException if any given PTU is not in the database.
     */
    public void validatePlanboardHasBeenInitialized(LocalDate period, List<PTU> ptus) throws BusinessValidationException {
        Map<Integer, PtuContainer> ptuContainersMap = ptuContainerRepository.findPtuContainersMap(period);
        Optional<PTU> notInitializedPtu = ptus.stream()
                .filter(ptu -> ptuContainersMap.get(ptu.getStart().intValue()) == null)
                .findAny();
        if (notInitializedPtu.isPresent()) {
            throw new BusinessValidationException(CoreBusinessError.NOT_INITIALIZED_PLANBOARD, period);
        }
    }

    /**
     * Validates whether an Aggregator Connection Group with the given usef identifier is active at the given period.
     *
     * @param period {@link LocalDate} period.
     * @param usefIdentifier {@link String} USEF identifier of the connection group.
     * @throws BusinessValidationException
     */
    public void validateAPlanConnectionGroup(LocalDate period, String usefIdentifier) throws BusinessValidationException {
        if (connectionGroupStateRepository.findConnectionGroupStatesByUsefIdentifier(usefIdentifier, period).isEmpty()) {
            throw new BusinessValidationException(CoreBusinessError.UNRECOGNIZED_CONNECTION_GROUP, usefIdentifier);
        }
    }

}
