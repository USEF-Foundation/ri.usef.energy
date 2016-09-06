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

import java.util.Map;

import org.joda.time.LocalDateTime;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * POJO of JSON:
 * {
 *      "FCTTIME":
 *      {
 *      "year": "2016"
 *      ,"mon": "3"
 *      ,"mday": "11"
 *      ,"hour": "11"
 *      ,... IGNORED
 *     }
 *   ,"sky": "5"
 *   ,... IGNORED
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HourlyForecast
{
    private final double sky;
    private final LocalDateTime dateTime;

    @JsonCreator
    public HourlyForecast
    (
        @JsonProperty("sky") double sky
        ,@JsonProperty("FCTTIME") Map<String, String> date
    )
    {
        int year = Integer.parseInt(date.get("year"))
            ,monthOfYear = Integer.parseInt(date.get("mon"))
            ,dayOfMonth = Integer.parseInt(date.get("mday"))
            ,hourOfDay = Integer.parseInt(date.get("hour"))
            ;

        dateTime = new LocalDateTime(year, monthOfYear, dayOfMonth, hourOfDay, 0, 0, 0);
        this.sky = sky;
    }

    public double getSkyCorrectionFactor(double irradiation)
    {
        return ((1D -irradiation) *(100D -sky)) /100D +irradiation;
    }

    public LocalDateTime getDateTime()
    {
        return dateTime;
    }

    public int getPtuIndex(int ptuDuration)
    {
        return PtuUtil.getPtuIndex(dateTime, ptuDuration);
    }

}
