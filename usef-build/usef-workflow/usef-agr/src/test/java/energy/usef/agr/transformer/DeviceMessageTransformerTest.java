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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import energy.usef.agr.dto.device.request.ConsumptionProductionTypeDto;
import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.dto.device.request.IncreaseRequestDto;
import energy.usef.agr.dto.device.request.InterruptRequestDto;
import energy.usef.agr.dto.device.request.ReduceRequestDto;
import energy.usef.agr.dto.device.request.ReportRequestDto;
import energy.usef.agr.dto.device.request.ShiftRequestDto;
import energy.usef.agr.model.ConsumptionProductionType;
import energy.usef.agr.model.DeviceMessage;
import energy.usef.agr.model.DeviceMessageStatus;
import energy.usef.agr.model.IncreaseRequest;
import energy.usef.agr.model.InterruptRequest;
import energy.usef.agr.model.ReduceRequest;
import energy.usef.agr.model.ReportRequest;
import energy.usef.agr.model.ShiftRequest;
import energy.usef.agr.model.Udi;
import energy.usef.core.util.DateTimeUtil;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to {@link DeviceMessageTransformer} class.
 */
public class DeviceMessageTransformerTest {

    private static final LocalDate PERIOD = new LocalDate(2015, 11, 26);
    private static final String EVENT_ID = "aaf8aa40-943c-11e5-a837-0800200c9a66";

    @Test
    public void testTransformIsSuccessfulFromDto() {
        DeviceMessageDto deviceMessageDto = buildDeviceMessage(1);
        Udi udi = new Udi();
        DeviceMessage deviceMessageModel = DeviceMessageTransformer.transform(deviceMessageDto, udi);
        assertNotNull(deviceMessageModel);
        assertTrue(udi == deviceMessageModel.getUdi());
        assertEquals("endpoint:1", deviceMessageModel.getEndpoint());
        assertEquals(2, deviceMessageModel.getDeviceRequests().stream().filter(o -> o instanceof ShiftRequest).count());
        assertEquals(2, deviceMessageModel.getDeviceRequests().stream().filter(o -> o instanceof ReduceRequest).count());
        assertEquals(2, deviceMessageModel.getDeviceRequests().stream().filter(o -> o instanceof IncreaseRequest).count());
        assertEquals(2, deviceMessageModel.getDeviceRequests().stream().filter(o -> o instanceof InterruptRequest).count());
        assertEquals(2, deviceMessageModel.getDeviceRequests().stream().filter(o -> o instanceof ReportRequest).count());
    }

    @Test
    public void testTransformListIsSuccessful() {
        List<DeviceMessage> deviceMessageModels = DeviceMessageTransformer.transform(
                Arrays.asList(buildDeviceMessage(1), buildDeviceMessage(2), buildDeviceMessage(3)), new HashMap<>());
        assertEquals(3, deviceMessageModels.size());
    }

    @Test
    public void testTransformDeviceMessageToDto() {
        final String endpoint = "openadr://agr1.usef-example.com/brand/dishwasher/1";
        final LocalDate period = new LocalDate(2015, 11, 26);
        DeviceMessage deviceMessage = new DeviceMessage();
        deviceMessage.setEndpoint(endpoint);
        deviceMessage.setUdi(new Udi());
        deviceMessage.setDeviceMessageStatus(DeviceMessageStatus.NEW);
        populateDeviceRequests(deviceMessage);
        DeviceMessageDto deviceMessageDto = DeviceMessageTransformer.transformToDto(deviceMessage);
        Assert.assertNotNull(deviceMessageDto);
        Assert.assertEquals(endpoint, deviceMessageDto.getEndpoint());
        assertDeviceRequests(deviceMessageDto);
    }

    private void assertDeviceRequests(DeviceMessageDto deviceMessageDto) {
        Assert.assertEquals(1, deviceMessageDto.getShiftRequestDtos().size());
        Assert.assertEquals(1, deviceMessageDto.getIncreaseRequestDtos().size());
        Assert.assertEquals(1, deviceMessageDto.getReduceRequestDtos().size());
        Assert.assertEquals(1, deviceMessageDto.getInterruptRequestDtos().size());
        Assert.assertEquals(1, deviceMessageDto.getReportRequestDtos().size());
        assertShiftRequestDto(deviceMessageDto.getShiftRequestDtos().get(0));
        assertReduceRequestDto(deviceMessageDto.getReduceRequestDtos().get(0));
        assertIncreaseRequestDto(deviceMessageDto.getIncreaseRequestDtos().get(0));
        assertInterruptRequestDto(deviceMessageDto.getInterruptRequestDtos().get(0));
        assertReportRequestDto(deviceMessageDto.getReportRequestDtos().get(0));
    }

    private void assertShiftRequestDto(ShiftRequestDto shiftRequestDto) {
        Assert.assertNotNull(shiftRequestDto);
        Assert.assertNotNull(shiftRequestDto.getId());
        Assert.assertEquals(PERIOD, shiftRequestDto.getDate());
        Assert.assertEquals(EVENT_ID, shiftRequestDto.getEventID());
        Assert.assertEquals(1, shiftRequestDto.getStartDTU().intValue());
    }

    private void assertReduceRequestDto(ReduceRequestDto reduceRequestDto) {
        Assert.assertNotNull(reduceRequestDto);
        Assert.assertNotNull(reduceRequestDto.getId());
        Assert.assertEquals(PERIOD, reduceRequestDto.getDate());
        Assert.assertEquals(EVENT_ID, reduceRequestDto.getEventID());
        Assert.assertEquals(1, reduceRequestDto.getStartDTU().intValue());
        Assert.assertEquals(5, reduceRequestDto.getEndDTU().intValue());
        Assert.assertEquals(-1000, reduceRequestDto.getPower().intValue());
        Assert.assertEquals(ConsumptionProductionTypeDto.CONSUMPTION, reduceRequestDto.getConsumptionProductionType());
    }

    private void assertIncreaseRequestDto(IncreaseRequestDto increaseRequestDto) {
        Assert.assertNotNull(increaseRequestDto);
        Assert.assertNotNull(increaseRequestDto.getId());
        Assert.assertEquals(PERIOD, increaseRequestDto.getDate());
        Assert.assertEquals(EVENT_ID, increaseRequestDto.getEventID());
        Assert.assertEquals(1, increaseRequestDto.getStartDTU().intValue());
        Assert.assertEquals(5, increaseRequestDto.getEndDTU().intValue());
        Assert.assertEquals(1000, increaseRequestDto.getPower().intValue());
        Assert.assertEquals(ConsumptionProductionTypeDto.PRODUCTION, increaseRequestDto.getConsumptionProductionType());
    }

    private void assertInterruptRequestDto(InterruptRequestDto interruptRequestDto) {
        Assert.assertNotNull(interruptRequestDto);
        Assert.assertNotNull(interruptRequestDto.getId());
        Assert.assertEquals(PERIOD, interruptRequestDto.getDate());
        Assert.assertEquals(EVENT_ID, interruptRequestDto.getEventID());
        Assert.assertEquals("1,2,3,5", interruptRequestDto.getDtus());
    }

    private void assertReportRequestDto(ReportRequestDto reportRequestDto) {
        Assert.assertNotNull(reportRequestDto);
        Assert.assertNotNull(reportRequestDto.getId());
        Assert.assertEquals(PERIOD, reportRequestDto.getDate());
        Assert.assertEquals("1,2,3,5", reportRequestDto.getDtus());
    }

    private DeviceMessageDto buildDeviceMessage(int i) {
        DeviceMessageDto deviceMessage = new DeviceMessageDto();
        deviceMessage.setEndpoint("endpoint:" + i);

        buildShiftRequests(deviceMessage);
        buildReduceRequests(deviceMessage);
        buildIncreaseRequests(deviceMessage);
        buildInterruptRequests(deviceMessage);
        buildReportRequests(deviceMessage);

        return deviceMessage;
    }

    private void buildShiftRequests(DeviceMessageDto deviceMessage) {
        IntStream.range(1, 3).mapToObj(index -> {
            ShiftRequestDto shiftRequest = new ShiftRequestDto();
            shiftRequest.setDate(DateTimeUtil.getCurrentDate());
            shiftRequest.setEventID("shift-" + index);
            shiftRequest.setStartDTU(BigInteger.valueOf(index));
            shiftRequest.setId("" + index);
            return shiftRequest;
        }).forEach(o -> deviceMessage.getShiftRequestDtos().add(o));
    }

    private void buildReduceRequests(DeviceMessageDto deviceMessage) {
        IntStream.range(1, 3).mapToObj(index -> {
            ReduceRequestDto reduceRequest = new ReduceRequestDto();
            reduceRequest.setDate(DateTimeUtil.getCurrentDate());
            reduceRequest.setEventID("reduce-" + index);
            reduceRequest.setStartDTU(BigInteger.valueOf(index));
            reduceRequest.setId("" + index);
            reduceRequest.setEndDTU(BigInteger.valueOf(index).add(BigInteger.valueOf(2)));
            reduceRequest.setPower(BigInteger.TEN);
            reduceRequest.setConsumptionProductionType(ConsumptionProductionTypeDto.CONSUMPTION);
            return reduceRequest;
        }).forEach(o -> deviceMessage.getReduceRequestDtos().add(o));
    }

    private void buildIncreaseRequests(DeviceMessageDto deviceMessage) {
        IntStream.range(1, 3).mapToObj(index -> {
            IncreaseRequestDto increaseRequest = new IncreaseRequestDto();
            increaseRequest.setDate(DateTimeUtil.getCurrentDate());
            increaseRequest.setEventID("increase-" + index);
            increaseRequest.setStartDTU(BigInteger.valueOf(index));
            increaseRequest.setId("" + index);
            increaseRequest.setEndDTU(BigInteger.valueOf(index).add(BigInteger.valueOf(2)));
            increaseRequest.setPower(BigInteger.TEN);
            increaseRequest.setConsumptionProductionType(ConsumptionProductionTypeDto.PRODUCTION);
            return increaseRequest;
        }).forEach(o -> deviceMessage.getIncreaseRequestDtos().add(o));
    }

    private void buildInterruptRequests(DeviceMessageDto deviceMessage) {
        IntStream.range(1, 3).mapToObj(index -> {
            InterruptRequestDto interruptRequest = new InterruptRequestDto();
            interruptRequest.setDate(DateTimeUtil.getCurrentDate());
            interruptRequest.setEventID("eventId-" + index);
            interruptRequest.setId("" + index);
            interruptRequest.setDtus(buildDtuIndexes());
            return interruptRequest;
        }).forEach(o -> deviceMessage.getInterruptRequestDtos().add(o));
    }

    private void buildReportRequests(DeviceMessageDto deviceMessage) {
        IntStream.range(1, 3).mapToObj(index -> {
            ReportRequestDto reportRequest = new ReportRequestDto();
            reportRequest.setDate(DateTimeUtil.getCurrentDate());
            reportRequest.setId("" + index);
            reportRequest.setDtus(buildDtuIndexes());
            return reportRequest;
        }).forEach(o -> deviceMessage.getReportRequestDtos().add(o));
    }

    private String buildDtuIndexes() {
        return "1" + DeviceMessageTransformer.DTU_INDEX_SEPERATOR
                + "2" + DeviceMessageTransformer.DTU_INDEX_SEPERATOR
                + "3" + DeviceMessageTransformer.DTU_INDEX_SEPERATOR
                + "4" + DeviceMessageTransformer.DTU_INDEX_SEPERATOR
                + "5";
    }

    private void populateDeviceRequests(DeviceMessage deviceMessage) {
        deviceMessage.getDeviceRequests().add(buildShiftRequest());
        deviceMessage.getDeviceRequests().add(buildReduceRequest());
        deviceMessage.getDeviceRequests().add(buildIncreaseRequest());
        deviceMessage.getDeviceRequests().add(buildInterruptRequest());
        deviceMessage.getDeviceRequests().add(buildReportRequest());
    }

    private ReportRequest buildReportRequest() {
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setId(uuid());
        reportRequest.setPeriod(PERIOD);
        reportRequest.setDtuIndexes(Arrays.asList(1, 2, 3, 5));
        return reportRequest;
    }

    private InterruptRequest buildInterruptRequest() {
        InterruptRequest interruptRequest = new InterruptRequest();
        interruptRequest.setId(uuid());
        interruptRequest.setPeriod(PERIOD);
        interruptRequest.setEventId(EVENT_ID);
        interruptRequest.setDtuIndexes(Arrays.asList(1, 2, 3, 5));
        return interruptRequest;
    }

    private IncreaseRequest buildIncreaseRequest() {
        IncreaseRequest increaseRequest = new IncreaseRequest();
        increaseRequest.setPower(BigInteger.valueOf(1000));
        increaseRequest.setStartDtu(1);
        increaseRequest.setEndDtu(5);
        increaseRequest.setEventId(EVENT_ID);
        increaseRequest.setPeriod(PERIOD);
        increaseRequest.setId(uuid());
        increaseRequest.setConsumptionProductionType(ConsumptionProductionType.PRODUCTION);
        return increaseRequest;
    }

    private ReduceRequest buildReduceRequest() {
        ReduceRequest reduceRequest = new ReduceRequest();
        reduceRequest.setId(uuid());
        reduceRequest.setStartDtu(1);
        reduceRequest.setEventId(EVENT_ID);
        reduceRequest.setPeriod(PERIOD);
        reduceRequest.setEndDtu(5);
        reduceRequest.setPower(BigInteger.valueOf(-1000));
        reduceRequest.setConsumptionProductionType(ConsumptionProductionType.CONSUMPTION);
        return reduceRequest;
    }

    private ShiftRequest buildShiftRequest() {
        ShiftRequest shiftRequest = new ShiftRequest();
        shiftRequest.setPeriod(PERIOD);
        shiftRequest.setEventId(EVENT_ID);
        shiftRequest.setStartDtu(1);
        shiftRequest.setId(uuid());
        return shiftRequest;
    }

    private String uuid() {
        return UUID.randomUUID().toString();
    }
}
