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

package energy.usef.core.endpoint;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.doThrow;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.SignedMessage;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.IncomingMessageVerificationService;
import energy.usef.core.service.business.MessageEncryptionService;
import energy.usef.core.service.business.MessageFilterService;
import energy.usef.core.service.business.ParticipantDiscoveryService;
import energy.usef.core.service.business.error.IncomingMessageError;
import energy.usef.core.service.business.error.MessageFilterError;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.KeystoreHelperService;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import energy.usef.core.service.business.error.ParticipantDiscoveryError;

/**
 * JUnit test for the ReceiverService class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ReceiverEndpoint.class, IncomingMessageVerificationService.class, KeystoreHelperService.class })
public class ReceiverEndpointTest extends BaseResourceTest {

    private static final String URL = "/ReceiverService/receiveMessage";
    private static final String BAD_URL = "/hsqsfqhgsfqgs";
    private static final String TEST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><TestMessage><MessageMetadata SenderDomain=\"agr.usef-example.com\" SenderRole=\"AGR\" RecipientDomain=\"cro.usef-example.com\" RecipientRole=\"CRO\" TimeStamp=\"2015-02-05T14:08:33.687\" MessageID=\"12345678-1234-1234-1234-1234567890ab\" ConversationID=\"12345678-1234-1234-1234-1234567890ab\" Precedence=\"Routine\" ValidUntil=\"2015-02-05T14:08:33.687\"/></TestMessage>";

    private ReceiverEndpoint receiverService;
    private JMSHelperService jmsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private JMSContext context;

    @Mock
    private Config config;

    @Mock
    private JMSProducer producer;

    @Mock
    private IncomingMessageVerificationService incomingMessageVerificationService;

    @Mock
    private ParticipantDiscoveryService participantDiscoveryService;
    @Mock
    private MessageFilterService messageFilterService;

    @Mock
    private MessageEncryptionService messageEncryptionService;

    /**
     * Setup for the test.
     */
    @Before
    public void setupResource() {

        receiverService = new ReceiverEndpoint();
        jmsService = new JMSHelperService();

        Whitebox.setInternalState(jmsService, "context", context);
        Whitebox.setInternalState(receiverService, "jmsService", jmsService);
        Whitebox.setInternalState(receiverService, "incomingMessageVerificationService", incomingMessageVerificationService);
        Whitebox.setInternalState(receiverService, "participantDiscoveryService", participantDiscoveryService);
        Whitebox.setInternalState(receiverService, "messageEncryptionService", messageEncryptionService);
        Whitebox.setInternalState(receiverService, "messageFilterService", messageFilterService);
        Whitebox.setInternalState(receiverService, "config", config);

        Mockito.when(context.createProducer()).thenReturn(producer);

        dispatcher.getRegistry().addSingletonResource(receiverService);

        ResteasyProviderFactory
                .getContextDataMap()
                .put(HttpServletRequest.class, Mockito.mock(HttpServletRequest.class));
    }

    /**
     * Removes inited resources.
     */
    @After
    public void removeResource() {
        dispatcher.getRegistry().removeRegistrations(ReceiverEndpoint.class);
    }

    /**
     * Tests the service with not correct URL.
     *
     * @throws URISyntaxException
     */
    @Test
    public void badRequestUrlTest() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.post(BAD_URL);
        request.contentType(TEXT_XML);
        request.content("<test>test</test>".getBytes());

        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        Assert.assertEquals(404, response.getStatus());
    }

    /**
     * Tests the service with not correct content type.
     *
     * @throws URISyntaxException
     */
    @Test
    public void badRequestContentTypeTest() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.post(URL);
        request.contentType(APPLICATION_XML);
        request.content("<test>test</test>".getBytes());

        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        // 415 HTTP Error 415 Unsupported media type
        Assert.assertEquals(415, response.getStatus());
    }

    /**
     * Tests the receiveMsg method.
     *
     * @throws URISyntaxException
     * @throws BusinessException
     */
    @Test
    public void receiveMsgTest() throws URISyntaxException, BusinessException {
        Mockito.when(messageEncryptionService.verifyMessage(anyString().getBytes(StandardCharsets.UTF_8), anyString()))
                .thenReturn(TEST_XML);

        MockHttpRequest request = MockHttpRequest.post(URL);
        request.contentType(TEXT_XML);
        request.content("<SignedMessage SenderDomain=\"stuff\" SenderRole=\"CRO\" Body=\"&lt;TestMessage /&gt;\"/>".getBytes());

        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Tests the receiveMsg method.
     *
     * @throws URISyntaxException
     * @throws BusinessException
     */
    @Test
    public void receiveEmptyMsgTest() throws URISyntaxException, BusinessException {
        Mockito.when(messageEncryptionService.verifyMessage(anyString().getBytes(StandardCharsets.UTF_8), anyString()))
                .thenReturn(TEST_XML);

        MockHttpRequest request = MockHttpRequest.post(URL);
        request.contentType(TEXT_XML);
        request.content("<SignedMessage SenderDomain=\"stuff\" SenderRole=\"CRO\" />".getBytes());

        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        Assert.assertEquals(400, response.getStatus());
    }
    /**
     * Tests the receiveMsg method.
     *
     * @throws URISyntaxException
     */
    @Test
    public void receiveWrongMsgTest() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.post(URL);
        request.contentType(TEXT_XML);
        request.content("<Test>Test</Test>".getBytes());

        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    /**
     * Verifies that the Response 400 Bad Request is sent in case the participant was not found through DNS exploration.
     *
     * @throws Exception
     */
    @Test
    public void testSendBadRequestIfNonValidParticipant() throws Exception {
        doThrow(new BusinessException(ParticipantDiscoveryError.PARTICIPANT_NOT_FOUND)).when(incomingMessageVerificationService).validateSender(
                Matchers.any(SignedMessage.class), Matchers.any(Message.class));

        Response response = receiverService.receiveMessage("<TestMessage>Test</TestMessage>", request);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    /**
     * Verifies that the Response 400 Bad Request is sent in case the participant was not found through DNS exploration.
     *
     * @throws Exception
     */
    @Test
    public void testReceiveEmptyMessage() throws Exception {
        doThrow(new BusinessException(ParticipantDiscoveryError.PARTICIPANT_NOT_FOUND)).when(incomingMessageVerificationService).validateSender(
                Matchers.any(SignedMessage.class), Matchers.any(Message.class));

        Response response = receiverService.receiveMessage("", request);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }


    @Test
    public void testUnsealingMessageContentIsCalled() throws URISyntaxException, BusinessException {

        Mockito.when(config.getBooleanProperty(ConfigParam.VALIDATE_INCOMING_XML).booleanValue()).thenReturn(true);
        Mockito.when(messageEncryptionService.verifyMessage(Matchers.any(byte[].class), Matchers.anyString()))
                .thenReturn(TEST_XML);

        MockHttpRequest request = MockHttpRequest.post(URL);
        request.contentType(TEXT_XML);
        String messageContent = "someBase64FancyContent==";
        request.content(("<SignedMessage "
                + "SenderDomain=\"test.usef.energy\" "
                + "SenderRole=\"AGR\" "
                + "Body=\"" + Base64.encodeBase64String(messageContent.getBytes()) + "\"/>").getBytes());

        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        byte[] message = messageContent.getBytes(StandardCharsets.UTF_8);
        Mockito.verify(messageEncryptionService, Mockito.times(1)).verifyMessage(
                eq(message), anyString());

        Mockito.verify(messageFilterService, Mockito.times(1)).filterMessage(Matchers.eq("test.usef.energy"),
                Matchers.any(String.class));

        Mockito.verify(incomingMessageVerificationService, Mockito.times(1)).validateMessageId(
                Matchers.eq("12345678-1234-1234-1234-1234567890ab"));
        Mockito.verify(incomingMessageVerificationService, Mockito.times(1)).validateSender(Matchers.any(SignedMessage.class),
                Matchers.any(Message.class));

    }

    /**
     * Tests the receiveMsg method.
     *
     * @throws URISyntaxException
     * @throws BusinessException
     */
    @Test
    public void testMessageFilterServiceFilterMessageAllowList() throws URISyntaxException, BusinessException {
        Mockito.doThrow(new BusinessException(MessageFilterError.PARTICIPANT_NOT_ALLOWLISTED)).when(messageFilterService).filterMessage(anyString(), anyString());
        Response response = receiverService.receiveMessage("", request);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    /**
     * Tests the receiveMsg method.
     *
     * @throws URISyntaxException
     * @throws BusinessException
     */
    @Test
    public void testMessageFilterServiceFilterMessageDenyList() throws URISyntaxException, BusinessException {
        Mockito.doThrow(new BusinessException(MessageFilterError.ADDRESS_IS_DENYLISTED)).when(messageFilterService).filterMessage(anyString(), anyString());
        Response response = receiverService.receiveMessage("", request);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

   /**
     * Tests the receiveMsg method.
     *
     * @throws URISyntaxException
     * @throws BusinessException
     */
    @Test
    public void testIncomingMessageVerificationServiceValidateMessageId() throws URISyntaxException, BusinessException {
        Mockito.when(messageEncryptionService.verifyMessage(anyString().getBytes(StandardCharsets.UTF_8), anyString()))
                .thenReturn(TEST_XML);
        Mockito.doThrow(new BusinessException(IncomingMessageError.MESSAGE_ID_ALREADY_USED)).when(incomingMessageVerificationService).validateMessageId(anyString());
        Response response = receiverService.receiveMessage("<SignedMessage SenderDomain=\"stuff\" SenderRole=\"CRO\" Body=\"&lt;TestMessage /&gt;\"/>", request);
        assertEquals(OK.getStatusCode(), response.getStatus());
    }

    /**
     * Tests the receiveMsg method.
     *
     * @throws URISyntaxException
     * @throws BusinessException
     */
    @Test
    public void testIncomingMessageVerificationServiceCheckSignedMessageHash() throws URISyntaxException, BusinessException {
        Mockito.doThrow(new BusinessException(IncomingMessageError.ALREADY_RECEIVED_AND_SUCCESSFULLY_PROCESSED)).when(incomingMessageVerificationService).checkSignedMessageHash(anyObject());
        Response response = receiverService.receiveMessage("", request);
        assertEquals(OK.getStatusCode(), response.getStatus());

    }
}
