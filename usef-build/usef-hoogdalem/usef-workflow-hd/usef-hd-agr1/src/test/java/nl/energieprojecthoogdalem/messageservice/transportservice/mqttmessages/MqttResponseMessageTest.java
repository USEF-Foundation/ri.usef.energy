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