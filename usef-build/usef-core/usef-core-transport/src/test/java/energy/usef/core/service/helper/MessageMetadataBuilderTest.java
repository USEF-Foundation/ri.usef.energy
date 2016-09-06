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

import javax.xml.datatype.DatatypeConfigurationException;

import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for the {@link MessageMetadataBuilder} helper class.
 */
public class MessageMetadataBuilderTest {

    @Test
    public void testBuildDefault() {
        MessageMetadata metadata = MessageMetadataBuilder.buildDefault();
        Assert.assertNotNull(metadata);
        Assert.assertNotNull("Did not expect a null Message ID.", metadata.getMessageID());
        Assert.assertNotNull("Did not expect a null Conversation ID.", metadata.getConversationID());
        Assert.assertNotNull("Did not expect a null Timestamp.", metadata.getTimeStamp());
        Assert.assertNull("Expected a null valid until date.", metadata.getValidUntil());
        Assert.assertNull("Expected a null recipient domain.", metadata.getRecipientDomain());
        Assert.assertNull("Expected a null sender domain.", metadata.getSenderDomain());
        Assert.assertNull("Expected a null recipient role.", metadata.getRecipientRole());
        Assert.assertNull("Expected a null sender role.", metadata.getSenderRole());
        Assert.assertEquals("Message precedence mismatch.", MessagePrecedence.ROUTINE, metadata.getPrecedence());
    }

    @Test
    public void testBuild() {
        MessageMetadata metadata = MessageMetadataBuilder.build("agr.usef-example.com", USEFRole.AGR, "dso.usef-example.com",
                USEFRole.DSO, MessagePrecedence.CRITICAL).build();
        Assert.assertNotNull(metadata);
        Assert.assertNotNull("Did not expect a null Message ID.", metadata.getMessageID());
        Assert.assertNotNull("Did not expect a null Conversation ID.", metadata.getConversationID());
        Assert.assertNotNull("Did not expect a null Timestamp.", metadata.getTimeStamp());
        Assert.assertNull("Expected a null valid until date.", metadata.getValidUntil());
        Assert.assertEquals("Sender domain mismatch.", "dso.usef-example.com", metadata.getSenderDomain());
        Assert.assertEquals("Recipient domain mismatch.", "agr.usef-example.com", metadata.getRecipientDomain());
        Assert.assertEquals("Sender role mismatch.", USEFRole.DSO, metadata.getSenderRole());
        Assert.assertEquals("Recipient role mismatch.", USEFRole.AGR, metadata.getRecipientRole());
        Assert.assertEquals("Message precedence mismatch.", MessagePrecedence.CRITICAL, metadata.getPrecedence());

    }

    @Test
    public void testTimeStamp() throws DatatypeConfigurationException {
        LocalDateTime timestamp = DateTimeUtil.getCurrentDateTime();
        MessageMetadata metadata = new MessageMetadataBuilder().timeStamp(timestamp).build();
        Assert.assertEquals("Timestamp mismatch.", timestamp, metadata.getTimeStamp());
    }

    @Test
    public void testMessageID() {
        String messageId = "12345678-1234-1234-1234-1234567890ab";
        MessageMetadata metadata = new MessageMetadataBuilder().messageID(messageId).build();
        Assert.assertEquals("messageId", messageId, metadata.getMessageID());
    }

    @Test
    public void testConversationID() {
        String conversationId = "12345678-1234-1234-1234-1234567890ab";
        MessageMetadata metadata = new MessageMetadataBuilder().conversationID(conversationId).build();
        Assert.assertEquals("messageId", conversationId, metadata.getConversationID());
    }

}
