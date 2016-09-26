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
import energy.usef.brp.repository.CommonReferenceOperatorRepository;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.exception.RestError;
import energy.usef.core.rest.RestResult;
import energy.usef.core.util.JsonUtil;
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

    @Before
    public void init() {
        service = new BalanceResponsiblePartyTopologyBusinessService();
        Whitebox.setInternalState(service, "validationService", validationService);
        Whitebox.setInternalState(service, "commonReferenceOperatorRepository", commonReferenceOperatorRepository);
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
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(persists)).persist(new CommonReferenceOperator(domain));
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(deletes)).deleteByDomain(domain);
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
