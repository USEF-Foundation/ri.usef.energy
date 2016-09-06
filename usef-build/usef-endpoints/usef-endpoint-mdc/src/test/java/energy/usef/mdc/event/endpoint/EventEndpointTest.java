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

package energy.usef.mdc.event.endpoint;

import energy.usef.mdc.workflow.CommonReferenceQueryEvent;

import javax.enterprise.event.Event;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class for the {@link EventEndpoint} of the MDC role.
 */
@RunWith(PowerMockRunner.class)
public class EventEndpointTest extends BaseResourceTest {

    private EventEndpoint eventEndpoint;

    @Mock
    private Event<CommonReferenceQueryEvent> commonReferenceQueryEventManager;
    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        eventEndpoint = new EventEndpoint();
        Whitebox.setInternalState(eventEndpoint, "commonReferenceQueryEventManager", commonReferenceQueryEventManager);

        dispatcher.getRegistry().addSingletonResource(eventEndpoint);
        ResteasyProviderFactory
                .getContextDataMap()
                .put(HttpServletRequest.class, Mockito.mock(HttpServletRequest.class));
    }

    /**
     * Removes initiated resources.
     */
    @After
    public void removeResource() {
        dispatcher.getRegistry().removeRegistrations(EventEndpoint.class);
    }

    @Test
    public void testSendCommonReferenceQueryEvent() throws Exception {
        Response response = eventEndpoint.sendCommonReferenceQueryEvent();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}
