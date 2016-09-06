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

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioEvent;

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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for Rest endpoint ReOptimizePortfolio.
 */
@RunWith(PowerMockRunner.class)
public class ReOptimizePortfolioEndpointTest extends BaseResourceTest {
    private static final String URL = "/ReOptimizePortfolio";
    private static final String REOPTIMIZE_PARAM = "/2014-12-20";
    private static final String REOPTIMIZE_WRONG_DATE = "/1998-21-130";

    private ReOptimizePortfolioEndpoint reOptimizePortfolioEndpoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Event<ReOptimizePortfolioEvent> eventManager;

    /**
     * Setup for the test.
     */
    @Before
    public void initResource() {
        reOptimizePortfolioEndpoint = new ReOptimizePortfolioEndpoint();
        Whitebox.setInternalState(reOptimizePortfolioEndpoint, "eventManager", eventManager);
        dispatcher.getRegistry().addSingletonResource(reOptimizePortfolioEndpoint);
        ResteasyProviderFactory
                .getContextDataMap()
                .put(HttpServletRequest.class, Mockito.mock(HttpServletRequest.class));
    }

    /**
     * Removes initiated resources.
     */
    @After
    public void removeResource() {
        dispatcher.getRegistry().removeRegistrations(ReOptimizePortfolioEndpoint.class);
    }

    /**
     * Test the parsing of the ptuIndexes to Integer[]
     *
     * @throws URISyntaxException
     */
    @Test
    public void ptuIndexesStringToIntParseSuccessExpected() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + REOPTIMIZE_PARAM);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test the formatting of the date with correct date
     *
     * @throws URISyntaxException
     */
    @Test
    public void periodCorrectDateformatFormatSuccessExpected() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + REOPTIMIZE_PARAM);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Test the formatting of the date with incorrect date
     *
     * @throws URISyntaxException
     */
    @Test
    public void periodIncorrectDateformatFormatFailExpected() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + REOPTIMIZE_WRONG_DATE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        Assert.assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());

    }

    /**
     * Test with correct parameters that fires event manager with success
     *
     * @throws URISyntaxException
     */
    @Test
    public void eventManagerFiredSuccessExpected() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + REOPTIMIZE_PARAM);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        Mockito.verify(eventManager, Mockito.times(1)).fire(Matchers.any(ReOptimizePortfolioEvent.class));
        Assert.assertEquals(200, response.getStatus());

    }
}
