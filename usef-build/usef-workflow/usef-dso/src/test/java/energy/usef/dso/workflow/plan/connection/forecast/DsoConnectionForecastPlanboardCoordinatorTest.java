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

package energy.usef.dso.workflow.plan.connection.forecast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.RequestMoveToValidateEvent;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.util.ReflectionUtil;

/**
 * Test class in charge of the unit tests related to the {@link DsoConnectionForecastPlanboardCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class DsoConnectionForecastPlanboardCoordinatorTest {
    private static final String WORKFLOW_ENDED = "Workflow is ended";

    private DsoConnectionForecastPlanboardCoordinator coordinator;

    @Mock
    private Logger LOGGER;

    @Mock
    private Config config;

    @Mock
    private ConfigDso configDso;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private Event<RequestMoveToValidateEvent> moveToValidateEventManager;

    @Before
    public void init() throws Exception {
        coordinator = new DsoConnectionForecastPlanboardCoordinator();
        ReflectionUtil.setFinalStatic(DsoConnectionForecastPlanboardCoordinator.class.getDeclaredField("LOGGER"), LOGGER);
        Whitebox.setInternalState(coordinator, dsoPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configDso);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, moveToValidateEventManager);

    }

    /**
     * Tests DSOConnectionForecastPlanBoardCoordinator.invoke method, when there are non-aggregator connections.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void invokeWorkflow() {
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        PowerMockito.when(configDso.getIntegerProperty(ConfigDsoParam.DSO_CONNECTION_FORECAST_DAYS_INTERVAL)).thenReturn(2);

        String usefIdentifer = "ea.1234-1234-121111";
        Mockito.when(corePlanboardBusinessService.findActiveConnectionGroupsWithConnections(Matchers.any(LocalDate.class)))
                .thenReturn(
                        Collections.singletonMap(new CongestionPointConnectionGroup(usefIdentifer), createConnections(200)));

        Mockito.when(
                dsoPlanboardBusinessService
                        .findAggregatorOnConnectionGroupStateByCongestionPointAddress(Matchers.eq(usefIdentifer),
                                Matchers.any(LocalDate.class))).thenReturn(createTestAggregators());

        Mockito.when(
                workflowStepExecuter.invoke(Mockito.anyString(), Mockito.any(WorkflowContext.class))).thenReturn(buildContext());

        coordinator.handleEvent(new CreateConnectionForecastEvent());

        ArgumentCaptor<WorkflowContext> captor = ArgumentCaptor.forClass(WorkflowContext.class);
        Mockito.verify(workflowStepExecuter, times(2)).invoke(Mockito.anyString(), captor.capture());
        WorkflowContext context = captor.getValue();

        verify(moveToValidateEventManager, times(2)).fire(Matchers.any(RequestMoveToValidateEvent.class));
        verify(corePlanboardBusinessService, times(2)).findActiveConnectionGroupsWithConnections(Matchers.any(LocalDate.class));

        assertEquals(2, ((List<String>) context.getValue(WorkflowParameter.AGR_DOMAIN_LIST)).size());
        assertEquals("test.com", ((List<String>) context.getValue(WorkflowParameter.AGR_DOMAIN_LIST)).get(0));
        assertEquals("test2.com", ((List<String>) context.getValue(WorkflowParameter.AGR_DOMAIN_LIST)).get(1));
        assertNotNull(context.getValue(WorkflowParameter.AGR_CONNECTION_COUNT_LIST));
        assertNotNull(context.getValue(WorkflowParameter.CONGESTION_POINT_ENTITY_ADDRESS));
        assertNotNull(context.getValue(WorkflowParameter.PTU_DURATION));
        assertNotNull(context.getValue(WorkflowParameter.PTU_DATE));
    }

    private WorkflowContext buildContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(DsoCreateNonAggregatorForecastParameter.OUT.POWER.name(), new ArrayList<Long>());
        context.setValue(DsoCreateNonAggregatorForecastParameter.OUT.MAXLOAD.name(), new ArrayList<Long>());

        return context;
    }

    /**
     * Tests DSOConnectionForecastPlanboardCoordinator.invoke method when there are no non-aggregator connections.
     */
    @Test
    public void invokeWorkflow2() {
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        PowerMockito.when(configDso.getIntegerProperty(ConfigDsoParam.DSO_CONNECTION_FORECAST_DAYS_INTERVAL)).thenReturn(2);

        String usefIdentifer = "ea.1234-1234-121111";
        Mockito.when(corePlanboardBusinessService.findActiveConnectionGroupsWithConnections(Matchers.any(LocalDate.class)))
                .thenReturn(
                        Collections.singletonMap(new CongestionPointConnectionGroup(usefIdentifer), createConnections(2)));
        Mockito.when(
                dsoPlanboardBusinessService
                        .findAggregatorOnConnectionGroupStateByCongestionPointAddress(Matchers.eq(usefIdentifer),
                                Matchers.any(LocalDate.class))).thenReturn(createTestAggregators());

        coordinator.handleEvent(new CreateConnectionForecastEvent());

        verify(moveToValidateEventManager, times(2)).fire(Matchers.any(RequestMoveToValidateEvent.class));

        Mockito.verify(dsoPlanboardBusinessService, Mockito.times(2))
                .saveNonAggregatorConnectionForecast(Matchers.eq(new CongestionPointConnectionGroup(usefIdentifer)),
                        Matchers.any(LocalDate.class), Matchers.any(), Matchers.any());

    }

    private List<Connection> createConnections(int connectionCount) {
        List<Connection> list = new ArrayList<>();
        for (int i = 0; i < connectionCount; i++) {
            list.add(new Connection());
        }
        return list;
    }

    private List<AggregatorOnConnectionGroupState> createTestAggregators() {
        List<AggregatorOnConnectionGroupState> aggregators = new ArrayList<>();
        Aggregator ag = new Aggregator();
        ag.setDomain("test.com");

        AggregatorOnConnectionGroupState aocgs = new AggregatorOnConnectionGroupState();
        aocgs.setAggregator(ag);
        aocgs.setConnectionCount(BigInteger.ONE);
        aggregators.add(aocgs);

        Aggregator ag2 = new Aggregator();
        ag2.setDomain("test2.com");
        AggregatorOnConnectionGroupState aocgs2 = new AggregatorOnConnectionGroupState();
        aocgs2.setAggregator(ag2);
        aocgs2.setConnectionCount(BigInteger.TEN);
        aggregators.add(aocgs2);

        return aggregators;
    }

}
