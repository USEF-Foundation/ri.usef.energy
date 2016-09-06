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

package energy.usef.dso.event.endpoint;

import energy.usef.core.config.Config;
import energy.usef.dso.workflow.operate.SendOperateEvent;
import energy.usef.dso.workflow.plan.commonreferenceupdate.CommonReferenceUpdateEvent;
import energy.usef.dso.workflow.plan.connection.forecast.CommonReferenceQueryEvent;
import energy.usef.dso.workflow.plan.connection.forecast.CreateConnectionForecastEvent;
import energy.usef.dso.workflow.settlement.collect.InitiateCollectOrangeRegimeDataEvent;
import energy.usef.dso.workflow.settlement.initiate.CollectSmartMeterDataEvent;
import energy.usef.dso.workflow.settlement.send.SendSettlementMessageEvent;
import energy.usef.dso.workflow.validate.create.flexorder.FlexOrderEvent;
import energy.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestEvent;

import java.net.URISyntaxException;
import java.util.Properties;

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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for Rest EventEndpoint.
 */
@RunWith(PowerMockRunner.class)
public class EventEndpointTest extends BaseResourceTest {
    private static final String URL = "/Event";

    private EventEndpoint eventEndpoint;

    @Mock
    private Config config;

    @Mock
    private Event<CreateConnectionForecastEvent> createConnectionForecastEventManager;

    @Mock
    private Event<SendOperateEvent> sendOperateEventManager;

    @Mock
    private Event<CommonReferenceUpdateEvent> commonReferenceUpdateEventManager;

    @Mock
    private Event<CommonReferenceQueryEvent> commonReferenceQueryEventManager;

    @Mock
    private Event<FlexOrderEvent> flexOrderEventManager;

    @Mock
    private Event<CreateFlexRequestEvent> createFlexRequestEventManager;

    @Mock
    private Event<SendSettlementMessageEvent> sendSettlementMessageEventManager;

    @Mock
    private Event<CollectSmartMeterDataEvent> collectSmartMeterDataEventManager;

    @Mock
    private Event<InitiateCollectOrangeRegimeDataEvent> initiateCollectOrangeRegimeDataEvent;

    @Mock
    private HttpServletRequest request;

    /**
     * Setup for the test.
     */
    @Before
    public void initResource() {
        eventEndpoint = new EventEndpoint();
        Whitebox.setInternalState(eventEndpoint, "config", config);
        Whitebox.setInternalState(eventEndpoint, "createConnectionForecastEventManager", createConnectionForecastEventManager);
        Whitebox.setInternalState(eventEndpoint, "commonReferenceUpdateEventManager", commonReferenceUpdateEventManager);
        Whitebox.setInternalState(eventEndpoint, "commonReferenceQueryEventManager", commonReferenceQueryEventManager);
        Whitebox.setInternalState(eventEndpoint, "flexOrderEventManager", flexOrderEventManager);
        Whitebox.setInternalState(eventEndpoint, "createFlexRequestEventManager", createFlexRequestEventManager);
        Whitebox.setInternalState(eventEndpoint, "sendSettlementMessageEventManager", sendSettlementMessageEventManager);
        Whitebox.setInternalState(eventEndpoint, "collectSmartMeterDataEventManager", collectSmartMeterDataEventManager);
        Whitebox.setInternalState(eventEndpoint, "sendOperateEventManager", sendOperateEventManager);
        Whitebox.setInternalState(eventEndpoint, "initiateCollectOrangeRegimeDataEvent", initiateCollectOrangeRegimeDataEvent);

        PowerMockito.when(config.getProperties()).thenReturn(new Properties());

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

    /**
     * Test if the scheduler of the scheduler is turned off.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void turnOnOffScheduler() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/Scheduler/false");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Assert.assertEquals(200, response.getStatus());
        // ByPass should be true
        // Assert.assertEquals("true", config.getProperties().getProperty(ConfigParam.BYPASS_SCHEDULED_EVENTS.name()));
    }

    /**
     * Test if the CreateConnectionForecastEvent is fired.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testCreateConnectionForecastEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/CreateConnectionForecastEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(createConnectionForecastEventManager, Mockito.times(1)).fire(
                Matchers.any(CreateConnectionForecastEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the SendOperateUpdateEvent is fired.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testSendOperateEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/SendOperateEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(sendOperateEventManager, Mockito.times(1)).fire(Matchers.any(SendOperateEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the CommonReferenceUpdateEvent is fired.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testCommonReferenceUpdateEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/CommonReferenceUpdateEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(commonReferenceUpdateEventManager, Mockito.times(1)).fire(Matchers.any(CommonReferenceUpdateEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the ConnectionForecastEvent is fired.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testConnectionForecastEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/CommonReferenceQueryEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(commonReferenceQueryEventManager, Mockito.times(1)).fire(Matchers.any(CommonReferenceQueryEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the FlexOrderEvent is fired.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testFlexOrderEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/FlexOrderEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(flexOrderEventManager, Mockito.times(1)).fire(Matchers.any(FlexOrderEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the {@link SendSettlementMessageEvent} is fired.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testSendSettlementMessageEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/SendSettlementMessageEvent/2015/4");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(sendSettlementMessageEventManager, Mockito.times(1)).fire(Matchers.any(SendSettlementMessageEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the {@link CollectSmartMeterDataEvent} is fired.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testInitiateSettlementEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/InitiateSettlementEvent/2014-02-17");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(collectSmartMeterDataEventManager, Mockito.times(1)).fire(Matchers.any(CollectSmartMeterDataEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the CreateFlexRequestEvent is fired.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testCreateFlexRequestEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/CreateFlexRequestEvent/entityAddress/2015-01-22/1,2,3,4,5");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(createFlexRequestEventManager, Mockito.times(1)).fire(Matchers.any(CreateFlexRequestEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the {@link InitiateCollectOrangeRegimeDataEvent} is fired.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testInitiateCollectOrangeRegimeDataEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/InitiateCollectOrangeRegimeDataEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(initiateCollectOrangeRegimeDataEvent, Mockito.times(1)).fire(
                Matchers.any(InitiateCollectOrangeRegimeDataEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

}
