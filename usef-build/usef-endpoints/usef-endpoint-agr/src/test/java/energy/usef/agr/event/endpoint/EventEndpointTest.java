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

package energy.usef.agr.event.endpoint;

import energy.usef.agr.workflow.nonudi.goals.AgrNonUdiSetAdsGoalsEvent;
import energy.usef.agr.workflow.nonudi.initialize.AgrNonUdiInitializeEvent;
import energy.usef.agr.workflow.nonudi.operate.AgrNonUdiRetrieveAdsGoalRealizationEvent;
import energy.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyEvent;
import energy.usef.agr.workflow.operate.deviation.DetectDeviationEvent;
import energy.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastEvent;
import energy.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesEvent;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizeFlagHolder;
import energy.usef.agr.workflow.plan.commonreferenceupdate.CommonReferenceUpdateEvent;
import energy.usef.agr.workflow.plan.connection.forecast.CommonReferenceQueryEvent;
import energy.usef.agr.workflow.plan.connection.profile.CreateConnectionProfileEvent;
import energy.usef.agr.workflow.plan.create.aplan.CreateAPlanEvent;
import energy.usef.agr.workflow.plan.create.aplan.FinalizeAPlansEvent;
import energy.usef.agr.workflow.validate.create.dprognosis.CreateDPrognosisEvent;
import energy.usef.agr.workflow.validate.flexoffer.FlexOfferEvent;
import energy.usef.core.config.Config;
import energy.usef.core.event.MoveToOperateEvent;
import energy.usef.core.event.StartValidateEvent;
import energy.usef.core.util.DateTimeUtil;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.test.BaseResourceTest;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.enterprise.event.Event;
import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.Properties;

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
    private Event<ControlActiveDemandSupplyEvent> cadsEventManager;

    @Mock
    private Event<DetectDeviationEvent> detectDeviationEventManager;

    @Mock
    private Event<CreateDPrognosisEvent> createDPrognosisEventManager;

    @Mock
    private Event<CreateAPlanEvent> createAPlanEventManager;

    @Mock
    private Event<FinalizeAPlansEvent> updateAPlanEventManager;

    @Mock
    private Event<FlexOfferEvent> flexOfferEventManager;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Event<ReCreatePrognosesEvent> reCreatePrognosesEventManager;

    @Mock
    private Event<IdentifyChangeInForecastEvent> identifyChangeInForecastEventManager;

    @Mock
    private Event<MoveToOperateEvent> moveToOperateEventManager;

    @Mock
    private Event<StartValidateEvent> startValidateEventManager;

    @Mock
    private Event<AgrNonUdiInitializeEvent> agrNonUdiInitializeEventManager;

    @Mock
    private Event<AgrNonUdiSetAdsGoalsEvent> agrNonUdiSetAdsGoalsEventManager;

    @Mock
    private Event<AgrNonUdiRetrieveAdsGoalRealizationEvent> agrNonUdiRetrieveAdsGoalRealizationEventManager;

    @Mock
    private ReOptimizeFlagHolder reOptimizeFlagHolder;

    @Mock
    private Event<CreateConnectionProfileEvent> createConnectionProfileEventManager;

    /**
     * Setup for the test.
     */
    @Before
    public void initResource() {
        eventEndpoint = new EventEndpoint();
        Whitebox.setInternalState(eventEndpoint, config);
        Whitebox.setInternalState(eventEndpoint, "commonReferenceUpdateEventManager", commonReferenceUpdateEventManager);
        Whitebox.setInternalState(eventEndpoint, "commonReferenceQueryEventManager", commonReferenceQueryEventManager);
        Whitebox.setInternalState(eventEndpoint, "cadsEventManager", cadsEventManager);
        Whitebox.setInternalState(eventEndpoint, "detectDeviationEventManager", detectDeviationEventManager);
        Whitebox.setInternalState(eventEndpoint, "createDPrognosisEventManager", createDPrognosisEventManager);
        Whitebox.setInternalState(eventEndpoint, "createAPlanEventManager", createAPlanEventManager);
        Whitebox.setInternalState(eventEndpoint, "updateAPlanEventManager", updateAPlanEventManager);
        Whitebox.setInternalState(eventEndpoint, "flexOfferEventManager", flexOfferEventManager);
        Whitebox.setInternalState(eventEndpoint, "reCreatePrognosesEventManager", reCreatePrognosesEventManager);
        Whitebox.setInternalState(eventEndpoint, "identifyChangeInForecastEventManager", identifyChangeInForecastEventManager);
        Whitebox.setInternalState(eventEndpoint, "moveToOperateEventManager", moveToOperateEventManager);
        Whitebox.setInternalState(eventEndpoint, "startValidateEventManager", startValidateEventManager);
        Whitebox.setInternalState(eventEndpoint, "agrNonUdiInitializeEventManager", agrNonUdiInitializeEventManager);
        Whitebox.setInternalState(eventEndpoint, "agrNonUdiSetAdsGoalsEventManager", agrNonUdiSetAdsGoalsEventManager);
        Whitebox.setInternalState(eventEndpoint, "createConnectionProfileEventManager", createConnectionProfileEventManager);
        Whitebox.setInternalState(eventEndpoint, "agrNonUdiRetrieveAdsGoalRealizationEventManager",
                agrNonUdiRetrieveAdsGoalRealizationEventManager);
        Whitebox.setInternalState(eventEndpoint, "reOptimizeFlagHolder", reOptimizeFlagHolder);

        PowerMockito.when(config.getProperties()).thenReturn(new Properties());

        dispatcher.getRegistry().addSingletonResource(eventEndpoint);
        ResteasyProviderFactory.getContextDataMap().put(HttpServletRequest.class, Mockito.mock(HttpServletRequest.class));
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
     * Test if the ControlActiveDemandSupplyEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testControlActiveDemandSupplyEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/ControlActiveDemandSupplyEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(cadsEventManager, Mockito.times(1)).fire(Matchers.any(ControlActiveDemandSupplyEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the DetectDeviationEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testDetectDeviationEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/DetectDeviationEvent/" + DateTimeUtil.getCurrentDate());
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(detectDeviationEventManager, Mockito.times(1)).fire(Matchers.any(DetectDeviationEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the CreateDPrognosisEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testCreateDPrognosisEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/CreateDPrognosisEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(createDPrognosisEventManager, Mockito.times(1)).fire(Matchers.any(CreateDPrognosisEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the CreateAPlanEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testCreateAPlanEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/CreateAPlanEvent/2015-02-12");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(createAPlanEventManager, Mockito.times(1)).fire(Matchers.any(CreateAPlanEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the UpdateAPlanEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testUpdateAPlanEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/UpdateAPlanEvent/2015-02-12");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(updateAPlanEventManager, Mockito.times(1)).fire(Matchers.any(FinalizeAPlansEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the FlexOfferEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testFlexOfferEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/FlexOfferEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(flexOfferEventManager, Mockito.times(1)).fire(Matchers.any(FlexOfferEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test whether the ReCreatePrognosesEvent is correctly fired with the endpoint.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testFireReCreatePrognosesEventManager() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/ReCreatePrognosesEvent/2015-02-02");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        ArgumentCaptor<ReCreatePrognosesEvent> eventCaptor = ArgumentCaptor.forClass(ReCreatePrognosesEvent.class);
        Mockito.verify(reCreatePrognosesEventManager, Mockito.times(1)).fire(eventCaptor.capture());
        Assert.assertEquals(200, response.getStatus());
        ReCreatePrognosesEvent capturedEvent = eventCaptor.getValue();
        Assert.assertNotNull(capturedEvent);
        Assert.assertEquals(new LocalDate(2015, 2, 2), capturedEvent.getPeriod());
    }

    @Test
    public void testSendCreateDPrognosisEventForPeriodForCongestionPoint() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/CreateDPrognosisEvent/2015-02-02/ean.123456789012345678");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        ArgumentCaptor<CreateDPrognosisEvent> eventCaptor = ArgumentCaptor.forClass(CreateDPrognosisEvent.class);
        Mockito.verify(createDPrognosisEventManager, Mockito.times(1)).fire(eventCaptor.capture());
        Assert.assertEquals(200, response.getStatus());
        CreateDPrognosisEvent capturedEvent = eventCaptor.getValue();
        Assert.assertNotNull(capturedEvent);
        Assert.assertEquals("ean.123456789012345678", capturedEvent.getCongestionPoint());
        Assert.assertEquals(new LocalDate(2015, 2, 2), capturedEvent.getPeriod());
    }

    /**
     * Test if the IdentifyChangeInForecastEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testIdentifyChangeInForecastEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/IdentifyChangeInForecastEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(identifyChangeInForecastEventManager, Mockito.times(1))
                .fire(Matchers.any(IdentifyChangeInForecastEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the MoveToOperateEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testFireMoveToOperateEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/MoveToOperateEvent/2015-02-12/12");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(moveToOperateEventManager, Mockito.times(1)).fire(Matchers.any(MoveToOperateEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testFireStartValidateEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/StartValidatePhase/2015-02-12");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        ArgumentCaptor<StartValidateEvent> startValidateEventArgumentCaptor = ArgumentCaptor.forClass(StartValidateEvent.class);

        Mockito.verify(startValidateEventManager, Mockito.times(1)).fire(startValidateEventArgumentCaptor.capture());
        Assert.assertEquals(200, response.getStatus());

        StartValidateEvent firedEvent = startValidateEventArgumentCaptor.getValue();
        Assert.assertNotNull(firedEvent);
        Assert.assertEquals(new LocalDate(2015, 2, 12), firedEvent.getPeriod());
    }

    /**
     * Test if the AgrNonUdiInitializeEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testAgrNonUdiInitializeEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/InitializePowerMatcherEvent");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(agrNonUdiInitializeEventManager, Mockito.times(1)).fire(Matchers.any(AgrNonUdiInitializeEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the AgrNonUdiSetAdsGoalsEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testAgrNonUdiSetAdsGoals() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/SetAdsGoalsEvent/2015-01-01/brp1.usef-example.com");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(agrNonUdiSetAdsGoalsEventManager, Mockito.times(1)).fire(Matchers.any(AgrNonUdiSetAdsGoalsEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test if the AgrNonUdiRetrieveAdsGoalRealizationEvent is fired.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testFireAgrNonUdiRetrieveAdsGoalRealizationEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/NonUdiRetrieveAdsGoalRealization");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(agrNonUdiRetrieveAdsGoalRealizationEventManager, Mockito.times(1))
                .fire(Matchers.any(AgrNonUdiRetrieveAdsGoalRealizationEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testResetReOptimizePortfolioFlags() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/ReOptimizePortfolioState/reset/2015-02-02");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        LocalDate period = new LocalDate(2015, 2, 2);
        Mockito.verify(reOptimizeFlagHolder, Mockito.times(1)).setIsRunning(Matchers.eq(period), Matchers.eq(false));
        Mockito.verify(reOptimizeFlagHolder, Mockito.times(1)).setToBeReoptimized(Matchers.eq(period), Matchers.eq(false));
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testFireCreateConnectionProfileEvent() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/CreateConnectionProfileEvent/2015-02-02");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Mockito.verify(createConnectionProfileEventManager, Mockito.times(1))
                .fire(Matchers.any(CreateConnectionProfileEvent.class));
        Assert.assertEquals(200, response.getStatus());
    }
}
