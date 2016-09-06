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

package energy.usef.dso.workflow.coloring;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.dto.PtuContainerDto;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.PtuState;
import energy.usef.core.model.RegimeType;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.PtuContainerTransformer;
import energy.usef.dso.model.GridSafetyAnalysis;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.operate.PrepareStepwiseLimitingStepParameter;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The "coloring process" workflow to determine if PTU(s) become orange.
 */
@Stateless
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class DsoColoringProcessCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoColoringProcessCoordinator.class);

    @Inject
    private Config config;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * This method handles the ColoringProcessEvent. This event determines if PTU(s) become orange.
     *
     * @param event
     */
    @Asynchronous
    public void handleEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) ColoringProcessEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodInFuture(event);

        // fetch the connection group for the congestion point and stop the process if no connection group found.
        ConnectionGroup connectionGroup = corePlanboardBusinessService.findConnectionGroup(event.getCongestionPoint());
        if (connectionGroup == null) {
            LOGGER.error("ConnectionGroup for congestion point {} not found! Unable to continue", event.getCongestionPoint());
            LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
            return;
        }

        // fetch all data for grid safety analysis using event period and congestion point.
        List<GridSafetyAnalysis> gridSafetyAnalysisList = dsoPlanboardBusinessService
                .findLatestGridSafetyAnalysisWithDispositionRequested(
                event.getCongestionPoint(), event.getPeriod());
        if (gridSafetyAnalysisList.isEmpty()) {
            LOGGER.info("No grid safety analysis found for date {} and congestion point {}. No coloring needed.", event.getPeriod(),
                    event.getCongestionPoint());
            LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
            return;
        }

        List<PtuContainerDto> orangePtuContainerDtoList = new ArrayList<>();

        // set regime to orange for all ptu's with disposition requested
        gridSafetyAnalysisList.forEach(gridSafetyAnalysis -> {
            if (gridSafetyAnalysis.getDisposition() == DispositionAvailableRequested.REQUESTED) {
                PtuState ptuState = corePlanboardBusinessService
                        .findOrCreatePtuState(gridSafetyAnalysis.getPtuContainer(), connectionGroup);
                ptuState.setRegime(RegimeType.ORANGE);
                orangePtuContainerDtoList.add(PtuContainerTransformer.transform(gridSafetyAnalysis.getPtuContainer()));
            }
        });

        // invoke DSO Prepare Stepwise Connection Limiting & Recovery (ALUS-240).
        sendConnectionLimitRecoveryRequests(event.getCongestionPoint(), orangePtuContainerDtoList);

        // Invoke the post process (PBC) of the coloring process
        invokePostColoringProcessPBC(event.getPeriod(), orangePtuContainerDtoList);

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    // Trigger the prepare connection limiting workflow
    private void sendConnectionLimitRecoveryRequests(String congestionPoint,
            List<PtuContainerDto> orangePtuContainerDtoList) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        inContext.setValue(PrepareStepwiseLimitingStepParameter.IN.PTU_DURATION.name(), ptuDuration);
        inContext.setValue(PrepareStepwiseLimitingStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), congestionPoint);
        inContext.setValue(PrepareStepwiseLimitingStepParameter.IN.PTU_CONTAINERS.name(), orangePtuContainerDtoList);

        workflowStepExecuter.invoke(DsoWorkflowStep.DSO_PREPARE_STEPWISE_LIMITING.name(), inContext);
    }

    // Trigger the post coloring process workflow
    private void invokePostColoringProcessPBC(LocalDate date, List<PtuContainerDto> orangePtuContainerDtoList) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        inContext.setValue(PostColoringProcessParameter.IN.PTU_DURATION.name(), ptuDuration);
        inContext.setValue(PostColoringProcessParameter.IN.PERIOD.name(), date);
        inContext.setValue(PostColoringProcessParameter.IN.PTU_CONTAINER_DTO_LIST.name(), orangePtuContainerDtoList);

        workflowStepExecuter.invoke(DsoWorkflowStep.DSO_POST_COLORING_PROCESS.name(), inContext);
    }

}
