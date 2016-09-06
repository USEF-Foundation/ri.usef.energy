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

package energy.usef.agr.workflow.step;

import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.dto.device.request.ReduceRequestDto;
import energy.usef.agr.dto.device.request.ReportRequestDto;
import energy.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.IN;
import energy.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.OUT;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;

import java.math.BigInteger;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link AgrControlActiveDemandSupplyStub} class.
 */
public class AgrControlActiveDemandSupplyStubTest {

    private static final String REPORT_REQUEST_ID = "ef4cd861-3e41-4f11-b474-1ad4e0799b78";
    private static final String REDUCE_REQUEST_ID = "f60b351f-6e02-43a7-a731-8534a4d3bcf5";
    private static final String UDI_EVENT_ID = "011b2fb9-16ef-41ba-88b4-9bc9c05e800a";
    private static final String ENDPOINT1 = "openadr://agr2.usef-example.com/brand/dishwasher/1";
    private static final LocalDate PERIOD = new LocalDate(2015, 11, 26);

    private AgrControlActiveDemandSupplyStub stub;

    @Before
    public void init() {
        stub = new AgrControlActiveDemandSupplyStub();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeSucceeds() {
        WorkflowContext context = stub.invoke(buildContext());
        Assert.assertNotNull(context);
        DeviceMessageDto outputDeviceMessageDto = context.get(OUT.FAILED_DEVICE_MESSAGE_DTO.name(), DeviceMessageDto.class);
        if (outputDeviceMessageDto != null) {
            Assert.assertNotNull(outputDeviceMessageDto.getEndpoint());
        }
    }

    private WorkflowContext buildContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.DEVICE_MESSAGE_DTO.name(), buildDeviceMessageDto(ENDPOINT1));
        return context;
    }

    private DeviceMessageDto buildDeviceMessageDto(String endpoint) {
        DeviceMessageDto deviceMessageDto = new DeviceMessageDto();
        deviceMessageDto.setEndpoint(endpoint);
        deviceMessageDto.getReduceRequestDtos().add(buildReduceRequestDto(PERIOD));
        deviceMessageDto.getReportRequestDtos().add(buildReportRequestDto(PERIOD));
        return deviceMessageDto;
    }

    private ReduceRequestDto buildReduceRequestDto(LocalDate period) {
        ReduceRequestDto reduceRequestDto = new ReduceRequestDto();
        reduceRequestDto.setDate(period);
        reduceRequestDto.setId(REDUCE_REQUEST_ID);
        reduceRequestDto.setStartDTU(BigInteger.ONE);
        reduceRequestDto.setEndDTU(BigInteger.valueOf(96));
        reduceRequestDto.setEventID(UDI_EVENT_ID);
        reduceRequestDto.setPower(BigInteger.valueOf(-1000));
        return reduceRequestDto;
    }

    private ReportRequestDto buildReportRequestDto(LocalDate period) {
        ReportRequestDto reportRequestDto = new ReportRequestDto();
        reportRequestDto.setDate(period);
        reportRequestDto.setId(REPORT_REQUEST_ID);
        reportRequestDto.setDtus("1,2,5,6");
        return reportRequestDto;
    }

}
