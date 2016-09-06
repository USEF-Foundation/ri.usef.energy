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

package energy.usef.agr.workflow.operate.deviation;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioEvent;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.PrognosisTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class responsible for the 'AGR Detect deviations from Prognoses' workflow.
 */
@Stateless
public class AgrDetectDeviationCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrDetectDeviationCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private Config config;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private Event<ReOptimizePortfolioEvent> eventManager;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * Handles the DetectDeviationEvent.
     *
     * @param event - A {@link DetectDeviationEvent}
     */
    @SuppressWarnings("unchecked")
    @Asynchronous
    public void handleEvent(@Observes DetectDeviationEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);

        LocalDate period = event.getPeriod();

        List<ConnectionPortfolioDto> connectionPortfolioDTOs = agrPortfolioBusinessService.findConnectionPortfolioDto(period);
        Map<String, List<String>> connectionGroupsToConnectionsMap = corePlanboardBusinessService
                .buildConnectionGroupsToConnectionsMap(period);

        boolean deviationDetected = detectDeviation(event, connectionGroupsToConnectionsMap, connectionPortfolioDTOs);

        if (deviationDetected) {
            LOGGER.warn("Deviation detected");
            sendReOptimizationEventForPlanPhase(period);
        } else {
            LOGGER.debug("No deviation detected");
        }

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private boolean detectDeviation(DetectDeviationEvent event, Map<String, List<String>> connectionGroupsToConnectionsMap,
            List<ConnectionPortfolioDto> connectionPortfolioDtos) {
        boolean deviationDetected = false;

        LocalDate period = event.getPeriod();

        Map<String, PrognosisDto> prognosisMap = findRelatedPrognoses(connectionGroupsToConnectionsMap.keySet(), period);

        Iterator<String> iterator = connectionGroupsToConnectionsMap.keySet().iterator();
        while (iterator.hasNext() && !deviationDetected) {
            String usefIdentifier = iterator.next();
            if (!prognosisMap.containsKey(usefIdentifier)) {
                continue;
            }

            List<String> relatedConnections = connectionGroupsToConnectionsMap.get(usefIdentifier);

            List<ConnectionPortfolioDto> filteredConnections = connectionPortfolioDtos.stream()
                    .filter(dto -> relatedConnections.contains(dto.getConnectionEntityAddress()))
                    .collect(Collectors.toList());

            deviationDetected = invokeDetectDeviationPbc(period, usefIdentifier, filteredConnections,
                    prognosisMap.get(usefIdentifier));
        }
        return deviationDetected;
    }

    private Map<String, PrognosisDto> findRelatedPrognoses(Set<String> usefIdentifiers,
            LocalDate period) {
        return usefIdentifiers.stream()
                .map(usefIdentifier -> corePlanboardBusinessService.findLastPrognoses(period, usefIdentifier))
                .filter(prognoses -> !prognoses.isEmpty()).map(PrognosisTransformer::mapToPrognosis)
                .collect(Collectors.toMap(PrognosisDto::getConnectionGroupEntityAddress, Function.identity()));
    }

    private boolean invokeDetectDeviationPbc(LocalDate period, String usefIdentifier,
            List<ConnectionPortfolioDto> filteredConnections, PrognosisDto prognosisDto) {

        if (configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            return invokeNonUdiDetectDeviationPbc(period, usefIdentifier, filteredConnections, prognosisDto);
        } else {
            return invokeDetectUdiDeviationPbc(period, usefIdentifier, filteredConnections, prognosisDto);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean invokeDetectUdiDeviationPbc(LocalDate period, String usefIdentifier,
            List<ConnectionPortfolioDto> filteredConnections, PrognosisDto prognoses) {
        WorkflowContext contextInput = buildUdiWorkflowContext(period, usefIdentifier, filteredConnections, prognoses);

        WorkflowContext contextOutput = workflowStepExecuter.invoke(AgrWorkflowStep.AGR_DETECT_DEVIATION_FROM_PROGNOSES.name(), contextInput);

        // Validate output
        WorkflowUtil.validateContext(AgrWorkflowStep.AGR_DETECT_DEVIATION_FROM_PROGNOSES.name(), contextOutput,
                DetectDeviationFromPrognosisStepParameter.OUT.values());

        List<Integer> deviationIndexes = contextOutput
                .get(DetectDeviationFromPrognosisStepParameter.OUT.DEVIATION_INDEX_LIST.name(), List.class);

        return !deviationIndexes.isEmpty();
    }

    private WorkflowContext buildUdiWorkflowContext(LocalDate period, String usefIdentifier,
            List<ConnectionPortfolioDto> filteredConnections, PrognosisDto prognoses) {
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);
        int currentPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(), ptuDuration);

        WorkflowContext workflowContext = new DefaultWorkflowContext();

        workflowContext.setValue(DetectDeviationFromPrognosisStepParameter.IN.PTU_DURATION.name(), ptuDuration);
        workflowContext.setValue(DetectDeviationFromPrognosisStepParameter.IN.PERIOD.name(), period);
        workflowContext.setValue(DetectDeviationFromPrognosisStepParameter.IN.CURRENT_PTU_INDEX.name(), currentPtuIndex);

        workflowContext.setValue(DetectDeviationFromPrognosisStepParameter.IN.USEF_IDENTIFIER.name(), usefIdentifier);
        workflowContext
                .setValue(DetectDeviationFromPrognosisStepParameter.IN.CONNECTION_PORTFOLIO_DTO.name(), filteredConnections);
        workflowContext.setValue(DetectDeviationFromPrognosisStepParameter.IN.LATEST_PROGNOSIS.name(), prognoses);

        return workflowContext;
    }

    @SuppressWarnings("unchecked")
    private boolean invokeNonUdiDetectDeviationPbc(LocalDate period, String usefIdentifier,
            List<ConnectionPortfolioDto> filteredConnections, PrognosisDto prognoses) {
        WorkflowContext contextInput = buildNonUdiWorkflowContext(period, usefIdentifier, filteredConnections, prognoses);

        // Detect deviations
        WorkflowContext contextOutput = workflowStepExecuter
                .invoke(AgrWorkflowStep.AGR_NON_UDI_DETECT_DEVIATION_FROM_PROGNOSES.name(), contextInput);

        // Validate output
        WorkflowUtil.validateContext(AgrWorkflowStep.AGR_NON_UDI_DETECT_DEVIATION_FROM_PROGNOSES.name(), contextOutput,
                NonUdiDetectDeviationFromPrognosisStepParameter.OUT.values());

        List<Integer> deviationIndexes = contextOutput
                .get(NonUdiDetectDeviationFromPrognosisStepParameter.OUT.DEVIATION_INDEX_LIST.name(), List.class);

        return !deviationIndexes.isEmpty();
    }

    private WorkflowContext buildNonUdiWorkflowContext(LocalDate period, String usefIdentifier,
            List<ConnectionPortfolioDto> filteredConnections, PrognosisDto prognoses) {
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);
        int currentPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(), ptuDuration);

        WorkflowContext workflowContext = new DefaultWorkflowContext();

        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.PTU_DURATION.name(), ptuDuration);
        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.PERIOD.name(), period);
        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.CURRENT_PTU_INDEX.name(), currentPtuIndex);

        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.USEF_IDENTIFIER.name(), usefIdentifier);
        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.CONNECTION_PORTFOLIO_DTO.name(),
                filteredConnections);
        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.LATEST_PROGNOSIS.name(), prognoses);

        return workflowContext;
    }

    private void sendReOptimizationEventForPlanPhase(LocalDate period) {
        LOGGER.debug("Triggering re-optimization portfolio workflow.");
        eventManager.fire(new ReOptimizePortfolioEvent(period));
    }

}
