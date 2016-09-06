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

package energy.usef.core.controller;

import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.TestMessage;
import energy.usef.core.data.xml.bean.message.TestMessageResponse;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.XMLUtil;

import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes incoming test messages.
 */
@Singleton
public class TestMessageController extends
        BaseIncomingMessageController<TestMessage> {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(TestMessageController.class);

    @Inject
    private JMSHelperService jmsService;

    /**
     * {@inheritDoc}
     */
    public void action(TestMessage message, Message savedMessage) throws BusinessException {
        LOGGER.info("Test message received");
        sendTestMessageResponse(message.getMessageMetadata());
    }

    private void sendTestMessageResponse(MessageMetadata metadata)
            throws BusinessException {
        LOGGER.info("Sending test message response");

        TestMessageResponse message = new TestMessageResponse();

        MessageMetadataBuilder messageMetadataBuilder = MessageMetadataBuilder.build(metadata.getSenderDomain(),
                metadata.getSenderRole(), metadata.getRecipientDomain(), metadata.getRecipientRole(), metadata.getPrecedence())
                .conversationID(metadata.getConversationID());

        message.setMessageMetadata(messageMetadataBuilder.build());

        String xml = XMLUtil.messageObjectToXml(message);
        jmsService.sendMessageToOutQueue(xml);

        LOGGER.info("Test message response is sent to the out queue");
    }

}
