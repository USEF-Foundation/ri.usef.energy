/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nl.energieprojecthoogdalem.util;


import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * utility to convert ptus to joda date and time classes
 * */
public class TimeUtil
{
    private static Logger LOGGER = LoggerFactory.getLogger(TimeUtil.class);

    /**
     * calculates the time from a ptu index and ptu duration
     * @param ptuIdx the index to calculate the time from
     * @param ptuDuration the duration of one ptu (15)
     * @return a string containing the time in hours and minutes in format "00:00" of the ptu index
     * */
    public static String getTimeFromPtu(int ptuIdx, int ptuDuration)
    {
        //96-1 *15 = 1425 -> floor(1425 / 60) == 23 : 1425 % 60 == 45
        //"01:15" == 60 *1 + 15 == 75, 75 / 15 +1 == 6

       return String.format("%02d:%02d", getHoursFromPtu(ptuIdx, ptuDuration), getMinutesFromPtu(ptuIdx, ptuDuration));
    }

    /**
     * calculates the minutes from a ptu index and ptu duration
     * @param ptuIdx the index to calculate the time from
     * @param ptuDuration the duration of one ptu (15)
     * @return a int containing the minutes of the ptu index
     * */
    public static int getMinutesFromPtu(int ptuIdx, int ptuDuration)
    {
       return  ptuToMinutes(ptuIdx, ptuDuration) % 60;
    }

    /**
     * calculates the hours from a ptu index and ptu duration
     * @param ptuIdx the index to calculate the time from
     * @param ptuDuration the duration of one ptu (15)
     * @return a int containing the hours of the ptu index
     * */
    public static int getHoursFromPtu(int ptuIdx, int ptuDuration)
    {
        return (int)Math.floor( ptuToMinutes(ptuIdx, ptuDuration) / 60);
    }

    /**
     * calculates the time from a ptu index and ptu duration
     * @param ptuIdx the index to calculate the time from
     * @param ptuDuration the duration of one ptu (15)
     * @return a {@link LocalTime} containing the time in hours and minutes of the ptu index
     * */
    public static LocalTime getLocalTimeFromPtuIndex(int ptuIdx, int ptuDuration)
    {
        return new LocalTime(getHoursFromPtu(ptuIdx, ptuDuration), getMinutesFromPtu(ptuIdx, ptuDuration));
    }

    /**
     * calculates the day from a string
     * @param dateString the date and time string to convert from in format "yyyy-mm-dd hh:mm"
     * @return a {@link Date} containing the day of the string
     * */
    public static Date getDateFromDateTimeString(String dateString)
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        simpleDateFormat.setLenient(false); //strict parsing
        try
        {
            return simpleDateFormat.parse(dateString);
        }
        catch(ParseException exception)
        {
            LOGGER.error("unable to parse datestring, stack: ", exception);
            return null;
        }
    }

    /**
     * calculates the day and time from a string
     * @param dateString the date and time string to convert from in format "yyyy-mm-dd hh:mm"
     * @return a {@link LocalDateTime} containing the day and time of the string
     * */
    public static LocalDateTime getLocalDateTimeFromDateTimeString(String dateString)
    {
        Date date = getDateFromDateTimeString(dateString);
        return (date != null) ? LocalDateTime.fromDateFields( date ) : null;
    }

    /**
     * calculates the total amount of hours and minutes from a ptu index and ptu duration
     * @param ptuIdx the index to calculate the time from
     * @param ptuDuration the duration of one ptu (15)
     * @return a int containing the time in minutes from both minutes and hours
     * */
    private static int ptuToMinutes(int ptuIdx, int ptuDuration)
    { return ((ptuIdx-1)*ptuDuration); }
}
