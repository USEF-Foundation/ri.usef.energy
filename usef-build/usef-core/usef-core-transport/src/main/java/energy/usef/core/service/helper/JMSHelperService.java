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

package energy.usef.core.service.helper;

import energy.usef.core.constant.USEFConstants;
import energy.usef.core.constant.USEFLogCategory;
import energy.usef.core.exception.TechnicalException;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMS Service. This service implements the sending a message to a jms queue.
 */
@Stateless
public class JMSHelperService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JMSHelperService.class);
    private static final Logger LOGGER_CONFIDENTIAL = LoggerFactory.getLogger(USEFLogCategory.CONFIDENTIAL);

    @Resource(mappedName = USEFConstants.OUT_QUEUE_NAME)
    private Queue outQueue;

    @Resource(mappedName = USEFConstants.IN_QUEUE_NAME)
    private Queue inQueue;

    @Resource(mappedName = USEFConstants.NOT_SENT_QUEUE_NAME)
    private Queue notSentQueue;

    @Inject
    private JMSContext context;

    private void sendMessage(Queue queue, String message) {
        try {
            context.createProducer().send(queue, message);
        } catch (Exception e) {
            LOGGER.error("Error sending the message: ", e);
            LOGGER_CONFIDENTIAL.debug("Error sending the message: '{}' to the queue", message, e);
            throw new TechnicalException(e);
        }
    }

    /**
     * Sets a message to the out queue.
     *
     * @param message message
     */
    public void sendMessageToOutQueue(String message) {
        LOGGER.debug("Started sending msg to the out queue {}");
        sendMessage(outQueue, message);
        LOGGER.debug("Msg is successfully sent to the out queue");
    }

    /**
     * Sets a message to the in queue.
     *
     * @param message message
     */
    public void sendMessageToInQueue(String message) {
        LOGGER.debug("Started sending msg to the in queue");
        sendMessage(inQueue, message);
        LOGGER.debug("Msg is successfully sent to the in queue");
    }

    /**
     * Sets a message to the not sent queue.
     *
     * @param message message
     */
    public void sendMessageToNotSentQueue(String message) {
        LOGGER.debug("Started sending msg to the not sent queue");
        sendMessage(notSentQueue, message);
        LOGGER.debug("Msg is successfully sent to the not sent queue");
    }
}
