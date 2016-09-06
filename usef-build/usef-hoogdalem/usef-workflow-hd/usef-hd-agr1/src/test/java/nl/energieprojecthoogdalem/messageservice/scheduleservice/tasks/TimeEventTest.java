/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.messageservice.scheduleservice.tasks;

import nl.energieprojecthoogdalem.messageservice.scheduleservice.MessageScheduler;
import nl.energieprojecthoogdalem.messageservice.transportservice.MqttConnection;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.ScheduledFuture;

@RunWith(MockitoJUnitRunner.class)
public class TimeEventTest
{
    private static String ENDPOINT = "TEST/usef/BATTERY"
                        , TOPIC_BATMODE = "/BatMode"
                        , TOPIC_SETPOWER = "/SetPowerBat"
                    ;

    @Mock
    private MessageScheduler scheduler;

    @Mock
    private MqttConnection connection;

    @Mock
    ScheduledFuture scheduledFuture;

    private ShiftEvent shiftEvent;
    private PrepareEvent prepareEvent;

    private ShiftTask shiftTask;
    private PrepareTask prepareTask;


    private DateTime date;

    @Before
    public void setUp() throws Exception
    {
        Mockito.doNothing().when(scheduler).removeTask(Matchers.anyString(), Matchers.any(DateTime.class), Matchers.anyString());
        Mockito.when(scheduledFuture.cancel(Matchers.anyBoolean())).thenReturn(false, true);

        date = new DateTime();

        prepareTask = new PrepareTask();
        prepareTask.init(ENDPOINT, date);

        shiftTask = new ShiftTask();
        shiftTask.init(ENDPOINT, date);

        shiftEvent = new ShiftEvent(ENDPOINT, date, scheduledFuture, shiftTask );
        prepareEvent = new PrepareEvent(ENDPOINT, date, scheduledFuture, prepareTask );

        Whitebox.setInternalState(prepareTask, "connection", connection);
        Whitebox.setInternalState(prepareTask, "scheduler", scheduler);
        Whitebox.setInternalState(shiftTask, "connection", connection);
        Whitebox.setInternalState(shiftTask, "scheduler", scheduler);
    }

    @Test
    public void testRunTasksConnected() throws Exception
    {
        Mockito.doReturn(true).when(connection).isConnected();

        prepareTask.run();

        shiftTask.run();

        Mockito.verify(connection, Mockito.times(1)).publish(ENDPOINT + TOPIC_BATMODE, "MAN");
        Mockito.verify(connection, Mockito.times(1)).publish(ENDPOINT + TOPIC_BATMODE, "SetDefault");
        Mockito.verify(connection, Mockito.times(1)).publish(ENDPOINT + TOPIC_SETPOWER, "0");

        Mockito.verify(connection, Mockito.times(2)).isConnected();
        Mockito.verify(scheduler, Mockito.times(2)).removeTask(Matchers.anyString(), Matchers.any(DateTime.class), Matchers.anyString());
    }

    @Test
    public void testRunTasksDisconnected() throws Exception
    {
        Mockito.doReturn(false).when(connection).isConnected();

        prepareTask.run();

        shiftTask.run();

        Mockito.verify(connection, Mockito.times(2)).isConnected();
        Mockito.verify(connection, Mockito.never()).publish(Matchers.anyString(), Matchers.anyString());

        Mockito.verify(scheduler, Mockito.times(2)).removeTask(Matchers.anyString(), Matchers.any(DateTime.class), Matchers.anyString());
    }

    @Test
    public void testCancelTasks()
    {
        shiftEvent.cancel();
        prepareEvent.cancel();
        Mockito.verify(scheduledFuture, Mockito.times(2) ).cancel(Matchers.anyBoolean());
    }

}