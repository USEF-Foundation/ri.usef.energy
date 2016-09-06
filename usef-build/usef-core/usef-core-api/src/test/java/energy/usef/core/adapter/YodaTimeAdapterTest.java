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

package energy.usef.core.adapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;

import junit.framework.TestCase;

/**
 *
 */
public class YodaTimeAdapterTest extends TestCase {

    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        assertEquals("There must be only one constructor", 1, YodaTimeAdapter.class.getDeclaredConstructors().length);
        Constructor<YodaTimeAdapter> constructor = YodaTimeAdapter.class.getDeclaredConstructor();
        assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    public void testParseDateTime() throws Exception {
        LocalDateTime localDateTime = YodaTimeAdapter.parseDateTime("2015-11-02T16:13:59");

        assertEquals(2015, localDateTime.getYear());
        assertEquals(11, localDateTime.getMonthOfYear());
        assertEquals(2, localDateTime.getDayOfMonth());
        assertEquals(16, localDateTime.getHourOfDay());
        assertEquals(13, localDateTime.getMinuteOfHour());
        assertEquals(59, localDateTime.getSecondOfMinute());

        assertNull(YodaTimeAdapter.parseDateTime(""));
        assertNull(YodaTimeAdapter.parseDateTime(null));
    }

    public void testPrintDateTime() throws Exception {
        LocalDateTime localDateTime = new LocalDateTime(2015, 11, 2, 16, 13, 59);

        assertEquals("2015-11-02T16:13:59.000", YodaTimeAdapter.printDateTime(localDateTime));
        assertNull(YodaTimeAdapter.printDateTime(null));
    }

    public void testParseTime() throws Exception {
        LocalTime localTime = YodaTimeAdapter.parseTime("21:32:52");

        assertEquals(21, localTime.getHourOfDay());
        assertEquals(32, localTime.getMinuteOfHour());
        assertEquals(52, localTime.getSecondOfMinute());
        assertEquals(0, localTime.getMillisOfSecond());

        assertNull(YodaTimeAdapter.parseTime(""));
        assertNull(YodaTimeAdapter.parseTime(null));
    }

    public void testPrintTime() throws Exception {
        LocalTime localTime = new LocalTime(16, 13, 59);

        assertEquals("16:13:59.000", YodaTimeAdapter.printTime(localTime));
        assertNull(YodaTimeAdapter.printTime(null));
    }

    public void testParseDate() throws Exception {
        LocalDate localDate = YodaTimeAdapter.parseDate("2015-11-02");

        assertEquals(2015, localDate.getYear());
        assertEquals(11, localDate.getMonthOfYear());
        assertEquals(2, localDate.getDayOfMonth());

        assertNull(YodaTimeAdapter.parseDate(""));
        assertNull(YodaTimeAdapter.parseDate(null));
    }

    public void testPrintDate() throws Exception {
        LocalDate localDate = new LocalDate(2015, 11, 2);

        assertEquals("2015-11-02", YodaTimeAdapter.printDate(localDate));
        assertNull(YodaTimeAdapter.printDate(null));
    }

    public void testParseDuration() throws Exception {
        Period duration = YodaTimeAdapter.parseDuration("PT2M10S");

        assertEquals(2, duration.getMinutes());
        assertEquals(10, duration.getSeconds());

        assertNull(YodaTimeAdapter.parseDuration(""));
        assertNull(YodaTimeAdapter.parseDuration(null));
    }

    public void testPrintDuration() throws Exception {
       Period duration = new Period(12,23,45,33);

        assertEquals("PT12H23M45.033S", YodaTimeAdapter.printDuration(duration));
        assertNull(YodaTimeAdapter.printDuration(null));
    }
}
