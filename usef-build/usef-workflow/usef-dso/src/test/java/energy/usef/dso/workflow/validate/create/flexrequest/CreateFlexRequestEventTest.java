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

package energy.usef.dso.workflow.validate.create.flexrequest;

import static org.junit.Assert.assertEquals;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import energy.usef.core.util.DateTimeUtil;

/**
 *
 */
public class CreateFlexRequestEventTest {

    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
    private Integer[] ptuIndexes = new Integer[]{};

    @Test
    public void testTodaysEvent() throws Exception {
        LocalDate period = DateTimeUtil.getCurrentDate();
        CreateFlexRequestEvent event = new CreateFlexRequestEvent(CONGESTION_POINT_ENTITY_ADDRESS, period, ptuIndexes);
        assertEquals(period, event.getPeriod());
        assertEquals(CONGESTION_POINT_ENTITY_ADDRESS, event.getCongestionPointEntityAddress());
    }
}
