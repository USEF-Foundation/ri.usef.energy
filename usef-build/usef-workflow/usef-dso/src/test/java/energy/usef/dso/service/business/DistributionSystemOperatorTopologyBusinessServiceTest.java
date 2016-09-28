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
package energy.usef.dso.service.business;

import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.exception.RestError;
import energy.usef.core.rest.RestResult;
import energy.usef.core.util.JsonUtil;
import energy.usef.dso.model.CommonReferenceOperator;
import energy.usef.dso.model.SynchronisationCongestionPoint;
import energy.usef.dso.model.SynchronisationCongestionPointStatus;
import energy.usef.dso.model.SynchronisationConnection;
import energy.usef.dso.repository.CommonReferenceOperatorRepository;
import energy.usef.dso.repository.SynchronisationCongestionPointRepository;
import energy.usef.dso.repository.SynchronisationCongestionPointStatusRepository;
import energy.usef.dso.repository.SynchronisationConnectionRepository;
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
public class DistributionSystemOperatorTopologyBusinessServiceTest {

    DistributionSystemOperatorTopologyBusinessService service;

    @Mock
    DistributionSystemOperatorValidationBusinessService validationService;

    @Mock
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Mock
    SynchronisationCongestionPointRepository synchronisationCongestionPointRepository;

    @Mock
    SynchronisationCongestionPointStatusRepository synchronisationCongestionPointStatusRepository;

    @Mock
    SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Before
    public void init() {
        service = new DistributionSystemOperatorTopologyBusinessService();
        Whitebox.setInternalState(service, "validationService", validationService);
        Whitebox.setInternalState(service, "commonReferenceOperatorRepository", commonReferenceOperatorRepository);
        Whitebox.setInternalState(service, "synchronisationCongestionPointRepository", synchronisationCongestionPointRepository);
        Whitebox.setInternalState(service, "synchronisationCongestionPointStatusRepository", synchronisationCongestionPointStatusRepository);
        Whitebox.setInternalState(service, "synchronisationConnectionRepository", synchronisationConnectionRepository);

    }

    @Test
    public void testProcessSynchronisationCongestionPointBatch() throws Exception
    {
        List<CommonReferenceOperator> participants = new ArrayList<>();
        participants.add(createCommonReferenceOperator(1L, "cro1.usef.example.com"));
        participants.add(createCommonReferenceOperator(2L, "cro2.usef.example.com"));
        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(participants);



        SynchronisationCongestionPoint scp0 = mockCongestionPoint("ea1.2007-11.net.usef.energy:1-0", true);
        SynchronisationCongestionPoint scp1 = mockCongestionPoint("ea1.2007-11.net.usef.energy:1-1", false);
        SynchronisationCongestionPoint scp2 = mockCongestionPoint("ea1.2007-11.net.usef.energy:1-2", true);
        SynchronisationCongestionPoint scp3 = mockCongestionPoint("ea1.2007-11.net.usef.energy:1-3", false);
        SynchronisationCongestionPoint scp4 = mockCongestionPoint("ea1.2007-11.net.usef.energy:1-4", false);
        SynchronisationCongestionPoint scp5 = mockCongestionPoint("ea1.2007-11.net.usef.energy:1-5", true);

        Mockito.when(synchronisationCongestionPointRepository.findByEntityAddress("ea1.2007-11.net.usef.energy:1-0")).thenReturn(scp0);
        Mockito.when(synchronisationCongestionPointRepository.findByEntityAddress("ea1.2007-11.net.usef.energy:1-2")).thenReturn(scp2);

        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-0\"},"
                    + "{\"method\" : \"POST\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-1\"},"
                    + "{\"method\" : \"DELETE\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-2\"},"
                    + "{\"method\" : \"POST\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-3\"},"
                    + "{\"method\" : \"DELETE\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-4\"},"
                    + "{\"method\" : \"POST\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-5\"},"
                    + "{\"method\" : \"MAIL\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-6\"},"
                    + "{\"methode\" : \"POST\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-7\"},"
                    + "{\"method\" : \"POST\", \"entityAddres\": \"ea1.2007-11.net.usef.energy:1-8\"},"
                    + "{\"method3\" : \"POST\", \"entityAddress\": \"ea1.2007-11.net.usef.energy:1-9\", \"additional\": 12}"
                    + "]";

            List<RestResult> result = service.processSynchronisationCongestionPointBatch(jsonText);
            String resultString = JsonUtil.createJsonText(result);

            /// Verify total number of persists and deletes
            Mockito.verify(synchronisationCongestionPointRepository, Mockito.times(2)).persist(Matchers.any(SynchronisationCongestionPoint.class));
            Mockito.verify(synchronisationCongestionPointRepository, Mockito.times(2)).delete(Matchers.any(SynchronisationCongestionPoint.class));

            Mockito.verify(synchronisationCongestionPointStatusRepository, Mockito.times(2)).deleteFor(Matchers.any(SynchronisationCongestionPoint.class));
            Mockito.verify(synchronisationConnectionRepository, Mockito.times(2)).deleteFor(Matchers.any(SynchronisationCongestionPoint.class));

            /// Verify specific persists and deletes
            Mockito.verify(synchronisationCongestionPointRepository, Mockito.times(1)).delete(Matchers.eq(scp0));
            Mockito.verify(synchronisationCongestionPointStatusRepository, Mockito.times(1)).deleteFor(scp0);
            Mockito.verify(synchronisationConnectionRepository, Mockito.times(1)).deleteFor(scp0);

            Mockito.verify(synchronisationCongestionPointRepository, Mockito.times(1)).delete(Matchers.eq(scp2));
            Mockito.verify(synchronisationCongestionPointStatusRepository, Mockito.times(1)).deleteFor(scp2);
            Mockito.verify(synchronisationConnectionRepository, Mockito.times(1)).deleteFor(scp2);

            Mockito.verify(synchronisationCongestionPointRepository, Mockito.times(1)).persist(Matchers.eq(scp1));

            Mockito.verify(synchronisationCongestionPointStatusRepository, Mockito.times(4)).persist(Matchers.any(SynchronisationCongestionPointStatus.class));

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyCongestionPointCalls(SynchronisationCongestionPoint scp, int persists, int deletes) {

        Mockito.verify(synchronisationCongestionPointRepository, Mockito.times(persists)).persist(scp);
        Mockito.verify(synchronisationCongestionPointRepository, Mockito.times(deletes)).delete(scp);
    }
    private SynchronisationCongestionPoint mockCongestionPoint(String entityAddress, boolean exists)  throws BusinessValidationException{
        SynchronisationCongestionPoint synchronisationCongestionPoint = createSynchronisationCongestionPoint(entityAddress);
        if (exists) {
            Mockito.when(synchronisationCongestionPointRepository.find(Matchers.eq(synchronisationCongestionPoint))).thenReturn(synchronisationCongestionPoint);
            Mockito.doNothing().when(synchronisationCongestionPointRepository).deleteByEntityAddress(entityAddress);
            Mockito.doThrow(new BusinessValidationException(RestError.DUPLICATE, "SynchronisationCongestionPoint", entityAddress))
                    .when(validationService)
                    .checkDuplicateSynchronisationCongestionPoint(Matchers.matches(entityAddress));
        } else {
            Mockito.doNothing().when(synchronisationCongestionPointRepository).persist(synchronisationCongestionPoint);
            Mockito.doNothing().when(synchronisationCongestionPointStatusRepository).persist(Matchers.any(SynchronisationCongestionPointStatus.class));
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "SynchronisationCongestionPoint", entityAddress))
                    .when(validationService)
                    .checkExistingSynchronisationCongestionPoint(Matchers.matches(entityAddress));
        }
        return synchronisationCongestionPoint;
    }

    private SynchronisationCongestionPoint createSynchronisationCongestionPoint(String entityAddress) {
        SynchronisationCongestionPoint synchronisationCongestionPoint = new SynchronisationCongestionPoint();
        synchronisationCongestionPoint.setEntityAddress(entityAddress);
        synchronisationCongestionPoint.setLastModificationTime(new LocalDateTime());
        return synchronisationCongestionPoint;
    }

    @Test
    public void testCommonReferenceOperatorBatch() throws Exception  {
        CommonReferenceOperator cro0 = mockCommonReferenceOperator(0L, "cro0.usef.energy", true);
        CommonReferenceOperator cro1 = mockCommonReferenceOperator(null, "cro1.usef.energy", false);
        CommonReferenceOperator cro2 = mockCommonReferenceOperator(null, "cro2.usef.energy", false);
        CommonReferenceOperator cro3 = mockCommonReferenceOperator(-3L, "cro3.usef.energy", true);

        Mockito.when(commonReferenceOperatorRepository.findByDomain("cro0.usef.energy")).thenReturn(cro0);

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

            /// Verify total number of persists and deletes
            Mockito.verify(commonReferenceOperatorRepository, Mockito.times(1)).persist(Matchers.any(CommonReferenceOperator.class));
            Mockito.verify(commonReferenceOperatorRepository, Mockito.times(1)).delete(Matchers.any(CommonReferenceOperator.class));

            Mockito.verify(synchronisationCongestionPointStatusRepository, Mockito.times(1)).deleteFor(Matchers.any(CommonReferenceOperator.class));

            /// Verify specific persists and deletes
            Mockito.verify(commonReferenceOperatorRepository, Mockito.times(1)).delete(Matchers.eq(cro0));
            Mockito.verify(synchronisationCongestionPointStatusRepository, Mockito.times(1)).deleteFor(cro0);

            Mockito.verify(commonReferenceOperatorRepository, Mockito.times(1)).persist(Matchers.eq(cro1));

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CommonReferenceOperator createCommonReferenceOperator(Long id, String domain) {
        CommonReferenceOperator participant = new CommonReferenceOperator();
        participant.setId(id);
        participant.setDomain(domain);
        return participant;
    }

    private CommonReferenceOperator mockCommonReferenceOperator(Long id, String domain, boolean exists)  throws BusinessValidationException{
        CommonReferenceOperator cro = createCommonReferenceOperator(id, domain);
        if (exists) {
            Mockito.when(commonReferenceOperatorRepository.find(Matchers.eq(cro))).thenReturn(cro);
            Mockito.doNothing().when(commonReferenceOperatorRepository).deleteByDomain(domain);
            Mockito.doThrow(new BusinessValidationException(RestError.DUPLICATE, "CommonReferenceOperator", domain))
                    .when(validationService)
                    .checkDuplicateCommonReferenceOperatorDomain(Matchers.matches(domain));
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "CommonReferenceOperator", domain))
                    .when(validationService)
                    .checkExistingCommonReferenceOperatorDomain(Matchers.matches(domain));
        }
        return cro;
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

    @Test
    public void testFindAllSynchronisationCongestionPoints() throws BusinessValidationException, IOException {
        List<SynchronisationCongestionPoint> objects = new ArrayList<>();

        SynchronisationCongestionPoint cp1 = createSynchronisationCongestionPoint(1L, "ea1.2007-11.net.usef.energy:1-0");
        cp1.getConnections().add(createSynchronisationConnection(0L, "ean.0000000000000"));
        cp1.getConnections().add(createSynchronisationConnection(1L, "ean.0000000000001"));
        cp1.getConnections().add(createSynchronisationConnection(2L, "ean.0000000000002"));

        SynchronisationCongestionPoint cp2 = createSynchronisationCongestionPoint(1L, "ea1.2007-11.net.usef.energy:1-1");
        cp2.getConnections().add(createSynchronisationConnection(0L, "ean.0000000000003"));
        cp2.getConnections().add(createSynchronisationConnection(1L, "ean.0000000000004"));
        cp2.getConnections().add(createSynchronisationConnection(2L, "ean.0000000000005"));

        objects.add(cp1);
        objects.add(cp2);


        Mockito.when(synchronisationCongestionPointRepository.findAll()).thenReturn(objects);
        RestResult result = service.findAllSynchronisationCongestionPoints();
        assertRestResult(200,
                "[{\"id\":1,\"entityAddress\":\"ea1.2007-11.net.usef.energy:1-0\",\"lastModificationTime\":\"2016-01-01T00:00:00.000\",\"connections\":[{\"id\":0,\"entityAddress\":\"ean.0000000000000\"},{\"id\":1,\"entityAddress\":\"ean.0000000000001\"},{\"id\":2,\"entityAddress\":\"ean.0000000000002\"}]},{\"id\":1,\"entityAddress\":\"ea1.2007-11.net.usef.energy:1-1\",\"lastModificationTime\":\"2016-01-01T00:00:00.000\",\"connections\":[{\"id\":0,\"entityAddress\":\"ean.0000000000003\"},{\"id\":1,\"entityAddress\":\"ean.0000000000004\"},{\"id\":2,\"entityAddress\":\"ean.0000000000005\"}]}]",
                0, result);
    }

    @Test
    public void testFindSynchronisationCongestionPoint() throws BusinessValidationException, IOException {
        String entityAddress = "ea1.2007-11.net.usef.energy:1-1";

        SynchronisationCongestionPoint cp1 = createSynchronisationCongestionPoint(1L, entityAddress);
        cp1.getConnections().add(createSynchronisationConnection(0L, "ean.0000000000000"));
        cp1.getConnections().add(createSynchronisationConnection(1L, "ean.0000000000001"));
        cp1.getConnections().add(createSynchronisationConnection(2L, "ean.0000000000002"));

        Mockito.when(synchronisationCongestionPointRepository.findByEntityAddress(entityAddress)).thenReturn(cp1);

        RestResult result = service.findSynchronisationCongestionPoint(entityAddress);
        assertRestResult(200,
                "{\"id\":1,\"entityAddress\":\"ea1.2007-11.net.usef.energy:1-1\",\"lastModificationTime\":\"2016-01-01T00:00:00.000\",\"connections\":[{\"id\":0,\"entityAddress\":\"ean.0000000000000\"},{\"id\":1,\"entityAddress\":\"ean.0000000000001\"},{\"id\":2,\"entityAddress\":\"ean.0000000000002\"}]}",
                0, result);
    }

    private SynchronisationCongestionPoint createSynchronisationCongestionPoint(Long id, String entityAddress) {
        SynchronisationCongestionPoint object = new SynchronisationCongestionPoint();
        object.setId(id);
        object.setEntityAddress(entityAddress);
        object.setLastModificationTime(new LocalDateTime(2016,1,1,0, 0,0,0));
        return object;
    }

    private SynchronisationConnection createSynchronisationConnection(Long id, String entityAddress) {
        SynchronisationConnection object = new SynchronisationConnection();
        object.setId(id);
        object.setEntityAddress(entityAddress);

        return object;
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
