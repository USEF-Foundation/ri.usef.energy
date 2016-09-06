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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import energy.usef.core.data.participant.ParticipantType;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.MessagePrecedence;
import energy.usef.core.data.xml.bean.message.SignedMessage;
import energy.usef.core.data.xml.bean.message.TestMessage;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.error.IncomingMessageError;
import energy.usef.core.util.DateTimeUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for {@link IncomingMessageVerificationService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class IncomingMessageVerificationServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncomingMessageVerificationService.class);

    private IncomingMessageVerificationService service;
    private Message incomingMessage;

    private static final String SENDER_DOMAIN = "sender.usef-test.com";
    private static final String RECIPIENT_DOMAIN = "recipient.usef-test.com";
    private static final String MESSAGE_ID = "12345678-1234-1234-1234-1234567890ab";

    @Mock
    private MessageFilterService messageFilterService;

    @Mock
    private ParticipantDiscoveryService participantDiscoveryService;

    @Mock
    private MessageService messageService;

    @Before
    public void init() {
        service = new IncomingMessageVerificationService();
        Whitebox.setInternalState(service, "participantDiscoveryService", participantDiscoveryService);
        Whitebox.setInternalState(service, "messageService", messageService);
        incomingMessage = createIncomingMessage();
    }

    /**
     * Verifies that the method {@link ParticipantDiscoveryService#discoverParticipant(Message)} is called exactly one time. And the
     * message is accepted.
     */
    @Test
    public void testValidMessageSender() {
        SignedMessage signedMessage = new SignedMessage();
        signedMessage.setSenderDomain(incomingMessage.getMessageMetadata().getSenderDomain());
        signedMessage.setSenderRole(incomingMessage.getMessageMetadata().getSenderRole());
        try {
            service.validateSender(signedMessage, incomingMessage);

            verify(participantDiscoveryService, times(1)).discoverParticipant(eq(incomingMessage),
                    Matchers.eq(ParticipantType.SENDER));
        } catch (BusinessException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail("No exception should be thrown because message is correct.");
        }
    }

    /**
     * Tests when the role is different in the incomingMessage compared to the signedMessage an error is thrown.
     */
    @Test
    public void testInvalidSenderRole() {
        SignedMessage signedMessage = new SignedMessage();
        signedMessage.setSenderDomain(incomingMessage.getMessageMetadata().getSenderDomain());
        signedMessage.setSenderRole(USEFRole.AGR);
        incomingMessage.getMessageMetadata().setSenderRole(USEFRole.BRP);
        try {
            service.validateSender(signedMessage, incomingMessage);

            Assert.fail("An exception should be thrown because of the invalid sender.");
        } catch (BusinessException e) {
            Assert.assertEquals("BusinessError mismatch", e.getBusinessError(),
                    IncomingMessageError.MESSAGE_CONTENT_INVALID_SENDER);
        }
    }

    /**
     * Tests when the role is different in the incomingMessage compared to the signedMessage an error is thrown.
     */
    @Test
    public void testInvalidSenderDomain() {
        SignedMessage signedMessage = new SignedMessage();
        signedMessage.setSenderDomain("DomainA.com");
        incomingMessage.getMessageMetadata().setSenderDomain("DomainB.com");
        signedMessage.setSenderRole(incomingMessage.getMessageMetadata().getSenderRole());
        try {
            service.validateSender(signedMessage, incomingMessage);

            Assert.fail("An exception should be thrown because of the invalid sender.");
        } catch (BusinessException e) {
            Assert.assertEquals("BusinessError mismatch", e.getBusinessError(),
                    IncomingMessageError.MESSAGE_CONTENT_INVALID_SENDER);
        }
    }

    /**
     * Test the MessageId validation, duplicate messageId.
     */
    @Test
    public void testIncomingMessageUsesExistingMessageId() {
        PowerMockito.when(messageService.isMessageIdAlreadyUsed(Matchers.eq(MESSAGE_ID))).thenReturn(true);
        try {
            service.validateMessageId(MESSAGE_ID);
            Assert.fail("Excepted to catch a BusinessException. MessageID is already in use for a different content.");
        } catch (BusinessException e) {
            Assert.assertEquals("BusinessError mismatch", e.getBusinessError(),
                    IncomingMessageError.MESSAGE_ID_ALREADY_USED);
        }
    }

    /**
     * Test the ValidUntil validation.
     * 
     * @throws BusinessException
     */
    @Test
    public void testIncomingMessageUsesValidUntil() throws BusinessException {
        service.validateMessageValidUntil(null);
        service.validateMessageValidUntil(DateTimeUtil.getCurrentDateTime().plusDays(1));
        try {
            service.validateMessageValidUntil(DateTimeUtil.getCurrentDateTime().minusDays(1));
            Assert.fail("Excepted to catch a BusinessException. ValidUntil field has expired.");
        } catch (BusinessException e) {
            Assert.assertEquals("BusinessError mismatch", e.getBusinessError(),
                    IncomingMessageError.MESSAGE_EXPIRED);
        }
    }

    /**
     * Tests the messageId validation, messageId correct.
     */
    @Test
    public void testIncomingIsANewOne() {
        PowerMockito.when(messageService.isMessageIdAlreadyUsed(Matchers.eq(MESSAGE_ID))).thenReturn(false);
        try {
            service.validateMessageId(MESSAGE_ID);
        } catch (BusinessException e) {
            LOGGER.error("Exception caught: {}", e.getBusinessError().getError());
            Assert.fail("Did not expect an exception since the message is not in the database yet.");
        }
    }

    private Message createIncomingMessage() {
        Message message = new TestMessage();
        MessageMetadata metadata = new MessageMetadata();
        metadata.setSenderDomain(SENDER_DOMAIN);
        metadata.setSenderRole(USEFRole.AGR);
        metadata.setRecipientDomain(RECIPIENT_DOMAIN);
        metadata.setPrecedence(MessagePrecedence.ROUTINE);
        metadata.setMessageID(MESSAGE_ID);

        message.setMessageMetadata(metadata);
        return message;
    }

}
