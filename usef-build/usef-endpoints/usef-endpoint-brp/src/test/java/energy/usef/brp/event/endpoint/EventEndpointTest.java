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

package energy.usef.brp.event.endpoint;

import energy.usef.brp.workflow.plan.aplan.finalize.FinalizeAPlansEvent;
import energy.usef.brp.workflow.plan.commonreferenceupdate.CommonReferenceUpdateEvent;
import energy.usef.brp.workflow.plan.connection.forecast.CommonReferenceQueryEvent;
import energy.usef.brp.workflow.plan.connection.forecast.ReceivedAPlanEvent;
import energy.usef.brp.workflow.plan.flexorder.place.FlexOrderEvent;
import energy.usef.brp.workflow.settlement.initiate.CollectSmartMeterDataEvent;
import energy.usef.core.config.Config;
import energy.usef.core.event.DayAheadClosureEvent;
import energy.usef.core.util.DateTimeUtil;

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
import org.mockito.ArgumentCaptor;
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
    private Event<CommonReferenceUpdateEvent> commonReferenceUpdateEventManager;
    @Mock
    private Event<CommonReferenceQueryEvent> commonReferenceQueryEventManager;
    @Mock
    private Event<FlexOrderEvent> flexOrderEventManager;
    @Mock
    private Event<CollectSmartMeterDataEvent> collectSmartMeterDataEventManager;
    @Mock
    private Event<ReceivedAPlanEvent> receivedAPlanEventManager;
    @Mock
    private Event<FinalizeAPlansEvent> finalizeAPlansEventManager;
    @Mock
    private Event<DayAheadClosureEvent> dayAheadClosureEventEventManager;

    @Mock
    private HttpServletRequest request;

    /**
     * Setup for the test.
     */
    @Before
    public void initResource() {
        eventEndpoint = new EventEndpoint();
        Whitebox.setInternalState(eventEndpoint, "config", config);
        Whitebox.setInternalState(eventEndpoint, "commonReferenceUpdateEventManager", commonReferenceUpdateEventManager);
        Whitebox.setInternalState(eventEndpoint, "commonReferenceQueryEventManager", commonReferenceQueryEventManager);
        Whitebox.setInternalState(eventEndpoint, "flexOrderEventManager", flexOrderEventManager);
        Whitebox.setInternalState(eventEndpoint, "collectSmartMeterDataEventManager", collectSmartMeterDataEventManager);
        Whitebox.setInternalState(eventEndpoint, "receivedAPlanEventManager", receivedAPlanEventManager);
        Whitebox.setInternalState(eventEndpoint, "finalizeAPlansEventManager", finalizeAPlansEventManager);
        Whitebox.setInternalState(eventEndpoint, "dayAheadClosureEventEventManager", dayAheadClosureEventEventManager);

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
     * Test if the CommonReferenceQueryEvent is fired.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testCommonReferenceQueryEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/CommonReferenceQueryEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(commonReferenceQueryEventManager, Mockito.times(1)).fire(Matchers.any(CommonReferenceQueryEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * .
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
     * Test for initiate settlement event.
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

    @Test
    public void testSendReceivedAPlanEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/ReceivedAPlanEvent/2014-02-17");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        ArgumentCaptor<ReceivedAPlanEvent> eventCaptor = ArgumentCaptor.forClass(ReceivedAPlanEvent.class);
        Mockito.verify(receivedAPlanEventManager, Mockito.times(1)).fire(eventCaptor.capture());
        ReceivedAPlanEvent event = eventCaptor.getValue();
        Assert.assertEquals(DateTimeUtil.parseDate("2014-02-17"), event.getPeriod());
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testFinalizeAPlansEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/FinalizeAPlansEvent/2014-02-17");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        ArgumentCaptor<FinalizeAPlansEvent> eventCaptor = ArgumentCaptor.forClass(FinalizeAPlansEvent.class);
        Mockito.verify(finalizeAPlansEventManager, Mockito.times(1)).fire(eventCaptor.capture());
        FinalizeAPlansEvent event = eventCaptor.getValue();
        Assert.assertEquals(DateTimeUtil.parseDate("2014-02-17"), event.getPeriod());
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testDayAheadClosureEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/DayAheadClosureEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(dayAheadClosureEventEventManager, Mockito.times(1)).fire(Matchers.any(DayAheadClosureEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }
}
