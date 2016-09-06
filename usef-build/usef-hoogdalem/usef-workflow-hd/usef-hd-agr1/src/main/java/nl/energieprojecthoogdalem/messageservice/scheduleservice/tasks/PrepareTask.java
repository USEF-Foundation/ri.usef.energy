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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * a task that is required for a shift request device message from USEF RI to hoogdalem mqtt topics
 * */
public class PrepareTask implements Runnable
{
    @Inject
    private MqttConnection connection;

    @Inject
    private MessageScheduler scheduler;

    private final Logger LOGGER = LoggerFactory.getLogger(PrepareTask.class);
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
     * sends the value, stopping the normal operation to the cqate over mqtt
     * removes the task from the memory map in the scheduler
     * */
    @Override
    public void run()
    {
        LOGGER.info("Executing prepare task for endpoint {} on {}", endpoint ,date);

        if(connection.isConnected() )
        {
            LOGGER.info("Publishing prepare message for endpoint {} on {} ", endpoint ,date);
            connection.publish(endpoint + "/BatMode", "MAN");
            connection.publish(endpoint + "/SetPowerBat", "0");
        }
        else
            LOGGER.warn("Dropping prepare message for endpoint {} on {}", endpoint, date);

        scheduler.removeTask(endpoint, date, MessageScheduler.PREPARATION_SUFFIX);
    }
}
