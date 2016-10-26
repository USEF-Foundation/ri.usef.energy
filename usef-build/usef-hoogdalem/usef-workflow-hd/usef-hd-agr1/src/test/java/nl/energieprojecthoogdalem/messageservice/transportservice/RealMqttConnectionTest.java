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