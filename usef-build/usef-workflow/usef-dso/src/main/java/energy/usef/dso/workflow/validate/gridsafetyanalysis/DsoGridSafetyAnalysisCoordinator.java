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

package energy.usef.dso.workflow.validate.gridsafetyanalysis;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_CREATE_GRID_SAFETY_ANALYSIS;
import static energy.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter.IN;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
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
import energy.usef.core.util.ConcurrentUtil;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.PrognosisTransformer;
import energy.usef.core.workflow.util.WorkflowUtil;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;
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
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private Config config;

    @Inject
    private ConfigDso configDso;

    @Inject
    private ConcurrentUtil concurrentUtil;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    @Inject
    private Event<StoreGridSafetyAnalysisEvent> storeGridSafetyEventManager;
    @Inject
    private Event<CreateFlexRequestEvent> flexRequestEventManager;

    @Inject
    private Event<ColoringProcessEvent> coloringEventManager;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * This method handles the GridSafetyAnalysisEvent. This Event creates GridSafetyAnalysis / CongestionPoint.
     *
     * @param event The {@link GridSafetyAnalysisEvent} that triggers the process.
     */

    @Asynchronous
    public void startGridSafetyAnalysis(@Observes(during = TransactionPhase.AFTER_COMPLETION) GridSafetyAnalysisEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);

        String entityAddress = event.getCongestionPointEntityAddress();
        LocalDate period = event.getPeriod();

        // PrognosisDto List, one for each participant
        List<PtuPrognosis> prognosisList = findLastPrognoses(period, entityAddress);
        WorkflowContext inContext = prepareInContext(event, prognosisList);
        Long timeout = configDso.getLongProperty(ConfigDsoParam.DSO_GRID_SAFETY_ANALYSIS_EXPIRATION_IN_MINUTES);

        CompletableFuture<WorkflowContext> completableFuture = CompletableFuture
                .supplyAsync(() -> callPluggableBusinessComponent(inContext));

        if (timeout != null && timeout > 0) {
            completableFuture = concurrentUtil.within(completableFuture, Duration.ofMinutes(timeout),
                    String.format("Grid Safety Analysis for %s on %s timed out after ",
                            entityAddress, period.toString("yyyy-MM-dd")));
        }

        completableFuture.whenCompleteAsync((result, throwable) -> {
            LOGGER.info("Processing Grid Gafety Analysis for {}", result.getValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name()));
            WorkflowUtil
                    .validateContext(DSO_CREATE_GRID_SAFETY_ANALYSIS.name(), result, CreateGridSafetyAnalysisStepParameter.OUT
                            .values());
            GridSafetyAnalysisDto dto = result
                    .get(CreateGridSafetyAnalysisStepParameter.OUT.GRID_SAFETY_ANALYSIS.name(), GridSafetyAnalysisDto.class);

            storeGridSafetyEventManager.fire(new StoreGridSafetyAnalysisEvent(entityAddress, period, dto));
        }).exceptionally(throwable -> {
            LOGGER.error(throwable.getMessage());
            return null;
        });

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    public WorkflowContext callPluggableBusinessComponent(WorkflowContext inContext) {
        LOGGER.info("Invoke Pluggable Business Component {}", DSO_CREATE_GRID_SAFETY_ANALYSIS.name());
        return workflowStepExecuter.invoke(DSO_CREATE_GRID_SAFETY_ANALYSIS.name(), inContext);
    }

    /**
     * This method process the StoreGridSafetyAnalysisEvent. Now that gridsafety PBC is async,
     * this process is completely seperate from the initiation of the GSA.
     *
     * @param event
     */
    @Asynchronous
    public void saveAndProcessGridSafetyAnalysis(@Observes StoreGridSafetyAnalysisEvent event) throws BusinessValidationException {
        eventValidationService.validateEventPeriodTodayOrInFuture(event);
        LocalDate period = event.getPeriod();
        String entityAddress = event.getCongestionPointEntityAddress();
        List<PtuPrognosis> lastPrognosisList = findLastPrognoses(period, entityAddress);

        LOGGER.debug("Storing Grid Safety Analysis for {} on {}", entityAddress, period);

        boolean aggregatorsAvailable = 0 < dsoPlanboardBusinessService
                .countActiveAggregatorsForCongestionPointOnDay(entityAddress, period);
        LOGGER.debug("Aggregators available: {} ", aggregatorsAvailable);

        long sequence = sequenceGeneratorService.next();

        // re-group by ptu index
        Map<Integer, List<PtuPrognosis>> prognosisByPtuIndex = lastPrognosisList.stream()
                .collect(Collectors.groupingBy(p -> p.getPtuContainer().getPtuIndex()));

        List<PtuGridSafetyAnalysisDto> flexRequestList = new ArrayList<>();

        ConnectionGroup connectionGroup = corePlanboardBusinessService.findConnectionGroup(entityAddress);
        Map<Integer, PtuContainer> ptuContainers = dsoPlanboardBusinessService.findPTUContainersForPeriod(period);

        int numberOfRowsDeleted = dsoPlanboardBusinessService.deletePreviousGridSafetyAnalysis(entityAddress, period);
        LOGGER.debug("Number of previous GSA records deleted {}",numberOfRowsDeleted);

        int currentPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(),
                config.getIntegerProperty(ConfigParam.PTU_DURATION));
        LocalDate today = DateTimeUtil.getCurrentDate();
        boolean futurePeriod = period.isAfter(today);

        int startPtu;
        if (futurePeriod) {
            startPtu = 1;
        } else {
            startPtu = currentPtuIndex;
        }

        GridSafetyAnalysisDto gridSafetyAnalysisDto = event.getGridSafetyAnalysisDto();
        for (int ptuIndex = startPtu; ptuIndex <= gridSafetyAnalysisDto.getPtus().size(); ptuIndex++) {
            PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto = gridSafetyAnalysisDto.getPtus().get(ptuIndex - 1);
            GridSafetyAnalysis gridSafetyAnalysis;
            gridSafetyAnalysis = createGridSafetyAnalysis(sequence, prognosisByPtuIndex, connectionGroup, ptuContainers,
                    ptuGridSafetyAnalysisDto);

            dsoPlanboardBusinessService.storeGridSafetyAnalysis(gridSafetyAnalysis);

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

    private List<PtuPrognosis> findLastPrognoses(LocalDate period, String usefIdentifier) {
        return corePlanboardBusinessService.findLastPrognoses(period, PrognosisType.D_PROGNOSIS, usefIdentifier);
    }

    private WorkflowContext prepareInContext(GridSafetyAnalysisEvent event, List<PtuPrognosis> prognosisList) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        inContext.setValue(IN.PTU_DURATION.name(), ptuDuration);
        inContext.setValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), event.getCongestionPointEntityAddress());
        // Getting non aggregator connection forecast
        NonAggregatorForecastDto nonAggregatorForecastDto = createNonAggregatorForecastInputMap(event);
        inContext.setValue(IN.NON_AGGREGATOR_FORECAST.name(), nonAggregatorForecastDto);
        inContext.setValue(IN.PERIOD.name(), event.getPeriod());
        List<PrognosisDto> dPrognosisDtos = prognosisList.stream()
                .collect(Collectors.groupingBy(PtuPrognosis::getParticipantDomain)).values().stream()
                .map(PrognosisTransformer::mapToPrognosis)
                .collect(Collectors.toList());
        inContext.setValue(IN.D_PROGNOSIS_LIST.name(), dPrognosisDtos);

        return inContext;
    }

    private NonAggregatorForecastDto createNonAggregatorForecastInputMap(GridSafetyAnalysisEvent event) {
        NonAggregatorForecastDto nonAggregatorForecastDto = NonAggregatorForecastDtoTransformer.transform(
                dsoPlanboardBusinessService.findLastNonAggregatorForecasts(event.getPeriod(),
                        Optional.of(event.getCongestionPointEntityAddress())).stream()
                        .collect(Collectors.toList()));

        if (nonAggregatorForecastDto == null) {
            nonAggregatorForecastDto = new NonAggregatorForecastDto();
        }

        LOGGER.debug("Got [{}] Non Aggregator Connection Forecast", nonAggregatorForecastDto.getPtus().size());
        return nonAggregatorForecastDto;
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
     *
     * @param event
     */
    private void startColoringProcess(StoreGridSafetyAnalysisEvent event) {
        coloringEventManager.fire(new ColoringProcessEvent(event.getPeriod(), event.getCongestionPointEntityAddress()));
    }

    /*
     * Call the workflow to send flex requests.
     */
    private void sendFlexRequests(StoreGridSafetyAnalysisEvent event, List<PtuGridSafetyAnalysisDto> flexRequestList) {
        if (flexRequestList.isEmpty()) {
            // no reason to send flex requests.
            return;
        }

        Integer[] ptuIndexes = flexRequestList.stream()
                .map(PtuGridSafetyAnalysisDto::getPtuIndex)
                .toArray(Integer[]::new);

        LOGGER.debug("Sending a flexibility request for Entity Adress: {}, PTU Date: {}, PTU Index array length: {}",
                event.getCongestionPointEntityAddress(), event.getPeriod(), ptuIndexes.length);
        flexRequestEventManager
                .fire(new CreateFlexRequestEvent(event.getCongestionPointEntityAddress(), event.getPeriod(), ptuIndexes));
    }
}
