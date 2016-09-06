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

package energy.usef.agr.endpoint;

import energy.usef.agr.endpoint.dto.ConnectionRestDto;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.agr.workflow.plan.updateforecast.UpdateConnectionForecastEvent;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.enterprise.event.Event;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests for the {@link UpdateConnectionForecastEndpoint}
 */
@RunWith(PowerMockRunner.class)
public class UpdateConnectionForecastEndpointTest extends BaseResourceTest {
    private static final String URL = "/connectionportfolio/update";
    private static final String ID = "ea.3213214";

    private UpdateConnectionForecastEndpoint endpoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private AgrPlanboardBusinessService agrPlanboardBusinessService;

    @Mock
    private Event<UpdateConnectionForecastEvent> eventManager;

    /**
     * Setup for the test.
     */
    @Before
    public void initResource() {
        endpoint = new UpdateConnectionForecastEndpoint();
        Whitebox.setInternalState(endpoint, "eventManager", eventManager);
        dispatcher.getRegistry().addSingletonResource(endpoint);
        ResteasyProviderFactory
                .getContextDataMap()
                .put(HttpServletRequest.class, Mockito.mock(HttpServletRequest.class));
    }

    /**
     * Removes initiated resources.
     */
    @After
    public void removeResource() {
        dispatcher.getRegistry().removeRegistrations(UpdateConnectionForecastEndpoint.class);
    }

    @Test
    public void updateSingleConnectionForecastPassesIdSuccess() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/" + ID);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        ArgumentCaptor<UpdateConnectionForecastEvent> argument = ArgumentCaptor.forClass(UpdateConnectionForecastEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(argument.capture());
        Assert.assertNotNull(argument.getValue().getConnections());
        Assert.assertTrue(argument.getValue().getConnections().get().contains(ID));
    }

    @Test
    public void updateMultipleConnectionForecastsSuccessExpected() throws URISyntaxException, IOException {

        MockHttpRequest request = MockHttpRequest.create("POST", URL);
        request.contentType(MediaType.APPLICATION_JSON_TYPE);

        ConnectionRestDto user = new ConnectionRestDto();
        user.getConnectionEntityAddressList().add("12345");
        user.getConnectionEntityAddressList().add("54321");

        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = mapper.writeValueAsString(user);
        request.content(jsonInString.getBytes(StandardCharsets.UTF_8));

        System.out.println(jsonInString);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        ArgumentCaptor<UpdateConnectionForecastEvent> argument = ArgumentCaptor.forClass(UpdateConnectionForecastEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(argument.capture());
        Assert.assertTrue(argument.getValue().getConnections().get().contains("12345"));
    }

    @Test
    public void updateAllConnectionForecastsSuccessExcpected() throws URISyntaxException, IOException {

        MockHttpRequest request = MockHttpRequest.get(URL + "/all");
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        ArgumentCaptor<UpdateConnectionForecastEvent> argument = ArgumentCaptor.forClass(UpdateConnectionForecastEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(argument.capture());
        Assert.assertNotNull(argument.getValue().getConnections());
        Assert.assertFalse(argument.getValue().getConnections().isPresent());
    }


}
