/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.forecastservice.forecast;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.ForecastPowerDataDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DefaultForecastTest
{
    private static String EAN               = "ean.800000000000000032"
                        , BATTERY_PROFILE   = "BATTERY"
                        , PV_PROFILE        = "PV"
                        , BATTERY_ENDPOINT  = "MZ29EBX000/usef/" + BATTERY_PROFILE
                        , PV_ENDPOINT       = "MZ29EBX000/usef/" + PV_PROFILE
                        ;

    private static int PTU_DURATION = 15
                     , PTU_COUNT = 96
                    ;

    private LocalDate date = new LocalDate(2016, 2, 25);

    private ConnectionPortfolioDto connection;

    @Before
    public void init() throws Exception
    {

        connection = new ConnectionPortfolioDto(EAN);
        connection.getUdis().addAll(buildUdis());
    }

    @Test
    public void testGetDefaultForecast() throws Exception
    {
        connection = DefaultForecast.getDefaultConnectionPortfolio(date, PTU_DURATION, connection );

        assertEquals(EAN, connection.getConnectionEntityAddress());

        Map<Integer, PowerContainerDto> powerPerPTU = connection.getConnectionPowerPerPTU();

        assertEquals(PTU_COUNT, powerPerPTU.size());

        for(UdiPortfolioDto udi : connection.getUdis())
            assertEquals(PTU_COUNT, udi.getUdiPowerPerDTU().size());

        for(int idx = 1; idx <= PTU_COUNT; idx++)
        {
            assertEquals(Integer.valueOf(idx), powerPerPTU.get(idx).getTimeIndex() );
            validateForecast(powerPerPTU.get(idx).getForecast());
            for(UdiPortfolioDto udi : connection.getUdis())
            {
                assertEquals(Integer.valueOf(idx), udi.getUdiPowerPerDTU().get(idx).getTimeIndex() );
                validateForecast( udi.getUdiPowerPerDTU().get(idx).getForecast());
            }
        }
    }

    private void validateForecast(ForecastPowerDataDto forecastPowerDataDto)
    {
        assertEquals(BigInteger.ZERO, forecastPowerDataDto.getUncontrolledLoad());

        assertEquals(BigInteger.ZERO, forecastPowerDataDto.getAverageConsumption());
        assertEquals(BigInteger.ZERO, forecastPowerDataDto.getAverageProduction());

        assertEquals(BigInteger.ZERO, forecastPowerDataDto.getPotentialFlexConsumption());
        assertEquals(BigInteger.ZERO, forecastPowerDataDto.getPotentialFlexProduction());

    }

    private List<UdiPortfolioDto> buildUdis()
    {
        List<UdiPortfolioDto> udis = new ArrayList<>();
        udis.add(new UdiPortfolioDto(PV_ENDPOINT, PTU_DURATION, PV_PROFILE) );
        udis.add(new UdiPortfolioDto(BATTERY_ENDPOINT, PTU_DURATION, BATTERY_PROFILE) );

        return udis;
    }
}