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

import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.exception.RestError;
import energy.usef.core.util.JsonUtil;
import energy.usef.mdc.model.BalanceResponsibleParty;
import energy.usef.mdc.model.DistributionSystemOperator;
import energy.usef.mdc.model.CommonReferenceOperator;
import energy.usef.mdc.repository.MdcConnectionRepository;
import energy.usef.mdc.repository.BalanceResponsiblePartyRepository;
import energy.usef.mdc.repository.DistributionSystemOperatorRepository;
import energy.usef.mdc.repository.CommonReferenceOperatorRepository;
import energy.usef.core.rest.RestResult;
import energy.usef.mdc.model.Connection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PowerMockRunner.class)
public class MeterDataCompanyTopologyBusinessServiceTest {

    MeterDataCompanyTopologyBusinessService service;

    @Mock
    MeterDataCompanyValidationBusinessService validationService;

    @Mock MdcConnectionRepository mdcConnectionRepository;

    @Mock BalanceResponsiblePartyRepository balanceResponsiblePartyRepository;

    @Mock DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Mock CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Before
    public void init() {
        service = new MeterDataCompanyTopologyBusinessService();
        Whitebox.setInternalState(service, "validationService", validationService);
        Whitebox.setInternalState(service, "mdcConnectionRepository", mdcConnectionRepository);
        Whitebox.setInternalState(service, "balanceResponsiblePartyRepository", balanceResponsiblePartyRepository);
        Whitebox.setInternalState(service, "distributionSystemOperatorRepository", distributionSystemOperatorRepository);
        Whitebox.setInternalState(service, "commonReferenceOperatorRepository", commonReferenceOperatorRepository);
    }

    @Test
    public void testProcessConnectionBatch() throws Exception
    {
        mockConnection("ean.0000000000000", true);
        mockConnection("ean.0000000000001", false);
        mockConnection("ea1.2007-11.net.usef.energy:1-2", true);
        mockConnection("ea1.2007-11.net.usef.energy:1-3", false);
        mockConnection("ea1.2007-11.net.usef.energy:1-4", false);
        mockConnection("ea1.2007-11.net.usef.energy:1-5", true);

        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"entityAddress\": \"ean.0000000000000\"},"
                    + "{\"method\" : \"POST\", \"entityAddress\": \"ean.0000000000001\"},"
                    + "{\"method\" : \"DELETE\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-2\"},"
                    + "{\"method\" : \"POST\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-3\"},"
                    + "{\"method\" : \"DELETE\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-4\"},"
                    + "{\"method\" : \"POST\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-5\"},"
                    + "{\"method\" : \"MAIL\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-6\"},"
                    + "{\"methode\" : \"POST\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-7\"},"
                    + "{\"method\" : \"POST\", \"entityAddres\": \"ea1.2007-11.net.usef.energy:1-8\"},"
                    + "{\"method3\" : \"POST\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-9\", \"additional\": 12}"
                    + "]";

            List<RestResult> result = service.processConnectionBatch(jsonText);
            String resultString = JsonUtil.createJsonText(result);

            verifyConnectionCalls("ean.0000000000000", 0, 1);
            verifyConnectionCalls("ean.0000000000001", 1, 0);
            verifyConnectionCalls("ea1.2007-11.net.usef.energy:1-2", 0, 1);
            verifyConnectionCalls("ea1.2007-11.net.usef.energy:1-3", 1, 0);
            verifyConnectionCalls("ea1.2007-11.net.usef.energy:1-4", 0, 0);
            verifyConnectionCalls("ea1.2007-11.net.usef.energy:1-5", 0, 0);
            verifyConnectionCalls("ea1.2007-11.net.usef.energy:1-6", 0, 0);
            verifyConnectionCalls("ea1.2007-11.net.usef.energy:1-7", 0, 0);
            verifyConnectionCalls("ea1.2007-11.net.usef.energy:1-8", 0, 0);
            verifyConnectionCalls("ea1.2007-11.net.usef.energy:1-9", 0, 0);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyConnectionCalls(String entityAddress, int persists, int deletes) {
        Mockito.verify(mdcConnectionRepository, Mockito.times(persists)).persist(new Connection(entityAddress));
        Mockito.verify(mdcConnectionRepository, Mockito.times(deletes)).deleteByEntityAddress(entityAddress);
    }
    private void mockConnection(String entityAddress, boolean exists)  throws BusinessValidationException{
        Connection connection = createConnection(entityAddress);
        if (exists) {
            Mockito.when(mdcConnectionRepository.find(Matchers.eq(connection))).thenReturn(connection);
            Mockito.doNothing().when(mdcConnectionRepository).deleteByEntityAddress(entityAddress);
            Mockito.doThrow(new BusinessValidationException(RestError.DUPLICATE, "Connection", entityAddress))
                    .when(validationService)
                    .checkDuplicateConnection(Matchers.matches(entityAddress));
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "Connection", entityAddress))
                    .when(validationService)
                    .checkExistingConnection(Matchers.matches(entityAddress));
        }
    }

    @Test
    public void testBalanceResponsiblePartyBatch() throws Exception  {
        mockBalanceResponsibleParty("brp0.usef.energy", true);
        mockBalanceResponsibleParty("brp1.usef.energy", false);
        mockBalanceResponsibleParty("brp2.usef.energy", false);
        mockBalanceResponsibleParty("brp3.usef.energy", true);

        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"domain\": \"brp0.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"brp1.usef.energy\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"brp2.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"brp3.usef.energy\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"brp4.usef.energy\", \"subdomain\": \"www.usef-example.com\"},"
                    + "{\"methode\" : \"POST\", \"domain\": \"brp5.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"brp6usefenergy\"},"
                    + "{\"method\" : \"GET\", \"gebied\": \"brp7.usef.energy\"}"
                    + "]";

            List<RestResult> result = service.processBalanceResponsiblePartyBatch(jsonText);

            String resultString = JsonUtil.createJsonText(result);

            verifyBalanceRsponsiblePartyCalls("brp0.usef.energy", 0, 1);
            verifyBalanceRsponsiblePartyCalls("brp1.usef.energy", 1, 0);
            verifyBalanceRsponsiblePartyCalls("brp2.usef.energy", 0, 0);
            verifyBalanceRsponsiblePartyCalls("brp3.usef.energy", 0, 0);
            verifyBalanceRsponsiblePartyCalls("brp4.usef.energy", 0, 0);
            verifyBalanceRsponsiblePartyCalls("brp5.usef.energy", 0, 0);
            verifyBalanceRsponsiblePartyCalls("brp6.usef.energy", 0, 0);
            verifyBalanceRsponsiblePartyCalls("brp7.usef.energy", 0, 0);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyBalanceRsponsiblePartyCalls(String domain, int persists, int deletes) {
        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(persists)).persist(new BalanceResponsibleParty(domain));
        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(deletes)).deleteByDomain(domain);
    }

    @Test
    public void testDistributionSystemOperatorBatch() throws Exception  {
        mockDistributionSystemOperator("mdc0.usef.energy", true);
        mockDistributionSystemOperator("mdc1.usef.energy", false);
        mockDistributionSystemOperator("mdc2.usef.energy", false);
        mockDistributionSystemOperator("mdc3.usef.energy", true);

        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"domain\": \"mdc0.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"mdc1.usef.energy\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"mdc2.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"mdc3.usef.energy\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"mdc4.usef.energy\", \"subdomain\": \"www.usef-example.com\"},"
                    + "{\"methode\" : \"POST\", \"domain\": \"mdc5.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"mdc6usefenergy\"},"
                    + "{\"method\" : \"GET\", \"gebied\": \"mdc7.usef.energy\"}"
                    + "]";

            List<RestResult> result = service.processDistributionSystemOperatorBatch(jsonText);

            String resultString = JsonUtil.createJsonText(result);

            verifyDistributionSystemOperatorCalls("mdc0.usef.energy", 0, 1);
            verifyDistributionSystemOperatorCalls("mdc1.usef.energy", 1, 0);
            verifyDistributionSystemOperatorCalls("mdc2.usef.energy", 0, 0);
            verifyDistributionSystemOperatorCalls("mdc3.usef.energy", 0, 0);
            verifyDistributionSystemOperatorCalls("mdc4.usef.energy", 0, 0);
            verifyDistributionSystemOperatorCalls("mdc5.usef.energy", 0, 0);
            verifyDistributionSystemOperatorCalls("mdc6.usef.energy", 0, 0);
            verifyDistributionSystemOperatorCalls("mdc7.usef.energy", 0, 0);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyDistributionSystemOperatorCalls(String domain, int persists, int deletes) {
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(persists)).persist(new DistributionSystemOperator(domain));
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(deletes)).deleteByDomain(domain);
    }

    @Test
    public void testCommonReferenceOperatorBatch() throws Exception  {
        mockCommonReferenceOperator("cro0.usef.energy", true);
        mockCommonReferenceOperator("cro1.usef.energy", false);
        mockCommonReferenceOperator("cro2.usef.energy", false);
        mockCommonReferenceOperator("cro3.usef.energy", true);

        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"domain\": \"cro0.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"cro1.usef.energy\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"cro2.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"cro3.usef.energy\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"cro4.usef.energy\", \"subdomain\": \"www.usef-example.com\"},"
                    + "{\"methode\" : \"POST\", \"domain\": \"cro5.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"cro6usefenergy\"},"
                    + "{\"method\" : \"GET\", \"gebied\": \"cro7.usef.energy\"}"
                    + "]";

            List<RestResult> result = service.processCommonReferenceOperatorBatch(jsonText);

            String resultString = JsonUtil.createJsonText(result);

            verifyCommonReferenceOperatorCalls("cro0.usef.energy", 0, 1);
            verifyCommonReferenceOperatorCalls("cro1.usef.energy", 1, 0);
            verifyCommonReferenceOperatorCalls("cro2.usef.energy", 0, 0);
            verifyCommonReferenceOperatorCalls("cro3.usef.energy", 0, 0);
            verifyCommonReferenceOperatorCalls("cro4.usef.energy", 0, 0);
            verifyCommonReferenceOperatorCalls("cro5.usef.energy", 0, 0);
            verifyCommonReferenceOperatorCalls("cro6.usef.energy", 0, 0);
            verifyCommonReferenceOperatorCalls("cro7.usef.energy", 0, 0);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyCommonReferenceOperatorCalls(String domain, int persists, int deletes) {
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(persists)).persist(new CommonReferenceOperator(domain));
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(deletes)).deleteByDomain(domain);
    }

    @Test
    public void testFindAllConnections() throws BusinessValidationException, IOException {
        List<Connection> connections = new ArrayList<>();
        connections.add(createConnection("ean.0000000000000"));
        connections.add(createConnection("ea1.2007-11.net.usef.energy:1-2"));
        Mockito.when(mdcConnectionRepository.findAllConnections()).thenReturn(connections);
        RestResult result = service.findAllConnections();
        assertRestResult(200,
                "[{\"entityAddress\":\"ean.0000000000000\"},{\"entityAddress\":\"ea1.2007-11.net.usef.energy:1-2\"}]", 0, result);
    }

    @Test
    public void testFindAllBalanceResponsibleParties() throws BusinessValidationException, IOException {
        List<BalanceResponsibleParty> participants = new ArrayList<>();
        participants.add(createBalanceResponsibleParty("brp1.usef.example.com"));
        participants.add(createBalanceResponsibleParty("brp2.usef.example.com"));
        Mockito.when(balanceResponsiblePartyRepository.findAll()).thenReturn(participants);
        RestResult result = service.findAllBalanceResponsibleParties();
        assertRestResult(200,
                "[{\"domain\":\"brp1.usef.example.com\"},{\"domain\":\"brp2.usef.example.com\"}]", 0, result);
    }

    @Test
    public void testFindAllDistributionSystemOperators() throws BusinessValidationException, IOException {
        List<DistributionSystemOperator> participants = new ArrayList<>();
        participants.add(createDistributionSystemOperator("dso1.usef.example.com"));
        participants.add(createDistributionSystemOperator("dso2.usef.example.com"));
        Mockito.when(distributionSystemOperatorRepository.findAll()).thenReturn(participants);
        RestResult result = service.findAllDistributionSystemOperators();
        assertRestResult(200,
                "[{\"domain\":\"dso1.usef.example.com\"},{\"domain\":\"dso2.usef.example.com\"}]", 0, result);
    }

    @Test
    public void testFindAllCommonReferenceOperators() throws BusinessValidationException, IOException {
        List<CommonReferenceOperator> participants = new ArrayList<>();
        participants.add(createCommonReferenceOperator("cro1.usef.example.com"));
        participants.add(createCommonReferenceOperator("cro2.usef.example.com"));
        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(participants);
        RestResult result = service.findAllCommonReferenceOperators();
        assertRestResult(200,
                "[{\"domain\":\"cro1.usef.example.com\"},{\"domain\":\"cro2.usef.example.com\"}]", 0, result);
    }

    /*
     * Convenience methods.
     */
    private void assertRestResult(int code, String body, int errors, RestResult result) {
        assertEquals("Code", code, result.getCode());
        assertEquals("Headers", 1, result.getHeaders().size());
        assertEquals("Header Name", "Content-Type", result.getHeaders().get(0).getName());
        assertEquals("Header value", "application/json", result.getHeaders().get(0).getValue());
        if (body == null) {
            assertNull("Body", result.getBody());
        } else {
            assertEquals("Body", body, result.getBody());
        }

        assertEquals("Errors", errors, result.getErrors().size());
    }

    private Connection createConnection(String entityAddress) {
        Connection connection = new Connection();
        connection.setEntityAddress(entityAddress);
        return connection;
    }

    private BalanceResponsibleParty createBalanceResponsibleParty(String domain) {
        BalanceResponsibleParty participant = new BalanceResponsibleParty();
        participant.setDomain(domain);
        return participant;
    }

    private void mockBalanceResponsibleParty(String domain, boolean exists)  throws BusinessValidationException{
        BalanceResponsibleParty participant = createBalanceResponsibleParty(domain);
        if (exists) {
            Mockito.when(balanceResponsiblePartyRepository.find(Matchers.eq(participant))).thenReturn(participant);
            Mockito.doNothing().when(balanceResponsiblePartyRepository).deleteByDomain(domain);
            Mockito.doThrow(new BusinessValidationException(RestError.DUPLICATE, "BalanceResponsibleParty", domain))
                    .when(validationService)
                    .checkDuplicateBalanceResponsiblePartyDomain(Matchers.matches(domain));
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "BalanceResponsibleParty", domain))
                    .when(validationService)
                    .checkExistingBalanceResponsiblePartyDomain(Matchers.matches(domain));
        }
    }

    private DistributionSystemOperator createDistributionSystemOperator(String domain) {
        DistributionSystemOperator participant = new DistributionSystemOperator();
        participant.setDomain(domain);
        return participant;
    }

    private void mockDistributionSystemOperator(String domain, boolean exists)  throws BusinessValidationException{
        DistributionSystemOperator participant = createDistributionSystemOperator(domain);
        if (exists) {
            Mockito.when(distributionSystemOperatorRepository.find(Matchers.eq(participant))).thenReturn(participant);
            Mockito.doNothing().when(distributionSystemOperatorRepository).deleteByDomain(domain);
            Mockito.doThrow(new BusinessValidationException(RestError.DUPLICATE, "DistributionSystemOperator", domain))
                    .when(validationService)
                    .checkDuplicateDistributionSystemOperatorDomain(Matchers.matches(domain));
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "DistributionSystemOperator", domain))
                    .when(validationService)
                    .checkExistingDistributionSystemOperatorDomain(Matchers.matches(domain));
        }
    }

    private CommonReferenceOperator createCommonReferenceOperator(String domain) {
        CommonReferenceOperator participant = new CommonReferenceOperator();
        participant.setDomain(domain);
        return participant;
    }

    private void mockCommonReferenceOperator(String domain, boolean exists)  throws BusinessValidationException{
        CommonReferenceOperator participant = createCommonReferenceOperator(domain);
        if (exists) {
            Mockito.when(commonReferenceOperatorRepository.find(Matchers.eq(participant))).thenReturn(participant);
            Mockito.doNothing().when(commonReferenceOperatorRepository).deleteByDomain(domain);
            Mockito.doThrow(new BusinessValidationException(RestError.DUPLICATE, "CommonReferenceOperator", domain))
                    .when(validationService)
                    .checkDuplicateCommonReferenceOperatorDomain(Matchers.matches(domain));
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "CommonReferenceOperator", domain))
                    .when(validationService)
                    .checkExistingCommonReferenceOperatorDomain(Matchers.matches(domain));
        }
    }
}
