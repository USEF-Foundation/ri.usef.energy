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

package energy.usef.core.service.business;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.model.Message;
import energy.usef.core.model.MessageDirection;
import energy.usef.core.model.MessageError;
import energy.usef.core.repository.MessageErrorRepository;
import energy.usef.core.repository.MessageRepository;

/**
 * Business Service class in charge of operations concerning the {@link Message} entities.
 */
@Stateless
public class MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private MessageErrorRepository messageErrorRepository;

    @Inject
    private Config config;

    /**
     * Creates message entity.
     *
     * @param xml The original XML message
     * @param dtoMessage The JAXB object representation of the xml message
     * @param direction Whether the direction is incoming or outgoing
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Message storeMessage(String xml, energy.usef.core.data.xml.bean.message.Message dtoMessage, MessageDirection direction) {
        Message message = new Message(xml, dtoMessage, direction);
        messageRepository.persist(message);

        LOGGER.debug("Saved {} message with ID [{}] in the database.", message.getDirection().toString(), message.getId());
        return message;
    }

    /**
     * Gets an ingoing message entity corresponding to an response message.
     *
     * @param conversationId conversation Id
     * @return ingoing message entity corresponding to an response message
     */
    public Message getMessageResponseByConversationId(String conversationId) {
        return messageRepository.getMessageResponseByConversationId(conversationId);
    }

    /**
     * Gets the first outgoing message of a conversation based on a conversation ID.
     *
     * @param conversationId {@link String} Conversation ID
     * @return a Message or <code>null</code> if not present in the database.
     */
    public Message getInitialMessageOfConversation(String conversationId) {
        return messageRepository.getInitialMessageOfConversation(conversationId);
    }

    /**
     * Creates message error entity.
     *
     * @param outMessage out message
     * @param errorMessage error message string
     * @param errorCode HTTP error code
     * @return message error entity
     */
    public MessageError storeMessageError(Message outMessage, String errorMessage, Integer errorCode) {
        String strippedErrorMessage = errorMessage;
        MessageError outMessageError = null;
        if (errorMessage.length() > config.getIntegerProperty(ConfigParam.MAX_ERROR_MESSAGE_LENGTH)) {
            strippedErrorMessage = errorMessage.substring(0, config.getIntegerProperty(ConfigParam.MAX_ERROR_MESSAGE_LENGTH));
        }
        // remove html from error message
        if (strippedErrorMessage.contains("\r\n")) {
            strippedErrorMessage = strippedErrorMessage.substring(0, strippedErrorMessage.indexOf("\r\n"));
        }

        if (outMessage != null) {
            outMessageError = new MessageError();
            outMessageError.setMessage(outMessage);
            outMessageError.setErrorCode(errorCode);
            outMessageError.setErrorMessage(strippedErrorMessage);

            messageErrorRepository.persist(outMessageError);
        }

        return outMessageError;
    }

    /**
     * Finds a {@link Message} based on its {@link Message#getMessageId()} value.
     *
     * @param messageId - {@link String} UUID of the message
     * @return a Message or <code>null</code> if no message with the messageId is in the database.
     */
    public boolean isMessageIdAlreadyUsed(String messageId) {
        if (messageId == null) {
            return false;
        }
        return messageRepository.isMessageIdAlreadyUsed(messageId);
    }

    /**
     * Checks whether each outbound {@link CommonReferenceQuery} message has a related inbound {@link
     * CommonReferenceQueryResponse} for the given creation time.
     *
     * @param creationTime {@link LocalDateTime} creation time of the common reference query.
     * @return <code>true</code> if every query has a response.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean hasEveryCommonReferenceQuerySentAResponseReceived(LocalDateTime creationTime) {
        return messageRepository.hasEveryCommonReferenceQuerySentAResponseReceived(creationTime);
    }
}
