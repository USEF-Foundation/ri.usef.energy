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

package energy.usef.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * JUnit test for the CalendarUtil class.
 */
@RunWith(MockitoJUnitRunner.class)
public class DateTimeUtilTest {

    private static final int DEFAULT_MINUTES_PER_DAY = 1439;
    private static final int MINUTES_PER_HOUR = 60;

    private static final String TIMEZONE_AMSTERDAM = "Europe/Amsterdam";

    private static DateTimeZone jodaTimeZone;
    private static TimeZone utilTimeZone;

    @BeforeClass
    public static void before() {
        jodaTimeZone = DateTimeZone.getDefault();
        utilTimeZone = TimeZone.getDefault();

        // Some tests implemented require the timezone to be Europe/Amsterdam (or another timezone that follows the same
        // daylight saving time regime as Amsterdam).
        DateTimeZone.setDefault(DateTimeZone.forID(TIMEZONE_AMSTERDAM));
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE_AMSTERDAM));
    }

    @AfterClass
    public static void after() {
        // Reset the default timezone settings to not mess up other tests.
        DateTimeZone.setDefault(jodaTimeZone);
        TimeZone.setDefault(utilTimeZone);
    }

    /**
     * Tests CalendarUtil.getDate method.
     */
    @Test
    public void testGetDate() {

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(DateTimeUtil.parseDate("2001-11-20").toDateMidnight().getMillis());

        Assert.assertEquals(2001, calendar.get(Calendar.YEAR));
        Assert.assertEquals(11, calendar.get(Calendar.MONTH) + 1);
        Assert.assertEquals(20, calendar.get(Calendar.DATE));
    }

    /**
     * Tests CalendarUtil.getDate method with wrong date string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetDateWithWrongDate() {
        DateTimeUtil.parseDate("qsqsqsqsqsqsqssq");
    }

    /**
     * Tests CalendarUtil.convertDateToStandardFormat method.
     */
    @Test
    public void testMillisecondDelayUntilNextTime() {
        LocalTime time = new LocalTime();
        time = time.plusHours(1).plusMillis(200);
        int calculatedMillis = DateTimeUtil.millisecondDelayUntilNextTime(time).intValue();
        int hourInMillis = 60 * 60 * 1000;
        assertTrue(calculatedMillis > hourInMillis);
    }

    @Test
    public void testGetElapsedMinutesNormalDay() {
        LocalDateTime endOfDay = new LocalDateTime().withYear(2014).withMonthOfYear(11).withDayOfMonth(26)
                .withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(0);
        int minutes = DateTimeUtil.getElapsedMinutesSinceMidnight(endOfDay);

        // default number of minutes
        assertEquals(DEFAULT_MINUTES_PER_DAY, minutes);
    }

    @Test
    public void testPrintDateTime() {
        LocalDateTime dateTime = new LocalDateTime().withYear(2014).withMonthOfYear(11).withDayOfMonth(26)
                .withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(0);
        assertEquals("2014-11-26 23:59:59.000", DateTimeUtil.printDateTime(dateTime));
    }


    @Test
    public void testgetStartOfDayFromDate() {
        LocalDate now = new LocalDate();
        LocalDateTime startOfDay = new LocalDateTime().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);

        assertEquals(DateTimeUtil.printDateTime(startOfDay), DateTimeUtil.printDateTime(DateTimeUtil.getStartOfDay(now)));
    }

    @Test
    public void testgetEndOfDayFromDate() {
        LocalDate now = new LocalDate();
        LocalDateTime endOfDay = new LocalDateTime().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).plusDays(1);

        assertEquals(DateTimeUtil.printDateTime(endOfDay), DateTimeUtil.printDateTime(DateTimeUtil.getEndOfDay(now)));
    }

    @Test
    public void testgetStartOfDayFromDateTime() {
        LocalDateTime now = new LocalDateTime();
        LocalDateTime startOfDay = new LocalDateTime().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);

        assertEquals(DateTimeUtil.printDateTime(startOfDay), DateTimeUtil.printDateTime(DateTimeUtil.getStartOfDay(now)));
    }

    @Test
    public void testgetEndOfDayFromDateTime() {
        LocalDateTime now = new LocalDateTime();
        LocalDateTime endOfDay = new LocalDateTime().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).plusDays(1);

        assertEquals(DateTimeUtil.printDateTime(endOfDay), DateTimeUtil.printDateTime(DateTimeUtil.getEndOfDay(now)));
    }



    @Test
    public void testGetElapsedMinutesSummertimeDay() {
        LocalDateTime endOfDay = new LocalDateTime().withYear(2014).withMonthOfYear(3).withDayOfMonth(30).withHourOfDay(23).
                withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(0);
        System.out.println(endOfDay);

        int minutes = DateTimeUtil.getElapsedMinutesSinceMidnight(endOfDay);

        // number of minutes in case to summer time switch
        assertEquals(DEFAULT_MINUTES_PER_DAY - MINUTES_PER_HOUR, minutes);
    }

    @Test
    public void testGetElapsedMinutesWintertimeDay() {
        LocalDateTime endOfDay = new LocalDateTime().withYear(2014).withMonthOfYear(10).withDayOfMonth(26).withHourOfDay(23)
                .withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(0);
        System.out.println(endOfDay);
        int minutes = DateTimeUtil.getElapsedMinutesSinceMidnight(endOfDay);

        // number of minutes in case to winter time switch
        assertEquals(DEFAULT_MINUTES_PER_DAY + MINUTES_PER_HOUR, minutes);
    }

    @Test
    public void test() {
        LocalDate startDate = new LocalDate(2015, 1, 1);
        LocalDate endDate = new LocalDate(2015, 1, 10);
        List<LocalDate> localDates = DateTimeUtil.generateDatesOfInterval(startDate, endDate);
        Assert.assertEquals(10, localDates.size());
    }

}
