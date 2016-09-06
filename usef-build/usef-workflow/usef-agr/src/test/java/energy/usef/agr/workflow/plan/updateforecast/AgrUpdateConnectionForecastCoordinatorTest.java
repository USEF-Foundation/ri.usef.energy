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

package energy.usef.agr.workflow.plan.updateforecast;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_CREATE_N_DAY_AHEAD_FORECAST;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.OUT;
import energy.usef.core.config.Config;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrUpdateConnectionForecastCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrUpdateConnectionForecastCoordinatorTest {

    private static final String CONNECTION_ENTITY_ADDRESS = "ean.00000000001";
    private AgrUpdateConnectionForecastCoordinator coordinator;

    @Mock
    private Config config;
    @Mock
    private ConfigAgr configAgr;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Before
    public void init() throws Exception {
        coordinator = new AgrUpdateConnectionForecastCoordinator();
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configAgr);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
    }

    /**
     * Test workflow with empty connections list on Event
     */
    @SuppressWarnings("unchecked") @Test
    public void invokeWorkflowWithEmptyConnectionsList() {

        Mockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_CONNECTION_FORECAST_DAYS_INTERVAL)).thenReturn(1);
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_CREATE_N_DAY_AHEAD_FORECAST.name()), Mockito.any()))
                .then(call -> parseContext((WorkflowContext) call.getArguments()[1]));

        coordinator.updateConnectionForecast(new UpdateConnectionForecastEvent());

        verify(agrPortfolioBusinessService, times(1)).updateConnectionPortfolio(Mockito.any(LocalDate.class), anyList());
    }

    @SuppressWarnings("unchecked") @Test
    public void testInvokeWorkflowWithConnections() {
        // stubbing
        Mockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_CONNECTION_FORECAST_DAYS_INTERVAL)).thenReturn(1);
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_CREATE_N_DAY_AHEAD_FORECAST.name()), Mockito.any()))
                .then(call -> parseContext((WorkflowContext) call.getArguments()[1]));
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new ConnectionPortfolioDto(CONNECTION_ENTITY_ADDRESS)));

        // actual invocation.
        coordinator.updateConnectionForecast(new UpdateConnectionForecastEvent(Optional.of(buildConnectionAddressList())));

        // verifications
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new ConnectionPortfolioDto(CONNECTION_ENTITY_ADDRESS)));
        Mockito.verify(workflowStepExecuter, Mockito.times(1))
                .invoke(Matchers.eq(AgrWorkflowStep.AGR_CREATE_N_DAY_AHEAD_FORECAST.name()), Matchers.any(WorkflowContext.class));
        Mockito.verify(agrPortfolioBusinessService, Mockito.times(1)).updateConnectionPortfolio(Matchers.any(LocalDate.class),anyList());
    }

    private List<String> buildConnectionAddressList() {
        return Collections.singletonList(CONNECTION_ENTITY_ADDRESS);
    }

    private WorkflowContext parseContext(WorkflowContext context) {
        context.setValue(OUT.CONNECTION_PORTFOLIO.name(), Collections.singletonList(new ConnectionPortfolioDto(CONNECTION_ENTITY_ADDRESS)));
        return context;
    }

}
