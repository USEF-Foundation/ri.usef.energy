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

package energy.usef.pbcfeeder;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.http.HttpServletRequest;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class PBCFeederEndpointTest extends BaseResourceTest {

    private static final String URL = "/PBCFeeder/";
    @Mock
    private HttpServletRequest request;
    @Mock
    private PbcFeeder pbcFeeder;

    private PbcFeederEndpoint endpoint;

    /**
     * Setup for the test.
     */
    @Before
    public void initResource() {
        endpoint = new PbcFeederEndpoint();
        Whitebox.setInternalState(endpoint, "pbcFeeder", pbcFeeder);
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
        dispatcher.getRegistry().removeRegistrations(PbcFeederEndpoint.class);
    }

    /**
     * Test for PBCFeederEndpoint expecting the retrieval of 96 rows.
     *
     * @throws Exception
     */
    //    @Ignore
    //    @Test
    //    public void testGetPtuRows() throws URISyntaxException {
    //        MockHttpRequest request = MockHttpRequest.get(URL + "ptu/96");
    //        MockHttpResponse response = new MockHttpResponse();
    //        Mockito.when(pbcFeeder.getStubRowInputList())
    //                .thenReturn(IntStream.rangeClosed(1, 96).mapToObj(index -> new PbcStubDataDto()).collect(
    //                        Collectors.toList()));
    //        dispatcher.invoke(request, response);
    //
    //        Mockito.verify(pbcFeeder, Mockito.times(1)).getStubRowInputList();
    //    }

    /**
     * Test method for getting congestion point load from PBC feeder endpoint for CP 1.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testGetCongestionPointLoadWithIndexOne() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "congestionpoint/1");
        MockHttpResponse response = new MockHttpResponse();
        Mockito.when(pbcFeeder.getUncontrolledLoadForCongestionPoint(Matchers.anyInt()))
                .thenReturn(IntStream.rangeClosed(1, 100).mapToObj(index -> (double) index).collect(Collectors.toList()));
        dispatcher.invoke(request, response);

        Mockito.verify(pbcFeeder, Mockito.times(1)).getUncontrolledLoadForCongestionPoint(1);
    }

    /**
     * Test to get list of APX from PBC Endpoint.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testGetApx() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "apx");
        MockHttpResponse response = new MockHttpResponse();
        Mockito.when(pbcFeeder.getApx()).thenReturn(
                IntStream.rangeClosed(1, 100).mapToObj(index -> (double) index).collect(Collectors.toList()));
        dispatcher.invoke(request, response);

        Mockito.verify(pbcFeeder, Mockito.times(1)).getApx();
    }

    /**
     * Test to get List of PV Actuals from PBC Endpoint.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testGetPvActual() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "pvactual");
        MockHttpResponse response = new MockHttpResponse();
        Mockito.when(pbcFeeder.getApx()).thenReturn(
                IntStream.rangeClosed(1, 100).mapToObj(index -> (double) index).collect(Collectors.toList()));
        dispatcher.invoke(request, response);

        Mockito.verify(pbcFeeder, Mockito.times(1)).getPvActual();
    }

    /**
     * Test to get List of PV Forecast from PBC Endpoint.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testGetPvForecast() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get(URL + "pvforecast");
        MockHttpResponse response = new MockHttpResponse();
        Mockito.when(pbcFeeder.getApx()).thenReturn(
                IntStream.rangeClosed(1, 100).mapToObj(index -> (double) index).collect(Collectors.toList()));
        dispatcher.invoke(request, response);

        Mockito.verify(pbcFeeder, Mockito.times(1)).getPvForecast();
    }

    @Test
    public void testGetCongestionPointPowerLimits() throws URISyntaxException {
        Mockito.when(pbcFeeder.getCongestionPointPowerLimits(Matchers.anyInt())).thenReturn(Arrays.asList(
                new BigDecimal("-1000"),
                new BigDecimal("1000")));
        MockHttpRequest request = MockHttpRequest.get(URL + "powerLimit/1");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        Mockito.verify(pbcFeeder, Mockito.times(1)).getCongestionPointPowerLimits(Matchers.eq(1));
    }
}
