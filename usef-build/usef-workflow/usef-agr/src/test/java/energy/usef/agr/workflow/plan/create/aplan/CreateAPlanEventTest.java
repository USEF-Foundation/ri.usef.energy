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

package energy.usef.agr.workflow.plan.create.aplan;

import static org.junit.Assert.*;

import org.joda.time.LocalDate;
import org.junit.Test;

import energy.usef.core.util.DateTimeUtil;

/**
 *
 */
public class CreateAPlanEventTest {

    public static final String USEF_ENERGY = "usef.energy";

    @Test
    public void testTodaysEvent() throws Exception {
        LocalDate period = DateTimeUtil.getCurrentDate();

        CreateAPlanEvent event = new CreateAPlanEvent(period, USEF_ENERGY);
        assertEquals(period, event.getPeriod());
        assertEquals(USEF_ENERGY, event.getUsefIdentifier());
        assertFalse(event.isExpired());
    }

    @Test
    public void testTomorrowsEvent() throws Exception {
        LocalDate period = DateTimeUtil.getCurrentDate().plusDays(1);

        CreateAPlanEvent event = new CreateAPlanEvent(period, USEF_ENERGY);
        assertEquals(period, event.getPeriod());
        assertEquals(USEF_ENERGY, event.getUsefIdentifier());
        assertFalse(event.isExpired());
    }

    @Test
    public void testYesterdaysEvent() throws Exception {
        LocalDate period = DateTimeUtil.getCurrentDate().minusDays(1);

        CreateAPlanEvent event = new CreateAPlanEvent(period, USEF_ENERGY);
        assertEquals(period, event.getPeriod());
        assertEquals(USEF_ENERGY, event.getUsefIdentifier());
        assertTrue(event.isExpired());
    }
}
