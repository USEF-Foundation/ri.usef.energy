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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * JUnit test for the PtuUtil class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PtuUtilTest {
    private static final int PTU_DURATION = 15; // minutes

    private static final String TIMEZONE_BERLIN = "Europe/Berlin";

    private static DateTimeZone jodaTimeZone;
    private static TimeZone utilTimeZone;

    @BeforeClass
    public static void before() {
        jodaTimeZone = DateTimeZone.getDefault();
        utilTimeZone = TimeZone.getDefault();

        // Some tests implemented require the timezone to be Europe/Amsterdam (or another timezone that follows the same
        // daylight saving time regime as Amsterdam).
        DateTimeZone.setDefault(DateTimeZone.forID(TIMEZONE_BERLIN));
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE_BERLIN));
    }

    @AfterClass
    public static void after() {
        // Reset the default timezone settings to not mess up other tests.
        DateTimeZone.setDefault(jodaTimeZone);
        TimeZone.setDefault(utilTimeZone);
    }

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, PtuUtil.class.getDeclaredConstructors().length);
        Constructor<PtuUtil> constructor = PtuUtil.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    /**
     * Tests PtuUtil.getNumberOfPtusPerDay method.
     */
    @Test
    public void testGetNumberOfPtusPerDay() throws DatatypeConfigurationException {
        LocalDate day = new LocalDate().withYear(2014).withMonthOfYear(11).withDayOfMonth(26);
        assertEquals(96, PtuUtil.getNumberOfPtusPerDay(day, PTU_DURATION));
    }

    /**
     * Tests PtuUtil.getNumberOfPtusPerDay method for a day with daylight saving time (Winter to Summer).
     */
    @Test
    public void testGetNumberOfPtusPerDayForShortDay() throws DatatypeConfigurationException {
        LocalDate day = new LocalDate().withYear(2015).withMonthOfYear(3).withDayOfMonth(29);
        assertEquals(92, PtuUtil.getNumberOfPtusPerDay(day, PTU_DURATION));
    }

    /**
     * Tests PtuUtil.getNumberOfPtusPerDay method for a day with daylight saving time (Summer to Winter).
     */
    @Test
    public void testGetNumberOfPtusPerDayForLongDay() throws DatatypeConfigurationException {
        LocalDate day = new LocalDate().withYear(2015).withMonthOfYear(10).withDayOfMonth(25);
        assertEquals(100, PtuUtil.getNumberOfPtusPerDay(day, PTU_DURATION));
    }

    /**
     * Tests PtuUtil.getNumberOfPtusPerDay method while summertime switch.
     */
    @Test
    public void testGetNumberOfPtusPerDayForSummerSwitch() throws DatatypeConfigurationException {
        LocalDate day = new LocalDate().withYear(2015).withMonthOfYear(3).withDayOfMonth(29);
        assertEquals(92, PtuUtil.getNumberOfPtusPerDay(day, PTU_DURATION));
    }

    /**
     * Tests PtuUtil.getNumberOfPtusPerDay method while wintertime switch.
     */
    @Test
    public void testGetNumberOfPtusPerDayForWinterSwitch() throws DatatypeConfigurationException {
        LocalDate day = new LocalDate().withYear(2015).withMonthOfYear(10).withDayOfMonth(25);
        assertEquals(100, PtuUtil.getNumberOfPtusPerDay(day, PTU_DURATION));
    }

    /**
     * Tests PtuUtil.getPtuIndex method.
     */
    @Test
    public void testGetPtuIndex() {
        LocalDateTime timestamp = new LocalDateTime().withYear(2014).withMonthOfYear(11).withDayOfMonth(26).withHourOfDay(1)
                .withMinuteOfHour(1).withSecondOfMinute(1).withMillisOfSecond(1);
        assertEquals(5, PtuUtil.getPtuIndex(timestamp, PTU_DURATION));
    }

    /**
     * Tests PtuUtil.getPtuIndex method.
     */
    @Test
    public void testGetPtuIndexZero() {
        LocalDateTime timestamp = new LocalDateTime().withYear(2014).withMonthOfYear(11).withDayOfMonth(26).withHourOfDay(0)
                .withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        assertEquals(1, PtuUtil.getPtuIndex(timestamp, PTU_DURATION));
    }

    /**
     * Tests PtuUtil.numberOfPtusBetween
     */
    @Test
    public void testNumberOfPtusBetween() {
        LocalDate startDate = new LocalDate("2015-1-1");
        LocalDate endDate = new LocalDate("2015-1-2");

        assertEquals(96, PtuUtil.numberOfPtusBetween(startDate, endDate, 1, 1, 15).intValue());

        endDate = endDate.plusDays(1);
        assertEquals(192, PtuUtil.numberOfPtusBetween(startDate, endDate, 1, 1, 15).intValue());

        assertEquals(191, PtuUtil.numberOfPtusBetween(startDate, endDate, 2, 1, 15).intValue());
        assertEquals(193, PtuUtil.numberOfPtusBetween(startDate, endDate, 1, 2, 15).intValue());
    }
}
