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

package energy.usef.core.controller;

import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.error.MessageControllerError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base controller class. This class is responsible for the general operations related to incoming response xml messages
 * processing.
 * <p>
 * The difference with {@link BaseIncomingMessageController} is that there is an additional check in the database to verify that
 * the
 * incoming response message is part of a conversation.
 *
 * @param <T> type of {@link Message} handled by the {@link BaseIncomingMessageController}
 */
public abstract class BaseIncomingResponseMessageController<T extends Message> extends BaseIncomingMessageController<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseIncomingResponseMessageController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(String xml, T message) throws BusinessException {
        // verify that a initial message related to the response has been sent first
        energy.usef.core.model.Message firstMessageOfConversation = findFirstMessageOfConversation(message);
        if (firstMessageOfConversation == null) {
            LOGGER.error("Message from {} {} contains an invalid ConversationID, messageId is : {}",
                    message.getMessageMetadata().getSenderRole(), message.getMessageMetadata().getSenderDomain(),
                    message.getMessageMetadata().getMessageID());
            throw new BusinessException(MessageControllerError.RESPONSE_MESSAGE_NOT_PART_OF_A_CONVERSATION);
        }

        // continue with the normal controller process.
        super.execute(xml, message);
    }

    protected energy.usef.core.model.Message findFirstMessageOfConversation(T message) {
        return messageService.getInitialMessageOfConversation(message.getMessageMetadata().getConversationID());
    }

}
