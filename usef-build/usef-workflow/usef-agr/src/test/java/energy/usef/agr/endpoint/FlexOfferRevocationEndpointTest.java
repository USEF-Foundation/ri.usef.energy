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

import static org.junit.Assert.assertEquals;

import energy.usef.agr.workflow.validate.flexoffer.FlexOfferRevocationEvent;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the FlexOfferRevocationEndpoint class.
 */
@RunWith(PowerMockRunner.class)
public class FlexOfferRevocationEndpointTest extends BaseResourceTest {
    private static final String URL = "/FlexOfferRevocationEndpoint/revokeFlexOffer";
    private static final String BAD_URL = "/sjgqsqsgfzydsfz";

    private static final String FLEX_OFFER_REVOKE_STR = "/12345/dso.usef-example.com/DSO";
    private static final String FLEX_OFFER_REVOKE_WRONG_SEQUENCE_STR = "/1sd2345/dso.usef-example.com/DSO";
    private static final String FLEX_OFFER_REVOKE_NO_RECIPIENT_STR = "/1sd2345";

    private FlexOfferRevocationEndpoint flexOfferRevocationEndpoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Event<FlexOfferRevocationEvent> eventManager;

    @Mock
    private CorePlanboardBusinessService planboardBusinessService;

    /**
     * Setup for the test.
     */
    @Before
    public void initResource() {
        flexOfferRevocationEndpoint = new FlexOfferRevocationEndpoint();
        Whitebox.setInternalState(flexOfferRevocationEndpoint, "eventManager", eventManager);
        Whitebox.setInternalState(flexOfferRevocationEndpoint, "planboardBusinessService", planboardBusinessService);
        dispatcher.getRegistry().addSingletonResource(flexOfferRevocationEndpoint);
        ResteasyProviderFactory
                .getContextDataMap()
                .put(HttpServletRequest.class, Mockito.mock(HttpServletRequest.class));
    }

    /**
     * Removes inited resources.
     */
    @After
    public void removeResource() {
        dispatcher.getRegistry().removeRegistrations(FlexOfferRevocationEndpoint.class);
    }

    /**
     * Tests the service with not correct URL.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testBadRequestUrl() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(BAD_URL);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        Assert.assertEquals(404, response.getStatus());
    }

    /**
     * Tests the revokeFlexOffer method.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testRevokeFlexOffer() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + FLEX_OFFER_REVOKE_STR);
        MockHttpResponse response = new MockHttpResponse();

        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentStatus(DocumentStatus.ACCEPTED);

        Mockito.when(
                planboardBusinessService.findSinglePlanboardMessage(Matchers.eq(12345L), Matchers.eq(DocumentType.FLEX_OFFER),
                        Matchers.eq("dso.usef-example.com"))).thenReturn(planboardMessage);

        // test
        dispatcher.invoke(request, response);

        Mockito.verify(planboardBusinessService, Mockito.times(1)).findSinglePlanboardMessage(Matchers.eq(12345L),
                Matchers.eq(DocumentType.FLEX_OFFER), Matchers.eq("dso.usef-example.com"));

        ArgumentCaptor<FlexOfferRevocationEvent> eventCaptor = ArgumentCaptor.forClass(FlexOfferRevocationEvent.class);

        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(12345L, eventCaptor.getValue().getFlexOfferSequenceNumber().longValue());
        Assert.assertEquals("dso.usef-example.com", eventCaptor.getValue().getRecipientDomainName());
        Assert.assertEquals(USEFRole.DSO, eventCaptor.getValue().getUsefRole());
    }

    /**
     * Tests the revokeFlexOffer with wrong SequenceNumber method.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testRevokeFlexOfferWithWrongSequenceNumber() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/" + FLEX_OFFER_REVOKE_WRONG_SEQUENCE_STR);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(400, response.getStatus());
    }

    /**
     * Tests the revokeFlexOffer with no recipient method.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testRevokeFlexOfferWithNoRecipient() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "/" + FLEX_OFFER_REVOKE_NO_RECIPIENT_STR);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(404, response.getStatus());
    }

}
