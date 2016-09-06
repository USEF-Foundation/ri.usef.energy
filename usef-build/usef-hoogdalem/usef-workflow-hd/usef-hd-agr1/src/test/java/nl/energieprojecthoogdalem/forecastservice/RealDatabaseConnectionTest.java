/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nl.energieprojecthoogdalem.forecastservice;

import nl.energieprojecthoogdalem.agr.dtos.Proposition;
import nl.energieprojecthoogdalem.forecastservice.weather.WeatherService;
import nl.energieprojecthoogdalem.util.EANUtil;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.sql.Date;
import java.util.Map;
import java.util.Properties;

/*real database test to check results*/

public class RealDatabaseConnectionTest {

    //BROKEN TEST
    private Properties prop;
    private WeatherService weatherService;
    private ForecastService forecastService;
    private boolean isConnected;

    private static int PTU_DURATION = 15;
    private static LocalDate PERIOD = new LocalDate().plusDays(1);

    @Before
    public void setup()
    {
        weatherService = new WeatherService(prop);
        forecastService = new ForecastService();
        Whitebox.setInternalState(forecastService, "weatherService", weatherService);
        isConnected = forecastService.connect();
    }

    @After
    public void teardown()
    {
        if(isConnected)
        {
            forecastService.disconnect();
            isConnected = false;
        }
    }

    @Test
    public void retrieveProposition() throws Exception {
        if(isConnected)
        {
            Proposition result = forecastService.retrieveProposition("031");
            System.out.println("hasBattery: " + result.hasBattery());
            System.out.println("hasPV: " + result.hasPv());
            forecastService.disconnect();
        }
        else
            System.err.println("unable to connect");
    }

    @Test
    public void retrieveForecast() throws Exception {

        if (isConnected)
        {
            Map<Integer, Long> result = forecastService.retrieveForecast(PERIOD, PTU_DURATION);
            readMap(result);
        }
    }

    @Test
    public void retrievePVForecast() throws Exception
    {
        String ean = EANUtil.EAN_PREFIX + "031";

        if(isConnected)
        {
            Map<Integer ,Long> result = forecastService.retrievePVForecast(EANUtil.toHomeString(ean) ,PERIOD, PTU_DURATION);
            readMap(result);
        }

    }

    private void readMap(Map<Integer, Long> map )
    {
        System.out.println("size: " + map.size());

        int found = 0;
        for (int idx = 1; idx <= 96; idx++)
        {
            if(found == 5)
                break;

            Long val = map.get(idx);
            val =  (val != null) ? val : 0L;
            if(val != 0L)
            {
                System.out.println( "ptuidx " + idx + " value " + map.get(idx));
                found++;
            }
        }

    }
}