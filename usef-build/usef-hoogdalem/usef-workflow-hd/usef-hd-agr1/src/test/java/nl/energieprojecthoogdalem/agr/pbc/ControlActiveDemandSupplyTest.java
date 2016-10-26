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
package nl.energieprojecthoogdalem.agr.pbc;

import info.usef.agr.dto.device.request.DeviceMessageDto;
import info.usef.agr.dto.device.request.ShiftRequestDto;
import info.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.IN;
import info.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.OUT;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.MessageScheduler;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigInteger;

@RunWith(MockitoJUnitRunner.class)
public class ControlActiveDemandSupplyTest
{

    private ControlActiveDemandSupply controlActiveDemandSupply;

    private static String SHIFT_REQUEST_ID = "85008580-4d4f-5363-6f6d-6d6f646f7265"
                        , SHIFT_EVENT_ID   = "41658173-656d-6963-6f6e-647563746f72"
                        , ENDPOINT = "TEST"
    ;

    private DeviceMessageDto deviceMessageDto;
    private WorkflowContext context;

    @Mock
    private MessageScheduler scheduler;

    @Before
    public void setup() throws Exception
    {
        controlActiveDemandSupply = new ControlActiveDemandSupply();

        deviceMessageDto = createDeviceMessage();

        context = new DefaultWorkflowContext();
        context.setValue(IN.DEVICE_MESSAGE_DTO.name(), deviceMessageDto );

        Mockito.doNothing().when(scheduler).scheduleShiftTask(ENDPOINT, deviceMessageDto.getShiftRequestDtos().get(0));

        Whitebox.setInternalState(controlActiveDemandSupply, "scheduler", scheduler);
    }

    @Test
    public void testInvoke() throws Exception
    {
        WorkflowContext result = controlActiveDemandSupply.invoke(context);

        Mockito.verify(scheduler, Mockito.atLeastOnce()).scheduleShiftTask(ENDPOINT, deviceMessageDto.getShiftRequestDtos().get(0) );

        Assert.assertNull(result.getValue(OUT.FAILED_DEVICE_MESSAGE_DTO.name()));
    }

    private DeviceMessageDto createDeviceMessage()
    {
        DeviceMessageDto deviceMessage = new DeviceMessageDto();
        deviceMessage.setEndpoint(ENDPOINT);

        deviceMessage.getShiftRequestDtos().add(createShift());
        return deviceMessage;
    }

    private ShiftRequestDto createShift()
    {
        ShiftRequestDto shiftRequest = new ShiftRequestDto();

        shiftRequest.setId(SHIFT_REQUEST_ID);
        shiftRequest.setEventID(SHIFT_EVENT_ID);
        shiftRequest.setDate(new LocalDate(2016, 2 ,11));
        shiftRequest.setStartDTU(BigInteger.ONE);

        return shiftRequest;

    }
}