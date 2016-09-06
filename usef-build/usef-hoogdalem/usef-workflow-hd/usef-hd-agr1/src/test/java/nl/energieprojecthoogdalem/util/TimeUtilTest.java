package nl.energieprojecthoogdalem.util;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.Assert;
import org.junit.Test;

/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class TimeUtilTest {

    private static int ptuDuration = 15;

    private static int day = 11, month = 2, year = 2016, hours = 14, minutes = 0;

    private static String formatedDate = String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hours, minutes);

    private static String[] expected =  {
                                             "00:00"
                                            ,"00:15"
                                            ,"00:30"
                                            ,"00:45"
                                            ,"01:00"
                                            ,"01:15"
                                            ,"01:30"
                                            ,"01:45"
                                            ,"02:00"
                                            ,"02:15"
                                            ,"02:30"
                                            ,"02:45"
                                            ,"03:00"
                                            ,"03:15"
                                            ,"03:30"
                                            ,"03:45"
                                            ,"04:00"
                                            ,"04:15"
                                            ,"04:30"
                                            ,"04:45"
                                            ,"05:00"
                                            ,"05:15"
                                            ,"05:30"
                                            ,"05:45"
                                            ,"06:00"
                                            ,"06:15"
                                            ,"06:30"
                                            ,"06:45"
                                            ,"07:00"
                                            ,"07:15"
                                            ,"07:30"
                                            ,"07:45"
                                            ,"08:00"
                                            ,"08:15"
                                            ,"08:30"
                                            ,"08:45"
                                            ,"09:00"
                                            ,"09:15"
                                            ,"09:30"
                                            ,"09:45"
                                            ,"10:00"
                                            ,"10:15"
                                            ,"10:30"
                                            ,"10:45"
                                            ,"11:00"
                                            ,"11:15"
                                            ,"11:30"
                                            ,"11:45"
                                            ,"12:00"
                                            ,"12:15"
                                            ,"12:30"
                                            ,"12:45"
                                            ,"13:00"
                                            ,"13:15"
                                            ,"13:30"
                                            ,"13:45"
                                            ,"14:00"
                                            ,"14:15"
                                            ,"14:30"
                                            ,"14:45"
                                            ,"15:00"
                                            ,"15:15"
                                            ,"15:30"
                                            ,"15:45"
                                            ,"16:00"
                                            ,"16:15"
                                            ,"16:30"
                                            ,"16:45"
                                            ,"17:00"
                                            ,"17:15"
                                            ,"17:30"
                                            ,"17:45"
                                            ,"18:00"
                                            ,"18:15"
                                            ,"18:30"
                                            ,"18:45"
                                            ,"19:00"
                                            ,"19:15"
                                            ,"19:30"
                                            ,"19:45"
                                            ,"20:00"
                                            ,"20:15"
                                            ,"20:30"
                                            ,"20:45"
                                            ,"21:00"
                                            ,"21:15"
                                            ,"21:30"
                                            ,"21:45"
                                            ,"22:00"
                                            ,"22:15"
                                            ,"22:30"
                                            ,"22:45"
                                            ,"23:00"
                                            ,"23:15"
                                            ,"23:30"
                                            ,"23:45"
                                        };

    @Test
    public void testGetTimeFromPtu() throws Exception {

        for(int ptuIdx = 1; ptuIdx <= expected.length; ptuIdx++)
        {
            String result = TimeUtil.getTimeFromPtu(ptuIdx ,ptuDuration);
            //System.out.println("result: " + result);
            Assert.assertEquals(expected[ptuIdx-1], result);
        }
    }

    @Test
    public void testGetLocalDate() throws Exception
    {
        //System.out.println("in: " + formatedDate);
        LocalDateTime result = TimeUtil.getLocalDateTimeFromDateTimeString(formatedDate);

        Assert.assertEquals(minutes, result.getMinuteOfHour());
        Assert.assertEquals(hours, result.getHourOfDay());

        Assert.assertEquals(day, result.getDayOfMonth());
        Assert.assertEquals(month, result.getMonthOfYear());
        Assert.assertEquals(year, result.getYear());

        result = TimeUtil.getLocalDateTimeFromDateTimeString("");
        Assert.assertNull(result);


    }

    @Test
    public void testGetLocalTime() throws Exception
    {
        LocalTime result = TimeUtil.getLocalTimeFromPtuIndex(6 ,ptuDuration);

        Assert.assertEquals(1, result.getHourOfDay());
        Assert.assertEquals(15, result.getMinuteOfHour());
    }
}