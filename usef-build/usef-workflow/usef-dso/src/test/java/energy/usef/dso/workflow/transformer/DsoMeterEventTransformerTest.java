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

package energy.usef.dso.workflow.transformer;

import energy.usef.core.data.xml.bean.message.MeterEventType;
import energy.usef.core.model.Connection;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.dto.MeterEventTypeDto;
import energy.usef.dso.model.ConnectionCapacityLimitationPeriod;
import energy.usef.dso.model.ConnectionMeterEvent;
import energy.usef.dso.model.EventType;
import energy.usef.dso.workflow.dto.ConnectionCapacityLimitationPeriodDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for the class {@link DsoMeterEventTransformer}.
 */
@RunWith(PowerMockRunner.class)
public class DsoMeterEventTransformerTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, DsoMeterEventTransformer.class.getDeclaredConstructors().length);
        Constructor<DsoMeterEventTransformer> constructor = DsoMeterEventTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransformToDto() throws Exception {
        Assert.assertEquals(MeterEventTypeDto.CAPACITY_MANAGEMENT,
                DsoMeterEventTransformer.transformToDto(EventType.CapacityManagement));
        Assert.assertEquals(MeterEventTypeDto.CONNECTION_INTERRUPTION,
                DsoMeterEventTransformer.transformToDto(EventType.ConnectionInterruption));
        Assert.assertEquals(MeterEventTypeDto.CONNECTION_RESUMPTION,
                DsoMeterEventTransformer.transformToDto(EventType.ConnectionResumption));
    }

    @Test
    public void testTransformToDtoList() throws Exception {
        ConnectionMeterEvent event1 = new ConnectionMeterEvent();
        event1.setConnection(new Connection());
        ConnectionMeterEvent event2 = new ConnectionMeterEvent();
        event2.setConnection(new Connection());

        List<ConnectionMeterEvent> meterEventsForPeriod = Arrays.asList(event1, event2);

        List<ConnectionMeterEventDto> connectionMeterEventDtos = DsoMeterEventTransformer.transformToDto(meterEventsForPeriod);

        Assert.assertNotNull(connectionMeterEventDtos);
        Assert.assertEquals(meterEventsForPeriod.size(), connectionMeterEventDtos.size());
    }

    @Test
    public void testTransformConnectionMeterEventToDto() throws Exception {
        ConnectionMeterEvent connectionMeterEvent = null;
        Assert.assertNull(DsoMeterEventTransformer.transformToDto(connectionMeterEvent));

        LocalDateTime now = DateTimeUtil.getCurrentDateTime();

        connectionMeterEvent = new ConnectionMeterEvent();
        connectionMeterEvent.setCapacity(BigInteger.TEN);
        connectionMeterEvent.setDateTime(now);
        connectionMeterEvent.setEventType(EventType.CapacityManagement);
        connectionMeterEvent.setConnection(new Connection("ean.1"));

        ConnectionMeterEventDto connectionMeterEventDto = DsoMeterEventTransformer.transformToDto(connectionMeterEvent);

        Assert.assertEquals(connectionMeterEvent.getCapacity(), connectionMeterEventDto.getEventData());
        Assert.assertEquals(connectionMeterEvent.getDateTime(), connectionMeterEventDto.getEventDateTime());
        Assert.assertEquals(DsoMeterEventTransformer.transformToDto(connectionMeterEvent.getEventType()),
                connectionMeterEventDto.getEventType());
        Assert.assertEquals(connectionMeterEvent.getConnection().getEntityAddress(), connectionMeterEventDto.getEntityAddress());
    }

    @Test
    public void testTransformConnectionCapacityLimitationPeriodDtoToModel() throws Exception {
        ConnectionCapacityLimitationPeriodDto connectionCapacityLimitationPeriodDto = null;
        Assert.assertNull(DsoMeterEventTransformer.transformToModel(connectionCapacityLimitationPeriodDto));

        LocalDateTime now = DateTimeUtil.getCurrentDateTime();

        connectionCapacityLimitationPeriodDto = new ConnectionCapacityLimitationPeriodDto();
        connectionCapacityLimitationPeriodDto.setCapacityReduction(BigInteger.TEN);
        connectionCapacityLimitationPeriodDto.setStartDateTime(now);
        connectionCapacityLimitationPeriodDto.setEndDateTime(now.plusDays(1));

        ConnectionCapacityLimitationPeriod connectionCapacityLimitationPeriod = DsoMeterEventTransformer
                .transformToModel(connectionCapacityLimitationPeriodDto);

        Assert.assertEquals(connectionCapacityLimitationPeriodDto.getCapacityReduction(),
                connectionCapacityLimitationPeriod.getCapacityReduction());
        Assert.assertEquals(connectionCapacityLimitationPeriodDto.getStartDateTime(),
                connectionCapacityLimitationPeriod.getStartDateTime());
        Assert.assertEquals(connectionCapacityLimitationPeriodDto.getEndDateTime(),
                connectionCapacityLimitationPeriod.getEndDateTime());
    }

    @Test
    public void testTransformXmlToModel() throws Exception {
        MeterEventType meterEventType = null;
        Assert.assertNull(DsoMeterEventTransformer.transformToModel(meterEventType));
        Assert.assertEquals(EventType.CapacityManagement,
                DsoMeterEventTransformer.transformToModel(MeterEventType.CAPACITY_MANAGEMENT));
        Assert.assertEquals(EventType.ConnectionInterruption,
                DsoMeterEventTransformer.transformToModel(MeterEventType.CONNECTION_INTERRUPTION));
        Assert.assertEquals(EventType.ConnectionResumption,
                DsoMeterEventTransformer.transformToModel(MeterEventType.CONNECTION_RESUMPTION));
    }

    @Test
    public void testTransformDtoToModel() throws Exception {
        MeterEventTypeDto meterEventTypeDto = null;
        Assert.assertNull(DsoMeterEventTransformer.transformToModel(meterEventTypeDto));
        Assert.assertEquals(EventType.CapacityManagement,
                DsoMeterEventTransformer.transformToModel(MeterEventTypeDto.CAPACITY_MANAGEMENT));
        Assert.assertEquals(EventType.ConnectionInterruption,
                DsoMeterEventTransformer.transformToModel(MeterEventTypeDto.CONNECTION_INTERRUPTION));
        Assert.assertEquals(EventType.ConnectionResumption,
                DsoMeterEventTransformer.transformToModel(MeterEventTypeDto.CONNECTION_RESUMPTION));
    }
}
