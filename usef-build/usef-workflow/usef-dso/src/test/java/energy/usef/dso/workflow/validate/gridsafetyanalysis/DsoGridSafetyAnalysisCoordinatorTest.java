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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.event.Event;

import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.model.PtuState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;
import energy.usef.dso.model.GridSafetyAnalysis;
import energy.usef.dso.model.NonAggregatorForecast;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.util.ReflectionUtil;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.coloring.ColoringProcessEvent;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import energy.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestEvent;

/**
 * Test class in charge of the unit tests related to the {@link DsoGridSafetyAnalysisCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class DsoGridSafetyAnalysisCoordinatorTest {
    private static final String TEST_DOMAIN = "abc.com";
    private static final String ENTITY_ADDRESS = "ean.12340001";
    private static final int PTUS_PER_DAY = 96;

    private DsoGridSafetyAnalysisCoordinator dsoGridSafetyAnalysisCoordinator;

    @Mock
    private Logger LOGGER;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Mock
    private Event<StoreGridSafetyAnalysisEvent> storeGridSafetyEventManager;

    @Mock
    private Event<CreateFlexRequestEvent> flexRequestEventManager;

    @Mock
    private Event<ColoringProcessEvent> coloringEventManager;

    @Mock
    private PtuState ptuStateMock;

    @Mock
    private Config config;

    @Mock
    private ConfigDso configDso;

    @Mock
    private EventValidationService eventValidationService;

    @Before
    public void init() throws Exception {
        dsoGridSafetyAnalysisCoordinator = new DsoGridSafetyAnalysisCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        ReflectionUtil.setFinalStatic(DsoGridSafetyAnalysisCoordinator.class.getDeclaredField("LOGGER"), LOGGER);
        Whitebox.setInternalState(dsoGridSafetyAnalysisCoordinator, workflowStepExecuter);
        Whitebox.setInternalState(dsoGridSafetyAnalysisCoordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(dsoGridSafetyAnalysisCoordinator, dsoPlanboardBusinessService);
        Whitebox.setInternalState(dsoGridSafetyAnalysisCoordinator, config);
        Whitebox.setInternalState(dsoGridSafetyAnalysisCoordinator, configDso);
        Whitebox.setInternalState(dsoGridSafetyAnalysisCoordinator, sequenceGeneratorService);
        Whitebox.setInternalState(dsoGridSafetyAnalysisCoordinator, eventValidationService);
        Whitebox.setInternalState(dsoGridSafetyAnalysisCoordinator, "storeGridSafetyEventManager", storeGridSafetyEventManager);
        Whitebox.setInternalState(dsoGridSafetyAnalysisCoordinator, "flexRequestEventManager", flexRequestEventManager);
        Whitebox.setInternalState(dsoGridSafetyAnalysisCoordinator, "coloringEventManager", coloringEventManager);

        Mockito.when(config.getProperty(ConfigParam.DAY_AHEAD_GATE_CLOSURE_TIME)).thenReturn("17:00");
        Mockito.when(config.getIntegerProperty(ConfigParam.DAY_AHEAD_GATE_CLOSURE_PTUS)).thenReturn(3);
        Mockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        Mockito.when(configDso.getLongProperty(ConfigDsoParam.DSO_GRID_SAFETY_ANALYSIS_EXPIRATION_IN_MINUTES)).thenReturn(0L);
        Mockito.when(dsoPlanboardBusinessService.findPTUContainersForPeriod(Matchers.any(LocalDate.class)))
                .then(call -> IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
                    PtuContainer ptu = new PtuContainer();
                    ptu.setPtuDate((LocalDate) call.getArguments()[0]);
                    ptu.setPtuIndex(index);
                    return ptu;
                }).collect(Collectors.toMap(PtuContainer::getPtuIndex, Function.identity())));
        Mockito.when(dsoPlanboardBusinessService.findGridSafetyAnalysis(Matchers.any(String.class), Matchers.any(LocalDate.class)))
                .then(call -> buildLatestSafetyAnalysis((String) call.getArguments()[0], (LocalDate) call.getArguments()[1]));

        Mockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Mockito.anyLong(), Mockito.any(), Mockito.anyString()))
                .thenReturn(new PlanboardMessage());

    }

    /**
     * Tests DsoGridSafetyAnalysisCoordinator.invoke method.
     */
    @Test
    public void invokeWorkflowWithWrongPtuDate() throws BusinessValidationException {
        LocalDate ptuDate = new LocalDate().minusDays(2);
        dsoGridSafetyAnalysisCoordinator.startGridSafetyAnalysis(new GridSafetyAnalysisEvent(ENTITY_ADDRESS, ptuDate));
        Mockito.verify(eventValidationService, Mockito.times(1));
    }

    /**
     * Tests DsoGridSafetyAnalysisCoordinator.invoke method.
     */
    @Test
    public void invokeWorkflowWith() throws BusinessValidationException {
        LocalDate ptuDate = new LocalDate().plusDays(2);

        ArgumentCaptor<WorkflowContext> contextCapturer = ArgumentCaptor.forClass(WorkflowContext.class);

        Mockito.when(corePlanboardBusinessService.findActiveCongestionPointAddresses(Matchers.any(LocalDate.class))).thenReturn(
                Collections.singletonList(TEST_DOMAIN));
        Mockito.when(
                dsoPlanboardBusinessService.countActiveAggregatorsForCongestionPointOnDay(
                        Matchers.any(String.class),
                        Matchers.any(LocalDate.class))).thenReturn(2L);

        Mockito.when(dsoPlanboardBusinessService.findLastNonAggregatorForecasts(Matchers.any(LocalDate.class), Matchers.eq(Optional.of(TEST_DOMAIN)))).thenReturn(
                createNonAggregatorForecasts(ptuDate));
        Mockito.when(corePlanboardBusinessService
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.D_PROGNOSIS),
                        Matchers.eq(ENTITY_ADDRESS)))
                .thenReturn(createDPrognoses(ptuDate));
        Mockito.when(workflowStepExecuter
                .invoke(Mockito.eq(DsoWorkflowStep.DSO_CREATE_GRID_SAFETY_ANALYSIS.name()), Matchers.any(WorkflowContext.class)))
                .thenReturn(
                        prepareResultTestContext(new DefaultWorkflowContext(), ptuDate));

        Mockito.when(corePlanboardBusinessService
                .findSinglePlanboardMessage(Matchers.anyLong(), Matchers.eq(DocumentType.D_PROGNOSIS), Matchers.anyString()))
                .thenReturn(new PlanboardMessage(DocumentType.D_PROGNOSIS, 1L, DocumentStatus.ACCEPTED, "agr.usef-example.com",
                        new LocalDate(), null, null, null));

        Mockito.when(corePlanboardBusinessService.findOrCreatePtuState(Mockito.any(PtuContainer.class), Mockito.any(ConnectionGroup.class)))
                .thenReturn(ptuStateMock);

        dsoGridSafetyAnalysisCoordinator.startGridSafetyAnalysis(new GridSafetyAnalysisEvent(ENTITY_ADDRESS, ptuDate));

        Mockito.verify(workflowStepExecuter, Mockito.timeout(1000).times(1)).invoke(Mockito.eq(DsoWorkflowStep.DSO_CREATE_GRID_SAFETY_ANALYSIS.name()),
                contextCapturer.capture());

        Assert.assertNotNull(contextCapturer.getValue().getValue(
                CreateGridSafetyAnalysisStepParameter.IN.D_PROGNOSIS_LIST.name()));
        Assert.assertNotNull(contextCapturer.getValue().getValue(
                CreateGridSafetyAnalysisStepParameter.IN.NON_AGGREGATOR_FORECAST.name()));
    }

    /**
     * Test case to test the workflow without aggregators so the coloring process is invoked
     */
    @Test
    public void testWithNoAggregators() throws BusinessValidationException {
        LocalDate ptuDate = new LocalDate().plusDays(2);
        ArgumentCaptor<WorkflowContext> inputContextCaptor = ArgumentCaptor.forClass(WorkflowContext.class);

        Mockito.when(corePlanboardBusinessService.findOrCreatePtuState(Mockito.any(PtuContainer.class), Mockito.any(ConnectionGroup.class)))
                .thenReturn(ptuStateMock);

        dsoGridSafetyAnalysisCoordinator.startGridSafetyAnalysis(new GridSafetyAnalysisEvent(ENTITY_ADDRESS, ptuDate));

        Mockito.verify(workflowStepExecuter, Mockito.timeout(1000).times(1)).invoke(Mockito.eq(DsoWorkflowStep.DSO_CREATE_GRID_SAFETY_ANALYSIS.name()),
                inputContextCaptor.capture());

        WorkflowContext inputContext = inputContextCaptor.getValue();
        Assert.assertNotNull(inputContext.get(CreateGridSafetyAnalysisStepParameter.IN.PERIOD.name(), LocalDate.class));
        Assert.assertNotNull(inputContext.get(CreateGridSafetyAnalysisStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), LocalDate.class));
        Assert.assertNotNull(inputContext.get(CreateGridSafetyAnalysisStepParameter.IN.D_PROGNOSIS_LIST.name(), LocalDate.class));
        Assert.assertNotNull(inputContext.get(CreateGridSafetyAnalysisStepParameter.IN.NON_AGGREGATOR_FORECAST.name(), LocalDate.class));
    }

    @Test
    public void testWithNoPreviousGridSafetyAnalysis() throws BusinessValidationException {
        LocalDate ptuDate = new LocalDate().plusDays(2);
        Mockito.when(dsoPlanboardBusinessService.findGridSafetyAnalysis(Matchers.any(String.class), Matchers.any(LocalDate.class)))
                .thenReturn(new ArrayList<>());
        Mockito.when(
                workflowStepExecuter
                        .invoke(Mockito.eq(DsoWorkflowStep.DSO_CREATE_GRID_SAFETY_ANALYSIS.name()), Matchers.any(WorkflowContext.class)))
                .thenReturn(prepareResultTestContext(new DefaultWorkflowContext(), ptuDate));

        GridSafetyAnalysisDto dto = new GridSafetyAnalysisDto();
        dto.setPtus(IntStream.rangeClosed(1,96).mapToObj(i -> {
            PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto = new PtuGridSafetyAnalysisDto();
            ptuGridSafetyAnalysisDto.setPtuIndex(i);
            ptuGridSafetyAnalysisDto.setPower(i*100L);
            return ptuGridSafetyAnalysisDto;
        }).collect(Collectors.toList()));
        dsoGridSafetyAnalysisCoordinator.saveAndProcessGridSafetyAnalysis(new StoreGridSafetyAnalysisEvent(ENTITY_ADDRESS, ptuDate, dto));

        Mockito.verify(dsoPlanboardBusinessService, Mockito.times(96)).storeGridSafetyAnalysis(Matchers.any(GridSafetyAnalysis.class));
    }

    private WorkflowContext prepareResultTestContext(WorkflowContext context, LocalDate ptuDate) {

        GridSafetyAnalysisDto outputDto = new GridSafetyAnalysisDto();
        outputDto.setEntityAddress(ENTITY_ADDRESS);
        outputDto.setPtuDate(ptuDate);

        for (int i = 1; i <= PTUS_PER_DAY; i++) {
            PtuGridSafetyAnalysisDto ptuDto = new PtuGridSafetyAnalysisDto();
            ptuDto.setPower(10L);
            ptuDto.setPtuIndex(i);
            DispositionTypeDto disposition = (i % 2 == 0 ? DispositionTypeDto.AVAILABLE : DispositionTypeDto.REQUESTED);
            ptuDto.setDisposition(disposition);
            outputDto.getPtus().add(ptuDto);
        }

        context.setValue(CreateGridSafetyAnalysisStepParameter.OUT.GRID_SAFETY_ANALYSIS.name(), outputDto);

        return context;
    }

    private List<PtuPrognosis> createDPrognoses(LocalDate ptuDate) {
        List<PtuPrognosis> prognoses = new ArrayList<>();
        CongestionPointConnectionGroup congestionPoint = createCongestionPoint();

        for (int i = 1; i <= PTUS_PER_DAY; i++) {
            PtuPrognosis prognosis = new PtuPrognosis();
            prognosis.setId((long) i);
            prognosis.setParticipantDomain(TEST_DOMAIN);
            prognosis.setPower(BigInteger.TEN);
            prognosis.setType(PrognosisType.D_PROGNOSIS);
            prognosis.setConnectionGroup(congestionPoint);

            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setId((long) i);
            ptuContainer.setPtuDate(ptuDate);
            ptuContainer.setPtuIndex(i);
            prognosis.setPtuContainer(ptuContainer);

            prognoses.add(prognosis);
        }

        return prognoses;
    }

    private CongestionPointConnectionGroup createCongestionPoint() {
        CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setDsoDomain("dsoTestDomain");
        congestionPoint.setUsefIdentifier(ENTITY_ADDRESS);

        return congestionPoint;
    }

    private List<NonAggregatorForecast> createNonAggregatorForecasts(LocalDate ptuDate) {
        List<NonAggregatorForecast> forecasts = new ArrayList<>();
        CongestionPointConnectionGroup congestionPoint = createCongestionPoint();

        for (int i = 1; i <= PTUS_PER_DAY; i++) {
            NonAggregatorForecast forecast = new NonAggregatorForecast();
            forecast.setId((long) i);
            forecast.setMaxLoad((long) 10 + i);
            forecast.setPower((long) 5 + i);
            forecast.setSequence((long) 111111 + i);
            forecast.setConnectionGroup(congestionPoint);

            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setId((long) i);
            ptuContainer.setPtuDate(ptuDate);
            ptuContainer.setPtuIndex(i);
            forecast.setPtuContainer(ptuContainer);

            forecasts.add(forecast);
        }

        return forecasts;
    }

    private List<GridSafetyAnalysis> buildLatestSafetyAnalysis(String usefIdentifier, LocalDate period) {
        List<GridSafetyAnalysis> latestSafetyAnalysis = new ArrayList<>();
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup(usefIdentifier);
        for (int i = 1; i <= PTUS_PER_DAY; ++i) {
            GridSafetyAnalysis gsa = new GridSafetyAnalysis();
            gsa.setDisposition(DispositionAvailableRequested.REQUESTED);
            gsa.setConnectionGroup(connectionGroup);
            gsa.setPtuContainer(new PtuContainer(period, i));
            gsa.setPower(10L);
            gsa.setPrognoses(new ArrayList<>());
            gsa.setId((long) i);
            gsa.setSequence(-1L);
            latestSafetyAnalysis.add(gsa);
        }
        return latestSafetyAnalysis;
    }

}
