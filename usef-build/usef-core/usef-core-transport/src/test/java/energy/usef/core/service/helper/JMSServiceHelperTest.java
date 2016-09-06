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

package energy.usef.core.service.helper;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Queue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * JUnit test for the JMSService class.
 */
@RunWith(MockitoJUnitRunner.class)
public class JMSServiceHelperTest {
    private static final String TEST_MSG = "<test>test</test>";

    @Mock
    private Queue outQueue;
    @Mock
    private Queue inQueue;
    @Mock
    private JMSContext context;
    @Mock
    private JMSProducer producer;
    private JMSHelperService jMSService;

    /**
     * Setup for the test.
     *
     * @throws Exception
     */
    @Before
    public void setupResource() throws Exception {
        jMSService = new JMSHelperService();

        setInternalState(jMSService, "outQueue", outQueue);
        setInternalState(jMSService, "inQueue", inQueue);
        setInternalState(jMSService, "context", context);
    }

    /**
     * Tests the sendMessageToOutQueue method.
     *
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Test
    public void sendMessageToOutQueueTest() throws InstantiationException,
            IllegalAccessException {
        when(context.createProducer()).thenReturn(producer);
        jMSService.sendMessageToOutQueue(TEST_MSG);
        verify(producer, Mockito.times(1)).send(Matchers.eq(outQueue),
                Matchers.eq(TEST_MSG));
    }

    /**
     * Tests the sendMessageToOutQueue method with exception.
     *
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Test(expected = RuntimeException.class)
    public void sendMessageToOutQueueWithExceptionTest() throws InstantiationException,
            IllegalAccessException {
        when(context.createProducer()).thenReturn(producer);
        when(producer.send(outQueue, TEST_MSG)).thenThrow(new Exception());
        jMSService.sendMessageToOutQueue(TEST_MSG);
    }

    /**
     * Tests the sendMessageToInQueue method.
     *
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Test
    public void sendMessageToInQueueTest() throws InstantiationException,
            IllegalAccessException {
        when(context.createProducer()).thenReturn(producer);
        jMSService.sendMessageToInQueue(TEST_MSG);
        verify(producer, Mockito.times(1)).send(Matchers.eq(inQueue),
                Matchers.eq(TEST_MSG));
    }

    /**
     * Tests the sendMessageToInQueue method with exception.
     *
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Test(expected = RuntimeException.class)
    public void sendMessageToInQueueWithExceptionTest() throws InstantiationException,
            IllegalAccessException {
        when(context.createProducer()).thenReturn(producer);
        when(producer.send(inQueue, TEST_MSG)).thenThrow(new Exception());
        jMSService.sendMessageToInQueue(TEST_MSG);
    }

}
