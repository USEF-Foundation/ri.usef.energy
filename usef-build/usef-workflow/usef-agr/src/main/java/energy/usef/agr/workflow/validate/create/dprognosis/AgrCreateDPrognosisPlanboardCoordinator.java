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

package energy.usef.agr.workflow.validate.create.dprognosis;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.nonudi.goals.AgrNonUdiSetAdsGoalsEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.PrognosisType;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.event.StartValidateEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregator coordinator class interacting with the planboard in the 'Create and Send D-Prognosis' workflow.
 * <p>
 * No longer a Singleton, as this shall be executed per congestionPoint.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class AgrCreateDPrognosisPlanboardCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrCreateDPrognosisPlanboardCoordinator.class);

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private CorePlanboardValidatorService planboardValidatorService;

    @Inject
    private AgrPlanboardBusinessService agrPlanboardBusinessService;

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private Event<CreateDPrognosisEvent> createDPrognosisEventManager;

    @Inject
    private Event<AgrNonUdiSetAdsGoalsEvent> agrSetAdsGoalsEventManager;

    @Inject
    private Config config;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * Handles a {@link StartValidateEvent} which contains the period for which D-Prognoses should be recreated.
     * <p>
     * This triggers an {@link ReCreateDPrognosisEvent} which is handled by the {@link #handleReCreateDPrognosisEvent}.
     *
     * @param event {@link StartValidateEvent}.
     */

    @Asynchronous
    public void handleStartValidateEvent(@Observes StartValidateEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodInFuture(event);
        handleReCreateDPrognosisEvent(new ReCreateDPrognosisEvent(event.getPeriod()));
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Handles a {@link ReCreateDPrognosisEvent} which contains the period for which D-Prognoses should be recreated.
     * <p>
     * For all D-Prognosis missing or flagged as 'TO_BE_RECREATED' in the database, the method will select the distinct congestion
     * points and fire one {@link CreateDPrognosisEvent} per congestion point.
     *
     * @param event
     */
    @Asynchronous
    public void handleReCreateDPrognosisEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) ReCreateDPrognosisEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);
        doHandleReCreateDPrognosisEvent(event);
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    @Lock(LockType.WRITE)
    private void doHandleReCreateDPrognosisEvent(ReCreateDPrognosisEvent event) {
        List<PlanboardMessage> dPrognoses = corePlanboardBusinessService
                .findPlanboardMessages(DocumentType.D_PROGNOSIS, event.getPeriod(), null);

        List<String> connectionGroups = corePlanboardBusinessService.findActiveCongestionPointAddresses(event.getPeriod());

        // find the congestionPoints that already have DPrognosis which do NOT need to be recreated and are NOT archived.
        List<String> ignorableUsefIdentifiers = dPrognoses
                .stream()
                .filter(dPrognosis -> !(DocumentStatus.ARCHIVED.equals(dPrognosis.getDocumentStatus())))
                .filter(dPrognosis -> !(DocumentStatus.TO_BE_RECREATED.equals(dPrognosis.getDocumentStatus())))
                .map(dPrognosis -> dPrognosis.getConnectionGroup().getUsefIdentifier())
                .collect(Collectors.toList());

        // fire a CreateDPrognosisEvent, for all the missing one's and the TO_BE_RECREATED
        connectionGroups.stream().filter(cpcg -> !ignorableUsefIdentifiers.contains(cpcg))
                .forEach(cpcg -> createDPrognosisEventManager.fire(new CreateDPrognosisEvent(event.getPeriod(), cpcg)));

        // change the status of each TO_BE_RECREATED -> ARCHIVED
        dPrognoses.stream()
                .filter(dPrognosis -> DocumentStatus.TO_BE_RECREATED.equals(dPrognosis.getDocumentStatus()))
                .forEach(dPrognosis -> dPrognosis.setDocumentStatus(DocumentStatus.ARCHIVED));
    }

    /**
     * Handles the {@link CreateDPrognosisEvent}. If the given period in the event is <code>null</code>, the period will be set to
     * the current date. If the given congestion point in the event is <code>null</code>, prognoses will be created for every active
     * congestion point.
     *
     * @param event
     */
    @Asynchronous
    public void handleEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) CreateDPrognosisEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);
        doHandleEvent(event);
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    @Lock(LockType.WRITE)
    private void doHandleEvent(CreateDPrognosisEvent event) {
        CongestionPointConnectionGroup connectionGroup = corePlanboardBusinessService
                .findCongestionPointConnectionGroup(event.getCongestionPoint());

        // initialize xmlDPrognosis message for DSO.
        Prognosis xmlDPrognosis = initializePrognosis(connectionGroup, event.getPeriod());

        // find Portfolio
        Map<Integer, PowerContainer> powerContainersPerPtu = agrPortfolioBusinessService
                .findActivePortfolioForConnectionGroupLevel(event.getPeriod(), Optional.of(connectionGroup)).get(connectionGroup);

        // fetch the latest d-xmlDPrognosis
        List<PrognosisDto> latestDPrognoses = agrPlanboardBusinessService.findLastPrognoses(event.getPeriod(),
                energy.usef.core.model.PrognosisType.D_PROGNOSIS, Optional.of(event.getCongestionPoint()));
        PrognosisDto latestDPrognosis = latestDPrognoses.stream()
                .filter(dPrognosis -> xmlDPrognosis.getMessageMetadata().getRecipientDomain()
                        .equals(dPrognosis.getParticipantDomain()))
                .findFirst().orElse(null);

        // Populate PTU in the xmlDPrognosis message
        powerContainersPerPtu.entrySet().stream()
                .map(entry -> buildPtu(event.getPeriod(), latestDPrognosis, entry.getKey(), entry.getValue()))
                .forEach(ptu -> xmlDPrognosis.getPTU().add(ptu));

        // store the xmlDPrognosis in the plan board
        corePlanboardBusinessService.storePrognosis(connectionGroup.getUsefIdentifier(), xmlDPrognosis, DocumentType.D_PROGNOSIS,
                DocumentStatus.SENT, xmlDPrognosis.getMessageMetadata().getRecipientDomain(), null, false);

        // send the message to the queue
        List<PTU> compressedPtus = PtuListConverter.compact(xmlDPrognosis.getPTU());
        xmlDPrognosis.getPTU().clear();
        compressedPtus.stream().forEach(ptu -> xmlDPrognosis.getPTU().add(ptu));
        String prognosisAsXml = XMLUtil.messageObjectToXml(xmlDPrognosis);
        jmsHelperService.sendMessageToOutQueue(prognosisAsXml);

        // trigger set ADS goals for non-udi aggregators
        if (configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            agrSetAdsGoalsEventManager.fire(new AgrNonUdiSetAdsGoalsEvent(event.getPeriod(), event.getCongestionPoint()));
        }

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private PTU buildPtu(LocalDate period, PrognosisDto latestDPrognosis, Integer ptuIndex, PowerContainer powerContainer) {
        PTU ptu = new PTU();
        ptu.setDuration(BigInteger.ONE);
        ptu.setStart(BigInteger.valueOf(ptuIndex));

        if (planboardValidatorService.isPtuContainerWithinIntradayGateClosureTime(new PtuContainer(period, ptuIndex))) {
            // get the power value from the previous d-prognosis if it exists. Otherwise, 0.
            ptu.setPower(findPreviousValueForDPrognosis(latestDPrognosis, ptuIndex));
        } else {
            if (powerContainer.getForecast() != null) {
                ptu.setPower(powerContainer.getForecast().calculatePower());
            } else if (powerContainer.getProfile() != null) {
                ptu.setPower(powerContainer.getProfile().calculatePower());
            } else {
                ptu.setPower(BigInteger.ZERO);
            }
        }
        return ptu;
    }

    private Prognosis initializePrognosis(CongestionPointConnectionGroup congestionPointConnectionGroup, LocalDate period) {
        Prognosis prognosis = new Prognosis();
        prognosis.setCongestionPoint(congestionPointConnectionGroup.getUsefIdentifier());
        prognosis.setPeriod(period);
        prognosis.setPTUDuration(Period.minutes(config.getIntegerProperty(ConfigParam.PTU_DURATION)));
        prognosis.setSequence(sequenceGeneratorService.next());
        prognosis.setMessageMetadata(buildMessageMetadata(congestionPointConnectionGroup.getDsoDomain()));
        prognosis.setTimeZone(config.getProperty(ConfigParam.TIME_ZONE));
        prognosis.setType(PrognosisType.D_PROGNOSIS);
        return prognosis;
    }

    /**
     * Fills the power consumption per ptu map with the power value of the latest (previous) d-prognosis for the specified ptu.
     *
     * @param latestPrognosis {@link PrognosisDto} prognosis dto (can be  null).
     * @param ptuIndex {@link Integer} which cannot be null.
     * @return
     */
    private BigInteger findPreviousValueForDPrognosis(PrognosisDto latestPrognosis, Integer ptuIndex) {
        if (latestPrognosis == null) {
            return BigInteger.ZERO;
        }
        Optional<PtuPrognosisDto> optionalPtu = latestPrognosis.getPtus().stream()
                .filter(ptuPrognosisDto -> ptuIndex.equals(ptuPrognosisDto.getPtuIndex().intValue()))
                .findFirst();
        if (optionalPtu.isPresent()) {
            return optionalPtu.get().getPower();
        }
        return BigInteger.ZERO;
    }

    private MessageMetadata buildMessageMetadata(String dsoDomain) {
        return MessageMetadataBuilder.build(dsoDomain, USEFRole.DSO, config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.AGR,
                TRANSACTIONAL).build();
    }
}
