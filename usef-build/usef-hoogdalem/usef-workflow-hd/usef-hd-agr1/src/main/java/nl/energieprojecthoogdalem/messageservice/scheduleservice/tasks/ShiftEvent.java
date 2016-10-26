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

import java.util.concurrent.ScheduledFuture;

/**
 * the shift class for shift request events for endpoints at a specific date
 * */
public class ShiftEvent extends TimeEvent
{
    private final ShiftTask shiftTask;

    /**
     * class initialisation with following arguments
     * @param endpoint the endpoint or cgate serial to send to ex (MZ29EBX000)
     * @param runDate the day and time this task will be executed
     * @param taskId the scheduled executor service id to cancel tasks
     * @param shiftTask the task for this shift event
     * */
    public ShiftEvent(String endpoint, DateTime runDate, ScheduledFuture taskId, ShiftTask shiftTask )
    {
        super(endpoint, runDate, taskId);
        this.shiftTask = shiftTask;
    }

    /**
     * @return the {@link ShiftTask} for this event
     * */
    @Override
    public ShiftTask getTask() {
        return shiftTask;
    }
}
