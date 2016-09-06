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

package energy.usef.core.workflow.transformer;

import energy.usef.core.data.xml.bean.message.ConnectionMeterEvent;
import energy.usef.core.data.xml.bean.message.MeterEventType;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.dto.MeterEventTypeDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

import org.joda.time.LocalDateTime;
import org.junit.Test;

import junit.framework.TestCase;

/**
 *
 */
public class MeterEventTransformerTest extends TestCase {


    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        assertEquals("There must be only one constructor", 1, MeterEventTransformer.class.getDeclaredConstructors().length);
        Constructor<MeterEventTransformer> constructor = MeterEventTransformer.class.getDeclaredConstructor();
        assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransformToXml() throws Exception {
        ConnectionMeterEventDto dto = new ConnectionMeterEventDto();
        dto.setEntityAddress("ea1.oeosk.s");
        dto.setEventDateTime(new LocalDateTime());
        dto.setEventType(MeterEventTypeDto.CAPACITY_MANAGEMENT);
        dto.setEventData(BigInteger.valueOf(12L));

        ConnectionMeterEvent event = MeterEventTransformer.transformToXml(dto);
        assertNotNull(event);
        assertEquals(dto.getEntityAddress(), event.getEntityAddress());
        assertEquals (dto.getEventData(), event.getEventData());
        assertEquals(dto.getEventDateTime(), event.getEventDateTime());
        assertEquals(MeterEventTransformer.trasformToXml(dto.getEventType()), event.getEventType());

        assertNull(MeterEventTransformer.transformToXml(null));
    }

    @Test
    public void testTrasformToXml() throws Exception {
        assertNull(MeterEventTransformer.trasformToXml(null));

        assertEquals(MeterEventType.CAPACITY_MANAGEMENT, MeterEventTransformer.trasformToXml(MeterEventTypeDto.CAPACITY_MANAGEMENT));
        assertEquals(MeterEventType.CONNECTION_INTERRUPTION, MeterEventTransformer.trasformToXml(MeterEventTypeDto.CONNECTION_INTERRUPTION));
        assertEquals(MeterEventType.CONNECTION_RESUMPTION, MeterEventTransformer.trasformToXml(MeterEventTypeDto.CONNECTION_RESUMPTION));

    }
}
