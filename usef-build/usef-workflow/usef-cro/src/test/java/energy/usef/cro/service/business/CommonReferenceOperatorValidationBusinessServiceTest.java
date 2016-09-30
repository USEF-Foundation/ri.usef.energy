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
import energy.usef.cro.model.Aggregator;
import energy.usef.cro.model.BalanceResponsibleParty;
import energy.usef.cro.model.DistributionSystemOperator;
import energy.usef.cro.model.MeterDataCompany;
import energy.usef.cro.repository.AggregatorRepository;
import energy.usef.cro.repository.BalanceResponsiblePartyRepository;
import energy.usef.cro.repository.DistributionSystemOperatorRepository;
import energy.usef.cro.repository.MeterDataCompanyRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class CommonReferenceOperatorValidationBusinessServiceTest {

    CommonReferenceOperatorValidationBusinessService service;

    @Mock
    AggregatorRepository aggregatorRepository;

    @Mock
    BalanceResponsiblePartyRepository balanceResponsiblePartyRepository;

    @Mock
    DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Mock
    MeterDataCompanyRepository meterDataCompanyRepository;

    @Before
    public void init() {
        service = new CommonReferenceOperatorValidationBusinessService();
        Whitebox.setInternalState(service, "aggregatorRepository", aggregatorRepository);
        Whitebox.setInternalState(service, "balanceResponsiblePartyRepository", balanceResponsiblePartyRepository);
        Whitebox.setInternalState(service, "distributionSystemOperatorRepository", distributionSystemOperatorRepository);
        Whitebox.setInternalState(service, "meterDataCompanyRepository", meterDataCompanyRepository);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkDuplicateCommonReferenceOperatorDomainFailure() throws Exception {
        String domain = "usef.example.com";
        Aggregator participant = createAggregator(1L, domain);
        Mockito.when(aggregatorRepository.findByDomain(domain)).thenReturn(participant);
        service.checkDuplicateAggregatorDomain(domain);
        Mockito.verify(aggregatorRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test
    public void checkDuplicateAggregatorDomainSuccess() throws Exception {
        String domain = "usef.example.com";
        Mockito.when(aggregatorRepository.findByDomain(domain)).thenReturn(null);
        service.checkDuplicateAggregatorDomain(domain);
        Mockito.verify(aggregatorRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkExistingAggregatorDomainFailure() throws Exception {
        String domain = "usef.example.com";
        Mockito.when(aggregatorRepository.findByDomain(domain)).thenReturn(null);
        service.checkExistingAggregatorDomain(domain);
    }

    @Test
    public void checkExistingAggregatorDomainSuccess() throws Exception {
        String domain = "usef.example.com";
        Aggregator participant = createAggregator(1L, domain);
        Mockito.when(aggregatorRepository.findByDomain(domain)).thenReturn(participant);
        service.checkExistingAggregatorDomain(domain);
        Mockito.verify(aggregatorRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkDuplicateBalanceResponsiblePartyDomainFailure() throws Exception {
        String domain = "usef.example.com";
        BalanceResponsibleParty participant = createBalanceResponsibleParty(1L, domain);
        Mockito.when(balanceResponsiblePartyRepository.findByDomain(domain)).thenReturn(participant);
        service.checkDuplicateBalanceResponsiblePartyDomain(domain);
        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test
    public void checkDuplicateBalanceResponsiblePartyDomainSuccess() throws Exception {
        String domain = "usef.example.com";
        Mockito.when(balanceResponsiblePartyRepository.findByDomain(domain)).thenReturn(null);
        service.checkDuplicateBalanceResponsiblePartyDomain(domain);
        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkExistingBalanceResponsiblePartyDomainFailure() throws Exception {
        String domain = "usef.example.com";
        Mockito.when(balanceResponsiblePartyRepository.findByDomain(domain)).thenReturn(null);
        service.checkExistingBalanceResponsiblePartyDomain(domain);
    }

    @Test
    public void checkExistingBalanceResponsiblePartyDomainSuccess() throws Exception {
        String domain = "usef.example.com";
        BalanceResponsibleParty participant = createBalanceResponsibleParty(1L, domain);
        Mockito.when(balanceResponsiblePartyRepository.findByDomain(domain)).thenReturn(participant);
        service.checkExistingBalanceResponsiblePartyDomain(domain);
        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkDuplicateDistributionSystemOperatorDomainFailure() throws Exception {
        String domain = "usef.example.com";
        DistributionSystemOperator participant = createDistributionSystemOperator(1L, domain);
        Mockito.when(distributionSystemOperatorRepository.findByDomain(domain)).thenReturn(participant);
        service.checkDuplicateDistributionSystemOperatorDomain(domain);
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test
    public void checkDuplicateDistributionSystemOperatorDomainSuccess() throws Exception {
        String domain = "usef.example.com";
        Mockito.when(distributionSystemOperatorRepository.findByDomain(domain)).thenReturn(null);
        service.checkDuplicateDistributionSystemOperatorDomain(domain);
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkExistingDistributionSystemOperatorDomainFailure() throws Exception {
        String domain = "usef.example.com";
        Mockito.when(distributionSystemOperatorRepository.findByDomain(domain)).thenReturn(null);
        service.checkExistingDistributionSystemOperatorDomain(domain);
    }

    @Test
    public void checkExistingDistributionSystemOperatorDomainSuccess() throws Exception {
        String domain = "usef.example.com";
        DistributionSystemOperator participant = createDistributionSystemOperator(1L, domain);
        Mockito.when(distributionSystemOperatorRepository.findByDomain(domain)).thenReturn(participant);
        service.checkExistingDistributionSystemOperatorDomain(domain);
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkDuplicateMeterDataCompanyDomainFailure() throws Exception {
        String domain = "usef.example.com";
        MeterDataCompany participant = createMeterDataCompany(1L, domain);
        Mockito.when(meterDataCompanyRepository.findByDomain(domain)).thenReturn(participant);
        service.checkDuplicateMeterDataCompanyDomain(domain);
        Mockito.verify(meterDataCompanyRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test
    public void checkDuplicateMeterDataCompanyDomainSuccess() throws Exception {
        String domain = "usef.example.com";
        Mockito.when(meterDataCompanyRepository.findByDomain(domain)).thenReturn(null);
        service.checkDuplicateMeterDataCompanyDomain(domain);
        Mockito.verify(meterDataCompanyRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkExistingMeterDataCompanyDomainFailure() throws Exception {
        String domain = "usef.example.com";
        Mockito.when(meterDataCompanyRepository.findByDomain(domain)).thenReturn(null);
        service.checkExistingMeterDataCompanyDomain(domain);
    }

    @Test
    public void checkExistingMeterDataCompanyDomainSuccess() throws Exception {
        String domain = "usef.example.com";
        MeterDataCompany participant = createMeterDataCompany(1L, domain);
        Mockito.when(meterDataCompanyRepository.findByDomain(domain)).thenReturn(participant);
        service.checkExistingMeterDataCompanyDomain(domain);
        Mockito.verify(meterDataCompanyRepository, Mockito.times(1)).findByDomain(domain);
    }

    private Aggregator createAggregator(Long id, String domain) {
        Aggregator participant = new Aggregator();
        participant.setId(id);
        participant.setDomain(domain);
        return participant;
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
