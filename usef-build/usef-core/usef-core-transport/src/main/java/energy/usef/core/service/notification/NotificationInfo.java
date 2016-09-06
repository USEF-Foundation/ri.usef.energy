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

package energy.usef.core.service.notification;

import energy.usef.core.model.MessageType;

import java.io.Serializable;

/**
 * Data transfer object required by the timer. The timer keeps data used by the NotificationHelperService.
 */
public class NotificationInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String conversationId;
    private MessageType messageType;
    private String xml;

    /**
     * Constructs a NotificationInfo with the specified messageType and conversationId.
     * 
     * @param messageType a {@link MessageType}.
     * @param conversationId a conversation Id.
     * @param xml {@link String} xml content of the message.
     *
     */
    public NotificationInfo(MessageType messageType, String conversationId, String xml) {
        this.messageType = messageType;
        this.conversationId = conversationId;
        this.xml = xml;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

}
