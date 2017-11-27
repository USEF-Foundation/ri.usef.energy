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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * a task that converts a shift request device message from USEF RI to hoogdalem mqtt topics
 * */
public class ShiftTask implements Runnable
{
    @Inject
    private MqttConnection connection;

    @Inject
    private MessageScheduler scheduler;

    private final Logger LOGGER = LoggerFactory.getLogger(ShiftTask.class);
    private  String endpoint;
    private  DateTime date;

    /**
     * class initialisation with following arguments
     * @param endpoint the endpoint or cgate serial to send to ex (MZ29EBX000)
     * @param date the day and time this task will be executed
     * */
    public void init(String endpoint, DateTime date)
    {
        this.endpoint = endpoint;
        this.date = date;
    }

    /**
     * sends the value for normal operation to the cqate over mqtt
     * removes the task from the memory map in the scheduler
     * */
    @Override
    public void run()
    {
        LOGGER.info("Executing shift task for endpoint {} on {}", endpoint ,date);

        if(connection.isConnected() )
        {
            LOGGER.info("Publishing shift message for endpoint {} on {} ", endpoint ,date);
            connection.publish(endpoint + "/BatMode", "SetDefault");
        }
        else
            LOGGER.warn("Dropping shift message for endpoint {} on {}", endpoint, date);

        scheduler.removeTask(endpoint, date, MessageScheduler.SHIFT_SUFFIX);
    }
}
