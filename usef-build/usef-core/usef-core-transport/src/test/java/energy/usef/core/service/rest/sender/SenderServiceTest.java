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

package energy.usef.core.service.rest.sender;

import static energy.usef.core.util.ReflectionUtil.setFinalStatic;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.participant.Participant;
import energy.usef.core.data.participant.ParticipantRole;
import energy.usef.core.data.participant.ParticipantType;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.model.MessageDirection;
import energy.usef.core.service.business.MessageEncryptionService;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.business.ParticipantDiscoveryService;
import energy.usef.core.service.helper.NotificationHelperService;
import energy.usef.core.util.DateTimeUtil;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;

/**
 * JUnit test for the SenderService class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DateTimeUtil.class, Config.class, HttpTransport.class, HttpRequest.class, HttpResponse.class })
@PowerMockIgnore("javax.net.ssl.*")
public class SenderServiceTest {

    private static final String MESSAGE_ID = "8bc30b49-7809-483c-86fe-0ce4caf83eb7";
    private static final String CONVERSATION_ID = "779a05e1-c395-46c0-a78f-a2b02c0a323b";

    private static final String MSG = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            +
            "<TestMessage>"
            +
            "<MessageMetadata SenderDomain=\"agr.usef-example.com\" SenderRole=\"AGR\" RecipientDomain=\"dso.usef-example.com\" RecipientRole=\"DSO\" MessageID=\""
            + MESSAGE_ID
            + "\" Precedence=\"Transactional\" TimeStamp=\"" + DateTimeUtil.getCurrentDateTime() + "\" ConversationID=\""
            + CONVERSATION_ID + "\"/>"
            +
            "</TestMessage>";

    private static final String INVALID_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            +
            "<TestMessage>"
            +
            "<MessageMetadata SenderDomain=\"localhost\" Body=\"test body\" SenderRole=\"AGR\" RecipientDomain=\"localhost\" RecipientRole=\"DSO\" MessageID=\""
            + MESSAGE_ID
            + "\" Precedence=\"Transactional\"/>"
            +
            "</TestMessage>";

    private static final String SENDER_DOMAIN = "localhost";
    private static final String TLS_VERIFICATION_DISABLED_MESSAGE = "TLS/SSL verification is disabled. Certificates of the destination of the message will not be checked.";

    @Mock
    private MessageService messageService;

    @Mock
    private MessageEncryptionService messageEncryptionService;

    @Mock
    private NotificationHelperService notificationHelperService;

    @Mock
    private ParticipantDiscoveryService participantDiscoveryService;

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private HttpResponse httpResponse;

    @Spy
    private Config config = new Config();

    private SenderService senderService;

    @Before
    public void init() throws Exception {
        senderService = new SenderService();

        PowerMockito.mockStatic(DateTimeUtil.class);
        PowerMockito.when(DateTimeUtil.updateSettings(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        config.initBean();

        Whitebox.setInternalState(senderService, "messageService", messageService);
        Whitebox.setInternalState(senderService, "messageEncryptionService", messageEncryptionService);
        Whitebox.setInternalState(senderService, "notificationHelperService", notificationHelperService);
        Whitebox.setInternalState(senderService, "participantDiscoveryService", participantDiscoveryService);
        Whitebox.setInternalState(senderService, "config", config);

        Mockito.when(
                participantDiscoveryService.discoverParticipant(Matchers.any(Message.class), Matchers.any(ParticipantType.class)))
                .thenReturn(buildParticipant());

        // message = (Message) XMLUtil.xmlToMessage(MSG);
    }

    /**
     * Basic test for the SenderService.sendScheduledMsg method. Verify whether the error massage is correctly saved.
     *
     * @throws Exception
     */
    @Test
    public void testCreateOutMessageError() throws Exception {
        Mockito.when(messageService.storeMessage(Matchers.any(String.class),
                Matchers.any(Message.class), Matchers.any(MessageDirection.class)))
                .thenReturn(new energy.usef.core.model.Message());

        senderService.sendMessage(MSG);

        verify(messageService).storeMessage(Matchers.anyString(), Matchers.any(Message.class),
                Matchers.eq(MessageDirection.OUTBOUND));
    }

    /**
     * Tests whether the Logger is correctly called to warn that the TLS verification is disabled. Expected result: the logger is
     * called once with the error message concerning the TLS verification being disabled.
     *
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws BusinessException
     */
    @Test
    public void testBypassTLSIsLogged() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException, BusinessException {
        Logger loggerMock = PowerMockito.mock(Logger.class);
        setFinalStatic(SenderService.class.getDeclaredField("LOGGER"), loggerMock);

        Mockito.when(config.getBooleanProperty(ConfigParam.BYPASS_TLS_VERIFICATION)).thenReturn(Boolean.TRUE);
        Mockito.when(messageService.storeMessage(Matchers.any(String.class),
                Matchers.any(Message.class), Matchers.any(MessageDirection.class)))
                .thenReturn(new energy.usef.core.model.Message());

        senderService.sendMessage(MSG);

        verify(loggerMock, times(1)).warn(eq(TLS_VERIFICATION_DISABLED_MESSAGE));
    }

    /**
     * Tests whether the Logger is correctly called to warn that the TLS verification is disabled. Expected result: the logger is
     * never called with the error message concerning the TLS verification being disabled.
     *
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws BusinessException
     */
    @Test
    public void testBypassTLSIsNeverLogged() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException, BusinessException {
        Logger loggerMock = PowerMockito.mock(Logger.class);
        Mockito.when(config.getBooleanProperty(ConfigParam.BYPASS_TLS_VERIFICATION)).thenReturn(Boolean.FALSE);
        setFinalStatic(SenderService.class.getDeclaredField("LOGGER"), loggerMock);

        Mockito.when(messageService.storeMessage(Matchers.any(String.class),
                Matchers.any(Message.class), Matchers.any(MessageDirection.class)))
                .thenReturn(new energy.usef.core.model.Message());

        senderService.sendMessage(MSG);

        verify(loggerMock, times(0)).warn(eq(TLS_VERIFICATION_DISABLED_MESSAGE));
    }

    /**
     * Basic test for the SenderService.sendScheduledMsg method.
     *
     * @throws Exception
     */
    @Test
    public void testSendScheduledMessage() throws Exception {
        SenderService instance = PowerMockito.spy(senderService);

        PowerMockito.when(messageService.storeMessage(Matchers.any(String.class), Matchers.any(Message.class),
                Matchers.any(MessageDirection.class))).thenReturn(new energy.usef.core.model.Message());

        PowerMockito.whenNew(HttpRequest.class).withAnyArguments().thenReturn(httpRequest);
        PowerMockito.when(httpRequest.execute()).thenReturn(httpResponse);
        PowerMockito.when(httpResponse.getContent()).thenReturn(new ByteArrayInputStream("dummy".getBytes()));

        instance.sendMessage(MSG);

        verify(messageService, Mockito.times(1)).storeMessage(eq(MSG), Matchers.any(Message.class),
                Matchers.eq(MessageDirection.OUTBOUND));
        verify(notificationHelperService, Mockito.times(1)).notifyNoMessageResponse(Mockito.anyString(),
                Mockito.any(Message.class));
        verify(config, Mockito.times(1)).getIntegerProperty(ConfigParam.TRANSACTIONAL_EXPONENTIAL_BACKOFF_INITIAL_INTERVAL_MILLIS);
        verify(config, Mockito.times(1)).getIntegerProperty(ConfigParam.TRANSACTIONAL_HTTP_REQUEST_MAX_RETRIES);
    }

    /**
     * Send invalid XML message
     *
     * @throws Exception
     */
    @Test(expected = TechnicalException.class)
    public void sendInvalidXMLMessage() throws Exception {
        SenderService instance = PowerMockito.spy(senderService);

        PowerMockito.when(messageService.storeMessage(Matchers.any(String.class), Matchers.any(Message.class),
                Matchers.any(MessageDirection.class))).thenReturn(new energy.usef.core.model.Message());

        PowerMockito.whenNew(HttpRequest.class).withAnyArguments().thenReturn(httpRequest);
        PowerMockito.when(httpRequest.execute()).thenReturn(httpResponse);
        PowerMockito.when(httpResponse.getContent()).thenReturn(new ByteArrayInputStream("dummy".getBytes()));

        // if validate outgoing xml is not enabled, force TechnicalException to succeed test
        PowerMockito.when(config.getBooleanProperty(ConfigParam.VALIDATE_OUTGOING_XML).booleanValue()).thenThrow(
                new TechnicalException(""));
        instance.sendMessage(INVALID_XML);
    }

    private Participant buildParticipant() {
        Participant participant = new Participant();
        participant.setDomainName(SENDER_DOMAIN);
        participant.setSpecVersion("2015");

        ParticipantRole role = new ParticipantRole(USEFRole.DSO);
        role.setUrl("http://localhost:666");
        role.setPublicKeys(Arrays.asList("unsigningKey", "KEY2"));

        participant.setRoles(Collections.singletonList(role));
        return participant;
    }
}
