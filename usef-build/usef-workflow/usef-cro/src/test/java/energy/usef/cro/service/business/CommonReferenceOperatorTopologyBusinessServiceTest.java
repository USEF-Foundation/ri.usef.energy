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
    public void testProcessAggregatorBatch() throws Exception

    {
        Aggregator agr0 = mockAggregator(null, "agr0.usef.energy", false);
        Aggregator agr1 = mockAggregator(1L, "agr1.usef.energy", false);
        Aggregator agr2 = mockAggregator(null, "agr2.usef.energy", true);
        Aggregator agr3 = mockAggregator(null, "agr3.usef.energy", false);
        Aggregator agr4 = mockAggregator(4L, "agr4.usef.energy", true);
        Aggregator agr5 = mockAggregator(null, "agr5.usef.energy", true);
        Aggregator agr6 = mockAggregator(null, "agr6usefenergy", false);
        Aggregator agr7 = mockAggregator(7L, "agr7.usef.energy", true);

        Assert.assertNull(agr0);
        Assert.assertNull(agr1);
        assertNotNull(agr2);
        Assert.assertNull(agr3);
        assertNotNull(agr4);
        assertNotNull(agr5);
        Assert.assertNull(agr6);
        assertNotNull(agr7);


        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"domain\": \"agr0.usef.energy\"},"
                    + "{\"method\" : \"MAIL\", \"domain\": \"agr1.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"agr2.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"gebied\": \"agr3.usef.energy\", \"subdomain\": \"www.usef-example.com\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"agr4.usef.energy\"},"
                    + "{\"methode\" : \"POST\", \"domain\": \"agr5.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"agr6usefenergy\"},"
                    + "{\"method\" : \"GET\", \"domain\": \"agr7.usef.energy\"}"
                    + "]";

            List<RestResult> result = service.processAggregatorBatch(jsonText);

            String resultString = JsonUtil.createJsonText(result);

            Mockito.verify(aggregatorRepository, Mockito.times(0)).persist(agr0);
            Mockito.verify(aggregatorRepository, Mockito.times(0)).delete(agr0);

            Mockito.verify(aggregatorRepository, Mockito.times(0)).persist(agr1);
            Mockito.verify(aggregatorRepository, Mockito.times(0)).delete(agr1);

            Mockito.verify(aggregatorRepository, Mockito.times(1)).persist(agr2);
            Mockito.verify(aggregatorRepository, Mockito.times(0)).delete(agr2);

            Mockito.verify(aggregatorRepository, Mockito.times(0)).persist(agr3);
            Mockito.verify(aggregatorRepository, Mockito.times(0)).delete(agr3);

            Mockito.verify(aggregatorRepository, Mockito.times(0)).persist(agr4);
            Mockito.verify(aggregatorRepository, Mockito.times(0)).delete(agr4);

            Mockito.verify(aggregatorRepository, Mockito.times(0)).persist(agr5);
            Mockito.verify(aggregatorRepository, Mockito.times(0)).delete(agr5);

            Mockito.verify(aggregatorRepository, Mockito.times(0)).persist(agr6);
            Mockito.verify(aggregatorRepository, Mockito.times(0)).delete(agr6);

            Mockito.verify(aggregatorRepository, Mockito.times(0)).persist(agr7);
            Mockito.verify(aggregatorRepository, Mockito.times(0)).delete(agr7);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBalanceResponsiblePartyBatch() throws Exception  {
        BalanceResponsibleParty brp0 = mockBalanceResponsibleParty(null, "brp0.usef.energy", false);
        BalanceResponsibleParty brp1 = mockBalanceResponsibleParty(1L, "brp1.usef.energy", false);
        BalanceResponsibleParty brp2 = mockBalanceResponsibleParty(null, "brp2.usef.energy", true);
        BalanceResponsibleParty brp3 = mockBalanceResponsibleParty(null, "brp3.usef.energy", false);
        BalanceResponsibleParty brp4 = mockBalanceResponsibleParty(4L, "brp4.usef.energy", true);
        BalanceResponsibleParty brp5 = mockBalanceResponsibleParty(null, "brp5.usef.energy", true);
        BalanceResponsibleParty brp6 = mockBalanceResponsibleParty(null, "brp6usefenergy", false);
        BalanceResponsibleParty brp7 = mockBalanceResponsibleParty(7L, "brp7.usef.energy", true);

        Assert.assertNull(brp0);
        Assert.assertNull(brp1);
        assertNotNull(brp2);
        Assert.assertNull(brp3);
        assertNotNull(brp4);
        assertNotNull(brp5);
        Assert.assertNull(brp6);
        assertNotNull(brp7);


        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"domain\": \"brp0.usef.energy\"},"
                    + "{\"method\" : \"MAIL\", \"domain\": \"brp1.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"brp2.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"gebied\": \"brp3.usef.energy\", \"subdomain\": \"www.usef-example.com\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"brp4.usef.energy\"},"
                    + "{\"methode\" : \"POST\", \"domain\": \"brp5.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"brp6usefenergy\"},"
                    + "{\"method\" : \"GET\", \"domain\": \"brp7.usef.energy\"}"
                    + "]";

            List<RestResult> result = service.processBalanceResponsiblePartyBatch(jsonText);

            String resultString = JsonUtil.createJsonText(result);

            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).persist(brp0);
            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).delete(brp0);

            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).persist(brp1);
            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).delete(brp1);

            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(1)).persist(brp2);
            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).delete(brp2);

            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).persist(brp3);
            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).delete(brp3);

            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).persist(brp4);
            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).delete(brp4);

            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).persist(brp5);
            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).delete(brp5);

            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).persist(brp6);
            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).delete(brp6);

            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).persist(brp7);
            Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(0)).delete(brp7);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDistributionSystemOperatorBatch() throws Exception {
        DistributionSystemOperator dso0 = mockDistributionSystemOperator(null, "dso0.usef.energy", false);
        DistributionSystemOperator dso1 = mockDistributionSystemOperator(1L, "dso1.usef.energy", false);
        DistributionSystemOperator dso2 = mockDistributionSystemOperator(null, "dso2.usef.energy", true);
        DistributionSystemOperator dso3 = mockDistributionSystemOperator(null, "dso3.usef.energy", false);
        DistributionSystemOperator dso4 = mockDistributionSystemOperator(4L, "dso4.usef.energy", true);
        DistributionSystemOperator dso5 = mockDistributionSystemOperator(null, "dso.usef.energy", true);
        DistributionSystemOperator dso6 = mockDistributionSystemOperator(null, "dso6usefenergy", false);
        DistributionSystemOperator dso7 = mockDistributionSystemOperator(7L, "dso7.usef.energy", true);

        Assert.assertNull(dso0);
        Assert.assertNull(dso1);
        assertNotNull(dso2);
        Assert.assertNull(dso3);
        assertNotNull(dso4);
        assertNotNull(dso5);
        Assert.assertNull(dso6);
        assertNotNull(dso7);


        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"domain\": \"dso0.usef.energy\"},"
                    + "{\"method\" : \"MAIL\", \"domain\": \"dso1.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"dso2.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"gebied\": \"dso3.usef.energy\", \"subdomain\": \"www.usef-example.com\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"dso4.usef.energy\"},"
                    + "{\"methode\" : \"POST\", \"domain\": \"dso5.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"dso6usefenergy\"},"
                    + "{\"method\" : \"GET\", \"domain\": \"dso7.usef.energy\"}"
                    + "]";

            List<RestResult> result = service.processDistributionSystemOperatorBatch(jsonText);

            String resultString = JsonUtil.createJsonText(result);

            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).persist(dso0);
            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).delete(dso0);

            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).persist(dso1);
            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).delete(dso1);

            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(1)).persist(dso2);
            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).delete(dso2);

            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).persist(dso3);
            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).delete(dso3);

            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).persist(dso4);
            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).delete(dso4);

            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).persist(dso5);
            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).delete(dso5);

            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).persist(dso6);
            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).delete(dso6);

            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).persist(dso7);
            Mockito.verify(distributionSystemOperatorRepository, Mockito.times(0)).delete(dso7);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMeterDataCompanyBatch() throws Exception {
        MeterDataCompany mdc0 = mockMeterDataCompany(null, "mdc0.usef.energy", false);
        MeterDataCompany mdc1 = mockMeterDataCompany(1L, "mdc1.usef.energy", false);
        MeterDataCompany mdc2 = mockMeterDataCompany(null, "mdc2.usef.energy", true);
        MeterDataCompany mdc3 = mockMeterDataCompany(null, "mdc3.usef.energy", false);
        MeterDataCompany mdc4 = mockMeterDataCompany(4L, "mdc4.usef.energy", true);
        MeterDataCompany mdc5 = mockMeterDataCompany(null, "mdc.usef.energy", true);
        MeterDataCompany mdc6 = mockMeterDataCompany(null, "mdc6usefenergy", false);
        MeterDataCompany mdc7 = mockMeterDataCompany(7L, "mdc7.usef.energy", true);

        Assert.assertNull(mdc0);
        Assert.assertNull(mdc1);
        assertNotNull(mdc2);
        Assert.assertNull(mdc3);
        assertNotNull(mdc4);
        assertNotNull(mdc5);
        Assert.assertNull(mdc6);
        assertNotNull(mdc7);


        try {
            String jsonText = "["
                    + "{\"method\" : \"DELETE\", \"domain\": \"mdc0.usef.energy\"},"
                    + "{\"method\" : \"MAIL\", \"domain\": \"mdc1.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"mdc2.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"gebied\": \"mdc3.usef.energy\", \"subdomain\": \"www.usef-example.com\"},"
                    + "{\"method\" : \"DELETE\", \"domain\": \"mdc4.usef.energy\"},"
                    + "{\"methode\" : \"POST\", \"domain\": \"mdc5.usef.energy\"},"
                    + "{\"method\" : \"POST\", \"domain\": \"mdc6usefenergy\"},"
                    + "{\"method\" : \"GET\", \"domain\": \"mdc7.usef.energy\"}"
                    + "]";

            List<RestResult> result = service.processMeterDataCompanyBatch(jsonText);

            String resultString = JsonUtil.createJsonText(result);

            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).persist(mdc0);
            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).delete(mdc0);

            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).persist(mdc1);
            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).delete(mdc1);

            Mockito.verify(meterDataCompanyRepository, Mockito.times(1)).persist(mdc2);
            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).delete(mdc2);

            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).persist(mdc3);
            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).delete(mdc3);

            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).persist(mdc4);
            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).delete(mdc4);

            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).persist(mdc5);
            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).delete(mdc5);

            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).persist(mdc6);
            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).delete(mdc6);

            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).persist(mdc7);
            Mockito.verify(meterDataCompanyRepository, Mockito.times(0)).delete(mdc7);

            assertNotNull(resultString);

        } catch (IOException e) {
            e.printStackTrace();
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

    private BalanceResponsibleParty mockBalanceResponsibleParty(Long id, String domain, boolean exists)  throws BusinessValidationException{
        if (exists) {
            BalanceResponsibleParty particicpant = createBalanceResponsibleParty(id, domain);
            Mockito.when(balanceResponsiblePartyRepository.findByDomain(Matchers.eq(domain))).thenReturn(particicpant);
            return particicpant;
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "BalanceResponsibleParty", domain))
                    .when(validationService)
                    .checkExistingBalanceResponsiblePartyDomain(Matchers.matches(domain));
            return null;
        }
    }

    private DistributionSystemOperator createDistributionSystemOperator(Long id, String domain) {
        DistributionSystemOperator participant = new DistributionSystemOperator();
        participant.setId(id);
        participant.setDomain(domain);
        return participant;
    }

    private DistributionSystemOperator mockDistributionSystemOperator(Long id, String domain, boolean exists)  throws BusinessValidationException{
        if (exists) {
            DistributionSystemOperator particicpant = createDistributionSystemOperator(id, domain);
            Mockito.when(distributionSystemOperatorRepository.findByDomain(Matchers.eq(domain))).thenReturn(particicpant);
            return particicpant;
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "DistributionSystemOperator", domain))
                    .when(validationService)
                    .checkExistingDistributionSystemOperatorDomain(Matchers.matches(domain));
            return null;
        }
    }

    private MeterDataCompany createMeterDataCompany(Long id, String domain) {
        MeterDataCompany participant = new MeterDataCompany();
        participant.setId(id);
        participant.setDomain(domain);
        return participant;
    }

    private MeterDataCompany mockMeterDataCompany(Long id, String domain, boolean exists)  throws BusinessValidationException{
        if (exists) {
            MeterDataCompany particicpant = createMeterDataCompany(id, domain);
            Mockito.when(meterDataCompanyRepository.findByDomain(Matchers.eq(domain))).thenReturn(particicpant);
            return particicpant;
        } else {
            Mockito.doThrow(new BusinessValidationException(RestError.NOT_FOUND, "MeterDataCompany", domain))
                    .when(validationService)
                    .checkExistingMeterDataCompanyDomain(Matchers.matches(domain));
            return null;
        }
    }
}
