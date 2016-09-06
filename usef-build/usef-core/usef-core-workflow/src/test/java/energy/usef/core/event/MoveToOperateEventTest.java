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

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link MoveToOperateEvent} class.
 */
public class MoveToOperateEventTest {

    @Test
    public void testInstantiationWithNullPeriod() {
        MoveToOperateEvent event = new MoveToOperateEvent(null, 1);
        Assert.assertNotNull("Did not expect a null event.", event);
        Assert.assertEquals("Expected today's date.", DateTimeUtil.getCurrentDate(), event.getPeriod());
        Assert.assertEquals("PTU index mismatch.", 1, event.getPtuIndex().intValue());
    }

    @Test
    public void testInstantiationWithSpecificDate() {
        MoveToOperateEvent event = new MoveToOperateEvent(new LocalDate(2015, 4, 14), 1);
        Assert.assertNotNull("Did not expect a null event.", event);
        Assert.assertEquals("Expected 2015-04-14 date.", DateTimeUtil.parseDate("2015-04-14"), event.getPeriod());
        Assert.assertEquals("PTU index mismatch.", 1, event.getPtuIndex().intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPtuIndexCannotBeNull() {
        MoveToOperateEvent event = new MoveToOperateEvent(null, null);
        Assert.assertNull(event);
    }

}
