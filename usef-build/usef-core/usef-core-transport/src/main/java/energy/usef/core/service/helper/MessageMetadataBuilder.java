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

import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.MessagePrecedence;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.util.DateTimeUtil;

import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.LocalDateTime;

/**
 * Builder class for the building a new {@link MessageMetadata}.
 */
public class MessageMetadataBuilder {

    private String senderDomain;
    private USEFRole senderRole;
    private String recipientDomain;
    private USEFRole recipientRole;
    private LocalDateTime timeStamp;
    private String messageID;
    private String conversationID;
    private MessagePrecedence precedence;
    private LocalDateTime validUntil;

    /**
     * Builds a {@link MessageMetadata} with the values contained in the builder.
     *
     * @return {@link MessageMetadata}.
     */
    public MessageMetadata build() {
        MessageMetadata metadata = new MessageMetadata();
        metadata.setConversationID(conversationID);
        metadata.setMessageID(messageID);
        metadata.setPrecedence(precedence);
        metadata.setRecipientDomain(recipientDomain);
        metadata.setRecipientRole(recipientRole);
        metadata.setSenderDomain(senderDomain);
        metadata.setSenderRole(senderRole);
        metadata.setTimeStamp(timeStamp);
        metadata.setValidUntil(validUntil);
        return metadata;
    }

    /**
     * Builds a default {@link MessageMetadata} with a message ID, conversation ID, timestamp and ROUTINE precedence.
     *
     * @return {@link MessageMetadata}.
     */
    public static MessageMetadata buildDefault() {
        return new MessageMetadataBuilder().messageID().conversationID().timeStamp().precedence(MessagePrecedence.ROUTINE).build();
    }

    /**
     * Default build method for building a new {@link MessageMetadata}. MessageID, ConversationID and timestamp are automatically
     * generated.
     *
     * @param recipientDomain {@link String} domain name of the recipient.
     * @param recipientRole {@link USEFRole} role of the recipient.
     * @param senderDomain {@link String} domain name of the sender.
     * @param senderRole {@link USEFRole} role of the sender.
     * @param precedence {@link MessagePrecedence} precedence of the message.
     * @return a {@link MessageMetadata} with a <code>null</code> valid until date.
     */
    public static MessageMetadataBuilder build(String recipientDomain, USEFRole recipientRole, String senderDomain,
            USEFRole senderRole,
            MessagePrecedence precedence) {
        return new MessageMetadataBuilder().messageID().conversationID().timeStamp().precedence(precedence)
                .recipientDomain(recipientDomain).recipientRole(recipientRole).senderDomain(senderDomain).senderRole(senderRole);
    }

    /**
     * Sets the sender domain.
     *
     * @param senderDomain {@link String} sender domain.
     * @return this builder.
     */
    public MessageMetadataBuilder senderDomain(String senderDomain) {
        this.senderDomain = senderDomain;
        return this;
    }

    /**
     * Sets the sender role.
     *
     * @param senderRole {@link USEFRole} sender role.
     * @return this builder.
     */
    public MessageMetadataBuilder senderRole(USEFRole senderRole) {
        this.senderRole = senderRole;
        return this;
    }

    /**
     * Sets the recipient domain.
     *
     * @param recipientDomain {@link String} recipient domain.
     * @return this builder.
     */
    public MessageMetadataBuilder recipientDomain(String recipientDomain) {
        this.recipientDomain = recipientDomain;
        return this;
    }

    /**
     * Sets the recipient role.
     *
     * @param recipientRole {@link USEFRole} recipient role.
     * @return this builder.
     */
    public MessageMetadataBuilder recipientRole(USEFRole recipientRole) {
        this.recipientRole = recipientRole;
        return this;
    }

    /**
     * Sets the timeStamp with the given parameter.
     *
     * @param timeStamp {@link XMLGregorianCalendar} timeStamp.
     * @return this builder.
     */
    public MessageMetadataBuilder timeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    /**
     * Sets the messageId with the given parameter.
     *
     * @param messageID{@link String} messageID.
     * @return this builder.
     */
    public MessageMetadataBuilder messageID(String messageID) {
        this.messageID = messageID;
        return this;
    }

    /**
     * Sets the conversationId with the given parameter.
     *
     * @param conversationID {@link String} conversationId.
     * @return this builder.
     */
    public MessageMetadataBuilder conversationID(String conversationID) {
        this.conversationID = conversationID;
        return this;
    }

    /**
     * Sets the precedence of the message with the given parameter.
     *
     * @param precedence {@link MessagePrecedence} precedence.
     * @return this builder.
     */
    public MessageMetadataBuilder precedence(MessagePrecedence precedence) {
        this.precedence = precedence;
        return this;
    }

    /**
     * Sets the valid until date of the message with the given parameter.
     *
     * @param validUntil {@link XMLGregorianCalendar} validUntil.
     * @return this builder.
     */
    public MessageMetadataBuilder validUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
        return this;
    }

    /**
     * Sets the timestamp to now.
     *
     * @return this {@link MessageMetadataBuilder}.
     */
    public MessageMetadataBuilder timeStamp() {
        this.timeStamp = DateTimeUtil.getCurrentDateTime();
        return this;
    }

    /**
     * Sets a random UUID for the messageId.
     *
     * @return this {@link MessageMetadataBuilder}.
     */
    public MessageMetadataBuilder messageID() {
        this.messageID = uuid();
        return this;
    }

    /**
     * Sets a random UUID for the conversationId.
     *
     * @return this {@link MessageMetadataBuilder}.
     */
    public MessageMetadataBuilder conversationID() {
        this.conversationID = uuid();
        return this;
    }

    /**
     * Creates a new UUID string.
     *
     * @return {@link String} UUID.
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

}
