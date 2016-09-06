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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.Message;
import energy.usef.core.model.MessageDirection;
import energy.usef.core.model.MessageError;
import energy.usef.core.model.MessageType;
import energy.usef.core.repository.MessageErrorRepository;
import energy.usef.core.repository.MessageRepository;
import energy.usef.core.util.XMLUtil;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * JUnit test for the IngoingMessageService class.
 */
@RunWith(PowerMockRunner.class)
public class MessageServiceTest {

    private static final String MESSAGE_ID = "testMessageId";
    private static final String CONVERSATION_ID = "testConversationId";

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            +
            "<TestMessage>"
            +
            "<MessageMetadata SenderDomain=\"localhost\" Body=\"test body\" SenderRole=\"aggregator\" "
            + "RecipientDomain=\"localhost\" RecipientRole=\"DSO\" MessageID=\""
            + MESSAGE_ID
            + "\" ConversationID=\""
            + CONVERSATION_ID
            + "\" Precedence=\"Transactional\"/>"
            +
            "</TestMessage>";

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageErrorRepository messageErrorRepository;

    private MessageService service;
    @Mock
    private Config config;

    @Before
    public void init() throws Exception {
        service = new MessageService();
        Whitebox.setInternalState(service, "messageRepository", messageRepository);
        Whitebox.setInternalState(service, "messageErrorRepository", messageErrorRepository);

        Whitebox.setInternalState(service, "config", config);
    }

    /**
     * Tests MessageService.storeMessage method.
     */
    @Test
    public void createIMessageTest() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(
                Message.class);

        service.storeMessage(XML, (energy.usef.core.data.xml.bean.message.Message) XMLUtil.xmlToMessage(XML),
                MessageDirection.OUTBOUND);

        verify(messageRepository).persist(captor.capture());

        Assert.assertEquals(MESSAGE_ID, captor.getValue().getMessageId());
        Assert.assertEquals(XML, captor.getValue().getXml());
        Assert.assertEquals(MessageType.TRANSACTIONAL, captor.getValue().getMessageType());
        Assert.assertEquals(CONVERSATION_ID, captor.getValue().getConversationId());
    }

    /**
     * , Tests IngoingMessageService.getMessageResponseByConversationId method.
     */
    @Test
    public void getMessageResponseByConversationIdTest() {
        service.getMessageResponseByConversationId(CONVERSATION_ID);

        verify(messageRepository, times(1)).getMessageResponseByConversationId(CONVERSATION_ID);
    }

    @Test
    public void testGetInitialMessageOfConversationSucceeds() {
        service.getInitialMessageOfConversation(CONVERSATION_ID);
        verify(messageRepository, times(1)).getInitialMessageOfConversation(Matchers.eq(CONVERSATION_ID));
    }

    /**
     * Tests OutgoingMessageService.createOutgoingMessageError method.
     */
    @Test
    public void createOutgoingMessageErrorTest() {
        ArgumentCaptor<MessageError> captor = ArgumentCaptor.forClass(MessageError.class);

        Message outgoingMessage = new Message();
        outgoingMessage.setId((long) 1);
        String testErrorMessage = "test error message";
        Integer testErrorCode = 400;
        Mockito.when(config.getIntegerProperty(ConfigParam.MAX_ERROR_MESSAGE_LENGTH)).thenReturn(100);
        service.storeMessageError(outgoingMessage, testErrorMessage, testErrorCode);

        verify(messageErrorRepository).persist(captor.capture());

        Assert.assertEquals(captor.getValue().getMessage(), outgoingMessage);
        Assert.assertEquals(captor.getValue().getErrorMessage(), testErrorMessage);
        Assert.assertEquals(captor.getValue().getErrorCode(), testErrorCode);
    }

    /**
     * Tests whether the repository is called once with the correct parameter.
     */
    @Test
    public void testFindMessageByMessageIdSucceeds() {
        String messageId = "12345678-1234-1234-1234567890ab";
        service.isMessageIdAlreadyUsed(messageId);
        Mockito.verify(messageRepository, Mockito.times(1)).isMessageIdAlreadyUsed(Matchers.eq(messageId));
    }

    @Test
    public void testHasEveryCommonReferenceQuerySentAResponseReceivedIsTrue() {
        // variables and mocking
        final LocalDateTime period = new LocalDate(2015, 8, 25).toDateTimeAtStartOfDay().toLocalDateTime();
        Mockito.when(messageRepository.hasEveryCommonReferenceQuerySentAResponseReceived(Matchers.any(LocalDateTime.class)))
                .thenReturn(true);
        // invocation
        boolean result = service.hasEveryCommonReferenceQuerySentAResponseReceived(period);
        // verifications
        Assert.assertTrue(result);
        Mockito.verify(messageRepository, Mockito.times(1)).hasEveryCommonReferenceQuerySentAResponseReceived(Matchers.eq(period));
    }
}
