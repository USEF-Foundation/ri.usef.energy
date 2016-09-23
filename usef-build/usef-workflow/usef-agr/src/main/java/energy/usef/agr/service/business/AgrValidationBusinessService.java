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

package energy.usef.agr.service.business;

import energy.usef.agr.exception.AgrBusinessError;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.DispositionAvailableRequested;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.repository.ConnectionGroupRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Business Service class which does the validation of incoming messages.
 */
@Stateless
public class AgrValidationBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrValidationBusinessService.class);

    @Inject
    private PtuPrognosisRepository prognosisRepository;

    @Inject
    private ConnectionGroupRepository connectionGroupRepository;

    @Inject
    private Config config;

    @Inject
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Inject
    private AgrPlanboardBusinessService agrPlanboardBusinessService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    /**
     * Validate if the Connection Group does exist. If not, the flex request will be rejected.
     * 
     * @param usefIdentifier the usefIdentifier of the ConnectionGroup.
     * @throws BusinessValidationException validation exception when business validation fails.
     */
    public void validateConnectionGroup(String usefIdentifier) throws BusinessValidationException {
        if (connectionGroupRepository.find(usefIdentifier) == null) {
            throw new BusinessValidationException(AgrBusinessError.NON_EXISTING_GRID_POINT, usefIdentifier);
        }
    }

    /**
     * Validate if there is a ptu with disposition=Requested.
     * 
     * @param ptus the list of PTU's.
     * @throws BusinessValidationException validation exception when business validation fails.
     */
    public void validatePTUsContainsDispositionRequested(List<PTU> ptus) throws BusinessValidationException {
        for (PTU ptu : ptus) {
            if (DispositionAvailableRequested.REQUESTED == ptu.getDisposition()) {
                return;
            }
        }
        throw new BusinessValidationException(AgrBusinessError.INVALID_PTUS);
    }

    /**
     * The flex request should map on the D-prognosis which is send earlier to the DSO. The DSO reacts with a flex request on the
     * D-prognosis. So, if the D-prognosis does not exist based on origin and sequence, the flex request should be rejected.
     * 
     * @param prognosisOrigin the origin of the prognosis.
     * @param prognosisSequence the sequence of the prognosis.
     * @throws BusinessValidationException validation exception when business validation fails.
     */
    public void validatePrognosis(String prognosisOrigin, long prognosisSequence) throws BusinessValidationException {
        if (!config.getProperty(ConfigParam.HOST_DOMAIN).equals(prognosisOrigin)
                || prognosisRepository.findBySequence(prognosisSequence).isEmpty()) {
            throw new BusinessValidationException(AgrBusinessError.NON_EXISTING_PROGNOSIS, prognosisOrigin,
                    prognosisSequence);
        }
    }

    /**
     * This method first fetches a list of PtuFlexOffers based on the given FlexOrder's sequence and domain, then attempts to match
     * the PTUs based on price and power. If it fails to find a match, it throws a BusinessValidationException.
     * 
     * @param order the FlexOrder for which a matching FlexOffer to be found
     * @throws BusinessValidationException when no matching FlexOffer is found for FlexOrder
     */
    public void validateCorrespondingFlexOffer(FlexOrder order) throws BusinessValidationException {
        List<PTU> orderPTUs = order.getPTU();

        orderPTUs = PtuListConverter.normalize(orderPTUs);
        Map<Integer, PtuFlexOffer> flexOfferMap = corePlanboardBusinessService
                .findPtuFlexOffer(order.getFlexOfferSequence(), order.getMessageMetadata().getSenderDomain());

        PlanboardMessage planboardMessage = corePlanboardValidatorService
                .validatePlanboardMessageExpirationDate(order.getFlexOfferSequence(), DocumentType.FLEX_OFFER,
                        order.getMessageMetadata().getSenderDomain());

        if (planboardMessage.getDocumentStatus() == DocumentStatus.REVOKED) {
            throw new BusinessValidationException(AgrBusinessError.FLEX_OFFER_REVOKED);
        }

        for (PTU orderPTU : orderPTUs) {
            PtuFlexOffer offerPTU = flexOfferMap.get(orderPTU.getStart().intValue());
            if (offerPTU == null ||
                    (!(offerPTU.getPrice().equals(orderPTU.getPrice())) || !(offerPTU.getPower().equals(orderPTU.getPower())))) {
                throw new BusinessValidationException(AgrBusinessError.NO_MATCHING_OFFER_FOR_ORDER, order.getSequence());
            }


        }
    }

    /**
     * FlexOrders should only be accepted if they refer to a future period or contain future PTU's with a power value != 0.
     *
     * @param order is the {@link FlexOrder} to validate
     * @throws BusinessValidationException
     */
    public void validateFlexOrderTiming(FlexOrder order) throws BusinessValidationException {
        Integer ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);
        LocalDateTime currentDateTime = DateTimeUtil.getCurrentDateTime();

        LocalDate currentPeriod = DateTimeUtil.getCurrentDate();
        int numberOfPtus = PtuUtil.getNumberOfPtusPerDay(currentPeriod, ptuDuration);
        int currentPtu = PtuUtil.getPtuIndex(currentDateTime, ptuDuration);

        // No further checking if the flex order is entirely in the future
        if (currentPeriod.isBefore(order.getPeriod())) {
            return;
        }

        // Do not accept the flex order if it is entirely in the past
        if (currentPeriod.isAfter(order.getPeriod())) {
            LOGGER.warn("Flex offer not acceptable, period is in the past. ");
            throw new BusinessValidationException(AgrBusinessError.FLEX_ORDER_PERIOD_IN_THE_PAST, order);
        }

        // Only accept flex order that have at least one future PTU with a power value <> 0
        List<PTU> orderPTUs = order.getPTU();
        orderPTUs = PtuListConverter.normalize(orderPTUs);

        boolean valid = false;
        for (int i = currentPtu - 1; i < numberOfPtus && !valid; i++) {
            if (orderPTUs.get(i).getPower().compareTo(BigInteger.ZERO) != 0) {
                valid = true;
            }
        }

        if (!valid) {
            LOGGER.warn("Flex order not acceptable, no future PTU's with non-zero power values. ");
            throw new BusinessValidationException(AgrBusinessError.FLEX_ORDER_WITHOUT_NON_ZERO_FUTURE_POWER, order);
        }
    }

}
