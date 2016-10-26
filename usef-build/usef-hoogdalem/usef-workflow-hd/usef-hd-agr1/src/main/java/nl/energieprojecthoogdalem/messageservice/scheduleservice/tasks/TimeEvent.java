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

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;

/**
 * a abstract class for time driven events for endpoints
 * */
public abstract class TimeEvent {

    private final Logger LOGGER = LoggerFactory.getLogger(TimeEvent.class);

    private final String endpoint;
    private final DateTime runDate;
    private final ScheduledFuture taskId;

    /**
     * class initialisation with following arguments
     * @param endpoint the endpoint or cgate serial to send to ex (MZ29EBX000)
     * @param runDate the day and time this task will be executed
     * @param taskId the scheduled executor service id to cancel tasks
     * */
    public TimeEvent(String endpoint, DateTime runDate, ScheduledFuture taskId )
    {
        this.endpoint = endpoint;
        this.runDate = runDate;
        this.taskId = taskId;
    }

    /**
     * cancels the task for this event
     * */
    public void cancel()
    {
        //while task is running and cancel is called, never interrupt
        if( taskId.cancel(false) )
            LOGGER.info("cancelled task for endpoint {} at date {}",endpoint ,runDate);

        else
            LOGGER.error("unable to cancel task for endpoint {} at date {}", endpoint, runDate);

    }

    /**
     * @return the task for this event
     * */
    public abstract Runnable getTask();

}
