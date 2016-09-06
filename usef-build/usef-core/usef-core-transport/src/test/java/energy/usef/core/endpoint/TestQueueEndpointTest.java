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

import energy.usef.core.service.helper.JMSHelperService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link TestQueueEndpoint} class.
 */
@RunWith(PowerMockRunner.class)
public class TestQueueEndpointTest {

    @Mock
    private JMSHelperService jmsHelperService;

    private TestQueueEndpoint endpoint;

    @Before
    public void init() {
        endpoint = new TestQueueEndpoint();
        Whitebox.setInternalState(endpoint, jmsHelperService);
    }

    @Test
    public void testSentToInQueue() {
        endpoint.sentToInQueue("String");
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToInQueue(Matchers.eq("String"));
    }

    @Test
    public void testSentToOutQueue() {
        endpoint.sentToOutQueue("String");
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(Matchers.eq("String"));
    }

}
