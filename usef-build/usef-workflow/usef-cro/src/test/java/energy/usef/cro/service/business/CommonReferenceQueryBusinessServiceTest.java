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

package energy.usef.cro.service.business;

import static org.junit.Assert.assertNotNull;
import energy.usef.cro.model.CongestionPoint;
import energy.usef.cro.model.Connection;
import energy.usef.cro.repository.CongestionPointRepository;
import energy.usef.cro.repository.ConnectionRepository;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit-tests for CommonReferenceQueryBusinessService class
 */
@RunWith(PowerMockRunner.class)
public class CommonReferenceQueryBusinessServiceTest {
    private static final String AGR_DOMAIN = "tesla.com";
    private static final String BRP_DOMAIN = "brp.test.com";

    private CommonReferenceQueryBusinessService service;

    @Mock
    private CongestionPointRepository congestionPointRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Before
    public void init() {
        service = new CommonReferenceQueryBusinessService();
        Whitebox.setInternalState(service, "congestionPointRepository", congestionPointRepository);
        Whitebox.setInternalState(service, "connectionRepository", connectionRepository);
    }

    /**
     * Test for findAllConnectionsForAggregatorDomain method.
     */
    @Test
    public void findAllConnnectionsForAggregatorDomainReturnsListSuccess() {
        String senderDomain = AGR_DOMAIN;
        List<Connection> mockList = new ArrayList<>();
        Mockito.when(connectionRepository.findConnectionsForAggregator(AGR_DOMAIN)).thenReturn(mockList);
        List<Connection> result = service.findAllConnectionsForAggregatorDomain(senderDomain);
        assertNotNull(result);
    }

    /**
     * Test for findAllConnectionsForBRPDomain method.
     */
    @Test
    public void findAllConnnectionsForBrpDomainReturnsListSuccess() {
        String senderDomain = BRP_DOMAIN;
        List<Connection> mockList = new ArrayList<>();
        Mockito.when(connectionRepository.findConnectionsForBRP(BRP_DOMAIN)).thenReturn(mockList);
        List<Connection> result = service.findAllConnectionsForBRPDomain(senderDomain);
        assertNotNull(result);
    }

    /**
     * Test for findAllConnectionsForCRODomain method.
     */
    @Test
    public void findAllConnnectionsForMdcDomainReturnsListSuccess() {
        List<Connection> mockList = new ArrayList<>();
        Mockito.when(connectionRepository.findConnectionsForMDC()).thenReturn(mockList);
        List<Connection> result = service.findAllConnectionsForMDCDomain();
        assertNotNull(result);
    }

    /**
     * Test for findAllCongestionPointsForAggregatorDomain method.
     */
    @Test
    public void findAllCongestionPointsForAggregatorDomainReturnsListSuccess() {
        String senderDomain = AGR_DOMAIN;
        List<CongestionPoint> mockList = new ArrayList<>();
        Mockito.when(service.findAllCongestionPointsForAggregatorDomain(AGR_DOMAIN)).thenReturn(mockList);
        List<CongestionPoint> result = service.findAllCongestionPointsForAggregatorDomain(senderDomain);
        assertNotNull(result);
    }

}
