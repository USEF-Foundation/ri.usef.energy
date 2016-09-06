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

import info.usef.agr.dto.device.request.ShiftRequestDto;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.tasks.PrepareEvent;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.tasks.PrepareTask;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.tasks.TimeEvent;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.tasks.ShiftEvent;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.tasks.ShiftTask;
import nl.energieprojecthoogdalem.util.TimeUtil;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The message scheduler class for sending mqtt messages to endpoints
 * */
@Startup
@Singleton
public class MessageScheduler
{
    public static final String PREPARATION_SUFFIX = "-prep";
    public static final String SHIFT_SUFFIX = "-shift";

    private Logger LOGGER = LoggerFactory.getLogger(MessageScheduler.class);
    private static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    private final Map<String, TimeEvent> scheduledTasks = Collections.synchronizedMap(new HashMap<>());

    private static final int DTU_DURATION = 15;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    @Inject
    private Instance<PrepareTask> prepareTaskInstance;

    @Inject
    private Instance<ShiftTask> shiftTaskInstance;

    /**
     * schedules a task from the USEF RI shift request device message
     * also schedules a prepare message to stop normal operation
     * note: prepare message is executed at 06:00 of the day from the shift request
     * @param endpoint the cgate for this task ex (MZ29EBX000)
     * @param shiftRequestDto the shift request with a date and start ptu for this task
     * */
    public void scheduleShiftTask(String endpoint ,ShiftRequestDto shiftRequestDto)
    {
        DateTime runDate = calculateSendDateTime(shiftRequestDto);

        synchronized(scheduledTasks)
        {
            TimeEvent shiftEvent = scheduledTasks.remove(endpoint + formatter.print(runDate) + SHIFT_SUFFIX);

            // 1) a shift request for a day that doesn't exist
            // 2) a shift request for another day
            if(shiftEvent == null)
            {
                DateTime prepareDate = calculateSendDateTime(shiftRequestDto, 25);
                long durationMillis = fromNow(prepareDate).getMillis();
                if(durationMillis > 0)
                {
                    PrepareTask prepareTask = prepareTaskInstance.get();
                    prepareTask.init(endpoint, prepareDate);

                    ScheduledFuture prepareTaskId = scheduler.schedule(prepareTask, durationMillis, TimeUnit.MILLISECONDS);

                    scheduledTasks.put(endpoint + formatter.print(prepareDate) + PREPARATION_SUFFIX, new PrepareEvent(endpoint, prepareDate, prepareTaskId, prepareTask) );
                    LOGGER.info("created prepare message");

                }
                else
                    LOGGER.info("no prepare message created, past 06:00 of scheduled day");

                newShiftTask(endpoint, runDate);
            }
            //a new shift request for the same day, override previous shift request
            else
            {
                shiftEvent.cancel();
                newShiftTask(endpoint, runDate);
            }
        }

    }

    /**
     * removes a task from the scheduler memory map
     * @param endpoint the cgate to remove the task from ex (MZ29EBX000)
     * @param date the date and time from the task to remove
     * @param suffix the suffix for the task type
     * */
    public void removeTask(String endpoint, DateTime date, String suffix)
    {
        TimeEvent event = scheduledTasks.remove(endpoint + formatter.print(date) + suffix);

        if(event != null)
        {
            LOGGER.info("removing {} event for endpoint {} on date {}", suffix, endpoint, date);
            switch(suffix)
            {
                case PREPARATION_SUFFIX:
                    prepareTaskInstance.destroy( (PrepareTask) event.getTask() );
                    break;

                case SHIFT_SUFFIX:
                    shiftTaskInstance.destroy( (ShiftTask) event.getTask() );
                    break;
            }
        }
        else
            LOGGER.error("Unable to remove {} event for endpoint {} on date {}", suffix, endpoint, date);
    }

    /**
     * creates a new shift task
     * @param endpoint the cgate for this task
     * @param runDate the date converted from the shift request date and start ptu time
     * */
    private void newShiftTask(String endpoint ,DateTime runDate)
    {
        LOGGER.info("new shift request for endpoint {} on date {}", endpoint, runDate);
        ShiftTask task = shiftTaskInstance.get();

        task.init(endpoint, runDate);

        long durationMillis = fromNow(runDate).getMillis();
        LOGGER.info("Duration is {} minutes or {} ms", (durationMillis / (1000L *60L)), durationMillis);

        ScheduledFuture taskId = scheduler.schedule(task, durationMillis, TimeUnit.MILLISECONDS);

        scheduledTasks.put(endpoint + formatter.print(runDate) + SHIFT_SUFFIX, new ShiftEvent(endpoint, runDate, taskId, task));
    }

    /**
     * calculates the duration from the current time
     * @param target the end {@link DateTime} to calculate the duration
     * @return {@link Duration} the duration between now and the target time in MS
     * */
    private Duration fromNow(DateTime target )
    {
        return new Duration(null, target);
    }

    /**
     * calculates the date and time from a shift request device message
     * @param shiftRequestDto the usef {@link ShiftRequestDto} containing the date and start ptu
     * @return {@link DateTime} the date and time to execute the task
     * */
    private DateTime calculateSendDateTime(ShiftRequestDto shiftRequestDto)
    {
        return shiftRequestDto.getDate()
                              .toDateTime(TimeUtil.getLocalTimeFromPtuIndex( shiftRequestDto.getStartDTU().intValue(), DTU_DURATION) );

    }

    /**
     * calculates the date and time from a shift request device message and a given ptu index
     * @param shiftRequestDto the usef {@link ShiftRequestDto} containing the date
     * @param ptuIndex the index to calculate the time from
     * @return {@link DateTime} the date and time to execute the task
     * */
    private DateTime calculateSendDateTime(ShiftRequestDto shiftRequestDto, int ptuIndex)
    {
        return shiftRequestDto.getDate()
                              .toDateTime(TimeUtil.getLocalTimeFromPtuIndex( ptuIndex, DTU_DURATION) );

    }

    /**
     * stops the scheduled executor service from planning more tasks
     * note: should only be called by EJB using @PreDestroy
     * */
    @PreDestroy
    public void shutdown()
    {
        scheduler.shutdown();
        LOGGER.info("ADS message scheduler has been shut down.");
    }
}
