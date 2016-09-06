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

package energy.usef.core.service.mdb;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.rest.sender.SenderService;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * JUnit test for the OutQueueMDBService class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SenderService.class, Message.class })
public class OutgoingQueueMDBTest {
    private static final String RECIPIENT = "recipient.test.com";

    private static final String TEST_MSG_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            +
            "<TestMessage>"
            +
            "<MessageMetadata SenderDomain=\"localhost\" Body=\"test body\" SenderRole=\"agr\" RecipientDomain=\""
            + RECIPIENT
            + "\" RecipientRole=\"dso\" Class=\"transactional\"/>"
            +
            "</TestMessage>";

    private OutgoingQueueMDB outgoingQueueMDB;
    @Mock
    private TextMessage textMessage;
    @Mock
    private javax.jms.ObjectMessage objMessage;
    @Mock
    private SenderService senderService;

    /**
     * Setup for the test.
     *
     * @throws Exception
     */
    @Before
    public void setupResource() throws Exception {
        outgoingQueueMDB = new OutgoingQueueMDB();
        Whitebox.setInternalState(outgoingQueueMDB, "senderService", senderService);
        textMessage.setText(TEST_MSG_XML);
    }

    /**
     * Tests onMessage method.
     *
     * @throws JMSException
     * @throws BusinessException
     */
    @Test
    public void onMessage() throws JMSException, BusinessException {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(textMessage.getText()).thenReturn(TEST_MSG_XML);
        outgoingQueueMDB.onMessage(textMessage);

        verify(senderService, Mockito.times(1)).sendMessage(captor.capture());

        assertTrue(captor.getValue().contains(RECIPIENT));
    }

    /**
     * Tests onMessage method with JMSException.
     *
     * @throws JMSException
     */
    @Test(expected = RuntimeException.class)
    public void onMessageWithJMSException() throws JMSException {
        when(textMessage.getText()).thenThrow(new JMSException("Test Exception"));
        outgoingQueueMDB.onMessage(textMessage);
    }

    /**
     * Tests onMessage method.
     *
     * @throws JMSException
     */
    @Test(expected = RuntimeException.class)
    public void onMessageWithWrongMessageClass() {
        outgoingQueueMDB.onMessage(objMessage);
    }

}
