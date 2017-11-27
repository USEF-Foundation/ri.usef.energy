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