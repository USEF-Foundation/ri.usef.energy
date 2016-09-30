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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdate;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.cro.config.ConfigCro;
import energy.usef.cro.config.ConfigCroParam;
import energy.usef.cro.model.Aggregator;
import energy.usef.cro.model.BalanceResponsibleParty;
import energy.usef.cro.model.CongestionPoint;
import energy.usef.cro.model.Connection;
import energy.usef.cro.model.DistributionSystemOperator;
import energy.usef.cro.repository.AggregatorRepository;
import energy.usef.cro.repository.BalanceResponsiblePartyRepository;
import energy.usef.cro.repository.CongestionPointRepository;
import energy.usef.cro.repository.ConnectionRepository;
import energy.usef.cro.repository.DistributionSystemOperatorRepository;
import energy.usef.cro.repository.MeterDataCompanyRepository;

/**
 * JUnit test for the CommonReferenceUpdateBusinessService class.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommonReferenceUpdateBusinessServiceTest {
    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
    private static final String CONGESTION_POINT_ANOTHER_ENTITY_ADDRESS = "ea1.1992-02.com.example:gridpoint.4f76ff19-a53b-49f5-99e1";

    private static final String CONNECTION_ENTITY_ADDRESS = "ean.673685900012637348";

    private static final String DSO_DOMAIN = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
    private static final String AGR_DOMAIN = "tesla.com";
    private static final String DSO_ANOTHER_DOMAIN = "another.domain";
    private static final String BRP_DOMAIN = "brp.test.com";

    private CommonReferenceUpdateBusinessService service;

    @Mock
    private DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Mock
    private CongestionPointRepository congestionPointRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private AggregatorRepository aggregatorRepository;

    @Mock
    private BalanceResponsiblePartyRepository balanceResponsiblePartyRepository;

    @Mock
    private MeterDataCompanyRepository meterDataCompanyRepository;

    private ConfigCro configCro = new ConfigCro();

    @Before
    public void init() {
        service = new CommonReferenceUpdateBusinessService();
        Whitebox.setInternalState(service, "distributionSystemOperatorRepository", distributionSystemOperatorRepository);
        Whitebox.setInternalState(service, "congestionPointRepository", congestionPointRepository);
        Whitebox.setInternalState(service, "connectionRepository", connectionRepository);
        Whitebox.setInternalState(service, "aggregatorRepository", aggregatorRepository);
        Whitebox.setInternalState(service, "balanceResponsiblePartyRepository", balanceResponsiblePartyRepository);
        Whitebox.setInternalState(service, "meterDataCompanyRepository", meterDataCompanyRepository);
        Whitebox.setInternalState(service, "configCro", configCro);
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.executeCommonReferenceUpdate method with wrong DSO domain.
     */
    @Test
    public void executeCommonReferenceUpdateWithWrongDSODomainTest() {
        CommonReferenceUpdate message = createCommonReferenceUpdate();
        List<String> errors = new ArrayList<>();

        CongestionPoint congestionPoint = new CongestionPoint();
        DistributionSystemOperator distributionSystemOperator = new DistributionSystemOperator(DSO_ANOTHER_DOMAIN);
        congestionPoint.setDistributionSystemOperator(distributionSystemOperator);
        when(congestionPointRepository
                .getCongestionPointByEntityAddress(message.getEntityAddress())).thenReturn(congestionPoint);

        service.updateCongestionPoints(message, errors);

        Assert.assertEquals(1, errors.size());
    }

    @Test
    public void executeCommonReferenceUpdateWithErrorInValidateByModeDso() {
        when(distributionSystemOperatorRepository.findByDomain(Matchers.anyString())).thenReturn(null);
        Properties props = (Properties) Whitebox.getInternalState(configCro, "properties");
        props.setProperty(ConfigCroParam.COMMON_REFERENCE_MODE.name(), "CLOSED");

        CommonReferenceUpdate message = createCommonReferenceUpdate();
        List<String> errors = new ArrayList<>();

        service.updateCongestionPoints(message, errors);

        Mockito.verify(connectionRepository, Mockito.times(0)).persist(Matchers.any(Connection.class));
        Assert.assertEquals(1, errors.size());
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.executeCommonReferenceUpdate method with CongestionPoint to be created in the
     * CLOSED mode.
     */
    @Test
    public void executeCommonReferenceUpdateCongestionPointToCreate() {
        Properties props = (Properties) Whitebox.getInternalState(configCro, "properties");
        props.setProperty(ConfigCroParam.COMMON_REFERENCE_MODE.name(), "CLOSED");

        Mockito.when(service.findDistributionSystemOperatorByDomain(DSO_DOMAIN)).thenReturn(
                new DistributionSystemOperator(DSO_DOMAIN));

        CommonReferenceUpdate message = createCommonReferenceUpdate();
        List<String> errors = new ArrayList<>();

        service.updateCongestionPoints(message, errors);

        Mockito.verify(connectionRepository, Mockito.times(1))
                .persist(Matchers.any(Connection.class));
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.executeCommonReferenceUpdate method with existing CongestionPoint.
     */
    @Test
    public void executeCommonReferenceUpdateWithExistingCongestionPoint() {
        CommonReferenceUpdate message = createCommonReferenceUpdate();
        List<String> errors = new ArrayList<>();

        CongestionPoint congestionPoint = new CongestionPoint();
        DistributionSystemOperator distributionSystemOperator = new DistributionSystemOperator(DSO_DOMAIN);
        congestionPoint.setDistributionSystemOperator(distributionSystemOperator);
        when(congestionPointRepository
                .getCongestionPointByEntityAddress(message.getEntityAddress())).thenReturn(congestionPoint);

        service.updateCongestionPoints(message, errors);

        Mockito.verify(connectionRepository, Mockito.times(1))
                .persist(Matchers.any(Connection.class));
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.executeCommonReferenceUpdate deleting congestion point.
     */
    @Test
    public void executeCommonReferenceUpdateDelete() {
        CommonReferenceUpdate message = createCommonReferenceUpdate(true);
        List<String> errors = new ArrayList<>();

        CongestionPoint congestionPoint = new CongestionPoint();
        DistributionSystemOperator distributionSystemOperator = new DistributionSystemOperator(DSO_DOMAIN);
        congestionPoint.setDistributionSystemOperator(distributionSystemOperator);

        congestionPoint.getConnections().add(new Connection("ean.000000000000000011"));
        congestionPoint.getConnections().add(new Connection("ean.000000000000000012"));
        Connection connection = new Connection("ean.000000000000000013");
        Aggregator aggregator = new Aggregator(AGR_DOMAIN);
        aggregator.setDomain("ea1.1992-01.com.example:gridpoint.7c76ff19-a53b");
        connection.setAggregator(aggregator);
        congestionPoint.getConnections().add(connection);

        when(congestionPointRepository
                .getCongestionPointByEntityAddress(message.getEntityAddress())).thenReturn(congestionPoint);

        service.updateCongestionPoints(message, errors);

        Mockito.verify(congestionPointRepository, Mockito.times(1))
                .delete(Matchers.any(CongestionPoint.class));
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.executeCommonReferenceUpdate method with wrong connections.
     */
    @Test
    public void executeCommonReferenceUpdateWithWrongConnectionsTest() {
        CommonReferenceUpdate message = createCommonReferenceUpdate();
        List<String> errors = new ArrayList<>();

        Connection connection = new Connection();
        CongestionPoint congestionPoint = new CongestionPoint();
        congestionPoint.setDistributionSystemOperator(new DistributionSystemOperator(DSO_ANOTHER_DOMAIN));
        congestionPoint.setEntityAddress(CONGESTION_POINT_ANOTHER_ENTITY_ADDRESS);
        connection.setCongestionPoint(congestionPoint);

        when(connectionRepository
                .findConnectionByEntityAddress(CONNECTION_ENTITY_ADDRESS)).thenReturn(connection);

        service.updateCongestionPoints(message, errors);

        Assert.assertEquals(1, errors.size());
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.getDistributionSystemOperatorByDomain method.
     */
    @Test
    public void getDistributionSystemOperatorByDomainTest() {
        DistributionSystemOperator expectedDistributionSystemOperator = new DistributionSystemOperator();

        when(distributionSystemOperatorRepository.findByDomain(DSO_DOMAIN)).thenReturn(
                expectedDistributionSystemOperator);

        DistributionSystemOperator actualDistributionSystemOperator = service.findDistributionSystemOperatorByDomain(DSO_DOMAIN);

        Assert.assertEquals(expectedDistributionSystemOperator, actualDistributionSystemOperator);

        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(1)).findByDomain(DSO_DOMAIN);
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.findOrCreateDistributionSystemOperatorByDomain method.
     */
    @Test
    public void testFindOrCreateDistributionSystemOperatorByDomain() {
        DistributionSystemOperator expectedDistributionSystemOperator = new DistributionSystemOperator();

        when(distributionSystemOperatorRepository.findOrCreateByDomain(DSO_DOMAIN)).thenReturn(expectedDistributionSystemOperator);

        DistributionSystemOperator actualDistributionSystemOperator = service
                .findOrCreateDistributionSystemOperatorByDomain(DSO_DOMAIN);

        Assert.assertEquals(expectedDistributionSystemOperator, actualDistributionSystemOperator);

        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(1)).findOrCreateByDomain(DSO_DOMAIN);
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.findByDomain method.
     */
    @Test
    public void getAggregatorByDomainTest() {
        Aggregator expectedAggregator = new Aggregator(AGR_DOMAIN);

        when(aggregatorRepository.findByDomain(AGR_DOMAIN)).thenReturn(
                expectedAggregator);

        Aggregator actualAggregator = service.getAggregatorByDomain(AGR_DOMAIN);

        Assert.assertEquals(expectedAggregator, actualAggregator);

        Mockito.verify(aggregatorRepository, Mockito.times(1))
                .findByDomain(AGR_DOMAIN);
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.getCongestionPointByEntityAddress method.
     */
    @Test
    public void getCongestionPointByEntityAddress() {
        CongestionPoint expectedCongestionPoint = new CongestionPoint();

        when(congestionPointRepository.getCongestionPointByEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS)).thenReturn(
                expectedCongestionPoint);

        CongestionPoint actualCongestionPoint = service.getCongestionPointByEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);

        Assert.assertEquals(expectedCongestionPoint, actualCongestionPoint);

        Mockito.verify(congestionPointRepository, Mockito.times(1))
                .getCongestionPointByEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
    }

    @Test
    public void findCongestionPointsWithAggregatorsByDSOTest() {
        // build context
        String dsoDomain = "usef-example.com";

        // expect
        when(congestionPointRepository.findAggregatorCountForCongestionPointsByDSO(dsoDomain, null)).thenReturn(
                new HashMap<>());

        Map<CongestionPoint, Map<Aggregator, Long>> congestionPointsWithAggregators = service
                .findCongestionPointsWithAggregatorsByDSO(dsoDomain, null);

        Assert.assertNotNull(congestionPointsWithAggregators);

        // verify
        Mockito.verify(congestionPointRepository, Mockito.times(1))
                .findAggregatorCountForCongestionPointsByDSO(Matchers.eq(dsoDomain), Matchers.isNull(String.class));
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.findCongestionPointsWithAggregatorsByAGR method.
     */
    @Test
    public void findCongestionPointsWithAggregatorsByAGRTest() {

        // expect
        when(congestionPointRepository.findConnectionsForCongestionPointsByAGR(AGR_DOMAIN, null)).thenReturn(
                new HashMap<>());

        Map<CongestionPoint, Set<Connection>> congestionPointsWithAggregators = service
                .findConnectionsForCongestionPointsByAGR(AGR_DOMAIN, null);

        Assert.assertNotNull(congestionPointsWithAggregators);

        // verify
        Mockito.verify(congestionPointRepository, Mockito.times(1))
                .findConnectionsForCongestionPointsByAGR(Matchers.eq(AGR_DOMAIN), Matchers.isNull(String.class));
    }

    private CommonReferenceUpdate createCommonReferenceUpdate() {
        return createCommonReferenceUpdate(false);
    }

    private CommonReferenceUpdate createCommonReferenceUpdate(boolean noConnection) {
        CommonReferenceUpdate message = new CommonReferenceUpdate();
        message.setEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
        message.setEntity(CommonReferenceEntityType.CONGESTION_POINT);

        if (!noConnection) {
            energy.usef.core.data.xml.bean.message.Connection connection = new energy.usef.core.data.xml.bean.message.Connection();
            connection.setEntityAddress(CONNECTION_ENTITY_ADDRESS);
            message.getConnection().add(connection);
        }

        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setMessageID("testId");
        messageMetadata.setSenderDomain(DSO_DOMAIN);
        message.setMessageMetadata(messageMetadata);

        return message;
    }

    private CommonReferenceUpdate createCommonReferenceUpdateFromAggregator(boolean noConnection, String senderDomain,
            String connectionEntityAddress, boolean isCustomer) {
        CommonReferenceUpdate message = new CommonReferenceUpdate();
        message.setEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
        message.setEntity(CommonReferenceEntityType.AGGREGATOR);

        if (!noConnection) {
            energy.usef.core.data.xml.bean.message.Connection connection = new energy.usef.core.data.xml.bean.message.Connection();
            connection.setEntityAddress(connectionEntityAddress);
            connection.setIsCustomer(isCustomer);
            message.getConnection().add(connection);
        }

        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setMessageID("testId");
        messageMetadata.setSenderDomain(senderDomain);
        message.setMessageMetadata(messageMetadata);

        return message;
    }

    private CommonReferenceUpdate createCommonReferenceUpdateFromBalanceResponsibleParty(String senderDomain,
            String connectionEntityAddress, boolean isCustomer) {
        CommonReferenceUpdate message = new CommonReferenceUpdate();
        message.setEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
        message.setEntity(CommonReferenceEntityType.BRP);

        energy.usef.core.data.xml.bean.message.Connection connection = new energy.usef.core.data.xml.bean.message.Connection();
        connection.setEntityAddress(connectionEntityAddress);
        connection.setIsCustomer(isCustomer);
        message.getConnection().add(connection);

        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setMessageID("testId");
        messageMetadata.setSenderDomain(senderDomain);
        message.setMessageMetadata(messageMetadata);

        return message;
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.updateAggregatorConnectionsNoValidAggregatorInClosedMode.
     */
    @Test
    public void updateAggregatorConnectionsNoValidAggregatorInClosedMode() {
        Properties props = (Properties) Whitebox.getInternalState(configCro, "properties");
        props.setProperty(ConfigCroParam.COMMON_REFERENCE_MODE.name(), "CLOSED");

        CommonReferenceUpdate message = createCommonReferenceUpdateFromAggregator(false, AGR_DOMAIN + "test",
                CONNECTION_ENTITY_ADDRESS, false);
        List<String> errors = new ArrayList<>();

        // do the business call
        service.updateAggregatorConnections(message, errors);

        Assert.assertTrue("Errors expected", !errors.isEmpty());
        Assert.assertEquals("Not a valid domain", "You are not allowed to register the aggregator " + AGR_DOMAIN + "test",
                errors.get(0));
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.updateAggregatorConnectionsNewConnection.
     */
    @Test
    public void updateAggregatorConnectionsNewConnection() {
        CommonReferenceUpdate message = createCommonReferenceUpdateFromAggregator(false, AGR_DOMAIN, CONNECTION_ENTITY_ADDRESS,
                true);
        List<String> errors = new ArrayList<>();
        Aggregator expectedAggregator = new Aggregator(AGR_DOMAIN);
        when(aggregatorRepository.findByDomain(AGR_DOMAIN)).thenReturn(expectedAggregator);
        ArgumentCaptor<Connection> captorConnection = ArgumentCaptor.forClass(Connection.class);

        // do the business call
        service.updateAggregatorConnections(message, errors);
        Mockito.verify(connectionRepository, Mockito.times(1)).findAll();
        Mockito.verify(connectionRepository, Mockito.times(1)).persist(captorConnection.capture());
//
        Assert.assertEquals(expectedAggregator, captorConnection.getValue().getAggregator());
        Assert.assertTrue("No errors expected", errors.isEmpty());
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.updateAggregatorConnectionsExisitingConnectionNoAggregator.
     */
    @Test
    public void updateAggregatorConnectionsExisitingConnectionNoAggregator() {
        CommonReferenceUpdate message = createCommonReferenceUpdateFromAggregator(false, AGR_DOMAIN, CONNECTION_ENTITY_ADDRESS,
                false);
        List<String> errors = new ArrayList<>();
        Aggregator expectedAggregator = new Aggregator(AGR_DOMAIN);
        when(aggregatorRepository.findByDomain(AGR_DOMAIN)).thenReturn(expectedAggregator);

        Connection connection = new Connection();
        connection.setEntityAddress(CONNECTION_ENTITY_ADDRESS);
        List<Connection> connectionList = new ArrayList<>();
        connectionList.add(connection);

        when(connectionRepository.findAll()).thenReturn(connectionList);

        // do the business call
        service.updateAggregatorConnections(message, errors);

        Assert.assertEquals(expectedAggregator, connection.getAggregator());

        Assert.assertTrue("No errors expected", errors.isEmpty());
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.updateAggregatorConnectionsExisitingConnectionChangeAggregatorCustomerTrue.
     */
    @Test
    public void updateAggregatorConnectionsChangeAggregatorCustomerTrue() {
        CommonReferenceUpdate message = createCommonReferenceUpdateFromAggregator(false, AGR_DOMAIN, CONNECTION_ENTITY_ADDRESS,
                true);
        List<String> errors = new ArrayList<>();
        Aggregator expectedAggregator = new Aggregator(AGR_DOMAIN);
        when(aggregatorRepository.findByDomain(AGR_DOMAIN)).thenReturn(expectedAggregator);

        Connection connection = new Connection();
        connection.setEntityAddress(CONNECTION_ENTITY_ADDRESS);
        List<Connection> connectionList = new ArrayList<>();
        connectionList.add(connection);
        when(connectionRepository.findAll()).thenReturn(connectionList);

        // do the business call
        service.updateAggregatorConnections(message, errors);

        Assert.assertEquals(expectedAggregator, connection.getAggregator());

        Assert.assertTrue("No errors expected", errors.isEmpty());
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.updateAggregatorConnectionsExisitingConnectionDifferentAggregatorCustomerTrue.
     */
    @Test
    public void updateAggregatorConnectionsDifferentAggregatorCustomerFalse() {
        CommonReferenceUpdate message = createCommonReferenceUpdateFromAggregator(false, AGR_DOMAIN, CONNECTION_ENTITY_ADDRESS,
                false);
        List<String> errors = new ArrayList<>();
        Aggregator initialAggregator = new Aggregator("intial.aggregator");
        Aggregator expectedAggregator = new Aggregator(AGR_DOMAIN);
        when(aggregatorRepository.findByDomain(AGR_DOMAIN)).thenReturn(expectedAggregator);

        Connection connection = mock(Connection.class);
        connection.setEntityAddress(CONNECTION_ENTITY_ADDRESS);
        when(connectionRepository.findConnectionByEntityAddress(CONNECTION_ENTITY_ADDRESS)).thenReturn(connection);
        when(connection.getAggregator()).thenReturn(initialAggregator);

        // do the business call
        service.updateAggregatorConnections(message, errors);

        Mockito.verify(connection, Mockito.times(0)).setAggregator(expectedAggregator);
        Assert.assertTrue("No errors expected", errors.isEmpty());
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.updateAggregatorConnectionsExisitingConnectionSameAggregatorCustomerTrue.
     */
    @Test
    public void updateAggregatorConnectionsSameAggregatorCustomerFalse() {
        CommonReferenceUpdate message = createCommonReferenceUpdateFromAggregator(false, AGR_DOMAIN, CONNECTION_ENTITY_ADDRESS,
                false);
        List<String> errors = new ArrayList<>();
        Aggregator initialAggregator = new Aggregator(AGR_DOMAIN);
        when(aggregatorRepository.findByDomain(AGR_DOMAIN)).thenReturn(initialAggregator);

        Connection connection = new Connection();
        connection.setEntityAddress(CONNECTION_ENTITY_ADDRESS);
        List<Connection> connectionList = new ArrayList<>();
        connectionList.add(connection);
        when(connectionRepository.findAll()).thenReturn(connectionList);

        // do the business call
        service.updateAggregatorConnections(message, errors);

        Assert.assertEquals(initialAggregator, connection.getAggregator());
        Assert.assertTrue("No errors expected", errors.isEmpty());
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.updateBalanceResponsiblePartyConnections with No Valid BalanceResponsibleParty In
     * Closed Mode.
     */
    @Test
    public void updateBalanceResponsiblePartyConnectionsNoValidBalanceResponsiblePartyInClosedMode() {
        Properties props = (Properties) Whitebox.getInternalState(configCro, "properties");
        props.setProperty(ConfigCroParam.COMMON_REFERENCE_MODE.name(), "CLOSED");

        CommonReferenceUpdate message = createCommonReferenceUpdateFromBalanceResponsibleParty(BRP_DOMAIN + "test",
                CONNECTION_ENTITY_ADDRESS, true);
        List<String> errors = new ArrayList<>();

        // do the business call
        service.updateBalanceResponsiblePartyConnections(message, errors);

        Assert.assertTrue("Errors expected", !errors.isEmpty());
        Assert.assertEquals("Not a valid domain", "You are not allowed to register the BRP " + BRP_DOMAIN + "test", errors.get(0));
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.updateBalanceResponsiblePartyConnections with New Connection.
     */
    @Test
    public void updateBalanceResponsiblePartyConnectionsNewConnection() {
        CommonReferenceUpdate message = createCommonReferenceUpdateFromBalanceResponsibleParty(BRP_DOMAIN,
                CONNECTION_ENTITY_ADDRESS,
                true);
        List<String> errors = new ArrayList<>();
        BalanceResponsibleParty expectedBalanceResponsibleParty = new BalanceResponsibleParty(BRP_DOMAIN);
        when(balanceResponsiblePartyRepository.findByDomain(BRP_DOMAIN)).thenReturn(
                expectedBalanceResponsibleParty);
        ArgumentCaptor<Connection> captorConnection = ArgumentCaptor.forClass(Connection.class);

        // do the business call
        service.updateBalanceResponsiblePartyConnections(message, errors);

        // Mockito.verify(connectionRepository, Mockito.times(1))
        // .findConnectionByEntityAddress(Matchers.eq(CONNECTION_ENTITY_ADDRESS));
        Mockito.verify(connectionRepository, Mockito.times(1)).persist(captorConnection.capture());

        Assert.assertEquals(expectedBalanceResponsibleParty, captorConnection.getValue().getBalanceResponsibleParty());
        Assert.assertTrue("No errors expected", errors.isEmpty());
    }

    /**
     * Tests CommonReferenceUpdateBusinessService.updateBalanceResponsiblePartyConnections with an existing connection and change of
     * Balance Responsible Party.
     */
    @Test
    public void updateBalanceResponsiblePartyConnectionsExistingConnection() {
        CommonReferenceUpdate message = createCommonReferenceUpdateFromBalanceResponsibleParty(BRP_DOMAIN,
                CONNECTION_ENTITY_ADDRESS,
                true);
        List<String> errors = new ArrayList<>();
        BalanceResponsibleParty expectedBalanceResponsibleParty = new BalanceResponsibleParty(BRP_DOMAIN);
        when(balanceResponsiblePartyRepository.findByDomain(BRP_DOMAIN)).thenReturn(
                expectedBalanceResponsibleParty);

        Connection connection = new Connection();
        connection.setEntityAddress(CONNECTION_ENTITY_ADDRESS);
        List<Connection> connectionList = new ArrayList<>();
        connectionList.add(connection);

        when(connectionRepository.findAll()).thenReturn(connectionList);

        // do the business call
        service.updateBalanceResponsiblePartyConnections(message, errors);

        Assert.assertEquals(expectedBalanceResponsibleParty, connection.getBalanceResponsibleParty());

        Assert.assertTrue("No errors expected", errors.isEmpty());
    }

    /**
     * Tests CommonReferenceUpdateBusinessService. updateBalanceResponsiblePartyConnectionsExisitingConnection with isCustomer
     * false.
     */
    @Test
    public void ignoreCustomerFalseForBalanceResponsiblePartyUpdate() {
        CommonReferenceUpdate message = createCommonReferenceUpdateFromBalanceResponsibleParty(BRP_DOMAIN,
                CONNECTION_ENTITY_ADDRESS,
                false);
        List<String> errors = new ArrayList<>();
        BalanceResponsibleParty balanceResponsibleParty = new BalanceResponsibleParty(BRP_DOMAIN);
        when(balanceResponsiblePartyRepository.findByDomain(BRP_DOMAIN)).thenReturn(
                balanceResponsibleParty);

        Connection connection = new Connection();
        connection.setEntityAddress(CONNECTION_ENTITY_ADDRESS);
        List<Connection> connectionList = new ArrayList<>();
        connectionList.add(connection);
        when(connectionRepository.findAll()).thenReturn(connectionList);

        // do the business call
        service.updateBalanceResponsiblePartyConnections(message, errors);

        Assert.assertEquals(balanceResponsibleParty, connection.getBalanceResponsibleParty());

        Assert.assertTrue("No errors expected", errors.isEmpty());
    }

    @Test
    public void testFindBRP() {
        service.findBRP("brp1.usef-example.com");

        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(1)).findByDomain(Matchers.eq("brp1.usef-example.com"));
    }

    @Test
    public void testFindMDC() {
        service.findMDC("mdc1.usef-example.com");

        Mockito.verify(meterDataCompanyRepository, Mockito.times(1)).findByDomain(Matchers.eq("mdc1.usef-example.com"));
    }
}
