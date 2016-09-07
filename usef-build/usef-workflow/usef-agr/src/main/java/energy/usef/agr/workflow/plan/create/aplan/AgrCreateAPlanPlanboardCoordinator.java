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

package energy.usef.agr.workflow.plan.create.aplan;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.nonudi.goals.AgrNonUdiSetAdsGoalsEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.PrognosisType;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.XMLUtil;

/**
 * Aggregator coordinator class interacting with the planboard in the 'Create and Send A-Plan' workflow.
 */
@Stateless
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class AgrCreateAPlanPlanboardCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrCreateAPlanPlanboardCoordinator.class);

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private CorePlanboardValidatorService planboardValidatorService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private Config config;

    @Inject
    private Event<AgrNonUdiSetAdsGoalsEvent> agrSetAdsGoalsEventManager;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * This method implements the logic to create A-Plan.
     *
     * @param event A {@link CreateAPlanEvent}
     */
    @Asynchronous
    public void handleEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) CreateAPlanEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);

        ConnectionGroup connectionGroup = corePlanboardBusinessService.findConnectionGroup(event.getUsefIdentifier());

        Map<Integer, PowerContainer> powerContainersPerPtu = agrPortfolioBusinessService
                .findActivePortfolioForConnectionGroupLevel(event.getPeriod(), Optional.of(connectionGroup))
            .get(connectionGroup);

        createAndSendAPlan(event.getPeriod(), connectionGroup, powerContainersPerPtu);
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void createAndSendAPlan(LocalDate period, ConnectionGroup connectionGroup,
            Map<Integer, PowerContainer> powerContainersPerPtu) {
        // Fetch latest A-Plan
        // Map: Ptu Index -> PtuPrognosis
        Map<Integer, PtuPrognosis> latestAPlans = corePlanboardBusinessService
                .findLastPrognoses(period, energy.usef.core.model.PrognosisType.A_PLAN, connectionGroup.getUsefIdentifier()).stream()
                .collect(Collectors.toMap(ptuPrognosis -> ptuPrognosis.getPtuContainer().getPtuIndex(), Function.identity()));

        // Initialize prognosis message for BRP
        Prognosis aPlan = initializeAPlan(connectionGroup.getUsefIdentifier(), period);

        // Populate PTU in the prognosis message
        powerContainersPerPtu.entrySet().stream()
                .map(entry -> buildPtu(period, latestAPlans, entry.getKey(), entry.getValue()))
                .forEach(ptu -> aPlan.getPTU().add(ptu));

        // Store the prognosis in the plan board
        corePlanboardBusinessService.storePrognosis(aPlan, connectionGroup, DocumentType.A_PLAN, DocumentStatus.SENT,
                connectionGroup.getUsefIdentifier(), null, false);

        // Send the message to the queue
        String aPlanAsXml = XMLUtil.messageObjectToXml(aPlan);
        jmsHelperService.sendMessageToOutQueue(aPlanAsXml);

        // trigger set ADS goals for non-udi aggregators
        if (configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            agrSetAdsGoalsEventManager.fire(new AgrNonUdiSetAdsGoalsEvent(period, connectionGroup.getUsefIdentifier()));
        }
    }

    private PTU buildPtu(LocalDate period, Map<Integer, PtuPrognosis> latestAPlans, Integer ptuIndex,
            PowerContainer powerContainer) {
        PTU ptu = new PTU();
        ptu.setDuration(BigInteger.ONE);
        ptu.setStart(BigInteger.valueOf(ptuIndex));

        if (powerContainer.getForecast() != null) {
            ptu.setPower(powerContainer.getForecast().calculatePower());
        } else if (powerContainer.getProfile() != null) {
            ptu.setPower(powerContainer.getProfile().calculatePower());
        } else {
            ptu.setPower(BigInteger.ZERO);
        }

        if (planboardValidatorService.isPtuContainerWithinIntradayGateClosureTime(new PtuContainer(period, ptuIndex))) {
            if (latestAPlans.containsKey(ptuIndex)) {
                LOGGER.warn(
                        "Connection forecast for ptu {} is within the intraday gate closure time. Latest A-Plan will be used and sent.",
                        ptuIndex);
                ptu.setPower(latestAPlans.get(ptuIndex).getPower());
            } else {
                LOGGER.warn(
                        "Connection forecast is within the intraday gate closure time. No latest A-Plan found, power will be 0 for ptu {}.",
                        ptuIndex);
                ptu.setPower(BigInteger.ZERO);
            }
        }
        return ptu;
    }

    private Prognosis initializeAPlan(String brpDomain, LocalDate period) {
        Prognosis aPlan = new Prognosis();
        aPlan.setPeriod(period);
        aPlan.setPTUDuration(Period.minutes(config.getIntegerProperty(ConfigParam.PTU_DURATION)));
        aPlan.setSequence(sequenceGeneratorService.next());
        aPlan.setMessageMetadata(buildMessageMetadata(brpDomain));
        aPlan.setTimeZone(config.getProperty(ConfigParam.TIME_ZONE));
        aPlan.setType(PrognosisType.A_PLAN);
        return aPlan;
    }

    private MessageMetadata buildMessageMetadata(String brpDomain) {
        return MessageMetadataBuilder.build(brpDomain, USEFRole.BRP, config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.AGR,
                TRANSACTIONAL).build();
    }

}
