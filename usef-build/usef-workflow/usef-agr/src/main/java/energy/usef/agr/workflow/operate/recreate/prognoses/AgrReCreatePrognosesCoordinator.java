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

package energy.usef.agr.workflow.operate.recreate.prognoses;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.plan.create.aplan.CreateAPlanEvent;
import energy.usef.agr.workflow.plan.recreate.aplan.ReCreateAPlanEvent;
import energy.usef.agr.workflow.validate.create.dprognosis.ReCreateDPrognosisEvent;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.RequestMoveToValidateEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class in charge of calling a PBC which will decide to trigger or not the creation of new A-Plans and/or D-Prognoses.
 */
@Singleton
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class AgrReCreatePrognosesCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrReCreatePrognosesCoordinator.class);

    @Inject
    private Config config;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private AgrPlanboardBusinessService agrPlanboardBusinessService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private Event<CreateAPlanEvent> createAPlanEventManager;

    @Inject
    private Event<ReCreateAPlanEvent> reCreateAPlanEventManager;

    @Inject
    private Event<ReCreateDPrognosisEvent> reCreateDPrognosisEventManager;

    @Inject
    private Event<RequestMoveToValidateEvent> moveToValidateEventManager;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * Handles the {@link ReCreatePrognosesEvent}.
     *
     * @param event {@link ReCreatePrognosesEvent}.
     */
    @Lock(LockType.WRITE)
    public void handleEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) ReCreatePrognosesEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);
        boolean moveToValidate = invokePbc(event);
        // try to move to validate
        if (moveToValidate) {
            moveToValidateEventManager.fire(new RequestMoveToValidateEvent(event.getPeriod()));
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    @SuppressWarnings("unchecked")
    private boolean invokePbc(ReCreatePrognosesEvent event) {
        LocalDate period = event.getPeriod();
        List<ConnectionPortfolioDto> connectionPortfolioDtos = agrPortfolioBusinessService.findConnectionPortfolioDto(period);

        // Find A-Plans and fire creation of new A-Plan for connection groups without any
        List<PrognosisDto> aPlans = agrPlanboardBusinessService.findLastPrognoses(period, PrognosisType.A_PLAN, Optional.empty());
        List<Long> rejectedAPlanSequences = agrPlanboardBusinessService.findRejectedPlanboardMessages(DocumentType.A_PLAN, period)
                .stream().map(PlanboardMessage::getSequence).collect(Collectors.toList());
        aPlans = aPlans.stream()
                .filter(aPlan -> !rejectedAPlanSequences.contains(aPlan.getSequenceNumber()))
                .collect(Collectors.toList());
        boolean moveToValidate = createMissingNewAPlansForAllConnectionGroups(period, aPlans);
        if (!moveToValidate) {
            return false;
        }
        // Find D-Prognoses
        List<PrognosisDto> dPrognoses = agrPlanboardBusinessService.findLastPrognoses(period, PrognosisType.D_PROGNOSIS, Optional.empty());

        // create a context with the latest A-Plans, D-Prognoses and current portfolio as input.
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(ReCreatePrognosesWorkflowParameter.IN.LATEST_A_PLANS_DTO_LIST.name(), aPlans);
        context.setValue(ReCreatePrognosesWorkflowParameter.IN.LATEST_D_PROGNOSES_DTO_LIST.name(), dPrognoses);
        context.setValue(ReCreatePrognosesWorkflowParameter.IN.CURRENT_PORTFOLIO.name(), connectionPortfolioDtos);
        context.setValue(ReCreatePrognosesWorkflowParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(),
                corePlanboardBusinessService.buildConnectionGroupsToConnectionsMap(period));
        context.setValue(ReCreatePrognosesWorkflowParameter.IN.PERIOD.name(), period);
        context.setValue(ReCreatePrognosesWorkflowParameter.IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        // call the PBC with context and validate the returned context .
        context = workflowStepExecuter.invoke(AgrWorkflowStep.AGR_RECREATE_PROGNOSES.name(), context);
        WorkflowUtil.validateContext(AgrWorkflowStep.AGR_RECREATE_PROGNOSES.name(), context, ReCreatePrognosesWorkflowParameter.OUT.values());

        List<Long> aPlanSequences = (List<Long>) context.getValue(ReCreatePrognosesWorkflowParameter.OUT.REQUIRES_NEW_A_PLAN_SEQUENCES_LIST.name());
        if (!aPlanSequences.isEmpty()) {
            moveToValidate = false;
        }

        // store the PBC result in the PlanboardMessage table.
        handlePrognosesToBeReCreated(period, context, dPrognoses, aPlans);

        return moveToValidate;
    }

    /**
     * Fires a new A-Plan for each connection group which has none.
     *
     * @param period {@link LocalDate} period for which new A-Plans have to be created.
     * @param aPlans {@link List} of {@link PrognosisDto} existing A-Plans for the period.
     * @return <code>true</code> if no new A-Plan had to be sent (move to validate permitted).
     */
    private boolean createMissingNewAPlansForAllConnectionGroups(LocalDate period, List<PrognosisDto> aPlans) {
        List<ConnectionGroup> activeConnectionGroups = corePlanboardBusinessService
                .findActiveConnectionGroupsWithConnections(period).keySet().stream().collect(Collectors.toList());
        List<String> connectionGroupsWithAPlans = aPlans.stream()
                .map(PrognosisDto::getConnectionGroupEntityAddress)
                .collect(Collectors.toList());
        List<String> brpUsefIdentifiersWithoutAPlans = activeConnectionGroups.stream()
                .filter(connectionGroup -> connectionGroup instanceof BrpConnectionGroup).map(ConnectionGroup::getUsefIdentifier)
                .filter(usefIdentifier -> !connectionGroupsWithAPlans.contains(usefIdentifier)).collect(Collectors.toList());
        brpUsefIdentifiersWithoutAPlans.stream().forEach(
                usefIdentifier -> createAPlanEventManager.fire(new CreateAPlanEvent(period, usefIdentifier)));
        return brpUsefIdentifiersWithoutAPlans.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void handlePrognosesToBeReCreated(LocalDate period, WorkflowContext workflowContext,
            List<PrognosisDto> dPrognoses,
            List<PrognosisDto> aPlans) {
        List<Long> dPrognosisSequences = (List<Long>) workflowContext.getValue(ReCreatePrognosesWorkflowParameter.OUT.REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST.name());
        List<Long> aPlanSequences = (List<Long>) workflowContext.getValue(ReCreatePrognosesWorkflowParameter.OUT.REQUIRES_NEW_A_PLAN_SEQUENCES_LIST.name());
        handleAPlansToBeRecreated(aPlans, aPlanSequences);
        handleDPrognosesToBeRecreated(dPrognoses, dPrognosisSequences);

        // if some A-Plans need to be re-created
        if (aPlanSequences != null && !aPlanSequences.isEmpty()) {
            LOGGER.debug("A-Plans have to be re-created.");
            reCreateAPlanEventManager.fire(new ReCreateAPlanEvent(period));
        }

        // if there are D-prognosis, trigger the re-creation of D-Prognoses immediately.
        if (dPrognosisSequences != null && !dPrognosisSequences.isEmpty()) {
            LOGGER.debug("D-Prognoses have to be re-created. It will be triggered immediately.");
            reCreateDPrognosisEventManager.fire(new ReCreateDPrognosisEvent(period));
        }
    }

    private void handleAPlansToBeRecreated(List<PrognosisDto> aPlans, List<Long> aPlanSequences) {
        // if A-Plans have to be re-created.
        if (aPlanSequences == null || aPlanSequences.isEmpty()) {
            LOGGER.debug("A-Plans do not have to be re-created.");
            return;
        }
        // for each of the latest A-Plans, retain only the ones with a sequence number which is returned by the PBC, find the
        // associated PlanboardMessage and set it status to TO_BE_RECREATED.
        aPlans.stream().filter(aPlan -> aPlanSequences.contains(aPlan.getSequenceNumber()))
                .map(aPlan -> corePlanboardBusinessService
                        .findSinglePlanboardMessage(aPlan.getSequenceNumber(), DocumentType.A_PLAN, aPlan.getParticipantDomain()))
                .filter(aPlanMessage -> aPlanMessage != null)
                .forEach(aPlanMessage -> {
                    LOGGER.debug("Setting the A-Plan with sequence [{}] to status TO_BE_RECREATED.", aPlanMessage.getSequence());
                    aPlanMessage.setDocumentStatus(DocumentStatus.TO_BE_RECREATED);
                });
    }

    private void handleDPrognosesToBeRecreated(List<PrognosisDto> dPrognoses, List<Long> dPrognosisSequences) {
        // if D-Prognoses flag is false, do nothing and exit the process.
        if (dPrognosisSequences == null || dPrognosisSequences.isEmpty()) {
            return;
        }
        // for each of the latest D-Prognosis, retain only the ones with a sequence number which is returned by the PBC, find the
        // associated PlanboardMessage and set it status to TO_BE_RECREATED.
        dPrognoses.stream()
                .filter(dPrognosis -> dPrognosisSequences.contains(dPrognosis.getSequenceNumber()))
                .map(dPrognosis -> corePlanboardBusinessService.findSinglePlanboardMessage(dPrognosis.getSequenceNumber(),
                        DocumentType.D_PROGNOSIS, dPrognosis.getParticipantDomain()))
                .filter(prognosisMessage -> prognosisMessage != null)
                .forEach(dprognosisMessage -> {
                    LOGGER.debug("Setting the D-Prognosis with sequence [{}] to status TO_BE_RECREATED.",
                            dprognosisMessage.getSequence());
                    dprognosisMessage.setDocumentStatus(DocumentStatus.TO_BE_RECREATED);
                });
    }
}
