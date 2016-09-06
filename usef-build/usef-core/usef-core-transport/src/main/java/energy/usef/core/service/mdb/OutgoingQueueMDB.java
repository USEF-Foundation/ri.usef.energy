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

package energy.usef.core.service.mdb;

import energy.usef.core.constant.USEFLogCategory;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.service.rest.sender.SenderService;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Message Driven Bean asynchronously receives and processes the messages that are sent to the out queue.
 */
public class OutgoingQueueMDB implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingQueueMDB.class);
    private static final Logger LOGGER_CONFIDENTIAL = LoggerFactory.getLogger(USEFLogCategory.CONFIDENTIAL);

    @Inject
    private SenderService senderService;

    /**
     ** Passes a message to the listener.
     *
     * @param rcvMessage the message passed to the listener
     */
    public void onMessage(javax.jms.Message rcvMessage) {
        try {
            if (rcvMessage instanceof TextMessage) {
                TextMessage message = (TextMessage) rcvMessage;
                LOGGER_CONFIDENTIAL.debug("Received Message from queue: {}", message.getText());

                senderService.sendMessage(message.getText());

            } else {
                String errorMessage = "Message of wrong type: "
                        + rcvMessage.getClass().getName();
                LOGGER.warn(errorMessage);
                throw new TechnicalException(errorMessage);
            }
        } catch (JMSException e) {
            LOGGER.error("Error receiving a message", e);
            throw new TechnicalException(e);
        }
    }

}
