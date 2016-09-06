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

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.mockito.Matchers.anyString;

import energy.usef.core.service.helper.JMSHelperService;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import junit.framework.TestCase;

/**
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ MessageEndpoint.class })
public class MessageEndpointTest extends TestCase {

    private MessageEndpoint endpoint;

    @Mock
    private JMSHelperService jmsService;

    /**
     * Setup for the test.
     */
    @Before
    public void setupResource() {

        endpoint = new MessageEndpoint();
        Whitebox.setInternalState(endpoint, "jmsService", jmsService);
    }

    public void testSendMessage() throws Exception {
        Mockito.doNothing().when(jmsService).sendMessageToOutQueue(anyString());
        Response response = endpoint
                .sendMessage("<SignedMessage SenderDomain=\"stuff\" SenderRole=\"CRO\" Body=\"&lt;TestMessage /&gt;\"/>");
        Assert.assertEquals(OK.getStatusCode(), response.getStatus());
        Assert.assertEquals("Correctly sent XML message", response.getEntity());
    }

    public void testSendEmptyMessage() throws Exception {
        Mockito.doNothing().when(jmsService).sendMessageToOutQueue(anyString());
        Response response = endpoint.sendMessage("");
        Assert.assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
        Assert.assertEquals("No message body", response.getEntity());
    }

    public void testSendNullMessage() throws Exception {
        Mockito.doNothing().when(jmsService).sendMessageToOutQueue(anyString());
        Response response = endpoint.sendMessage(null);
        Assert.assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
        Assert.assertEquals("No message body", response.getEntity());
    }
}
