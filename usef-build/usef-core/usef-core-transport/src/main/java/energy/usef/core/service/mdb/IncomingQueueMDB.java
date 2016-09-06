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
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.service.helper.DispatcherHelperService;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Message Driven Bean asynchronously receives and processes the messages that are sent to the in queue.
 */
public class IncomingQueueMDB implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(IncomingQueueMDB.class);
    private static final Logger LOGGER_CONFIDENTIAL = LoggerFactory.getLogger(USEFLogCategory.CONFIDENTIAL);

    @Inject
    private DispatcherHelperService dispatcherService;

    /**
     * Passes a message to the listener.
     *
     * @param messageReceived the message passed to the listener
     */
    @Override
    public void onMessage(Message messageReceived) {
        try {
            if (messageReceived instanceof TextMessage) {
                TextMessage message = (TextMessage) messageReceived;

                LOGGER_CONFIDENTIAL.debug("Received Message from queue: {}", message.getText());
                dispatcherService.dispatch(message.getText());

            } else {
                String errorStr = "Message of wrong type: "
                        + messageReceived.getClass().getName();
                LOGGER.error(errorStr);
                throw new TechnicalException(errorStr);
            }
        } catch (BusinessException e) {
            LOGGER.error("Error processing incoming XML: {}", e.getBusinessError().getError());
            throw new TechnicalException(e);
        } catch (JMSException e) {
            LOGGER.error("Error receiving a message", e);
            throw new TechnicalException(e);
        }
    }

}
