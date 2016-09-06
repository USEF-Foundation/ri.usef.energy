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
import info.usef.core.util.PtuUtil;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.IN;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.MessageScheduler;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.tasks.PrepareTask;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.tasks.ShiftTask;
import nl.energieprojecthoogdalem.messageservice.transportservice.MessageCallback;
import nl.energieprojecthoogdalem.messageservice.transportservice.MqttConnection;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;


import javax.enterprise.inject.Instance;
import java.math.BigInteger;

@RunWith(PowerMockRunner.class)

public class RealMessageTest
{
    private static String SHIFT_REQUEST_ID = "85008580-4d4f-5363-6f6d-6d6f646f7265"
                        , SHIFT_EVENT_ID   = "41658173-656d-6963-6f6e-647563746f72"
                        , ENDPOINT = "TEST"
                        ;

    private ControlActiveDemandSupply controlActiveDemandSupply;

    private WorkflowContext context;

    private DeviceMessageDto deviceMessageDto;

    private LocalDateTime dateTime;

    private MessageCallback messageCallback;

    private MessageScheduler scheduler;
    private MqttConnection connection;


    @Mock
    private Instance<ShiftTask> shiftTaskInstance;

    @Mock
    private Instance<PrepareTask> prepareTaskInstance;

    private ShiftTask shiftTask;
    private PrepareTask prepareTask;

    @Before
    public void setup() throws Exception
    {
        PowerMockito.doAnswer(invocation ->
        {
            shiftTask = new ShiftTask();
            shiftTask.init(ENDPOINT, dateTime.toDateTime());
            Whitebox.setInternalState(shiftTask, "scheduler", scheduler);
            Whitebox.setInternalState(shiftTask, "connection", connection);
            return shiftTask;
        })
                    .when(shiftTaskInstance).get();

        PowerMockito.doAnswer(invocation -> {
                    prepareTask = new PrepareTask();
                    prepareTask.init(ENDPOINT,dateTime.toDateTime());
                    Whitebox.setInternalState(prepareTask, "scheduler", scheduler);
                    Whitebox.setInternalState(prepareTask, "connection", connection);
                    return prepareTask;
                })
                    .when(prepareTaskInstance).get() ;

        messageCallback = new MessageCallback();

        scheduler = new MessageScheduler();
        Whitebox.setInternalState(scheduler, "shiftTaskInstance", shiftTaskInstance);
        Whitebox.setInternalState(scheduler, "prepareTaskInstance", prepareTaskInstance);

        connection = new MqttConnection();

        Whitebox.setInternalState(connection, "mqttConnection", connection);
        Whitebox.setInternalState(connection, "messageCallback", messageCallback);


        connection.init();

        controlActiveDemandSupply = new ControlActiveDemandSupply();
        Whitebox.setInternalState(controlActiveDemandSupply, "scheduler", scheduler);

        dateTime = new LocalDateTime().plusSeconds(2);
        System.out.println("scheduling for " + dateTime);

        deviceMessageDto = createDeviceMessage();

        context = new DefaultWorkflowContext();
        context.setValue(IN.DEVICE_MESSAGE_DTO.name(), deviceMessageDto);

    }

    @Test
    public void testInvokeAndSend() throws Exception
    {
        controlActiveDemandSupply.invoke(context);
        Thread.sleep(10 *1000);
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
        shiftRequest.setDate(dateTime.toLocalDate());
        shiftRequest.setStartDTU( BigInteger.valueOf( PtuUtil.getPtuIndex(dateTime, 15) ) );

        return shiftRequest;

    }

}