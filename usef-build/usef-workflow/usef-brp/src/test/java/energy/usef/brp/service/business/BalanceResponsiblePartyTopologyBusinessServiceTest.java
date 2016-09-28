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
package energy.usef.brp.service.business;

import energy.usef.brp.model.CommonReferenceOperator;
import energy.usef.brp.model.SynchronisationConnection;
import energy.usef.brp.model.SynchronisationConnectionStatus;
import energy.usef.brp.repository.CommonReferenceOperatorRepository;
import energy.usef.brp.repository.SynchronisationConnectionRepository;
import energy.usef.brp.repository.SynchronisationConnectionStatusRepository;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.exception.RestError;
import energy.usef.core.rest.RestResult;
import energy.usef.core.util.JsonUtil;
import org.joda.time.LocalDateTime;
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
public class BalanceResponsiblePartyTopologyBusinessServiceTest {

    BalanceResponsiblePartyTopologyBusinessService service;

    @Mock
    BalanceResponsiblePartyValidationBusinessService validationService;

    @Mock
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Mock
    SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Mock
    SynchronisationConnectionStatusRepository synchronisationConnectionStatusRepository;

    @Before
    public void init() {
        service = new BalanceResponsiblePartyTopologyBusinessService();
        Whitebox.setInternalState(service, "validationService", validationService);
        Whitebox.setInternalState(service, "commonReferenceOperatorRepository", commonReferenceOperatorRepository);
        Whitebox.setInternalState(service, "synchronisationConnectionRepository", synchronisationConnectionRepository);
        Whitebox.setInternalState(service, "synchronisationConnectionStatusRepository", synchronisationConnectionStatusRepository);
    }

    @Test
    public void testProcessSynchronisationConnectionBatch() throws Exception
    {
        List<CommonReferenceOperator> participants = new ArrayList<>();
        participants.add(createCommonReferenceOperator(1L, "cro1.usef.example.com"));
        participants.add(createCommonReferenceOperator(2L, "cro2.usef.example.com"));
        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(participants);

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

            List<RestResult> result = service.processSynchronisationConnectionBatch(jsonText);
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

            Mockito.verify(synchronisationConnectionStatusRepository, Mockito.times(4)).persist(Matchers.any(SynchronisationConnectionStatus.class));

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyConnectionCalls(String entityAddress, int persists, int deletes) {
        Mockito.verify(synchronisationConnectionRepository, Mockito.times(persists)).persist(createSynchronisationConnection(entityAddress));
        Mockito.verify(synchronisationConnectionRepository, Mockito.times(deletes)).deleteByEntityAddress(entityAddress);
    }
    private void mockConnection(String entityAddress, boolean exists)  throws BusinessValidationException{
        SynchronisationConnection connection = createSynchronisationConnection(entityAddress);
        if (exists) {
            Mockito.when(synchronisationConnectionRepository.find(Matchers.eq(connection))).thenReturn(connection);
            Mockito.doNothing().when(synchronisationConnectionRepository).deleteByEntityAddress(entityAddress);
            Mockito.doThrow(new BusinessValidationException(RestError.DUPLICATE, "SynchronisationConnection", entityAddress))
                    .when(validationService)
                    .checkDuplicateSynchronisationConnection(Matchers.matches(entityAddress));
        } else {
            Mockito.doNothing().when(synchronisationConnectionRepository).persist(connection);
            Mockito.doNothing().when(synchronisationConnectionStatusRepository).persist(Matchers.any(SynchronisationConnectionStatus.class));
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "SynchronisationConnection", entityAddress))
                    .when(validationService)
                    .checkExistingSynchronisationConnection(Matchers.matches(entityAddress));
        }
    }

    private SynchronisationConnection createSynchronisationConnection(String entityAddress) {
        SynchronisationConnection synchronisationConnection = new SynchronisationConnection();
        synchronisationConnection.setEntityAddress(entityAddress);
        synchronisationConnection.setLastModificationTime(new LocalDateTime());
        return synchronisationConnection;
    }

    @Test
    public void testCommonReferenceOperatorBatch() throws Exception  {
        mockCommonReferenceOperator(0L, "cro0.usef.energy", true);
        mockCommonReferenceOperator(null, "cro1.usef.energy", false);
        mockCommonReferenceOperator(null, "cro2.usef.energy", false);
        mockCommonReferenceOperator(-3L, "cro3.usef.energy", true);

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
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(persists)).persist(newCommonReferenceOperator(domain));
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(deletes)).deleteByDomain(domain);
    }

    private CommonReferenceOperator newCommonReferenceOperator(String domain) {
        CommonReferenceOperator object = new CommonReferenceOperator();
        object.setDomain(domain);
        return object;
    }

    private CommonReferenceOperator createCommonReferenceOperator(Long id, String domain) {
        CommonReferenceOperator participant = new CommonReferenceOperator();
        participant.setId(id);
        participant.setDomain(domain);
        return participant;
    }

    private void mockCommonReferenceOperator(Long id, String domain, boolean exists)  throws BusinessValidationException{
        CommonReferenceOperator participant = createCommonReferenceOperator(id, domain);
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

    @Test
    public void testFindAllCommonReferenceOperators() throws BusinessValidationException, IOException {
        List<CommonReferenceOperator> participants = new ArrayList<>();
        participants.add(createCommonReferenceOperator(1L, "cro1.usef.example.com"));
        participants.add(createCommonReferenceOperator(2L, "cro2.usef.example.com"));
        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(participants);
        RestResult result = service.findAllCommonReferenceOperators();
        assertRestResult(200,
                "[{\"id\":1,\"domain\":\"cro1.usef.example.com\"},{\"id\":2,\"domain\":\"cro2.usef.example.com\"}]", 0, result);
    }


    private SynchronisationConnection createSynchronisationConnection(Long id, String entityAddress) {
        SynchronisationConnection object = new SynchronisationConnection();
        object.setId(id);
        object.setEntityAddress(entityAddress);
        object.setLastModificationTime(new LocalDateTime(2016, 1, 1, 0, 0, 0,0));
        return object;
    }

    @Test
    public void testFindAllSynchronisationConnections() throws BusinessValidationException, IOException{
        List<SynchronisationConnection> participants = new ArrayList<>();
        participants.add(createSynchronisationConnection(1L, "ean.0000000000000"));
        participants.add(createSynchronisationConnection(2L, "ean.0000000000002"));
        Mockito.when(synchronisationConnectionRepository.findAll()).thenReturn(participants);
        RestResult result = service.findAllSynchronisationConnections();
        assertRestResult(200,
                "[{\"id\":1,\"entityAddress\":\"ean.0000000000000\",\"lastModificationTime\":\"2016-01-01T00:00:00.000\"},{\"id\":2,\"entityAddress\":\"ean.0000000000002\",\"lastModificationTime\":\"2016-01-01T00:00:00.000\"}]", 0, result);

    }


    @Test
    public void testFindAllCommonReferenceOperatorNone() throws BusinessValidationException, IOException {
        List<CommonReferenceOperator> participants = new ArrayList<>();
        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(participants);
        RestResult result = service.findAllCommonReferenceOperators();
        assertRestResult(200, "[]", 0, result);
    }

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


}
