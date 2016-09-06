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

import energy.usef.core.data.participant.ParticipantType;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.SignedMessage;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.error.IncomingMessageError;
import energy.usef.core.util.DateTimeUtil;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class in charge of business validations on incoming messages (before they are sent to the incoming queue).
 */
@Stateless
public class IncomingMessageVerificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncomingMessageVerificationService.class);

    @Inject
    private ParticipantDiscoveryService participantDiscoveryService;

    @Inject
    private MessageService messageService;

    @Inject
    private SignedMessageHashService signedMessageHashService;

    /**
     * Perform the validations concerning the sender of the incoming message.
     *
     * @param signedMessage - {@link SignedMessage}
     * @param incomingMessage - {@link Message}
     * @throws BusinessException if an business error is encountered
     */
    public void validateSender(SignedMessage signedMessage, Message incomingMessage) throws BusinessException {
        MessageMetadata messageMetadata = incomingMessage.getMessageMetadata();

        if (!(signedMessage.getSenderRole().equals(messageMetadata.getSenderRole())
        && signedMessage.getSenderDomain().equals(messageMetadata.getSenderDomain()))) {
            throw new BusinessException(IncomingMessageError.MESSAGE_CONTENT_INVALID_SENDER);
        }

        participantDiscoveryService.discoverParticipant(incomingMessage, ParticipantType.SENDER);
    }

    /**
     * Checks whether the message is using an already existing messageId.
     *
     * @param messageId - {@link String} UUID of the incoming message
     * @throws BusinessException if the {@link MessageMetadata#getMessageID()} of the message is already present in the database.
     */
    public void validateMessageId(String messageId) throws BusinessException {
        if (messageService.isMessageIdAlreadyUsed(messageId)) {
            LOGGER.warn("Message ID is already used but content is different: {}", messageId);
            throw new BusinessException(IncomingMessageError.MESSAGE_ID_ALREADY_USED);
        }
    }

    /**
     * Checks whether the message is received.
     *
     * @param validUntil - {@link LocalDateTime} when the validity of this message ends.
     * @throws BusinessException if the {@link MessageMetadata#getMessageID()} of the message is already present in the database.
     */
    public void validateMessageValidUntil(LocalDateTime validUntil) throws BusinessException {

        if (validUntil != null && DateTimeUtil.getCurrentDateTime().isAfter(validUntil)) {
            LOGGER.warn("ValidUntil is expired: {}", validUntil.toString());
            throw new BusinessException(IncomingMessageError.MESSAGE_EXPIRED);
        }
    }

    /**
     * Checks whether the Sha256 hash of an incoming signed message is not present already. If already present, a BusinessException
     * with {@link IncomingMessageError#ALREADY_RECEIVED_AND_SUCCESSFULLY_PROCESSED} is thrown. Otherwise, create a new entry in the
     * database and exits the method.
     *
     * @param hashedContent - Byte array with the Sha256 of the incoming signed message.
     * @throws BusinessException
     */
    public void checkSignedMessageHash(byte[] hashedContent) throws BusinessException {
        if (signedMessageHashService.isSignedMessageHashAlreadyPresent(hashedContent)) {
            throw new BusinessException(IncomingMessageError.ALREADY_RECEIVED_AND_SUCCESSFULLY_PROCESSED);
        } else {
            signedMessageHashService.createSignedMessageHash(hashedContent);
        }
    }
}
