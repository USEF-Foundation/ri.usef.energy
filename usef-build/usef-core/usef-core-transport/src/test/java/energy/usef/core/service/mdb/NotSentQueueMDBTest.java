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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.helper.OutgoingErrorMessageDispatcherHelperService;
import energy.usef.core.util.XMLUtil;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the NotSentQueueMDB class.
 */
@RunWith(PowerMockRunner.class)
public class NotSentQueueMDBTest {
    @Mock
    private OutgoingErrorMessageDispatcherHelperService outgoingErrorMessageDispatcherHelperService;

    private NotSentQueueMDB notSentQueueMDB;

    @Mock
    private TextMessage textMessage;

    @Mock
    private javax.jms.ObjectMessage objMessage;

    /**
     * Setup for the test.
     *
     * @throws Exception
     */
    @Before
    public void init() throws Exception {
        notSentQueueMDB = new NotSentQueueMDB();
        setInternalState(notSentQueueMDB, "outgoingErrorMessageDispatcherHelperService",
                outgoingErrorMessageDispatcherHelperService);
        String xml = XMLUtil.messageObjectToXml(new CommonReferenceQuery());
        textMessage.setText(xml);
    }

    /**
     * Tests onMessage method with JMSException.
     *
     * @throws JMSException
     */
    @Test(expected = RuntimeException.class)
    public void onMessageWithJMSExceptionTest() throws JMSException {
        when(textMessage.getText()).thenThrow(
                new JMSException("Test Exception"));
        notSentQueueMDB.onMessage(textMessage);
    }

    /**
     * Tests onMessage method.
     *
     * @throws JMSException
     */
    @Test(expected = RuntimeException.class)
    public void onMessageWithWrongMessageClassTest() {
        notSentQueueMDB.onMessage(objMessage);
    }

    /**
     * Tests onMessage method.
     *
     * @throws JMSException
     * @throws BusinessException
     */
    @Test
    public void onMessageTest() throws BusinessException {
        notSentQueueMDB.onMessage(textMessage);

        verify(outgoingErrorMessageDispatcherHelperService, times(1)).dispatch(Matchers.anyString());
    }

}
