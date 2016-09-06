/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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