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
import org.eclipse.paho.client.mqttv3.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MqttMessage.class)

public class MessageCallbackTest
{

    private MessageCallback messageCallback;

    private static String TOPIC = "MQTT/TOPIC"
                        , PAYLOAD = "PAYLOAD";

    private static int reconnectAttempts = 2;
    private int reconnectCount = 0;


    private MqttMessage message;

    @Spy
    private Map<String, MqttResponseMessage> receiveMap = new HashMap<>();

    @Mock
    private IMqttDeliveryToken deliveryToken;

    @Mock
    private Future<Boolean> isConnected;

    @Mock
    private MqttConnection mqttConnection;

    @Mock
    private Throwable throwable;

    @Before
    public void setUp() throws Exception
    {
        messageCallback = new MessageCallback();
        Whitebox.setInternalState(messageCallback, "mqttConnection", mqttConnection);
    }

    @Test
    public void testConnectionLost() throws Exception
    {
        Mockito.doReturn(true).when(isConnected).isDone();

        Mockito.when(mqttConnection.connect()).thenAnswer(invocation ->
        {
            reconnectCount++;
            if(reconnectCount < reconnectAttempts)
            {
                messageCallback.connectionLost(throwable);
                Mockito.doReturn(false).when(isConnected).get();
                return isConnected;
            }
            else
            {
                Mockito.doReturn(true).when(isConnected).get();
                return isConnected;
            }


        });

        messageCallback.connectionLost(throwable);
        Mockito.verify(mqttConnection, Mockito.times(reconnectAttempts)).connect();
    }

    @Test
    public void testMessageArrived() throws Exception
    {
        Whitebox.setInternalState(messageCallback, "receiveMap", receiveMap);

        MqttResponseMessage responseMessage = new MqttResponseMessage(TOPIC, 1);
        messageCallback.addReceiveMessage(responseMessage);

        Assert.assertEquals(responseMessage, receiveMap.get(TOPIC));

        message = PowerMockito.spy(new MqttMessage());
        message.setPayload(PAYLOAD.getBytes());

        messageCallback.messageArrived(TOPIC, message);

        Assert.assertEquals(0L, responseMessage.getLatch().getCount());
        Assert.assertEquals(message, responseMessage.getMessage());
        Assert.assertEquals(PAYLOAD, responseMessage.getMessage().toString());

        messageCallback.removeReceiveMessage(TOPIC) ;
    }

    @Test
    public void testDeliveryComplete() throws Exception
    {
        String[] strings = {TOPIC};
        Mockito.doReturn(strings).when(deliveryToken).getTopics();

        messageCallback.deliveryComplete(deliveryToken);

        Mockito.verify(deliveryToken, Mockito.times(1) ).getTopics();
    }
}