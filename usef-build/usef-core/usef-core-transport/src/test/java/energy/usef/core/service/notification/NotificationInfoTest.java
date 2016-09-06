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

import static org.junit.Assert.assertEquals;
import energy.usef.core.model.MessageType;

import org.junit.Test;

public class NotificationInfoTest {

    /**
     * Tests NotificationHelperServiceImpl.notifyMessageNotSent method.
     *
     * @throws Exception
     */
    @Test
    public void notifyMessageNotSentTest() throws Exception {
        NotificationInfo n = new NotificationInfo(MessageType.ROUTINE, "conversationId", "<xml>test</xml>");

        n.setMessageType(MessageType.CRITICAL);
        n.setConversationId("newId");
        assertEquals(MessageType.CRITICAL, n.getMessageType());
        assertEquals("newId", n.getConversationId());

    }
}
