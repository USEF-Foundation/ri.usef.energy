/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.messageservice.transportservice.mqttmessages;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.CountDownLatch;

/**
 * a class containing the mqtt response message content
 * */
public class MqttResponseMessage
{
    private final String topic;
    private final CountDownLatch latch;
    private MqttMessage message;

    /**
     * A class that wraps the {@link MqttMessage},
     * in order to contain a topic where we expect a message for
     * and a {@link CountDownLatch} in order to notify another process that a message has been received
     * @param topic the topic we expect a response from
     * @param countDownCount the amount of countdown that will be performed, before a message has been successfully received
     * */
    public MqttResponseMessage(String topic, int countDownCount)
    {
        this.topic = topic;
        latch = new CountDownLatch(countDownCount);
    }

    public String getTopic(){return topic;}
    public CountDownLatch getLatch(){return latch;}
    public MqttMessage getMessage(){return message;}
    public void setMessage(MqttMessage message){this.message = message;}

}
