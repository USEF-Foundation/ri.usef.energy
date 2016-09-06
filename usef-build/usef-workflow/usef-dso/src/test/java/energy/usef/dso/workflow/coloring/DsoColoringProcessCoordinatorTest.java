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

import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_POST_COLORING_PROCESS;
import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_PREPARE_STEPWISE_LIMITING;

import java.util.ArrayList;
import java.util.List;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
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

import energy.usef.core.dto.PtuContainerDto;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.model.GridSafetyAnalysis;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.util.ReflectionUtil;

/**
 * Unit tests for testing the {@link DsoColoringProcessCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class DsoColoringProcessCoordinatorTest {
    private static final String CONGESTION_POINT = "1234567890";
    private static final String DSO_DOMAIN = "dso.usef-example.com";

    @Mock
    private Logger LOGGER;

    @Mock
    private Config config;

    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private EventValidationService eventValidationService;

    private DsoColoringProcessCoordinator coordinator;

    @Before
    public void init() throws Exception {
        coordinator = new DsoColoringProcessCoordinator();

        ReflectionUtil.setFinalStatic(DsoColoringProcessCoordinator.class.getDeclaredField("LOGGER"), LOGGER);
        Whitebox.setInternalState(coordinator, "config", config);
        Whitebox.setInternalState(coordinator, "workflowStepExecuter", workflowStepExecuter);
        Whitebox.setInternalState(coordinator, "corePlanboardBusinessService", corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, "dsoPlanboardBusinessService", dsoPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, "eventValidationService", eventValidationService);
    }

    @Test
    public void testHandleEvent() throws BusinessValidationException {
        CongestionPointConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(CONGESTION_POINT);
        connectionGroup.setDsoDomain(DSO_DOMAIN);

        Mockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        Mockito.when(corePlanboardBusinessService.findConnectionGroup(Matchers.any())).thenReturn(connectionGroup);
        Mockito.when(
                dsoPlanboardBusinessService.findLatestGridSafetyAnalysisWithDispositionRequested(Matchers.any(), Matchers.any()))
                .thenReturn(buildGridSafetyAnalysisList());
        Mockito.when(corePlanboardBusinessService.findOrCreatePtuState(Matchers.any(), Matchers.any())).thenReturn(buildPtuState());

        ColoringProcessEvent event = new ColoringProcessEvent(DateTimeUtil.parseDate("2015-04-17"), CONGESTION_POINT);
        coordinator.handleEvent(event);

        Mockito.verify(workflowStepExecuter, Mockito.times(1))
                .invoke(Matchers.eq(DSO_PREPARE_STEPWISE_LIMITING.name()), Matchers.any());

        ArgumentCaptor<WorkflowContext> contextCaptor = ArgumentCaptor.forClass(WorkflowContext.class);
        Mockito.verify(workflowStepExecuter, Mockito.times(1))
                .invoke(Matchers.eq(DSO_POST_COLORING_PROCESS.name()), contextCaptor.capture());

        @SuppressWarnings("unchecked")
        List<PtuContainerDto> ptuContainerList = (List<PtuContainerDto>) contextCaptor.getValue().getValue(
                PostColoringProcessParameter.IN.PTU_CONTAINER_DTO_LIST.name());

        Assert.assertNotNull(ptuContainerList.get(0));
        Assert.assertEquals(DateTimeUtil.parseDate("2015-04-17"), ptuContainerList.get(0).getPtuDate());
        Assert.assertEquals(17, ptuContainerList.get(0).getPtuIndex());
    }

    @Test
    public void testHandleEventWithConnectionGroupNull() throws BusinessValidationException {
        Mockito.when(corePlanboardBusinessService.findConnectionGroup(Matchers.any())).thenReturn(null);
        Mockito.when(
                dsoPlanboardBusinessService.findLatestGridSafetyAnalysisWithDispositionRequested(Matchers.any(), Matchers.any()))
                .thenReturn(new ArrayList<>());

        ColoringProcessEvent event = new ColoringProcessEvent(DateTimeUtil.parseDate("2015-04-17"), CONGESTION_POINT);
        coordinator.handleEvent(event);

        Mockito.verify(workflowStepExecuter, Mockito.times(0)).invoke(Matchers.anyString(), Matchers.any(WorkflowContext.class));
    }

    @Test
    public void testHandleEventWithNoGridSafetyAnalysis() throws BusinessValidationException {
        CongestionPointConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(CONGESTION_POINT);
        connectionGroup.setDsoDomain(DSO_DOMAIN);

        Mockito.when(corePlanboardBusinessService.findConnectionGroup(Matchers.any())).thenReturn(connectionGroup);

        ColoringProcessEvent event = new ColoringProcessEvent(DateTimeUtil.parseDate("2015-04-17"), CONGESTION_POINT);
        coordinator.handleEvent(event);

        Mockito.verify(workflowStepExecuter, Mockito.times(0)).invoke(Matchers.anyString(), Matchers.any(WorkflowContext.class));
    }

    private PtuState buildPtuState() {
        PtuState ptuState = new PtuState();
        ptuState.setPtuContainer(buildPtuContainer());

        return ptuState;
    }

    private PtuContainer buildPtuContainer() {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuDate(DateTimeUtil.parseDate("2015-04-17"));
        ptuContainer.setPtuIndex(17);

        return ptuContainer;
    }

    private List<GridSafetyAnalysis> buildGridSafetyAnalysisList() {
        List<GridSafetyAnalysis> gridSafetyAnalysisList = new ArrayList<>();

        GridSafetyAnalysis gridSafetyAnalysis = new GridSafetyAnalysis();
        gridSafetyAnalysis.setDisposition(DispositionAvailableRequested.REQUESTED);
        gridSafetyAnalysis.setPtuContainer(buildPtuContainer());

        gridSafetyAnalysisList.add(gridSafetyAnalysis);
        return gridSafetyAnalysisList;
    }

}
