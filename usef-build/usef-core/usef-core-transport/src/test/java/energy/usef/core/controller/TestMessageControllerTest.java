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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.powermock.reflect.Whitebox.setInternalState;
import energy.usef.core.data.xml.bean.message.TestMessage;
import energy.usef.core.data.xml.bean.message.TestMessageResponse;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.XMLUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the TestMessageController class.
 */
@RunWith(PowerMockRunner.class)
public class TestMessageControllerTest {
    private static final String MESSAGE_ID = "testMessageId";
    private static final String SENDER_DOMAIN = "testSenderDomain";

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            +
            "<TestMessage>"
            +
            "<MessageMetadata SenderDomain=\"" + SENDER_DOMAIN
            + "\" Body=\"test body\" SenderRole=\"AGR\" RecipientDomain=\"localhost\" RecipientRole=\"DSO\" MessageID=\""
            + MESSAGE_ID
            + "\" Precedence=\"Transactional\"/>"
            +
            "</TestMessage>";
    @Mock
    private JMSHelperService jmsService;

    @Mock
    private MessageService messageService;

    private TestMessageController controller;

    @Before
    public void init() throws Exception {
        controller = new TestMessageController();
        setInternalState(controller, "jmsService", jmsService);
        setInternalState(controller, "messageService", messageService);
    }

    /**
     * Tests TestMessageController.action method.
     *
     * @throws Exception
     */
    @Test
    public void actionTest() throws Exception {
        ArgumentCaptor<String> captor = forClass(String.class);

        TestMessage xmlObject = (TestMessage) XMLUtil.xmlToMessage(XML);
        controller.action(xmlObject, null);

        verify(jmsService).sendMessageToOutQueue(captor.capture());

        String xml = captor.getValue();
        TestMessageResponse message = (TestMessageResponse) XMLUtil.xmlToMessage(xml);
        assertEquals(SENDER_DOMAIN, message.getMessageMetadata().getRecipientDomain());
    }

}
