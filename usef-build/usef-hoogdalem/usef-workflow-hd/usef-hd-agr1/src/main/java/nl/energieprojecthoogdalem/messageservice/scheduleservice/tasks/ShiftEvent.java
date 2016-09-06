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
