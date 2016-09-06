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

package energy.usef.agr.controller;

import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;
import energy.usef.agr.workflow.settlement.receive.ReceiveSettlementMessageEvent;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.SettlementMessage;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.helper.MessageMetadataBuilder;

import javax.enterprise.event.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link SettlementMessageController}.
 */
@RunWith(PowerMockRunner.class)
public class SettlementMessageControllerTest {

    @Mock
    private Event<ReceiveSettlementMessageEvent> eventManager;

    private SettlementMessageController controller;

    @Before
    public void init() {
        controller = new SettlementMessageController();
        Whitebox.setInternalState(controller, eventManager);
    }

    @Test
    public void testActionSucceeds() throws BusinessException {
        controller.action(buildSettlementMessage(), null);
        Mockito.verify(eventManager, Mockito.times(1)).fire(Mockito.any(ReceiveSettlementMessageEvent.class));
    }

    private SettlementMessage buildSettlementMessage() {
        MessageMetadata metadata = new MessageMetadataBuilder().conversationID().messageID().timeStamp()
                .senderDomain("dso.usef-example.com").senderRole(USEFRole.DSO)
                .recipientDomain("agr.usef-example.com").recipientRole(USEFRole.AGR)
                .precedence(TRANSACTIONAL)
                .build();
        SettlementMessage message = new SettlementMessage();
        message.setMessageMetadata(metadata);
        return message;
    }

}
