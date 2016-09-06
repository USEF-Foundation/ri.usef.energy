/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.agr.devicemessages;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * POJO of
 *  {
 *      "startIndex": 70
 *      ,"period": "yyyy-MM-dd"
 *  }
 * */
public class ReservedDevice
{
    private final int startIndex;
    private final LocalDate period;

    @JsonCreator
    public ReservedDevice
    (
        @JsonProperty("startIndex") int startIndex
        ,@JsonProperty("period") String period
    )
    {
        this.startIndex = startIndex;
        DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
        this.period = dateFormat.parseDateTime(period).toLocalDate();
    }

    public int getStartIndex(){return startIndex;}
    public LocalDate getPeriod()
    {
        return period;
    }
}
