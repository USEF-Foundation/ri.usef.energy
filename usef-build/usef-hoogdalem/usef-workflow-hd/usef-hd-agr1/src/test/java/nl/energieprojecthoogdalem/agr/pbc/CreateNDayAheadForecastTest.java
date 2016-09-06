/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
