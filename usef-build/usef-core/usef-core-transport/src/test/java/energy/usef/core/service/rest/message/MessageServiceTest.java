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

package energy.usef.core.service.rest.message;

import static org.junit.Assert.assertEquals;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import energy.usef.core.endpoint.MessageEndpoint;
import energy.usef.core.service.helper.JMSHelperService;

import java.net.URISyntaxException;

import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * JUnit test for the MessageService class.
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageServiceTest extends BaseResourceTest {
    private static final String URL = "/MessageService/sendMessage";
    private static final String BAD_URL = "/hsqsfqhgsfqgs";
    private static final String BAD_CLIENT_STR = "xxxxxxxxxxxxxxxxx d";
    private static final String CLIENT_STR = "ea1.1992-01.localhost:aggregator.1 ea1.1994-01.localhost:dso.1";

    private MessageEndpoint messageService;
    private JMSHelperService jmsService;

    @Mock
    private JMSContext context;

    @Mock
    private JMSProducer producer;

    /**
     * Setup for the test.
     */
    @Before
    public void setupResource() {

        messageService = new MessageEndpoint();
        jmsService = new JMSHelperService();

        setInternalState(jmsService, "context", context);
        setInternalState(messageService, "jmsService", jmsService);

        dispatcher.getRegistry().addSingletonResource(messageService);
    }

    /**
     * Removes inited resources.
     */
    @After
    public void removeResource() {
        dispatcher.getRegistry().removeRegistrations(MessageEndpoint.class);
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
        request.content(CLIENT_STR.getBytes());

        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        assertEquals(404, response.getStatus());
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
        request.content(CLIENT_STR.getBytes());

        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        // 415 HTTP Error 415 Unsupported media type
        assertEquals(415, response.getStatus());
    }

    /**
     * Tests the sendMsg method with bad client data.
     *
     * @throws URISyntaxException
     */
    @Test(expected = RuntimeException.class)
    public void sendMsgBagClientDataTest() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.post(URL);
        request.contentType(TEXT_XML);
        request.content(BAD_CLIENT_STR.getBytes());

        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);
    }

    /**
     * Tests the sendMsg method.
     *
     * @throws URISyntaxException
     */
    @Test
    public void sendMsgTest() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.post(URL);
        request.contentType(TEXT_XML);
        request.content(CLIENT_STR.getBytes());

        MockHttpResponse response = new MockHttpResponse();

        Mockito.when(context.createProducer()).thenReturn(producer);

        dispatcher.invoke(request, response);

        Mockito.verify(producer, Mockito.times(1)).send(Matchers.any(Destination.class), Matchers.any(String.class));

        assertEquals(200, response.getStatus());
    }
}
