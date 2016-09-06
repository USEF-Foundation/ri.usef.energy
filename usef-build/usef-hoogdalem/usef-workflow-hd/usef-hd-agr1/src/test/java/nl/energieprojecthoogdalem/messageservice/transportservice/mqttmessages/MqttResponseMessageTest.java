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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MqttResponseMessage.class)
public class MqttResponseMessageTest {

    private MqttResponseMessage mqttResponseMessage;

    private static final boolean RETAIN = true;
    private static final int QOS = 0
                        ,COUNTDOWN = 1
                        ;
    private static final String TOPIC = "t"
            , PAYLOAD = "p"
            ;

    @Mock
    private CountDownLatch latch;

    @Test
    public void testMessage() throws Exception
    {
        PowerMockito.whenNew(CountDownLatch.class).withArguments(Matchers.anyInt()).thenReturn(latch);

        mqttResponseMessage = new MqttResponseMessage(TOPIC, COUNTDOWN);
        assertEquals(TOPIC, mqttResponseMessage.getTopic());

        CountDownLatch resultLatch = mqttResponseMessage.getLatch();
        assertEquals(latch, resultLatch);

        MqttMessage message = new MqttMessage(PAYLOAD.getBytes());
        message.setQos(QOS);
        message.setRetained(RETAIN);

        mqttResponseMessage.setMessage(message);

        MqttMessage result = mqttResponseMessage.getMessage();

        assertEquals(QOS, result.getQos());
        assertEquals(PAYLOAD, result.toString());
    }
}