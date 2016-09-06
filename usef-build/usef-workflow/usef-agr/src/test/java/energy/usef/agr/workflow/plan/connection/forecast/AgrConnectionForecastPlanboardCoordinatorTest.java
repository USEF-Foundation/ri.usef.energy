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

package energy.usef.agr.workflow.plan.connection.forecast;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_CREATE_N_DAY_AHEAD_FORECAST;
import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_NON_UDI_CREATE_N_DAY_AHEAD_FORECAST;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.util.ReflectionUtil;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioEvent;
import energy.usef.core.config.Config;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Event;

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

/**
 * Test class in charge of the unit tests related to the {@link AgrConnectionForecastPlanboardCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class AgrConnectionForecastPlanboardCoordinatorTest {
    private static final String ENTITY_ADDRESS = "abc.com";

    private AgrConnectionForecastPlanboardCoordinator coordinator;

    @Mock
    private Logger LOGGER;

    @Mock
    private Config config;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Mock
    private Event<ReOptimizePortfolioEvent> reOptimizePortfolioEventManager;

    @Before
    public void init() throws Exception {
        coordinator = new AgrConnectionForecastPlanboardCoordinator();
        ReflectionUtil.setFinalStatic(AgrConnectionForecastPlanboardCoordinator.class.getDeclaredField("LOGGER"), LOGGER);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configAgr);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, "reOptimizePortfolioEventManager", reOptimizePortfolioEventManager);
    }

    private WorkflowContext buildContext(WorkflowContext context) {
        List<ConnectionPortfolioDto> connectionPortfolioResults = new ArrayList<>();
        connectionPortfolioResults.add(new ConnectionPortfolioDto(ENTITY_ADDRESS));

        context.setValue(ConnectionForecastStepParameter.OUT.CONNECTION_PORTFOLIO.name(), connectionPortfolioResults);
        return context;
    }

    /**
     * Tests AgrConnectionForecastPlanBoardCoordinator.invoke method.
     */
    @Test
    public void testHandleEvent() {
        ArgumentCaptor<WorkflowContext> inContextCaptor = ArgumentCaptor.forClass(WorkflowContext.class);
        Mockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_CONNECTION_FORECAST_DAYS_INTERVAL)).thenReturn(1);
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_CREATE_N_DAY_AHEAD_FORECAST.name()), inContextCaptor.capture()))
                .then(call -> buildContext((WorkflowContext) call.getArguments()[1]));

        coordinator.handleEvent(new CreateConnectionForecastEvent());

        // verify input context
        WorkflowContext inContext = inContextCaptor.getValue();
        Assert.assertNotNull(inContext);
        // verify calls for portfolio modification
        verify(agrPortfolioBusinessService, times(1)).createConnectionForecasts(Matchers.any(LocalDate.class), Matchers.anyListOf(
                ConnectionPortfolioDto.class));
        verify(reOptimizePortfolioEventManager, times(1)).fire(Matchers.any(ReOptimizePortfolioEvent.class));
    }

    @Test
    public void testHandleEventForNonUdiAggregator() {
        ArgumentCaptor<WorkflowContext> inContextCaptor = ArgumentCaptor.forClass(WorkflowContext.class);
        Mockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_CONNECTION_FORECAST_DAYS_INTERVAL)).thenReturn(1);
        Mockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(true);
        Mockito.when(
                workflowStepExecuter.invoke(Mockito.eq(AGR_NON_UDI_CREATE_N_DAY_AHEAD_FORECAST.name()), inContextCaptor.capture()))
                .then(call -> buildContext((WorkflowContext) call.getArguments()[1]));

        // invocation
        coordinator.handleEvent(new CreateConnectionForecastEvent());
        // validations
        WorkflowContext inContext = inContextCaptor.getValue();
        Assert.assertNotNull(inContext);
        verify(agrPortfolioBusinessService, times(1))
                .createConnectionForecasts(Matchers.any(LocalDate.class), Matchers.anyListOf(ConnectionPortfolioDto.class));
    }
}
