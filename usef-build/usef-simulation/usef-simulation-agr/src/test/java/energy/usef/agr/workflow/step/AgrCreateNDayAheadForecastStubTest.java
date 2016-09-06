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

package energy.usef.agr.workflow.step;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.pbcfeederimpl.PbcFeederService;
import energy.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.IN;
import energy.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.OUT;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class AgrCreateNDayAheadForecastStubTest {
    private static final int PTU_DURATION = 15;// 15 minutes
    private AgrCreateNDayAheadForecastStub agrCreateNDayAheadForecastStub;
    @Mock
    private PbcFeederService pbcFeederService;

    @Before
    public void init() {
        agrCreateNDayAheadForecastStub = new AgrCreateNDayAheadForecastStub();
        Whitebox.setInternalState(agrCreateNDayAheadForecastStub, "pbcFeederService", pbcFeederService);
    }

    @Test
    public void testInvoke() {
        WorkflowContext workflowContext = new DefaultWorkflowContext();

        List<ConnectionPortfolioDto> connections = createConnections();

        workflowContext.setValue(IN.PTU_DURATION.name(), PTU_DURATION);
        LocalDate forecastDay = DateTimeUtil.getCurrentDate().plusDays(1);
        workflowContext.setValue(IN.PTU_DATE.name(), forecastDay);
        workflowContext.setValue(IN.CONNECTION_PORTFOLIO.name(), connections);

        Mockito.when(pbcFeederService
                .updateConnectionUncontrolledLoadForecast(Matchers.any(LocalDate.class), Matchers.anyInt(), Matchers.anyInt(),
                        Matchers.any()))
                .thenReturn(connections);

        Mockito.when(pbcFeederService
                .retrieveUDIListWithPvLoadForecast(Matchers.any(LocalDate.class), Matchers.any(),
                        Matchers.anyInt(), Matchers.anyInt())).thenReturn(connections);

        Mockito.when(pbcFeederService
                .updateUdiLoadForecast(Matchers.any(LocalDate.class), Matchers.anyInt(), Matchers.any())).thenReturn(connections);

        workflowContext = agrCreateNDayAheadForecastStub.invoke(workflowContext);

        @SuppressWarnings("unchecked")
        List<ConnectionPortfolioDto> connectionPortfolioResults = (List<ConnectionPortfolioDto>) workflowContext.getValue(
                OUT.CONNECTION_PORTFOLIO.name());

        Assert.assertEquals(3, connectionPortfolioResults.size());
    }

    private Map<String, String> createConnectionToCongestionPointMap(List<ConnectionPortfolioDto> connections) {
        Map<String, String> connectionToCongestionPointMap = new HashMap<>();
        for (ConnectionPortfolioDto connection : connections) {
            connectionToCongestionPointMap.put(connection.getConnectionEntityAddress(), "ean.1234567890");
        }
        return connectionToCongestionPointMap;
    }

    private List<ConnectionPortfolioDto> createConnections() {
        List<ConnectionPortfolioDto> connections = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            connections.add(new ConnectionPortfolioDto("ean.234234234234443215" + i));
        }
        return connections;
    }

}
