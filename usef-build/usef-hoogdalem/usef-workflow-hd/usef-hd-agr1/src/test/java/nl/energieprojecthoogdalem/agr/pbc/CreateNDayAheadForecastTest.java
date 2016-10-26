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

package nl.energieprojecthoogdalem.agr.pbc;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.pbcfeederimpl.PbcFeederService;
import info.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.IN;
import info.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.OUT;
import info.usef.core.util.DateTimeUtil;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.energieprojecthoogdalem.forecastservice.ForecastService;
import nl.energieprojecthoogdalem.forecastservice.forecast.ForecastFactory;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test to test the AgrCreateNDayAheadForecastStub.
 */
@RunWith(PowerMockRunner.class)
public class CreateNDayAheadForecastTest {
    private static final int PTU_DURATION = 15;// 15 minutes
    private CreateNDayAheadForecast agrCreateNDayAheadForecast;
    @Mock
    private ForecastFactory forecastFactory;

    @Before
    public void init()
    {
        agrCreateNDayAheadForecast = new CreateNDayAheadForecast();
        Whitebox.setInternalState(agrCreateNDayAheadForecast, "forecastFactory", forecastFactory);
    }

    @Test
    public void testInvoke()
    {
        WorkflowContext workflowContext = new DefaultWorkflowContext();

        List<ConnectionPortfolioDto> connections = createConnections();

        workflowContext.setValue(IN.PTU_DURATION.name(), PTU_DURATION);
        LocalDate forecastDay = DateTimeUtil.getCurrentDate().plusDays(1);
        workflowContext.setValue(IN.PTU_DATE.name(), forecastDay);
        workflowContext.setValue(IN.CONNECTION_PORTFOLIO.name(), connections);

        Mockito.when(forecastFactory.createNdayAheadForecast(Matchers.any(LocalDate.class), Matchers.anyInt(), Matchers.anyList()))
                .thenReturn(connections);

        workflowContext = agrCreateNDayAheadForecast.invoke(workflowContext);

        @SuppressWarnings("unchecked")
        List<ConnectionPortfolioDto> connectionPortfolioResults = (List<ConnectionPortfolioDto>) workflowContext.getValue(
                OUT.CONNECTION_PORTFOLIO.name());

        Assert.assertEquals(3, connectionPortfolioResults.size());
    }

    private List<ConnectionPortfolioDto> createConnections() {
        List<ConnectionPortfolioDto> connections = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            connections.add(new ConnectionPortfolioDto("ean.234234234234443215" + i));
        }
        return connections;
    }

}
