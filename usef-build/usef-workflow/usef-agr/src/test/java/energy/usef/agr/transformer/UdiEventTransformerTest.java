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

package energy.usef.agr.transformer;

import energy.usef.agr.dto.device.capability.DeviceCapabilityDto;
import energy.usef.agr.dto.device.capability.IncreaseCapabilityDto;
import energy.usef.agr.dto.device.capability.InterruptCapabilityDto;
import energy.usef.agr.dto.device.capability.InterruptCapabilityTypeDto;
import energy.usef.agr.dto.device.capability.ReduceCapabilityDto;
import energy.usef.agr.dto.device.capability.ReportCapabilityDto;
import energy.usef.agr.dto.device.capability.ShiftCapabilityDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.dto.device.capability.UdiEventTypeDto;
import energy.usef.agr.dto.device.request.ConsumptionProductionTypeDto;
import energy.usef.agr.model.ConsumptionProductionType;
import energy.usef.agr.model.Udi;
import energy.usef.agr.model.UdiEvent;
import energy.usef.agr.model.UdiEventType;
import energy.usef.agr.model.device.capability.DeviceCapability;
import energy.usef.agr.model.device.capability.IncreaseCapability;
import energy.usef.agr.model.device.capability.InterruptCapability;
import energy.usef.agr.model.device.capability.InterruptCapabilityType;
import energy.usef.agr.model.device.capability.ReduceCapability;
import energy.usef.agr.model.device.capability.ReportCapability;
import energy.usef.agr.model.device.capability.ShiftCapability;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link UdiEventTransformer} class.
 */
public class UdiEventTransformerTest {

    private static final String UDI_ENDPOINT = "udi.usef-example.com";

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, UdiEventTransformer.class.getDeclaredConstructors().length);
        Constructor<UdiEventTransformer> constructor = UdiEventTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransformToUdiEventToDto() throws Exception {
        UdiEvent udiEvent = buildUdiEvent();
        UdiEventDto udiEventDto = UdiEventTransformer.transformToDto(udiEvent);
        Assert.assertNotNull(udiEventDto);
        Assert.assertEquals(new LocalDate(2015, 11, 4), udiEventDto.getPeriod());
        Assert.assertEquals(4, udiEventDto.getStartDtu().intValue());
        Assert.assertEquals(8, udiEventDto.getEndDtu().intValue());
        Assert.assertEquals(3, udiEventDto.getStartAfterDtu().intValue());
        Assert.assertEquals(9, udiEventDto.getFinishBeforeDtu().intValue());
        Assert.assertEquals("selector", udiEventDto.getDeviceSelector());
        Assert.assertEquals("udi.usef-example.com", udiEventDto.getUdiEndpoint());
        Assert.assertEquals(UdiEventTypeDto.ON_DEMAND_CONSUMPTION, udiEventDto.getUdiEventType());
        Assert.assertEquals(5, udiEventDto.getDeviceCapabilities().size());

    }

    private UdiEvent buildUdiEvent() {
        UdiEvent udiEvent = new UdiEvent();
        udiEvent.setPeriod(new LocalDate(2015, 11, 4));
        udiEvent.setStartDtu(4);
        udiEvent.setEndDtu(8);
        udiEvent.setStartAfterDtu(3);
        udiEvent.setFinishBeforeDtu(9);
        udiEvent.setDeviceSelector("selector");
        Udi udi = new Udi();
        udi.setEndpoint(UDI_ENDPOINT);
        udiEvent.setUdi(udi);
        udiEvent.setUdiEventType(UdiEventType.ON_DEMAND_CONSUMPTION);
        udiEvent.getDeviceCapabilities().add(buildReportCapability(udiEvent));
        udiEvent.getDeviceCapabilities().add(buildInterruptCapability(udiEvent));
        udiEvent.getDeviceCapabilities().add(buildIncreaseCapability(udiEvent));
        udiEvent.getDeviceCapabilities().add(buildReduceCapability(udiEvent));
        udiEvent.getDeviceCapabilities().add(buildShiftCapability(udiEvent));
        return udiEvent;
    }

    @Test
    public void testTransformUdiEventTypeToDto() throws Exception {
        Assert.assertEquals(UdiEventTypeDto.CONSUMPTION, UdiEventTransformer.transformToDto(UdiEventType.CONSUMPTION));
        Assert.assertEquals(UdiEventTypeDto.PRODUCTION, UdiEventTransformer.transformToDto(UdiEventType.PRODUCTION));
        Assert.assertEquals(UdiEventTypeDto.ON_DEMAND_CONSUMPTION,
                UdiEventTransformer.transformToDto(UdiEventType.ON_DEMAND_CONSUMPTION));
        Assert.assertEquals(UdiEventTypeDto.ON_DEMAND_PRODUCTION,
                UdiEventTransformer.transformToDto(UdiEventType.ON_DEMAND_PRODUCTION));
        UdiEventType nullType = null;
        Assert.assertNull(UdiEventTransformer.transformToDto(nullType));
    }

    @Test
    public void testTransformDeviceCapabilityToDto() throws Exception {
        DeviceCapability deviceCapability = buildShiftCapability(new UdiEvent());
        DeviceCapabilityDto deviceCapabilityDto = UdiEventTransformer.transformToDto(deviceCapability);
        Assert.assertTrue(deviceCapabilityDto instanceof ShiftCapabilityDto);
    }

    @Test
    public void testTransformShiftCapabilityToDto() throws Exception {
        ShiftCapability shiftCapability = buildShiftCapability(new UdiEvent());
        ShiftCapabilityDto shiftCapabilityDto = UdiEventTransformer.transformCapabilityToDto(shiftCapability);
        Assert.assertNotNull(shiftCapabilityDto);
        Assert.assertNotNull(shiftCapabilityDto.getId());
    }

    @Test
    public void testTransformReportCapabilityToDto() throws Exception {
        ReportCapability reportCapability = buildReportCapability(new UdiEvent());
        ReportCapabilityDto reportCapabilityDto = UdiEventTransformer.transformCapabilityToDto(reportCapability);
        Assert.assertNotNull(reportCapabilityDto);
        Assert.assertNotNull(reportCapabilityDto.getId());
    }

    @Test
    public void testTransformInterruptCapabilityToDto() throws Exception {
        InterruptCapability interruptCapability = buildInterruptCapability(new UdiEvent());
        InterruptCapabilityDto interruptCapabilityDto = UdiEventTransformer.transformCapabilityToDto(interruptCapability);
        Assert.assertNotNull(interruptCapabilityDto);
        Assert.assertNotNull(interruptCapabilityDto.getId());
        Assert.assertEquals(8, interruptCapabilityDto.getMaxDtus().intValue());
        Assert.assertEquals(InterruptCapabilityTypeDto.FULL, interruptCapabilityDto.getType());
    }

    @Test
    public void testTransformInterruptCapabilityTypeToDto() throws Exception {
        Assert.assertEquals(InterruptCapabilityTypeDto.NONE, UdiEventTransformer.transformToDto(InterruptCapabilityType.NONE));
        Assert.assertEquals(InterruptCapabilityTypeDto.FULL, UdiEventTransformer.transformToDto(InterruptCapabilityType.FULL));
        Assert.assertEquals(InterruptCapabilityTypeDto.PER_DTU,
                UdiEventTransformer.transformToDto(InterruptCapabilityType.PER_DTU));
        InterruptCapabilityType nullType = null;
        Assert.assertNull(UdiEventTransformer.transformToDto(nullType));

    }

    @Test
    public void testTransformIncreaseCapabilityToDto() throws Exception {
        IncreaseCapability increaseCapability = buildIncreaseCapability(new UdiEvent());
        IncreaseCapabilityDto increaseCapabilityDto = UdiEventTransformer.transformCapabilityToDto(increaseCapability);
        Assert.assertNotNull(increaseCapabilityDto);
        Assert.assertNotNull(increaseCapabilityDto.getId());
        Assert.assertEquals(1, increaseCapability.getDurationMultiplier().intValue());
        Assert.assertEquals(8, increaseCapability.getMaxDtus().intValue());
        Assert.assertEquals(100, increaseCapability.getPowerStep().intValue());
        Assert.assertEquals(1000, increaseCapability.getMaxPower().intValue());
    }

    @Test
    public void testTransformReduceCapabilityToDto() throws Exception {
        ReduceCapability reduceCapability = buildReduceCapability(new UdiEvent());
        ReduceCapabilityDto reduceCapabilityDto = UdiEventTransformer.transformCapabilityToDto(reduceCapability);
        Assert.assertNotNull(reduceCapabilityDto);
        Assert.assertNotNull(reduceCapabilityDto.getId());
        Assert.assertEquals(1, reduceCapabilityDto.getDurationMultiplier().intValue());
        Assert.assertEquals(8, reduceCapabilityDto.getMaxDtus().intValue());
        Assert.assertEquals(100, reduceCapabilityDto.getPowerStep().intValue());
        Assert.assertEquals(1000, reduceCapabilityDto.getMinPower().intValue());
    }

    @Test
    public void testTransformInterruptCapabilityTypeToModel() {
        InterruptCapabilityTypeDto nullType = null;
        Assert.assertNull(UdiEventTransformer.transformToModel(nullType));
        Assert.assertEquals(InterruptCapabilityType.FULL, UdiEventTransformer.transformToModel(InterruptCapabilityTypeDto.FULL));
        Assert.assertEquals(InterruptCapabilityType.NONE, UdiEventTransformer.transformToModel(InterruptCapabilityTypeDto.NONE));
        Assert.assertEquals(InterruptCapabilityType.PER_DTU,
                UdiEventTransformer.transformToModel(InterruptCapabilityTypeDto.PER_DTU));
    }

    @Test
    public void testTransformUdiEventTypeToModel() {
        UdiEventTypeDto nullType = null;
        Assert.assertNull(UdiEventTransformer.transformToModel(nullType));
        Assert.assertEquals(UdiEventType.CONSUMPTION, UdiEventTransformer.transformToModel(UdiEventTypeDto.CONSUMPTION));
        Assert.assertEquals(UdiEventType.ON_DEMAND_CONSUMPTION,
                UdiEventTransformer.transformToModel(UdiEventTypeDto.ON_DEMAND_CONSUMPTION));
        Assert.assertEquals(UdiEventType.PRODUCTION, UdiEventTransformer.transformToModel(UdiEventTypeDto.PRODUCTION));
        Assert.assertEquals(UdiEventType.ON_DEMAND_PRODUCTION,
                UdiEventTransformer.transformToModel(UdiEventTypeDto.ON_DEMAND_PRODUCTION));
    }

    @Test
    public void testTransformUdiEventToModel() {
        UdiEventDto udiEventDto = buildUdiEventDto();
        Udi udi = new Udi();
        udi.setEndpoint(UDI_ENDPOINT);
        UdiEvent udiEvent = UdiEventTransformer.transformToModel(udiEventDto, udi);
        Assert.assertEquals(new LocalDate(2015, 11, 4), udiEvent.getPeriod());
        Assert.assertEquals(4, udiEvent.getStartDtu().intValue());
        Assert.assertEquals(8, udiEvent.getEndDtu().intValue());
        Assert.assertEquals(3, udiEvent.getStartAfterDtu().intValue());
        Assert.assertEquals(9, udiEvent.getFinishBeforeDtu().intValue());
        Assert.assertEquals("selector", udiEvent.getDeviceSelector());
        Assert.assertEquals("udi.usef-example.com", udiEvent.getUdi().getEndpoint());
        Assert.assertEquals(UdiEventType.ON_DEMAND_CONSUMPTION, udiEvent.getUdiEventType());
    }

    @Test
    public void testTransformToDto() {
        Assert.assertEquals(ConsumptionProductionTypeDto.CONSUMPTION, UdiEventTransformer.transformToDto(ConsumptionProductionType.CONSUMPTION));
        Assert.assertEquals(ConsumptionProductionTypeDto.PRODUCTION, UdiEventTransformer.transformToDto(ConsumptionProductionType.PRODUCTION));
        Assert.assertNull(UdiEventTransformer.transformToDto((ConsumptionProductionType)null));
    }

    @Test
    public void testTransformToModel () {
        Assert.assertEquals(InterruptCapabilityType.FULL, UdiEventTransformer.transformToModel(InterruptCapabilityTypeDto.FULL));
        Assert.assertEquals(InterruptCapabilityType.NONE, UdiEventTransformer.transformToModel(InterruptCapabilityTypeDto.NONE));
        Assert.assertEquals(InterruptCapabilityType.PER_DTU, UdiEventTransformer.transformToModel(InterruptCapabilityTypeDto.PER_DTU));
        Assert.assertNull(UdiEventTransformer.transformToModel((InterruptCapabilityTypeDto)null));
    }


    private UdiEventDto buildUdiEventDto() {
        UdiEventDto udiEventDto = new UdiEventDto();
        udiEventDto.setPeriod(new LocalDate(2015, 11, 4));
        udiEventDto.setStartDtu(4);
        udiEventDto.setEndDtu(8);
        udiEventDto.setStartAfterDtu(3);
        udiEventDto.setFinishBeforeDtu(9);
        udiEventDto.setDeviceSelector("selector");
        udiEventDto.setUdiEndpoint(UDI_ENDPOINT);
        udiEventDto.setUdiEventType(UdiEventTypeDto.ON_DEMAND_CONSUMPTION);
        return udiEventDto;
    }


    private ShiftCapability buildShiftCapability(UdiEvent udiEvent) {
        ShiftCapability shiftCapability = new ShiftCapability();
        shiftCapability.setId(uuid());
        shiftCapability.setUdiEvent(udiEvent);
        return shiftCapability;
    }

    private ReduceCapability buildReduceCapability(UdiEvent udiEvent) {
        ReduceCapability reduceCapability = new ReduceCapability();
        reduceCapability.setId(uuid());
        reduceCapability.setUdiEvent(udiEvent);
        reduceCapability.setDurationMultiplier(BigDecimal.ONE);
        reduceCapability.setMinPower(BigInteger.valueOf(1000));
        reduceCapability.setPowerStep(BigInteger.valueOf(100));
        reduceCapability.setMaxDtus(8);
        return reduceCapability;
    }

    private IncreaseCapability buildIncreaseCapability(UdiEvent udiEvent) {
        IncreaseCapability increaseCapability = new IncreaseCapability();
        increaseCapability.setId(uuid());
        increaseCapability.setUdiEvent(udiEvent);
        increaseCapability.setDurationMultiplier(1);
        increaseCapability.setMaxPower(BigInteger.valueOf(1000));
        increaseCapability.setPowerStep(BigInteger.valueOf(100));
        increaseCapability.setMaxDtus(8);
        return increaseCapability;
    }

    private InterruptCapability buildInterruptCapability(UdiEvent udiEvent) {
        InterruptCapability interruptCapability = new InterruptCapability();
        interruptCapability.setId(uuid());
        interruptCapability.setUdiEvent(udiEvent);
        interruptCapability.setMaxDtus(8);
        interruptCapability.setType(InterruptCapabilityType.FULL);
        return interruptCapability;
    }

    private ReportCapability buildReportCapability(UdiEvent udiEvent) {
        ReportCapability reportCapability = new ReportCapability();
        reportCapability.setId(uuid());
        reportCapability.setUdiEvent(udiEvent);
        return reportCapability;
    }

    private String uuid() {
        return UUID.randomUUID().toString();
    }

}
