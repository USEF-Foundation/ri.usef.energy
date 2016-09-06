/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.forecastservice.weather;

import info.usef.core.util.PtuUtil;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.*;

public class HourlyForecastTest
{
    private HourlyForecast hourlyForecast;

    private static final double SKY = 50D
                                , IRRADIATION = 0.5D
                                , RESULT = ((1D - IRRADIATION) *(100D -SKY)) /100D +IRRADIATION
            ;
    private static final int PTU_DURATION = 15;

    private static final Map<String, String> DATE = new HashMap<>();
    private static final LocalDateTime datetime = new LocalDateTime(2016, 3, 10, 12, 0, 0 );

    @Test
    public void testHourlyForecast() throws Exception
    {
        DATE.put("year", ""+ datetime.getYear());
        DATE.put("mon", "" + datetime.getMonthOfYear());
        DATE.put("mday", "" + datetime.getDayOfMonth());
        DATE.put("hour", "" + datetime.getHourOfDay());

        hourlyForecast = new HourlyForecast(SKY, DATE);

        assertEquals(RESULT, hourlyForecast.getSkyCorrectionFactor(IRRADIATION), 0.02D);
        assertEquals(PtuUtil.getPtuIndex(datetime, PTU_DURATION), hourlyForecast.getPtuIndex(PTU_DURATION));
        assertTrue(datetime.equals(hourlyForecast.getDateTime()));
    }

}