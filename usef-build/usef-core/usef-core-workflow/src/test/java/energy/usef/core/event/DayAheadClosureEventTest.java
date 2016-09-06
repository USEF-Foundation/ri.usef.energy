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

package energy.usef.core.event;

import energy.usef.core.util.DateTimeUtil;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to {@link DayAheadClosureEvent} class.
 */
public class DayAheadClosureEventTest {

    @Test
    public void testInstantiationWithNoDate() {
        DayAheadClosureEvent event = new DayAheadClosureEvent();
        Assert.assertNotNull(event);
        Assert.assertEquals("Expected tomorrow's date.", DateTimeUtil.getCurrentDate().plusDays(1), event.getPeriod());
    }

    @Test
    public void testInstantiationWithSpecificDate() {
        DayAheadClosureEvent event = new DayAheadClosureEvent(new LocalDate(2015, 4, 14));
        Assert.assertNotNull(event);
        Assert.assertEquals("Expected 2015-Apr-14 date.", new LocalDate(2015, 4, 14), event.getPeriod());
    }

    @Test
    public void testInstantiationWithNullDate() {
        DayAheadClosureEvent event = new DayAheadClosureEvent(null);
        Assert.assertNotNull(event);
        Assert.assertEquals("Expected 2015-Apr-14 date.", DateTimeUtil.getCurrentDate().plus(Days.ONE), event.getPeriod());
    }

}
