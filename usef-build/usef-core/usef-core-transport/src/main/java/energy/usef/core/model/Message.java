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

package energy.usef.core.model;

import static javax.persistence.TemporalType.TIMESTAMP;

import java.util.Arrays;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;

import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.LocalDateTime;

import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.util.DateTimeUtil;

/**
 * Entity class {@link Message}: This class is a representation of a Message send or received.
 */
@Entity
@Table(name = "MESSAGE",
        indexes = { @Index(name = "MSG_CONV_DIR_IDX", columnList = "CONVERSATION_ID, DIRECTION", unique = false) })
public class Message {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "CREATION_TIME", nullable = false)
    @Temporal(TIMESTAMP)
    private Date creationTime;

    @Column(name = "DIRECTION", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageDirection direction;

    @Column(name = "SENDER")
    private String sender;

    @Column(name = "RECEIVER")
    private String receiver;

    @Lob
    @Column(name = "XML", nullable = false)
    private String xml;

    @Column(name = "MESSAGE_ID", nullable = false, unique = false)
    private String messageId;

    @Column(name = "CONVERSATION_ID")
    private String conversationId;

    @Column(name = "MESSAGE_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Lob
    @Column(name = "CONTENT_HASH", nullable = false)
    private byte[] contentHash;

    /**
     * Default constructor.
     */
    public Message() {
        // default constructor, can remain empty
    }

    /**
     * Constructs a Message with the specified xml, dtoMessage and direction.
     *
     * @param xml the XML representation of the error.
     * @param dtoMessage a {@link energy.usef.core.data.xml.bean.message.Message}.
     * @param direction the {@link MessageDirection}.
     */
    public Message(String xml, energy.usef.core.data.xml.bean.message.Message dtoMessage, MessageDirection direction) {
        MessageMetadata messageMetadata = dtoMessage.getMessageMetadata();
        setCreationTime(DateTimeUtil.getCurrentDateTime());
        this.xml = xml;
        this.direction = direction;
        this.sender = messageMetadata.getSenderDomain();
        this.receiver = messageMetadata.getRecipientDomain();
        this.messageId = messageMetadata.getMessageID();
        this.conversationId = messageMetadata.getConversationID();
        this.messageType = messageMetadata.getPrecedence() != null ? MessageType.fromValue(messageMetadata.getPrecedence()) : null;
        this.contentHash = DigestUtils.sha256(xml);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreationTime() {
        if (creationTime == null) {
            return null;
        }
        return LocalDateTime.fromDateFields(creationTime);
    }

    public void setCreationTime(LocalDateTime creationTime) {
        if (creationTime == null) {
            this.creationTime = null;
        } else {
            this.creationTime = creationTime.toDateTime().toDate();
        }
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    /**
     * Returns the message id.
     *
     * @return a UUID.
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Sets the message id.
     *
     * @param messageId a UUID.
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Returns the conversation id.
     *
     * @return a UUID.
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * Sets the conversation id.
     *
     * @param conversationId a UUID.
     */
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public MessageDirection getDirection() {
        return direction;
    }

    public void setDirection(MessageDirection direction) {
        this.direction = direction;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public byte[] getContentHash() {
        return contentHash == null ? new byte[0] : Arrays.copyOf(contentHash, contentHash.length);
    }

    public void setContentHash(byte[] contentHash) {
        this.contentHash = contentHash;
    }

    @Override
    public String toString() {
        return "Message" + "[" +
                "id=" + id +
                ", creationTime=" + creationTime +
                ", direction=" + direction +
                ", sender='" + sender + "'" +
                ", receiver='" + receiver + "'" +
                ", messageId='" + messageId + "'" +
                ", conversationId='" + conversationId + "'" +
                ", messageType=" + messageType +
                ", contentHash=" + Arrays.toString(contentHash) +
                "]";
    }
}
