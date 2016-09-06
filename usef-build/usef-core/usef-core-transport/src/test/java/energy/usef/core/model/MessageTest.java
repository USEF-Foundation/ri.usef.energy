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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import energy.usef.core.data.xml.bean.message.MessageMetadata;

import org.joda.time.LocalDateTime;
import org.junit.Test;

public class MessageTest implements energy.usef.core.data.xml.bean.message.Message {

    /**
     * Tests Message class.
     */
    @Test
    public void messageTest() {
        Message message = new Message();
        message = new Message("xml", this, MessageDirection.OUTBOUND);
        message.setCreationTime(new LocalDateTime());
        message.setSender("sender");
        message.setReceiver("receiver");
        message.setContentHash(null);
        assertNotNull(message.getCreationTime());
        assertEquals("sender", message.getSender());
        assertEquals("receiver", message.getReceiver());
        assertEquals(0, message.getContentHash().length);

        message.setContentHash(new byte[0]);
        assertEquals(0, message.getContentHash().length);
    }

    @Override
    public MessageMetadata getMessageMetadata() {
        MessageMetadata data = new MessageMetadata();
        return data;
    }

    @Override
    public void setMessageMetadata(MessageMetadata messageMetadata) {
        // No need to do anything.
    }

}
