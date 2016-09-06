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
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastStepParameter;
import info.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastStepParameter.OUT;
import info.usef.core.dto.PtuContainerDto;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.PrognosisDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test to test the {@link IdentifyChangeInForecast}.
 */
@RunWith(PowerMockRunner.class)
public class IdentifyChangeInForecastTest {

    private IdentifyChangeInForecast identifyChangeInForecast;

    public static final int DTU_SIZE = 360;
    public static final int PTU_SIZE = 360;

    public static final String ENTITY_ADDRESS_1 = "ean.000000000001";
    public static final String ENTITY_ADDRESS_2 = "ean.000000000002";

    public static final LocalDate DAY1 = new LocalDate(2015, 1, 1);

    @Before
    public void setUp()
    {
        identifyChangeInForecast = new IdentifyChangeInForecast();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void invokeTestWithNotEmptyConnectionPortfolioForDay1()
    {
        WorkflowContext inContext = new DefaultWorkflowContext();
        List<ConnectionPortfolioDto> connectionPortfolioDto = buildConnectionPortfolioDtoForDay1();
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.CONNECTION_PORTFOLIO.name(), connectionPortfolioDto);
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.LATEST_A_PLANS_DTO_LIST.name(), Collections.singletonList(new PrognosisDto()));
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.PTU_DURATION.name(), PTU_SIZE);
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.PERIOD.name(), DAY1);

        WorkflowContext outContext = identifyChangeInForecast.invoke(inContext);

        boolean isChanged = (boolean) outContext.getValue(OUT.FORECAST_CHANGED.name());
        List<PtuContainerDto> resultList = (List<PtuContainerDto>) outContext.getValue(OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name());

        Assert.assertNotNull(resultList);
        Assert.assertEquals(0, resultList.size());

        Assert.assertNotNull(isChanged);
        Assert.assertFalse(isChanged);
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDtoForDay1() {
        ConnectionPortfolioDto connectionDto1OnDay1 = new ConnectionPortfolioDto(ENTITY_ADDRESS_1);
        ConnectionPortfolioDto connectionDto2OnDay1 = new ConnectionPortfolioDto(ENTITY_ADDRESS_2);

        IntStream.rangeClosed(1, 4).mapToObj(index -> {
            PowerContainerDto ptuDto = new PowerContainerDto(DAY1, index);
            ptuDto.getForecast().setAverageConsumption(BigInteger.TEN);
            return ptuDto;
        }).forEach(ptu -> connectionDto1OnDay1.getConnectionPowerPerPTU().put(ptu.getTimeIndex(), ptu));
        IntStream.rangeClosed(1, 4).mapToObj(index -> {
            PowerContainerDto ptuDto = new PowerContainerDto(DAY1, index);
            ptuDto.getForecast().setAverageConsumption(BigInteger.ONE);
            return ptuDto;
        }).forEach(ptu -> connectionDto2OnDay1.getConnectionPowerPerPTU().put(ptu.getTimeIndex(), ptu));

        UdiPortfolioDto udi1 = new UdiPortfolioDto(null, DTU_SIZE, null);
        IntStream.rangeClosed(1, 4).mapToObj(dtuIndex -> {
            PowerContainerDto dtu = new PowerContainerDto(DAY1, dtuIndex);
            dtu.getForecast().setAverageConsumption(BigInteger.valueOf(5));
            return dtu;
        }).forEach(dtu -> udi1.getUdiPowerPerDTU().put(dtu.getTimeIndex(), dtu));
        UdiPortfolioDto udi2 = new UdiPortfolioDto(null, DTU_SIZE, null);
        IntStream.rangeClosed(1, 4).mapToObj(dtuIndex -> {
            PowerContainerDto dtu = new PowerContainerDto(DAY1, dtuIndex);
            dtu.getForecast().setAverageConsumption(BigInteger.valueOf(5));
            return dtu;
        }).forEach(dtu -> udi2.getUdiPowerPerDTU().put(dtu.getTimeIndex(), dtu));
        connectionDto1OnDay1.getUdis().add(udi1);
        connectionDto2OnDay1.getUdis().add(udi2);

        List<ConnectionPortfolioDto> result = new ArrayList<>();
        result.add(connectionDto1OnDay1);
        result.add(connectionDto2OnDay1);

        return result;
    }

}
