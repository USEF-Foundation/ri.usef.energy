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

package energy.usef.mdc.service.business;

import static org.junit.Assert.assertTrue;

import energy.usef.mdc.model.Aggregator;
import energy.usef.mdc.model.AggregatorConnection;
import energy.usef.mdc.model.CommonReferenceOperator;
import energy.usef.mdc.model.CommonReferenceQueryState;
import energy.usef.mdc.model.Connection;
import energy.usef.mdc.repository.AggregatorConnectionRepository;
import energy.usef.mdc.repository.AggregatorRepository;
import energy.usef.mdc.repository.CommonReferenceOperatorRepository;
import energy.usef.mdc.repository.CommonReferenceQueryStateRepository;
import energy.usef.mdc.repository.DistributionSystemOperatorRepository;
import energy.usef.mdc.repository.MdcConnectionRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
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

/**
 * Test class in charge of the unit tests related to the {@link MdcCoreBusinessService} class.
 */
@RunWith(PowerMockRunner.class)
public class MdcCoreBusinessServiceTest {

    private MdcCoreBusinessService service;

    @Mock
    private MdcConnectionRepository mdcConnectionRepository;
    @Mock
    private CommonReferenceOperatorRepository commonReferenceOperatorRepository;
    @Mock
    private AggregatorConnectionRepository aggregatorConnectionRepository;
    @Mock
    private AggregatorRepository aggregatorRepository;
    @Mock
    private DistributionSystemOperatorRepository distributionSystemOperatorRepository;
    @Mock
    private CommonReferenceQueryStateRepository commonReferenceQueryStateRepository;

    @Before
    public void setUp() throws Exception {
        service = new MdcCoreBusinessService();
        Whitebox.setInternalState(service, mdcConnectionRepository);
        Whitebox.setInternalState(service, commonReferenceOperatorRepository);
        Whitebox.setInternalState(service, aggregatorConnectionRepository);
        Whitebox.setInternalState(service, aggregatorRepository);
        Whitebox.setInternalState(service, commonReferenceQueryStateRepository);
        Whitebox.setInternalState(service, distributionSystemOperatorRepository);
    }

    @Test
    public void testFindAllConnectionEntityAddresses() throws Exception {
        // stubbing of the connection repository: returns 10 connections
        PowerMockito.when(mdcConnectionRepository.findAllConnections())
                .then(invocation -> IntStream.rangeClosed(1, 10).mapToObj(i -> new Connection("ean.00000" + i))
                        .collect(Collectors.toList()));

        List<String> entityAddresses = service.findAllConnectionEntityAddresses();
        Assert.assertNotNull(entityAddresses);
        Assert.assertEquals(10, entityAddresses.size());
        IntStream.rangeClosed(1, 10)
                .forEach(
                        i -> assertTrue(entityAddresses.stream().anyMatch(entityAddress -> entityAddress.equals("ean.00000" + i))));
    }

    @Test
    public void testFindAllCommonReferenceOperatorDomains() throws Exception {
        // stubbing of the connection repository: returns 10 connections
        PowerMockito.when(commonReferenceOperatorRepository.findAll()).then(invocation -> IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new CommonReferenceOperator("cro" + i + ".usef-example.com"))
                .collect(Collectors.toList()));

        List<String> croDomains = service.findAllCommonReferenceOperatorDomains();
        Assert.assertNotNull(croDomains);
        Assert.assertEquals(5, croDomains.size());
        IntStream.rangeClosed(1, 5)
                .forEach(i -> assertTrue(
                        croDomains.stream().anyMatch(croDomain -> croDomain.equals("cro" + i + ".usef-example.com"))));
    }

    /**
     * Received data in XmlConnection:
     * <ul>
     * <li>ean.000001 - agr1.usef-example.com: same data as the current states</li>
     * <li>ean.000002 - agr2.usef-example.com: present in states, but aggregator changed.</li>
     * <li>ean.000004 - agr3.usef-example.com: not present in states, will create new record.</li>
     * </ul>
     * <p>
     * Current state in the database:
     * <ul>
     * <li>ean.000001 - agr1.usef-example.com</li>
     * <li>ean.000002 - agr1.usef-example.com: aggregator changes, state will be closed and new record will be created.</li>
     * <li>ean.000003 - agr1.usef-example.com: not present in the incoming list of connections, state will be closed</li>
     * <li>ean.000099 - agr1.usef-example.com: not present in the incoming list of connections, state will be closed</li>
     * </ul>
     */
    @Test
    public void testStoreConnectionsForCommonReferenceOperator() {
        // stubbing of repositories
        PowerMockito.when(aggregatorRepository.findOrCreate(Matchers.any(String.class)))
                .then(invocation -> new Aggregator((String) invocation.getArguments()[0]));
        PowerMockito.when(mdcConnectionRepository.find(Matchers.any(String.class)))
                .then(invocation -> new Connection((String) invocation.getArguments()[0]));
        PowerMockito.when(commonReferenceOperatorRepository.find(Matchers.any(String.class)))
                .then(invocation -> new CommonReferenceOperator((String) invocation.getArguments()[0]));
        PowerMockito.when(aggregatorConnectionRepository.findActiveAggregatorConnectionsForCommonReferenceOperator(
                Matchers.eq("cro1.usef-example.com"), Matchers.any(LocalDate.class)))
                .thenReturn(buildCurrentAggregatorOnConnectionStates());
        service.storeConnectionsForCommonReferenceOperator(buildXmlConnections(), "cro1.usef-example.com");

        ArgumentCaptor<AggregatorConnection> stateCaptor = ArgumentCaptor.forClass(AggregatorConnection.class);
        Mockito.verify(aggregatorConnectionRepository, Mockito.times(2)).persist(stateCaptor.capture());
        List<AggregatorConnection> createdStates = stateCaptor.getAllValues();
        Assert.assertEquals(2, createdStates.size());
        createdStates.sort(
                (state1, state2) -> state1.getConnection().getEntityAddress().compareTo(state2.getConnection().getEntityAddress()));
        Assert.assertEquals("agr2.usef-example.com", createdStates.get(0).getAggregator().getDomain());
        Assert.assertEquals("agr3.usef-example.com", createdStates.get(1).getAggregator().getDomain());
    }

    private List<energy.usef.core.data.xml.bean.message.Connection> buildXmlConnections() {
        return Arrays.asList(buildXmlConnection("ean.000001", "agr1.usef-example.com"),
                buildXmlConnection("ean.000002", "agr2.usef-example.com"),
                buildXmlConnection("ean.000004", "agr3.usef-example.com"));
    }

    private energy.usef.core.data.xml.bean.message.Connection buildXmlConnection(String connectionEntityAddress,
            String aggregatorDomain) {
        energy.usef.core.data.xml.bean.message.Connection connection = new energy.usef.core.data.xml.bean.message.Connection();
        connection.setAGRDomain(aggregatorDomain);
        connection.setEntityAddress(connectionEntityAddress);
        return connection;
    }

    private List<AggregatorConnection> buildCurrentAggregatorOnConnectionStates() {
        return Arrays.asList(buildAggregatorOnConnectionState("ean.000001", "agr1.usef-example.com", "cro1.usef-example.com"),
                buildAggregatorOnConnectionState("ean.000002", "agr1.usef-example.com", "cro1.usef-example.com"),
                buildAggregatorOnConnectionState("ean.000003", "agr1.usef-example.com", "cro1.usef-example.com"),
                buildAggregatorOnConnectionState("ean.000099", "agr1.usef-example.com", "cro1.usef-example.com"));
    }

    private AggregatorConnection buildAggregatorOnConnectionState(String connectionEntityAddress, String aggregatorDomain,
            String croDomain) {
        AggregatorConnection state = new AggregatorConnection();
        state.setCommonReferenceOperator(new CommonReferenceOperator(croDomain));
        state.setAggregator(new Aggregator(aggregatorDomain));
        state.setConnection(new Connection(connectionEntityAddress));
        state.setValidFrom(new LocalDate(1990, 1, 1));
        return state;
    }

    @Test
    public void testStoreCommonReferenceQueryState() {
        service.storeCommonReferenceQueryState(new CommonReferenceQueryState());
        Mockito.verify(commonReferenceQueryStateRepository, Mockito.times(1))
                .persist(Matchers.any(CommonReferenceQueryState.class));

    }

    @Test
    public void testFindConnectionState() throws Exception {
        LocalDate date = new LocalDate();
        List<String> connectionEntityAddressList = Arrays.asList("ean.1", "ean.2");
        service.findConnectionState(date, connectionEntityAddressList);
        Mockito.verify(aggregatorConnectionRepository, Mockito.times(1)).findAggregatorForEachConnection(date,
                connectionEntityAddressList);
    }

    @Test
    public void testFindDistributionSystemOperator() throws Exception {
        String dsoDomain = "dso.usef-example.com";
        service.findDistributionSystemOperator(dsoDomain);
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(1)).find(dsoDomain);
    }
}
