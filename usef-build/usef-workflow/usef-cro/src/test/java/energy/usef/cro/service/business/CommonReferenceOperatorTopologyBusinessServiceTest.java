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

import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.exception.RestError;
import energy.usef.core.util.JsonUtil;
import energy.usef.cro.model.Aggregator;
import energy.usef.cro.model.BalanceResponsibleParty;
import energy.usef.cro.model.DistributionSystemOperator;
import energy.usef.cro.model.MeterDataCompany;
import energy.usef.cro.repository.AggregatorRepository;
import energy.usef.cro.repository.BalanceResponsiblePartyRepository;
import energy.usef.cro.repository.DistributionSystemOperatorRepository;
import energy.usef.cro.repository.MeterDataCompanyRepository;
import energy.usef.core.rest.RestResult;
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
public class CommonReferenceOperatorTopologyBusinessServiceTest {

    CommonReferenceOperatorTopologyBusinessService service;

    @Mock
    CommonReferenceOperatorValidationBusinessService validationService;

    @Mock AggregatorRepository aggregatorRepository;

    @Mock BalanceResponsiblePartyRepository balanceResponsiblePartyRepository;

    @Mock DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Mock MeterDataCompanyRepository meterDataCompanyRepository;

    @Before
    public void init() {
        service = new CommonReferenceOperatorTopologyBusinessService();
        Whitebox.setInternalState(service, "validationService", validationService);
        Whitebox.setInternalState(service, "aggregatorRepository", aggregatorRepository);
        Whitebox.setInternalState(service, "balanceResponsiblePartyRepository", balanceResponsiblePartyRepository);
        Whitebox.setInternalState(service, "distributionSystemOperatorRepository", distributionSystemOperatorRepository);
        Whitebox.setInternalState(service, "meterDataCompanyRepository", meterDataCompanyRepository);
    }

    @Test
    public void testAggregatorBatch() throws Exception  {
        mockAggregeator(0L, "agr0.usef.energy", true);
        mockAggregeator(null, "agr1.usef.energy", false);
        mockAggregeator(null, "agr2.usef.energy", false);
        mockAggregeator(-3L, "agr3.usef.energy", true);

        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"domain\": \"agr0.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"agr1.usef.energy\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"agr2.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"agr3.usef.energy\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"agr4.usef.energy\", \"subdomain\": \"www.usef-example.com\"},"
                    + "{\"methode\" : \"POST\", \"domain\": \"agr5.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"agr6usefenergy\"},"
                    + "{\"method\" : \"GET\", \"gebied\": \"agr7.usef.energy\"}"
                    + "]";

            List<RestResult> result = service.processAggregatorBatch(jsonText);

            String resultString = JsonUtil.createJsonText(result);

            verifyAggregatorCalls("agr0.usef.energy", 0, 1);
            verifyAggregatorCalls("agr1.usef.energy", 1, 0);
            verifyAggregatorCalls("agr2.usef.energy", 0, 0);
            verifyAggregatorCalls("agr3.usef.energy", 0, 0);
            verifyAggregatorCalls("agr4.usef.energy", 0, 0);
            verifyAggregatorCalls("agr5.usef.energy", 0, 0);
            verifyAggregatorCalls("agr6.usef.energy", 0, 0);
            verifyAggregatorCalls("agr7.usef.energy", 0, 0);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyAggregatorCalls(String domain, int persists, int deletes) {
        Mockito.verify(aggregatorRepository, Mockito.times(persists)).persist(new Aggregator(domain));
        Mockito.verify(aggregatorRepository, Mockito.times(deletes)).deleteByDomain(domain);
    }


    private void mockAggregeator(Long id, String domain, boolean exists)  throws BusinessValidationException{
        Aggregator participant = createAggregator(id, domain);
        if (exists) {
            Mockito.when(aggregatorRepository.find(Matchers.eq(participant))).thenReturn(participant);
            Mockito.doNothing().when(aggregatorRepository).deleteByDomain(domain);
            Mockito.doThrow(new BusinessValidationException(RestError.DUPLICATE, "Aggregator", domain))
                    .when(validationService)
                    .checkDuplicateAggregatorDomain(Matchers.matches(domain));
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "Aggregator", domain))
                    .when(validationService)
                    .checkExistingAggregatorDomain(Matchers.matches(domain));
        }
    }

    @Test
    public void testBalanceResponsiblePartyBatch() throws Exception  {
        mockBalanceResponsibleParty(0L, "brp0.usef.energy", true);
        mockBalanceResponsibleParty(null, "brp1.usef.energy", false);
        mockBalanceResponsibleParty(null, "brp2.usef.energy", false);
        mockBalanceResponsibleParty(-3L, "brp3.usef.energy", true);

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

            verifyBalanceResponsiblePartyCalls("brp0.usef.energy", 0, 1);
            verifyBalanceResponsiblePartyCalls("brp1.usef.energy", 1, 0);
            verifyBalanceResponsiblePartyCalls("brp2.usef.energy", 0, 0);
            verifyBalanceResponsiblePartyCalls("brp3.usef.energy", 0, 0);
            verifyBalanceResponsiblePartyCalls("brp4.usef.energy", 0, 0);
            verifyBalanceResponsiblePartyCalls("brp5.usef.energy", 0, 0);
            verifyBalanceResponsiblePartyCalls("brp6.usef.energy", 0, 0);
            verifyBalanceResponsiblePartyCalls("brp7.usef.energy", 0, 0);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyBalanceResponsiblePartyCalls(String domain, int persists, int deletes) {
        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(persists)).persist(new BalanceResponsibleParty(domain));
        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(deletes)).deleteByDomain(domain);
    }

    private void mockBalanceResponsibleParty(Long id, String domain, boolean exists)  throws BusinessValidationException{
        BalanceResponsibleParty participant = createBalanceResponsibleParty(id, domain);
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

    @Test
    public void testDistributionSystemOperatorBatch() throws Exception  {
        mockDistributionSystemOperator(0L, "dso0.usef.energy", true);
        mockDistributionSystemOperator(null, "dso1.usef.energy", false);
        mockDistributionSystemOperator(null, "dso2.usef.energy", false);
        mockDistributionSystemOperator(-3L, "dso3.usef.energy", true);

        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"domain\": \"dso0.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"dso1.usef.energy\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"dso2.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"dso3.usef.energy\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"dso4.usef.energy\", \"subdomain\": \"www.usef-example.com\"},"
                    + "{\"methode\" : \"POST\", \"domain\": \"dso5.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"dso6usefenergy\"},"
                    + "{\"method\" : \"GET\", \"gebied\": \"dso7.usef.energy\"}"
                    + "]";

            List<RestResult> result = service.processDistributionSystemOperatorBatch(jsonText);

            String resultString = JsonUtil.createJsonText(result);

            verifyDistributionSystemOperatorCalls("dso0.usef.energy", 0, 1);
            verifyDistributionSystemOperatorCalls("dso1.usef.energy", 1, 0);
            verifyDistributionSystemOperatorCalls("dso2.usef.energy", 0, 0);
            verifyDistributionSystemOperatorCalls("dso3.usef.energy", 0, 0);
            verifyDistributionSystemOperatorCalls("dso4.usef.energy", 0, 0);
            verifyDistributionSystemOperatorCalls("dso5.usef.energy", 0, 0);
            verifyDistributionSystemOperatorCalls("dso6.usef.energy", 0, 0);
            verifyDistributionSystemOperatorCalls("dso7.usef.energy", 0, 0);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyDistributionSystemOperatorCalls(String domain, int persists, int deletes) {
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(persists)).persist(new DistributionSystemOperator(domain));
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(deletes)).deleteByDomain(domain);
    }

    private void mockDistributionSystemOperator(Long id, String domain, boolean exists)  throws BusinessValidationException{
        DistributionSystemOperator participant = createDistributionSystemOperator(id, domain);
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

    @Test
    public void testMeterDataCompanyBatch() throws Exception  {
        mockMeterDataCompany(0L, "mdc0.usef.energy", true);
        mockMeterDataCompany(null, "mdc1.usef.energy", false);
        mockMeterDataCompany(null, "mdc2.usef.energy", false);
        mockMeterDataCompany(-3L, "mdc3.usef.energy", true);

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

            List<RestResult> result = service.processMeterDataCompanyBatch(jsonText);

            String resultString = JsonUtil.createJsonText(result);

            verifyMeterDataCompanyCalls("mdc0.usef.energy", 0, 1);
            verifyMeterDataCompanyCalls("mdc1.usef.energy", 1, 0);
            verifyMeterDataCompanyCalls("mdc2.usef.energy", 0, 0);
            verifyMeterDataCompanyCalls("mdc3.usef.energy", 0, 0);
            verifyMeterDataCompanyCalls("mdc4.usef.energy", 0, 0);
            verifyMeterDataCompanyCalls("mdc5.usef.energy", 0, 0);
            verifyMeterDataCompanyCalls("mdc6.usef.energy", 0, 0);
            verifyMeterDataCompanyCalls("mdc7.usef.energy", 0, 0);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyMeterDataCompanyCalls(String domain, int persists, int deletes) {
        Mockito.verify(meterDataCompanyRepository, Mockito.times(persists)).persist(new MeterDataCompany(domain));
        Mockito.verify(meterDataCompanyRepository, Mockito.times(deletes)).deleteByDomain(domain);
    }

    private void mockMeterDataCompany(Long id, String domain, boolean exists)  throws BusinessValidationException{
        MeterDataCompany participant = createMeterDataCompany(id, domain);
        if (exists) {
            Mockito.when(meterDataCompanyRepository.find(Matchers.eq(participant))).thenReturn(participant);
            Mockito.doNothing().when(meterDataCompanyRepository).deleteByDomain(domain);
            Mockito.doThrow(new BusinessValidationException(RestError.DUPLICATE, "MeterDataCompany", domain))
                    .when(validationService)
                    .checkDuplicateMeterDataCompanyDomain(Matchers.matches(domain));
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "MeterDataCompany", domain))
                    .when(validationService)
                    .checkExistingMeterDataCompanyDomain(Matchers.matches(domain));
        }
    }

    @Test
    public void testFindAllAggregators() throws BusinessValidationException, IOException {
        List<Aggregator> participants = new ArrayList<>();
        participants.add(createAggregator(1L, "agr1.usef.example.com"));
        participants.add(createAggregator(2L, "agr2.usef.example.com"));
        Mockito.when(aggregatorRepository.findAll()).thenReturn(participants);
        RestResult result = service.findAllAggregators();
        assertRestResult(200,
                "[{\"id\":1,\"domain\":\"agr1.usef.example.com\"},{\"id\":2,\"domain\":\"agr2.usef.example.com\"}]", 0, result);
    }

    @Test
    public void testFindAllBalanceResponsibleParties() throws BusinessValidationException, IOException {
        List<BalanceResponsibleParty> participants = new ArrayList<>();
        participants.add(createBalanceResponsibleParty(1L, "brp1.usef.example.com"));
        participants.add(createBalanceResponsibleParty(2L, "brp2.usef.example.com"));
        Mockito.when(balanceResponsiblePartyRepository.findAll()).thenReturn(participants);
        RestResult result = service.findAllBalanceResponsibleParties();
        assertRestResult(200,
                "[{\"id\":1,\"domain\":\"brp1.usef.example.com\"},{\"id\":2,\"domain\":\"brp2.usef.example.com\"}]", 0, result);
    }

    @Test
    public void testFindAllDistributionSystemOperators() throws BusinessValidationException, IOException {
        List<DistributionSystemOperator> participants = new ArrayList<>();
        participants.add(createDistributionSystemOperator(1L, "dso1.usef.example.com"));
        participants.add(createDistributionSystemOperator(2L, "dso2.usef.example.com"));
        Mockito.when(distributionSystemOperatorRepository.findAll()).thenReturn(participants);
        RestResult result = service.findAllDistributionSystemOperators();
        assertRestResult(200,
                "[{\"id\":1,\"domain\":\"dso1.usef.example.com\"},{\"id\":2,\"domain\":\"dso2.usef.example.com\"}]", 0, result);
    }

    @Test
    public void testFindAllMeterDataCompanies() throws BusinessValidationException, IOException {
        List<MeterDataCompany> participants = new ArrayList<>();
        participants.add(createMeterDataCompany(1L, "mdc1.usef.example.com"));
        participants.add(createMeterDataCompany(2L, "mdc2.usef.example.com"));
        Mockito.when(meterDataCompanyRepository.findAll()).thenReturn(participants);
        RestResult result = service.findAllMeterDataCompanies();
        assertRestResult(200,
                "[{\"id\":1,\"domain\":\"mdc1.usef.example.com\"},{\"id\":2,\"domain\":\"mdc2.usef.example.com\"}]", 0, result);
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

    private Aggregator createAggregator(Long id, String domain) {
        Aggregator participant = new Aggregator();
        participant.setId(id);
        participant.setDomain(domain);
        return participant;
    }

    private Aggregator mockAggregator(Long id, String domain, boolean exists)  throws BusinessValidationException{
        if (exists) {
            Aggregator participant = createAggregator(id, domain);
            Mockito.when(aggregatorRepository.findByDomain(Matchers.eq(domain))).thenReturn(participant);
            return participant;
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "Aggregator", domain))
                    .when(validationService)
                    .checkExistingAggregatorDomain(Matchers.matches(domain));
            return null;
        }
    }

    private BalanceResponsibleParty createBalanceResponsibleParty(Long id, String domain) {
        BalanceResponsibleParty participant = new BalanceResponsibleParty();
        participant.setId(id);
        participant.setDomain(domain);
        return participant;
    }

    private DistributionSystemOperator createDistributionSystemOperator(Long id, String domain) {
        DistributionSystemOperator participant = new DistributionSystemOperator();
        participant.setId(id);
        participant.setDomain(domain);
        return participant;
    }

    private MeterDataCompany createMeterDataCompany(Long id, String domain) {
        MeterDataCompany participant = new MeterDataCompany();
        participant.setId(id);
        participant.setDomain(domain);
        return participant;
    }
}
