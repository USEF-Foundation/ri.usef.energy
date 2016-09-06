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
import energy.usef.core.data.xml.bean.message.MessagePrecedence;

import org.junit.Test;

public class MessageTypeTest {

    /**
     * Tests Message class.
     */
    @Test
    public void messageTypeTest() {
        assertEquals("CRITICAL", MessageType.CRITICAL.name());
        assertEquals("ROUTINE", MessageType.ROUTINE.name());
        assertEquals("TRANSACTIONAL", MessageType.TRANSACTIONAL.name());
        assertEquals(MessagePrecedence.CRITICAL, MessageType.CRITICAL.getXmlValue());
        assertEquals(MessagePrecedence.ROUTINE, MessageType.ROUTINE.getXmlValue());
        assertEquals(MessagePrecedence.TRANSACTIONAL, MessageType.TRANSACTIONAL.getXmlValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void messageTypeFailureTest() {
        MessageType.fromValue(null);
    }

}
