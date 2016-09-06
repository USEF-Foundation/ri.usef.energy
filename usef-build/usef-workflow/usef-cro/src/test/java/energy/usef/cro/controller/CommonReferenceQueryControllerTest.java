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

package energy.usef.cro.controller;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.cro.config.ConfigCro;
import energy.usef.cro.config.ConfigCroParam;
import energy.usef.cro.model.Aggregator;
import energy.usef.cro.model.BalanceResponsibleParty;
import energy.usef.cro.model.CongestionPoint;
import energy.usef.cro.model.Connection;
import energy.usef.cro.model.DistributionSystemOperator;
import energy.usef.cro.service.business.CommonReferenceQueryBusinessService;
import energy.usef.cro.service.business.CommonReferenceUpdateBusinessService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Text class for CommonReferenceQueryController.
 *
 */
@RunWith(PowerMockRunner.class)
public class CommonReferenceQueryControllerTest {
    private static final String SENDER_DOMAIN = "usef-example.com";
    private static final String DSO_DOMAIN = "usef-dso.com";
    private static final String AGR_DOMAIN = "tesla.com";

    @Mock
    private CommonReferenceUpdateBusinessService commonReferenceUpdateBusinessService;

    @Mock
    private CommonReferenceQueryBusinessService commonReferenceQueryBusinessService;

    @Mock
    private JMSHelperService jmsHelperService;

    @Mock
    private Config config;

    private ConfigCro configCro = new ConfigCro();

    private CommonReferenceQueryController commonReferenceQueryController = new CommonReferenceQueryController();

    @Before
    public void init() {
        Whitebox.setInternalState(commonReferenceQueryController,
                "commonReferenceUpdateBusinessService",
                commonReferenceUpdateBusinessService);
        Whitebox.setInternalState(commonReferenceQueryController,
                "commonReferenceQueryBusinessService", commonReferenceQueryBusinessService);
        Whitebox.setInternalState(commonReferenceQueryController,
                "jmsHelperService", jmsHelperService);
        Whitebox.setInternalState(commonReferenceQueryController, "configCro", configCro);
        Whitebox.setInternalState(commonReferenceQueryController, config);

        Mockito.when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("cro.usef-example.com");
    }

    /**
     * Tests a CongestionPoint query from DSO. Closed mode.
     */
    @Test
    public void testDSOQueryWithClosedMode() {
        setMode("CLOSED");
        Mockito.when(commonReferenceUpdateBusinessService
                .findDistributionSystemOperatorByDomain(SENDER_DOMAIN)).thenReturn(new DistributionSystemOperator(SENDER_DOMAIN));
        testDSOCongestionPointQuery();
    }

    /**
     * Tests a CongestionPoint query from DSO. Open mode.
     */
    @Test
    public void testDSOQueryWithOpenMode() {
        setMode("OPEN");
        testDSOCongestionPointQuery();
    }

    /**
     * Tests a CongestionPoint query from AGR. Closed mode.
     */
    @Test
    public void testAGRQueryWithClosedMode() {
        setMode("CLOSED");

        Aggregator aggregator = new Aggregator(SENDER_DOMAIN);

        Mockito.when(commonReferenceUpdateBusinessService
                .getAggregatorByDomain(SENDER_DOMAIN)).thenReturn(aggregator);

        testAGRCongestionPointQuery();
    }

    /**
     * Tests a CongestionPoint query from AGR. Open mode.
     */
    @Test
    public void testAGRQueryWithOpenMode() {
        setMode("OPEN");
        testAGRCongestionPointQuery();
    }

    /**
     * Tests a CongestionPoint query without role/not supported role. Open mode.
     */
    @Test
    public void testWithoutRoleWithOpenMode() {
        setMode("OPEN");
        testWithoutRoleCongestionPointQuery();
    }

    /**
     * Tests a CongestionPoint query without role/not supported role. Closed mode.
     */
    @Test
    public void testWithoutRoleWithClosedMode() {
        setMode("CLOSED");
        testWithoutRoleCongestionPointQuery();
    }

    /**
     * Tests an AGR-query with AGR role in Open mode.
     */
    @Test
    public void testBrpQueryWithAgrRoleInOpenMode() {
        setMode("OPEN");
        testBrpQueryWithAgrRole();
    }

    /**
     * Tests AGR-query with AGR role in closed mode.
     */
    @Test
    public void testBrpQueryWithAgrRoleInClosedMode() {
        setMode("CLOSED");
        testBrpQueryWithAgrRole();
    }

    /**
     * Tests AGR-query with AGR that is not found.
     */
    @Test
    public void testBrpQueryWithNoneExistingAgrInClosedMode() {
        setMode("CLOSED");
        testAgrQueryWithNonExistingAggregator();
    }

    /**
     * Tests AGR-query with role missing in closed mode.
     */
    @Test
    public void testAgrQueryWithoutRoleClosedMode() {
        setMode("CLOSED");
        testWithOutRoleAgrQuery();
    }

    /**
     * Tests AGR-query with role missing in open mode.
     */
    @Test
    public void testAgrQueryWithoutRoleOpenMode() {
        setMode("OPEN");
        testWithOutRoleAgrQuery();
    }

    private void setMode(String mode) {
        Properties props = Whitebox.getInternalState(configCro, "properties");
        props.setProperty(ConfigCroParam.COMMON_REFERENCE_MODE.name(), mode);
    }

    private void testDSOCongestionPointQuery() {
        String entityAddress = "ea.dfjsap983aj30fja0pf";

        Map<CongestionPoint, Map<Aggregator, Long>> results = new HashMap<>();

        CongestionPoint congestionPoint = new CongestionPoint();
        congestionPoint.setEntityAddress(entityAddress);
        congestionPoint.setDistributionSystemOperator(new DistributionSystemOperator());
        congestionPoint.getDistributionSystemOperator().setDomain(DSO_DOMAIN);

        Aggregator aggregator = new Aggregator(AGR_DOMAIN);
        aggregator.setDomain("agr-domain.com");

        results.put(congestionPoint, new HashMap<>());
        results.get(congestionPoint).put(aggregator, 2L);

        Mockito.when(commonReferenceUpdateBusinessService
                .findCongestionPointsWithAggregatorsByDSO(SENDER_DOMAIN, entityAddress)).thenReturn(results);

        CommonReferenceQuery commonReferenceQuery = new CommonReferenceQuery();
        commonReferenceQuery.setEntityAddress(entityAddress);
        commonReferenceQuery.setEntity(CommonReferenceEntityType.CONGESTION_POINT);
        commonReferenceQuery.setMessageMetadata(new MessageMetadata());
        commonReferenceQuery.getMessageMetadata().setSenderDomain(SENDER_DOMAIN);
        commonReferenceQuery.getMessageMetadata().setSenderRole(USEFRole.DSO);

        try {
            commonReferenceQueryController.action(commonReferenceQuery, null);
        } catch (BusinessException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.verify(commonReferenceUpdateBusinessService, Mockito.times(1))
                .findCongestionPointsWithAggregatorsByDSO(SENDER_DOMAIN, entityAddress);

        Mockito.verify(jmsHelperService, Mockito.times(1))
                .sendMessageToOutQueue(
                        Matchers.contains("<CongestionPoint EntityAddress=\"ea.dfjsap983aj30fja0pf\" DSO-Domain=\"usef-dso.com\"><Aggregator Domain=\"agr-domain.com\" ConnectionCount=\"2\"/>"));
    }

    private void testWithOutRoleAgrQuery() {

        CommonReferenceQuery commonReferenceQuery = new CommonReferenceQuery();
        commonReferenceQuery.setEntity(CommonReferenceEntityType.AGGREGATOR);
        commonReferenceQuery.setMessageMetadata(new MessageMetadata());
        commonReferenceQuery.getMessageMetadata().setSenderDomain(SENDER_DOMAIN);

        try {
            commonReferenceQueryController.action(commonReferenceQuery, null);
        } catch (BusinessException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.verify(jmsHelperService, Mockito.times(1))
                .sendMessageToOutQueue(
                        Matchers.contains("<CommonReferenceQueryResponse Result=\"Failure\" Message=\"Role is not supported!\">"));
    }

    private void testAgrQueryWithNonExistingAggregator() {

        CommonReferenceQuery commonReferenceQuery = new CommonReferenceQuery();
        commonReferenceQuery.setEntity(CommonReferenceEntityType.BRP);
        commonReferenceQuery.setMessageMetadata(new MessageMetadata());
        commonReferenceQuery.getMessageMetadata().setSenderRole(USEFRole.AGR);
        commonReferenceQuery.getMessageMetadata().setSenderDomain(AGR_DOMAIN);

        Mockito.when(commonReferenceUpdateBusinessService.getAggregatorByDomain(AGR_DOMAIN)).thenReturn(null);

        try {
            commonReferenceQueryController.action(commonReferenceQuery, null);
        } catch (BusinessException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.verify(jmsHelperService, Mockito.times(1))
                .sendMessageToOutQueue(
                        Matchers.contains("AGR is not registered"));
    }

    private void testBrpQueryWithAgrRole() {

        List<Connection> connectionList = new ArrayList<>();
        connectionList.add(buildConnection());

        String congestionPointEntityAddress = "ea.dfjsap983aj30fja0pf";
        CongestionPoint congestionPoint = buildCongestionPoint(congestionPointEntityAddress);

        CommonReferenceQuery commonReferenceQuery = new CommonReferenceQuery();
        commonReferenceQuery.setEntity(CommonReferenceEntityType.BRP);
        commonReferenceQuery.setMessageMetadata(new MessageMetadata());
        commonReferenceQuery.getMessageMetadata().setSenderRole(USEFRole.AGR);
        commonReferenceQuery.getMessageMetadata().setSenderDomain(AGR_DOMAIN);

        Aggregator aggregator = new Aggregator(AGR_DOMAIN);

        Mockito.when(commonReferenceUpdateBusinessService.getAggregatorByDomain(AGR_DOMAIN)).thenReturn(aggregator);
        Mockito.when(commonReferenceQueryBusinessService.findAllConnectionsForAggregatorDomain(Matchers.anyString())).thenReturn(
                connectionList);

        try {
            commonReferenceQueryController.action(commonReferenceQuery, null);
        } catch (BusinessException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.verify(jmsHelperService)
                .sendMessageToOutQueue(
                        Matchers.contains("brp-example.com"));
    }

    private Connection buildConnection() {
        Connection connection = new Connection();
        connection.setAggregator(new Aggregator(SENDER_DOMAIN));
        connection.setBalanceResponsibleParty(new BalanceResponsibleParty("brp-example.com"));
        connection.setCongestionPoint(buildCongestionPoint("ea.dfjsap983aj30fja0pf"));
        connection.setEntityAddress("ea.dfjsap983aj30fja0pf");
        return connection;
    }

    private CongestionPoint buildCongestionPoint(String entityAddress) {
        String congestionPointEntityAddress = entityAddress;
        CongestionPoint congestionPoint = new CongestionPoint();
        congestionPoint.setEntityAddress(congestionPointEntityAddress);
        DistributionSystemOperator distributionSystemOperator = new DistributionSystemOperator();
        distributionSystemOperator.setDomain(DSO_DOMAIN);
        congestionPoint.setDistributionSystemOperator(distributionSystemOperator);
        return congestionPoint;
    }

    private void testAGRCongestionPointQuery() {
        Map<CongestionPoint, Set<Connection>> results = new HashMap<>();

        String congestionPointEntityAddress = "ea.dfjsap983aj30fja0pf";
        CongestionPoint congestionPoint = buildCongestionPoint(congestionPointEntityAddress);

        String connectionEntityAddress = "ea.abc000000000123";
        Connection connection = new Connection(connectionEntityAddress);

        results.put(congestionPoint, new HashSet<>());
        results.get(congestionPoint).add(connection);

        Mockito.when(
                commonReferenceUpdateBusinessService.findConnectionsForCongestionPointsByAGR(SENDER_DOMAIN,
                        congestionPointEntityAddress))
                .thenReturn(results);

        CommonReferenceQuery commonReferenceQuery = new CommonReferenceQuery();
        commonReferenceQuery.setEntityAddress(congestionPointEntityAddress);
        commonReferenceQuery.setEntity(CommonReferenceEntityType.CONGESTION_POINT);
        commonReferenceQuery.setMessageMetadata(new MessageMetadata());
        commonReferenceQuery.getMessageMetadata().setSenderDomain(SENDER_DOMAIN);
        commonReferenceQuery.getMessageMetadata().setSenderRole(USEFRole.AGR);

        try {
            commonReferenceQueryController.action(commonReferenceQuery, null);
        } catch (BusinessException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.verify(commonReferenceUpdateBusinessService, Mockito.times(1))
                .findConnectionsForCongestionPointsByAGR(SENDER_DOMAIN, congestionPointEntityAddress);

        Mockito.verify(jmsHelperService, Mockito.times(1))
                .sendMessageToOutQueue(
                        Matchers.contains("<CongestionPoint EntityAddress=\"" + congestionPointEntityAddress
                                + "\" DSO-Domain=\"" + DSO_DOMAIN + "\"><Connection EntityAddress=\"" + connectionEntityAddress
                                + "\"/>"));
    }

    private void testWithoutRoleCongestionPointQuery() {
        String entityAddress = "ea.dfjsap983aj30fja0pf";

        CommonReferenceQuery commonReferenceQuery = new CommonReferenceQuery();
        commonReferenceQuery.setEntityAddress(entityAddress);
        commonReferenceQuery.setEntity(CommonReferenceEntityType.CONGESTION_POINT);
        commonReferenceQuery.setMessageMetadata(new MessageMetadata());
        commonReferenceQuery.getMessageMetadata().setSenderDomain(SENDER_DOMAIN);

        try {
            commonReferenceQueryController.action(commonReferenceQuery, null);
        } catch (BusinessException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.verify(jmsHelperService, Mockito.times(1))
                .sendMessageToOutQueue(
                        Matchers.contains("<CommonReferenceQueryResponse Result=\"Failure\" Message=\"Role is not supported!\">"));
    }

}
