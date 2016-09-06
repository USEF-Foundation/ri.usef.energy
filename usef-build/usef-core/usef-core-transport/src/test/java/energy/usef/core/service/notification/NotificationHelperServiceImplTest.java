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

import static energy.usef.core.util.ReflectionUtil.setFinalStatic;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.MessagePrecedence;
import energy.usef.core.model.MessageType;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.helper.JMSHelperService;

import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

/**
 * JUnit test for the NotificationHelperServiceImpl class.
 */
@RunWith(PowerMockRunner.class)
public class NotificationHelperServiceImplTest {
    private static final String CRITICAL_MESSAGE_IS_NOT_SENT = "CRITICAL MESSAGE IS NOT SENT";
    private static final String TRANSACTIONAL_MESSAGE_IS_NOT_SENT = "TRANSACTIONAL MESSAGE IS NOT SENT";

    @Mock
    private Logger LOGGER;

    @Mock
    private TimerService timerService;

    @Mock
    private JMSHelperService jmsService;

    private NotificationHelperServiceImpl service;

    @Mock
    private MessageService ingoingMessageService;

    @Mock
    private Config config;

    @Mock
    private Timer timer;

    @Before
    public void init() throws Exception {
        service = new NotificationHelperServiceImpl();
        setFinalStatic(NotificationHelperServiceImpl.class.getDeclaredField("LOGGER"), LOGGER);
        setInternalState(service, "timerService", timerService);
        setInternalState(service, "ingoingMessageService", ingoingMessageService);
        setInternalState(service, "config", config);
        setInternalState(service, "jmsService", jmsService);
    }

    /**
     * Tests NotificationHelperServiceImpl.notifyNoMessageResponse method with critical message class.
     *
     * @throws Exception
     */
    @Test
    public void notifyNoMessageResponseCriticalTest() throws Exception {
        Mockito.when(config.getProperty(ConfigParam.PTU_DURATION)).thenReturn("15");

        Message message = new CommonReferenceQuery();
        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setPrecedence(MessagePrecedence.CRITICAL);
        message.setMessageMetadata(messageMetadata);
        service.notifyNoMessageResponse("<xml>test</xml>", message);
    }

    /**
     * Tests NotificationHelperServiceImpl.notifyNoMessageResponse method with critical message class.
     *
     * @throws Exception
     */
    @Test
    public void notifyNoMessageResponseTransactionalTest() throws Exception {
        Mockito.when(config.getProperty(ConfigParam.PTU_DURATION)).thenReturn("15");

        Message message = new CommonReferenceQuery();
        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setPrecedence(MessagePrecedence.TRANSACTIONAL);
        message.setMessageMetadata(messageMetadata);
        service.notifyNoMessageResponse("<xml>test</xml>", message);
    }
    /**
     * Tests NotificationHelperServiceImpl.notifyMessageNotSent method.
     *
     * @throws Exception
     */
    @Test
    public void notifyMessageNotSentTest() throws Exception {
        Message message = new CommonReferenceQuery();
        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setPrecedence(MessagePrecedence.CRITICAL);
        message.setMessageMetadata(messageMetadata);

        service.notifyMessageNotSent("<xml>test</xml>", message);
        verify(LOGGER, times(1)).warn(contains(CRITICAL_MESSAGE_IS_NOT_SENT), Matchers.any(Object.class));

        messageMetadata.setPrecedence(MessagePrecedence.TRANSACTIONAL);
        service.notifyMessageNotSent("<xml>test</xml>", message);
        verify(LOGGER, times(1)).warn(contains(TRANSACTIONAL_MESSAGE_IS_NOT_SENT), Matchers.any(Object.class));
    }

    @Test
    public void timeoutActionNoResponseReceived() throws Exception {
        String guid = "c0991d88-40d5-4ba6-8c97-d30af6ab7aef";
        String xml = "<xml>Common reference Query</xml>";
        NotificationInfo notificationInfo = new NotificationInfo(MessageType.CRITICAL, guid, xml);
        Mockito.when(timer.getInfo()).thenReturn(notificationInfo);
        Mockito.when(ingoingMessageService.getMessageResponseByConversationId("c0991d88-40d5-4ba6-8c97-d30af6ab7aef"))
                .thenReturn(null);
        service.timeoutAction(timer);

        verify(jmsService, times(1)).sendMessageToNotSentQueue(xml);

    }

    @Test
    public void timeoutActionResponseReceived() throws Exception {
        String guid = "c0991d88-40d5-4ba6-8c97-d30af6ab7aef";
        String xml = "<xml>Common reference Query</xml>";

        NotificationInfo notificationInfo = new NotificationInfo(MessageType.CRITICAL, guid, xml);
        Mockito.when(timer.getInfo()).thenReturn(notificationInfo);
        Mockito.when(ingoingMessageService.getMessageResponseByConversationId(Matchers.any(String.class)))
                .thenReturn(new energy.usef.core.model.Message());

        service.timeoutAction(timer);
        verify(jmsService, times(0)).sendMessageToNotSentQueue(Matchers.any(String.class));
    }
}
