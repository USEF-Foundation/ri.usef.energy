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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.TimeUnit;

@RunWith(PowerMockRunner.class)
//@PrepareForTest(MqttConnection.class)
public class RealMqttConnectionTest
{
    private static String TOPIC = "TEST/usef/GetActual"
            , MESSAGE   = "get"
            , RECV   = "/usef/actual"
            ;

    private MqttConnection connection;
    private MessageCallback messageCallback;

    @Test
    public void testSubscribeAndSend() throws Exception
    {
        messageCallback = new MessageCallback();
        connection = new MqttConnection();

        Whitebox.setInternalState(messageCallback, "mqttConnection", connection);

        Whitebox.setInternalState(connection, "messageCallback", messageCallback);
        Whitebox.setInternalState(connection, "mqttConnection", connection);

        connection.init();

        if(connection.isConnected())
        {
            MqttResponseMessage responseMessage = new MqttResponseMessage("TEST" + RECV, 1);
            connection.subscribe('+' + RECV);
            connection.publishAndReceive(TOPIC, MESSAGE, responseMessage);

            responseMessage.getLatch().await( 5 *1000, TimeUnit.MILLISECONDS);

            if(responseMessage.getMessage() != null)
                System.out.println("received " + responseMessage.getMessage().toString());

            connection.unsubscribe('+' + RECV);
            connection.disconnect();
        }
    }
}