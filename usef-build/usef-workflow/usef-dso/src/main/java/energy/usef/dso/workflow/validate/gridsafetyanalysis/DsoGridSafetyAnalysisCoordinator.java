/*
 * Copyright 2015 USEF Foundation
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

package energy.usef.dso.workflow.validate.gridsafetyanalysis;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_CREATE_GRID_SAFETY_ANALYSIS;
import static energy.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter.IN;
import static energy.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter.OUT;

import java.util.ArrayList;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.model.PtuState;
import energy.usef.core.model.RegimeType;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.PrognosisTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;
import energy.usef.dso.model.GridSafetyAnalysis;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.coloring.ColoringProcessEvent;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.NonAggregatorForecastDto;
import energy.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import energy.usef.dso.workflow.transformer.NonAggregatorForecastDtoTransformer;
import energy.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestEvent;

/**
 * Grid Safety Analysis workflow coordinator.
 * <p>
 * This is not a singleton, because this process should be able to run per congestionPoint.
 */
@Stateless
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class DsoGridSafetyAnalysisCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoGridSafetyAnalysisCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStubLoader;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private Config config;

    @Inject
    private Event<CreateFlexRequestEvent> eventManager;

    @Inject
    private Event<ColoringProcessEvent> coloringEventManager;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * This method handles the GridSafetyAnalysisEvent. This Event creates GridSafetyAnalysis / CongestionPoint.
     *
     * @param event The {@link GridSafetyAnalysisEvent} that triggers the process.
     */
    @Asynchronous
    public void startGridSafetyAnalysis(@Observes(during = TransactionPhase.AFTER_COMPLETION) GridSafetyAnalysisEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        String congestionPointEntityAddress = event.getCongestionPointEntityAddress();
        LocalDate analysisDay = event.getAnalysisDay();

        if (isEventAllowed(event)) {
            // PrognosisDto List, one for each participant
            List<PtuPrognosis> lastDPrognoses = corePlanboardBusinessService
                    .findLastPrognoses(analysisDay, PrognosisType.D_PROGNOSIS, congestionPointEntityAddress);

            GridSafetyAnalysisDto gridSafetyAnalysisDto = invokeGridSafetyPBC(event, lastDPrognoses);

            saveAndProcessGridSafetyAnalysis(event, gridSafetyAnalysisDto, lastDPrognoses);
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private boolean isEventAllowed(GridSafetyAnalysisEvent event) {
        if (event.getAnalysisDay().compareTo(DateTimeUtil.getCurrentDate()) < 0) {
            LOGGER.warn("Grid safety analysis is not allowed for the date {}, the workflow is stopped.", event.getAnalysisDay());
            return false;
        }
        return true;
    }

    private GridSafetyAnalysisDto invokeGridSafetyPBC(GridSafetyAnalysisEvent event, List<PtuPrognosis> prognosisList) {
        WorkflowContext inContext = new DefaultWorkflowContext();

        inContext.setValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), event.getCongestionPointEntityAddress());
        // Getting non aggregator connection forecast
        NonAggregatorForecastDto nonAggregatorForecastDto = createNonAggregatorForecastInputMap(event);
        inContext.setValue(IN.NON_AGGREGATOR_FORECAST.name(), nonAggregatorForecastDto);
        inContext.setValue(IN.PERIOD.name(), event.getAnalysisDay());
        List<PrognosisDto> dPrognosisDtos = prognosisList.stream()
                .collect(Collectors.groupingBy(PtuPrognosis::getParticipantDomain)).values().stream()
                .map(PrognosisTransformer::mapToPrognosis)
                .collect(Collectors.toList());
        inContext.setValue(IN.D_PROGNOSIS_LIST.name(), dPrognosisDtos);

        // Stub invocation
        WorkflowContext outContext = workflowStubLoader.invoke(DSO_CREATE_GRID_SAFETY_ANALYSIS.name(), inContext);

        WorkflowUtil.validateContext(DSO_CREATE_GRID_SAFETY_ANALYSIS.name(), outContext, OUT.values());

        LOGGER.debug("Got results from DsoCreateGridSafetyAnalysis component");
        return outContext.get(OUT.GRID_SAFETY_ANALYSIS.name(), GridSafetyAnalysisDto.class);
    }

    private void saveAndProcessGridSafetyAnalysis(GridSafetyAnalysisEvent event, GridSafetyAnalysisDto gridSafetyAnalysisDto,
            List<PtuPrognosis> lastPrognosisList) {
        LOGGER.debug("Started saving GridSafetyAnalysis");

        boolean aggregatorsAvailable = 0 < dsoPlanboardBusinessService
                .countActiveAggregatorsForCongestionPointOnDay(event.getCongestionPointEntityAddress(), event.getAnalysisDay());
        LOGGER.debug("Aggregators available: {} ", aggregatorsAvailable);

        long sequence = sequenceGeneratorService.next();

        // re-group by ptu index
        Map<Integer, List<PtuPrognosis>> prognosisByPtuIndex = lastPrognosisList.stream()
                .collect(Collectors.groupingBy(p -> p.getPtuContainer().getPtuIndex()));

        List<PtuGridSafetyAnalysisDto> flexRequestList = new ArrayList<>();

        ConnectionGroup connectionGroup = corePlanboardBusinessService.findConnectionGroup(event.getCongestionPointEntityAddress());
        Map<Integer, PtuContainer> ptuContainers = dsoPlanboardBusinessService.findPTUContainersForPeriod(event.getAnalysisDay());

        Map<Integer, GridSafetyAnalysis> previousGridSafetyAnalysis = dsoPlanboardBusinessService.findGridSafetyAnalysis(
                event.getCongestionPointEntityAddress(), event.getAnalysisDay())
                .stream()
                .collect(Collectors.toMap(gsa -> gsa.getPtuContainer().getPtuIndex(), Function.identity()));
        int currentPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(),
                config.getIntegerProperty(ConfigParam.PTU_DURATION));
        LocalDate today = DateTimeUtil.getCurrentDate();
        boolean noPreviousGridSafetyAnalysis = previousGridSafetyAnalysis.isEmpty();
        boolean futurePeriod = event.getAnalysisDay().isAfter(today);

        int startPtu;
        if (noPreviousGridSafetyAnalysis || futurePeriod) {
            startPtu = 1;
        } else {
            startPtu = currentPtuIndex;
        }

        for (int ptuIndex = startPtu; ptuIndex <= gridSafetyAnalysisDto.getPtus().size(); ptuIndex++) {
            PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto = gridSafetyAnalysisDto.getPtus().get(ptuIndex - 1);
            GridSafetyAnalysis gridSafetyAnalysis;
            if (noPreviousGridSafetyAnalysis) {
                gridSafetyAnalysis = createGridSafetyAnalysis(sequence, prognosisByPtuIndex, connectionGroup, ptuContainers,
                        ptuGridSafetyAnalysisDto);

                dsoPlanboardBusinessService.storeGridSafetyAnalysis(gridSafetyAnalysis);
            } else {
                gridSafetyAnalysis = previousGridSafetyAnalysis.get(ptuGridSafetyAnalysisDto.getPtuIndex());
                if (DispositionTypeDto.AVAILABLE.equals(ptuGridSafetyAnalysisDto.getDisposition())) {
                    gridSafetyAnalysis.setDisposition(DispositionAvailableRequested.AVAILABLE);
                } else {
                    gridSafetyAnalysis.setDisposition(DispositionAvailableRequested.REQUESTED);
                }
                gridSafetyAnalysis.setPower(ptuGridSafetyAnalysisDto.getPower());
                gridSafetyAnalysis.setPrognoses(prognosisByPtuIndex.get(ptuGridSafetyAnalysisDto.getPtuIndex()));
            }

            PtuState ptuState = corePlanboardBusinessService.findOrCreatePtuState(gridSafetyAnalysis.getPtuContainer(),
                    gridSafetyAnalysis.getConnectionGroup());

            // Changing PTU regime and preparing gridSafetyAnalysis items for further processing
            if (DispositionTypeDto.REQUESTED.equals(ptuGridSafetyAnalysisDto.getDisposition()) && aggregatorsAvailable) {
                ptuState.setRegime(RegimeType.YELLOW);
                flexRequestList.add(ptuGridSafetyAnalysisDto);
            }
        }

        // fire next steps
        if (flexRequestList.isEmpty()) {
            // no flex requests possible, start the coloring process
            startColoringProcess(event);
        } else {
            // there are flex requests possible, send them
            sendFlexRequests(event, flexRequestList);
        }
        LOGGER.debug("Ended saving GridSafetyAnalysis");
    }

    private GridSafetyAnalysis createGridSafetyAnalysis(long sequence, Map<Integer, List<PtuPrognosis>> prognosisByPtuIndex,
            ConnectionGroup connectionGroup, Map<Integer, PtuContainer> ptuContainers,
            PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto) {
        GridSafetyAnalysis gridSafetyAnalysis = new GridSafetyAnalysis();
        gridSafetyAnalysis.setConnectionGroup(connectionGroup);
        gridSafetyAnalysis.setSequence(sequence);
        gridSafetyAnalysis.setPtuContainer(ptuContainers.get(ptuGridSafetyAnalysisDto.getPtuIndex()));
        gridSafetyAnalysis.setPrognoses(prognosisByPtuIndex.get(ptuGridSafetyAnalysisDto.getPtuIndex()));
        gridSafetyAnalysis.setPower(ptuGridSafetyAnalysisDto.getPower());

        if (DispositionTypeDto.AVAILABLE.equals(ptuGridSafetyAnalysisDto.getDisposition())) {
            gridSafetyAnalysis.setDisposition(DispositionAvailableRequested.AVAILABLE);
        } else {
            gridSafetyAnalysis.setDisposition(DispositionAvailableRequested.REQUESTED);
        }
        return gridSafetyAnalysis;
    }

    /**
     * Start the coloring process to determine if PTU(s) become orange.
     */
    private void startColoringProcess(GridSafetyAnalysisEvent event) {
        coloringEventManager.fire(new ColoringProcessEvent(event.getAnalysisDay(), event.getCongestionPointEntityAddress()));
    }

    /*
     * Call the workflow to send flex requests.
     */
    private void sendFlexRequests(GridSafetyAnalysisEvent event, List<PtuGridSafetyAnalysisDto> flexRequestList) {
        if (flexRequestList.isEmpty()) {
            // no reason to send flex requests.
            return;
        }

        Integer[] ptuIndexes = flexRequestList.stream()
                .map(PtuGridSafetyAnalysisDto::getPtuIndex)
                .toArray(Integer[]::new);

        LOGGER.debug("Sending a flexibility request for Entity Adress: {}, PTU Date: {}, PTU Index array length: {}",
                event.getCongestionPointEntityAddress(), event.getAnalysisDay(), ptuIndexes.length);
        eventManager.fire(new CreateFlexRequestEvent(event.getCongestionPointEntityAddress(), event.getAnalysisDay(), ptuIndexes));
    }

    private NonAggregatorForecastDto createNonAggregatorForecastInputMap(GridSafetyAnalysisEvent event) {
        NonAggregatorForecastDto nonAggregatorForecastDto = NonAggregatorForecastDtoTransformer.transform(
                dsoPlanboardBusinessService.findLastNonAggregatorForecasts(event.getAnalysisDay(), Optional.of(event.getCongestionPointEntityAddress())).stream()
                        .collect(Collectors.toList()));

        if (nonAggregatorForecastDto == null) {
            nonAggregatorForecastDto = new NonAggregatorForecastDto();
        }

        LOGGER.debug("Got [{}] Non Aggregator Connection Forecast", nonAggregatorForecastDto.getPtus().size());
        return nonAggregatorForecastDto;
    }
}
