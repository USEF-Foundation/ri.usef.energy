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

package energy.usef.brp.endpoint;

import java.net.URISyntaxException;

import javax.enterprise.event.Event;
import javax.servlet.http.HttpServletRequest;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for Rest FlexOrderEndpoint.
 */
@RunWith(PowerMockRunner.class)
public class FlexOrderEndpointTest extends BaseResourceTest {
    private static final String URL = "/FlexOrderEvent";

    private FlexOrderEndpoint flexOrderEndpoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Event<FlexOrderEndpoint> flexOrderEventManager;

    /**
     * Setup for the test.
     */
    @Before
    public void initResource() {
        flexOrderEndpoint = new FlexOrderEndpoint();

        Whitebox.setInternalState(flexOrderEndpoint, "flexOrderEventManager", flexOrderEventManager);
        dispatcher.getRegistry().addSingletonResource(flexOrderEndpoint);
        ResteasyProviderFactory.getContextDataMap()
                .put(HttpServletRequest.class, Mockito.mock(HttpServletRequest.class));

    }

    /**
     * Removes initiated resources.
     */
    @After
    public void removeResource() {
        dispatcher.getRegistry().removeRegistrations(FlexOrderEndpoint.class);
    }

    @Test
    public void testFlexOrderEndpoint() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        Assert.assertEquals(200, response.getStatus());
    }

}
