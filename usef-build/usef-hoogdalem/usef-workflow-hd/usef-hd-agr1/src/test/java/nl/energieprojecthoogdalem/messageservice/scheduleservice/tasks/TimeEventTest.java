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