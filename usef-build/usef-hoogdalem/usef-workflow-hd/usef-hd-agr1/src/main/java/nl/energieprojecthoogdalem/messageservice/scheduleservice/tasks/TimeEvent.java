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
