/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nl.energieprojecthoogdalem.messageservice.scheduleservice;

import info.usef.agr.dto.device.request.DeviceMessageDto;
import info.usef.agr.dto.device.request.ShiftRequestDto;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.tasks.*;
import nl.energieprojecthoogdalem.util.TimeUtil;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.enterprise.inject.Instance;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MessageScheduler.class)
public class MessageSchedulerTest
{

    private MessageScheduler messageScheduler;

    private static String SHIFT_REQUEST_ID = "85008580-4d4f-5363-6f6d-6d6f646f7265"
                        , SHIFT_EVENT_ID   = "41658173-656d-6963-6f6e-647563746f72"
                        , ENDPOINT = "TEST"
                        ;

    private static int ptuIndex = 80
                     , ptuDuration = 15
                     ;

    private static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    private DeviceMessageDto deviceMessageDtoToday, deviceMessageDtoTomorrow;

    private LocalDate today
                    , tomorrow
                    ;

    private Map<String, ShiftEvent> scheduledTasks;

    @Mock
    private ShiftEvent shiftEvent;

    @Mock
    private PrepareEvent prepareEvent;

    @Mock
    private Instance<ShiftTask> shiftTaskInstance;

    @Mock
    private Instance<PrepareTask> prepareTaskInstance;

    @Mock
    private ShiftTask shiftTask;

    @Mock
    private PrepareTask prepareTask;

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private ScheduledFuture scheduledFuture;

    @Before
    @SuppressWarnings("unchecked") public void setup() throws Exception
    {
        scheduledTasks = new HashMap<>();

        today = new LocalDate();
        tomorrow = today.plusDays(1);

        deviceMessageDtoToday = createDeviceMessage(today);
        deviceMessageDtoTomorrow = createDeviceMessage(tomorrow);

        messageScheduler = new MessageScheduler();

        Mockito.when(scheduledFuture.cancel(Matchers.anyBoolean())).thenReturn(true);

        Mockito.doNothing().when(scheduler).shutdown();
        Mockito.when(scheduler.schedule( Matchers.any(Runnable.class), Matchers.anyLong(), Matchers.any(TimeUnit.class))).thenReturn(scheduledFuture);

        PowerMockito.doNothing().when(shiftEvent).cancel();
        PowerMockito.doNothing().when(prepareEvent).cancel();
        PowerMockito.whenNew(PrepareEvent.class).withAnyArguments().thenReturn(prepareEvent);
        PowerMockito.whenNew(ShiftEvent.class).withAnyArguments().thenReturn(shiftEvent);

        Mockito.doNothing().when(shiftTask).run();
        Mockito.doNothing().when(prepareTask).run();
        Mockito.doReturn(shiftTask).when(shiftTaskInstance).get();
        Mockito.doReturn(prepareTask).when(prepareTaskInstance).get();


        Whitebox.setInternalState(messageScheduler, "scheduledTasks", scheduledTasks);
        Whitebox.setInternalState(messageScheduler, "scheduler", scheduler);

        Whitebox.setInternalState(messageScheduler, "shiftTaskInstance", shiftTaskInstance);
        Whitebox.setInternalState(messageScheduler, "prepareTaskInstance", prepareTaskInstance);
    }

    @After
    public void cleanup() throws Exception
    {
        messageScheduler.shutdown();
    }

    @Test
    public void testScheduleShiftTaskWithoutPrepare() throws Exception
    {
        messageScheduler.scheduleShiftTask(deviceMessageDtoToday.getEndpoint(), deviceMessageDtoToday.getShiftRequestDtos().get(0));

        Assert.assertEquals(1, scheduledTasks.size());
        Assert.assertNotNull( scheduledTasks.get(ENDPOINT + formatter.print(today) + MessageScheduler.SHIFT_SUFFIX) );
        Assert.assertNull( scheduledTasks.get(ENDPOINT + formatter.print(today)+ MessageScheduler.PREPARATION_SUFFIX) );

        messageScheduler.removeTask(ENDPOINT, today.toDateTime(TimeUtil.getLocalTimeFromPtuIndex(ptuIndex, ptuDuration)), MessageScheduler.PREPARATION_SUFFIX );

        Assert.assertEquals(1, scheduledTasks.size());
        Assert.assertNotNull( scheduledTasks.get(ENDPOINT + formatter.print(today)+ MessageScheduler.SHIFT_SUFFIX) );
    }

    @Test
    public void testOverrideScheduleShiftTaskWithoutPrepare() throws Exception
    {
        //test false case
        Mockito.when(scheduledFuture.cancel(Matchers.anyBoolean())).thenReturn(false);


        messageScheduler.scheduleShiftTask(deviceMessageDtoToday.getEndpoint(), deviceMessageDtoToday.getShiftRequestDtos().get(0));
        messageScheduler.scheduleShiftTask(deviceMessageDtoToday.getEndpoint(), deviceMessageDtoToday.getShiftRequestDtos().get(0));

        Assert.assertEquals(1, scheduledTasks.size());
        Assert.assertNotNull( scheduledTasks.get(ENDPOINT + formatter.print(today) + MessageScheduler.SHIFT_SUFFIX) );
        Assert.assertNull( scheduledTasks.get(ENDPOINT + formatter.print(today)+ MessageScheduler.PREPARATION_SUFFIX) );

    }

    @Test
    public void testScheduleShiftTaskWithPrepare() throws Exception
    {
        messageScheduler.scheduleShiftTask(deviceMessageDtoTomorrow.getEndpoint(), deviceMessageDtoTomorrow.getShiftRequestDtos().get(0));

        Assert.assertEquals(2, scheduledTasks.size());
        Assert.assertNotNull( scheduledTasks.get(ENDPOINT + formatter.print(tomorrow) + MessageScheduler.PREPARATION_SUFFIX) );
        Assert.assertNotNull( scheduledTasks.get(ENDPOINT + formatter.print(tomorrow) + MessageScheduler.SHIFT_SUFFIX) );

    }

    @Test
    public void testOverrideScheduleShiftTaskWithPrepare() throws Exception
    {
        messageScheduler.scheduleShiftTask(deviceMessageDtoTomorrow.getEndpoint(), deviceMessageDtoTomorrow.getShiftRequestDtos().get(0));
        messageScheduler.scheduleShiftTask(deviceMessageDtoTomorrow.getEndpoint(), deviceMessageDtoTomorrow.getShiftRequestDtos().get(0));

        Assert.assertEquals(2, scheduledTasks.size());
        Assert.assertNotNull( scheduledTasks.get(ENDPOINT + formatter.print(tomorrow) + MessageScheduler.PREPARATION_SUFFIX) );
        Assert.assertNotNull( scheduledTasks.get(ENDPOINT + formatter.print(tomorrow) + MessageScheduler.SHIFT_SUFFIX) );

        messageScheduler.removeTask(ENDPOINT, tomorrow.toDateTime(TimeUtil.getLocalTimeFromPtuIndex(ptuIndex, ptuDuration)), MessageScheduler.PREPARATION_SUFFIX );
        messageScheduler.removeTask(ENDPOINT, tomorrow.toDateTime(TimeUtil.getLocalTimeFromPtuIndex(ptuIndex, ptuDuration)), MessageScheduler.SHIFT_SUFFIX );

        Assert.assertEquals(0, scheduledTasks.size());
        Assert.assertNull( scheduledTasks.get(ENDPOINT + formatter.print(tomorrow)+ MessageScheduler.PREPARATION_SUFFIX) );
        Assert.assertNull( scheduledTasks.get(ENDPOINT + formatter.print(tomorrow)+ MessageScheduler.SHIFT_SUFFIX) );

    }

    private DeviceMessageDto createDeviceMessage(LocalDate date)
    {
        DeviceMessageDto deviceMessage = new DeviceMessageDto();
        deviceMessage.setEndpoint(ENDPOINT);

        deviceMessage.getShiftRequestDtos().add(createShift(date));
        return deviceMessage;
    }

    private ShiftRequestDto createShift(LocalDate date)
    {
        ShiftRequestDto shiftRequest = new ShiftRequestDto();

        shiftRequest.setId(SHIFT_REQUEST_ID);
        shiftRequest.setEventID(SHIFT_EVENT_ID);
        shiftRequest.setDate(date);
        shiftRequest.setStartDTU(BigInteger.valueOf(ptuIndex));

        return shiftRequest;

    }
}