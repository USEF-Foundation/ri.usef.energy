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

import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import energy.usef.core.service.helper.DispatcherHelperService;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the InQueueMDBService class.
 */
@RunWith(PowerMockRunner.class)
public class IncomingQueueMDBTest {
    private static final String RECIPIENT = "recipient.test.com";
    private static final String TEST_MSG_XML = "<TestMessage><MessageMetadata Sender=\"sender.test.com\" Recipient=\""
            + RECIPIENT
            + "\" Body=\"Test Body\" SenderRole=\"Test Sender Role\" MessageID=\"Test Message ID\"/></TestMessage>";

    @Mock
    private DispatcherHelperService dispatcherService;
    @Mock
    private TextMessage textMessage;
    @Mock
    private javax.jms.ObjectMessage objMessage;
    private IncomingQueueMDB incomingQueueMDB;

    /**
     * Setup for the test.
     *
     * @throws Exception
     */
    @Before
    public void setupResource() throws Exception {
        incomingQueueMDB = new IncomingQueueMDB();
        setInternalState(incomingQueueMDB, "dispatcherService", dispatcherService);
        textMessage.setText(TEST_MSG_XML);
    }

    /**
     * Tests onMessage method with JMSException.
     *
     * @throws JMSException
     */
    @Test(expected = RuntimeException.class)
    public void onMessageWithJMSException() throws JMSException {
        when(textMessage.getText()).thenThrow(
                new JMSException("Test Exception"));
        incomingQueueMDB.onMessage(textMessage);
    }

    /**
     * Tests onMessage method.
     *
     * @throws JMSException
     */
    @Test(expected = RuntimeException.class)
    public void onMessageWithWrongMessageClass() {
        incomingQueueMDB.onMessage(objMessage);
    }

}
