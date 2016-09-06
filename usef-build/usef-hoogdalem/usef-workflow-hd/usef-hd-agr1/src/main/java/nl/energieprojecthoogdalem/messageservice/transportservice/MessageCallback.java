/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.messageservice.transportservice;

import nl.energieprojecthoogdalem.messageservice.transportservice.mqttmessages.MqttResponseMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
/**
 * callback implementation of the paho {@link MqttCallback} interface
 * */
public class MessageCallback implements MqttCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCallback.class);

    private int attempts = 0;
    private static int delay = 2 * 1000, upperLimit = 60 * 1000;

    private final Map<String, MqttResponseMessage> receiveMap = Collections.synchronizedMap(new HashMap<>());


    @Inject
    private MqttConnection mqttConnection;

    /**
     * will be called by the {@link org.eclipse.paho.client.mqttv3.MqttClient} when a connection has been made and is lost
     * will also be called if {@link MqttConnection} fails to execute the connect() method
     * if connecting fails, this will wait a small time before attempting to reconnect
     * it will wait a maximum of one minute between reconnect attempts
     *
     * @param throwable the error that caused the connection loss
     */
    @Override
    public void connectionLost(Throwable throwable) {
        LOGGER.error("Lost connection to broker Reason: ", throwable);

        attempts++;
        int total = (attempts * delay < upperLimit) ? attempts * delay : upperLimit;

        try {
            //wait a fixed time with upper limit to reconnect
            Thread.sleep(total);

            Future<Boolean> isConnected = mqttConnection.connect();

            //wait a small delay until the async function is done
            while (!isConnected.isDone())
                Thread.sleep(delay);

            //if we reconnected successfully, reset reconnect attempts,
            //otherwise this function will be called again by mqttConnection.connect()
            if (isConnected.get())
                attempts = 0;
        } catch (InterruptedException exception) {
            LOGGER.error("mqtt connectionLost interrupt error during connect: ", exception);
            Thread.currentThread().interrupt();
        } catch (ExecutionException exception) {
            LOGGER.error("mqtt connectionLost execution error during connect: ", exception);
        }
    }

    /**
     * a callback fired from the {@link org.eclipse.paho.client.mqttv3.MqttClient},
     * when a message has been received for topics we have subscribed on,
     * looks in a local Map if a response is expected,
     * if found sets the message in the response,
     * and counts down a latch in the response in order to notify the waiting thread on the response
     *
     * @param topic   the topic we have subscribed on
     * @param message the {@link MqttMessage} received for the subscribed topic containing a string payload
     */
    @Override
    public void messageArrived(String topic, MqttMessage message)
    {
        //both map and message obj in map needs to be synchronized
        LOGGER.info("{} RECEIVED topic: {}", new DateTime(), topic);
        MqttResponseMessage received = receiveMap.remove(topic);
        if(received != null )
        {
            received.setMessage(message);
            received.getLatch().countDown();
        }
        //else not expecting a response or response not set
    }

    /**
     * a callback fired from the {@link org.eclipse.paho.client.mqttv3.MqttClient} when a message has been delivered, depending on QOS
     *
     * @param iMqttDeliveryToken the {@link IMqttDeliveryToken} containing the topic(s) and message that has been delivered
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        LOGGER.info("message delivered for {}", iMqttDeliveryToken.getTopics()[0]);
    }

    /**
     * adds a response message to the local receive map
     *
     * @param toReceive a response message with topic set to the expected subscription topic
     * */
    public void addReceiveMessage(MqttResponseMessage toReceive)
    {
        receiveMap.put(toReceive.getTopic() ,toReceive);
    }

    /**
     * removes a response message from the local receive map
     *
     * @param topic a subscription topic
     * */
    public void removeReceiveMessage(String topic)
    {
        receiveMap.remove(topic);
    }
}
