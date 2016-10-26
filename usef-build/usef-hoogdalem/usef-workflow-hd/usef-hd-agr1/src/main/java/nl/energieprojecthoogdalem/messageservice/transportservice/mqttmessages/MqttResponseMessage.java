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
