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

package energy.usef.core.service.validation;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOfferRevocation;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.Document;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuState;
import energy.usef.core.repository.ConnectionGroupRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuStateRepository;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;

/**
 * Validation service class containing the role-independant planboard validations.
 */
@Stateless
public class CorePlanboardValidatorService {

    @Inject
    private PtuContainerRepository ptuContainerRepository;

    @Inject
    private PlanboardMessageRepository planboardMessageRepository;

    @Inject
    private PtuStateRepository ptuStateRepository;

    @Inject
    private ConnectionGroupRepository connectionGroupRepository;

    @Inject
    private Config config;

    /**
     * Checks whether a planboard entry is not saved if within the gate closure time (amount of PTUs from the start of the operate
     * phase).
     *
     * @param planboardItem {@link Document} planboard entry.
     * @return <code>true</code> if the planboard entry is too close to the operate phase.
     */
    public boolean isPlanboardItemWithingIntradayGateClosureTime(Document planboardItem) {
        // FIX-ME: Refactor further to use method isPtuContainerWithinIntradayGateClosureTime?
        if (planboardItem.getPtuContainer() != null) {
            return isPtuContainerWithinIntradayGateClosureTime(planboardItem.getPtuContainer());
        } else {
            return false;
        }

    }

    /**
     * Verify whether PTU Container is within IntradayGateClosureTime.
     *
     * @param ptuContainer PTU Container
     * @return true if the PTU Container is within IntradayGateClosureTime, false otherwise
     */
    public boolean isPtuContainerWithinIntradayGateClosureTime(PtuContainer ptuContainer) {
        Integer gateClosure = config.getIntegerProperty(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS);
        Integer ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);
        LocalDateTime todayNow = DateTimeUtil.getCurrentDateTime();
        Integer ptusToday = PtuUtil.getNumberOfPtusPerDay(todayNow.toLocalDate(), ptuDuration);
        Integer minutesToday = DateTimeUtil.getElapsedMinutesSinceMidnight(todayNow);

        Integer ptuIndexPivot = 1 + gateClosure + minutesToday / ptuDuration;
        LocalDate ptuDatePivot = todayNow.toLocalDate();

        if (ptuIndexPivot > ptusToday) {
            ptuIndexPivot = ptuIndexPivot % ptusToday;
            ptuDatePivot = ptuDatePivot.plusDays(1);
        }

        int ptuDateComparedToDatePivot = new LocalDate(ptuContainer.getPtuDate()).compareTo(ptuDatePivot);
        if (ptuDateComparedToDatePivot == 1) {
            // PTU date is the one day after the current limit, no problem
            return false;
        } else if (ptuDateComparedToDatePivot == 0
                && ptuContainer.getPtuIndex().compareTo(ptuIndexPivot) == 1) {
            // PTU date is the same day as the limit but the PTU index is after the index limit, no problem
            return false;
        }
        // PTU date is the same day (or before) and the PTU index is the same or before the index limit, problem
        return true;
    }

    /**
     * This methods checks whether a planboard item (all the documents of the same type with the same sequence number) has one of
     * its PTUs in the operate phase already.
     *
     * @param planboardItem {@link Document} a planboard item.
     * @return <code>true</code> if at least one of the PTUs for a document of the same type with the same sequence number is in
     * operate phase. <code>false</code> otherwise.
     */
    public boolean hasPlanboardItemPtusInOperatePhase(Document planboardItem) {
        // fetch the list of PTU container
        List<PtuContainer> ptuContainers = ptuContainerRepository.findPtuContainersForDocumentSequence(planboardItem.getSequence(),
                planboardItem.getClass());
        for (PtuContainer ptuContainer : ptuContainers) {
            PtuState ptuState = ptuStateRepository.findOrCreatePtuState(ptuContainer, planboardItem.getConnectionGroup());
            if (PtuContainerState.Operate == ptuState.getState()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates the timezone against the configured timezone.
     *
     * @param timezone
     * @throws BusinessValidationException
     */
    public void validateTimezone(String timezone) throws BusinessValidationException {
        if (!config.getProperty(ConfigParam.TIME_ZONE).equals(timezone)) {
            throw new BusinessValidationException(CoreBusinessError.INVALID_TIMEZONE);
        }
    }

    /**
     * Validates the PTU Duration against the configured PTU Duration.
     *
     * @param ptuDuration
     * @throws BusinessValidationException
     */
    public void validatePTUDuration(Period ptuDuration) throws BusinessValidationException {
        if (!config.getIntegerProperty(ConfigParam.PTU_DURATION).equals(ptuDuration.getMinutes())) {
            throw new BusinessValidationException(CoreBusinessError.INVALID_PTU_DURATION);
        }
    }

    /**
     * Validates the currency against the configured currency.
     *
     * @param currency
     * @throws BusinessValidationException
     */
    public void validateCurrency(String currency) throws BusinessValidationException {
        if (!config.getProperty(ConfigParam.CURRENCY).equals(currency)) {
            throw new BusinessValidationException(CoreBusinessError.INVALID_CURRENCY);
        }
    }

    /**
     * Validates if the domain is the participants own domain.
     *
     * @param domain
     * @throws BusinessValidationException
     */
    public void validateDomain(String domain) throws BusinessValidationException {
        if (!config.getProperty(ConfigParam.HOST_DOMAIN).equals(domain)) {
            throw new BusinessValidationException(CoreBusinessError.INVALID_DOMAIN, domain,
                    config.getProperty(ConfigParam.HOST_DOMAIN));
        }
    }

    /**
     * Validates if all PTU's are present for the required period.
     *
     * @param ptus
     * @param period
     * @param normalized
     * @throws BusinessValidationException
     */
    public void validatePTUsForPeriod(List<PTU> ptus, LocalDate period, boolean normalized) throws BusinessValidationException {
        int numberOfPtusPerDay = PtuUtil.getNumberOfPtusPerDay(period, config.getIntegerProperty(ConfigParam.PTU_DURATION));
        List<PTU> normalizedPtus = ptus;
        if (!normalized) {
            normalizedPtus = PtuListConverter.normalize(normalizedPtus);
        }
        if (normalizedPtus == null || numberOfPtusPerDay != normalizedPtus.size()) {
            throw new BusinessValidationException(CoreBusinessError.WRONG_NUMBER_OF_PTUS, normalizedPtus.size(),
                    numberOfPtusPerDay);
        }
        // check if all are present
        PtuUtil.orderByStart(normalizedPtus);
        int expectedStart = 1;
        for (PTU ptu : normalizedPtus) {
            if (ptu.getStart().intValue() != expectedStart) {
                throw new BusinessValidationException(CoreBusinessError.INCOMPLETE_PTUS, expectedStart);
            }
            expectedStart++;
        }

    }

    /**
     * Find a {@Link planboardMessage} given it's sequence, documentType and senderDomain, validating the expirationDate.
     *
     * @param sequence
     * @param documentType
     * @param senderDomain
     * @return flexRequest
     * @throws BusinessValidationException
     */
    public PlanboardMessage validatePlanboardMessageExpirationDate(long sequence, DocumentType documentType, String senderDomain)
            throws BusinessValidationException {
         PlanboardMessage planboardMessage = planboardMessageRepository
                .findSinglePlanboardMessage(sequence, documentType, senderDomain);

        if (planboardMessage == null) {
            throw new BusinessValidationException(CoreBusinessError.RELATED_MESSAGE_NOT_FOUND, sequence);
        }

        if (planboardMessage.getExpirationDate() != null &&  planboardMessage.getExpirationDate().isBefore(DateTimeUtil.getCurrentDateTime())) {
            throw new BusinessValidationException(CoreBusinessError.DOCUMENT_EXIRED, documentType, sequence,
                    planboardMessage.getExpirationDate());
        }
        return planboardMessage;
    }

    /**
     * Check's if not all ptu's in this period are in the requested phases. For Example: to check if there is still a ptu in the
     * period of a received FlexOffer that is not in PENDING_SETTLEMENT or SETTLEMENT.
     *
     * @param usefIdentifier
     * @param period
     * @param phaseTypes
     * @throws BusinessValidationException
     */
    public void validateIfPTUForPeriodIsNotInPhase(String usefIdentifier, LocalDate period, PtuContainerState... phaseTypes)
            throws BusinessValidationException {
        List<PtuContainerState> phaseTypesList = Arrays.asList(phaseTypes);

        ConnectionGroup connectionGroup = connectionGroupRepository.find(usefIdentifier);

        Collection<PtuContainer> ptuContainers = ptuContainerRepository.findPtuContainersMap(period).values();
        for (PtuContainer ptuContainer : ptuContainers) {
            PtuState ptuState = ptuStateRepository.findOrCreatePtuState(ptuContainer, connectionGroup);
            if (!phaseTypesList.contains(ptuState.getState())) {
                return;
            }
        }
        throw new BusinessValidationException(CoreBusinessError.PTUS_IN_WRONG_PHASE, usefIdentifier, period, phaseTypesList);
    }

    /**
     * Check's if all ptu's in this period are in the given phases.
     *
     * @param usefIdentifier
     * @param period
     * @param ptuContainerStates
     * @throws BusinessValidationException
     */
    public void validateIfAllPTUForPeriodAreNotInPhase(String usefIdentifier, LocalDate period,
            PtuContainerState... ptuContainerStates)
            throws BusinessValidationException {
        List<PtuContainerState> ptuContainerStateList = Arrays.asList(ptuContainerStates);

        List<PtuState> ptuStates = ptuStateRepository.findPtuStates(period, usefIdentifier);
        for (PtuState ptuState : ptuStates) {
            if (ptuContainerStateList.contains(ptuState.getState())) {
                throw new BusinessValidationException(CoreBusinessError.PTUS_IN_WRONG_PHASE, usefIdentifier, period,
                        ptuContainerStateList);
            }
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if planboardMessages is empty.
     *
     * @param flexOfferRevocation the flexOfferRevocation event
     * @param planboardMessages list of planboardMessages
     * @throws BusinessValidationException
     */
    public void checkRelatedPlanboardMessagesExist(FlexOfferRevocation flexOfferRevocation,
            List<PlanboardMessage> planboardMessages)
            throws BusinessValidationException {
        if (planboardMessages.isEmpty()) {
            throw new BusinessValidationException(CoreBusinessError.NO_PLAN_BOARD_MESSAGE_RELATED,
                    flexOfferRevocation.getSequence());
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if flexOffers is empty.
     *
     * @param flexOfferRevocation the flexOfferRevocation event
     * @param flexOffers Map of Ptu Index -> flexOffers
     * @throws BusinessValidationException
     */
    public void checkRelatedFlexOffersExist(FlexOfferRevocation flexOfferRevocation, Map<Integer, PtuFlexOffer> flexOffers)
            throws BusinessValidationException {
        if (flexOffers.isEmpty()) {
            throw new BusinessValidationException(CoreBusinessError.NO_FLEX_OFFER_RELATED,
                    flexOfferRevocation.getSequence(),
                    flexOfferRevocation.getMessageMetadata().getSenderDomain());
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if one of the PTU phases in flex offers is operate state or later.
     *
     * @param flexOfferRevocation the flexOfferRevocation event
     * @param flexOffers Map of Ptu Index -> flexOffers
     * @throws BusinessValidationException
     */
    public void checkPtuPhase(FlexOfferRevocation flexOfferRevocation, Map<Integer, PtuFlexOffer> flexOffers)
            throws BusinessValidationException {
        for (PtuFlexOffer flexOffer : flexOffers.values()) {
            PtuState ptuState = ptuStateRepository
                    .findOrCreatePtuState(flexOffer.getPtuContainer(), flexOffer.getConnectionGroup());
            if (ptuState.getState().getIndex() >= PtuContainerState.Operate.getIndex()) {
                throw new BusinessValidationException(CoreBusinessError.FLEX_OFFER_HAS_PTU_IN_OPERATE_OR_LATER_PHASE,
                        flexOfferRevocation.getSequence(),
                        flexOfferRevocation.getMessageMetadata().getSenderDomain(),
                        flexOffer.getPtuContainer().getPtuIndex());
            }
        }
    }

}
