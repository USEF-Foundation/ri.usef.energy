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
import energy.usef.mdc.model.BalanceResponsibleParty;
import energy.usef.mdc.model.Connection;
import energy.usef.mdc.model.DistributionSystemOperator;
import energy.usef.mdc.repository.BalanceResponsiblePartyRepository;
import energy.usef.mdc.repository.DistributionSystemOperatorRepository;
import energy.usef.mdc.repository.MdcConnectionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class MeterDataCompanyValidationBusinessServiceTest {

    MeterDataCompanyValidationBusinessService service;

    @Mock
    BalanceResponsiblePartyRepository balanceResponsiblePartyRepository;

    @Mock
    DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Mock
    MdcConnectionRepository connectionRepository;

    @Before
    public void init() {
        service = new MeterDataCompanyValidationBusinessService();
        Whitebox.setInternalState(service, "balanceResponsiblePartyRepository", balanceResponsiblePartyRepository);
        Whitebox.setInternalState(service, "distributionSystemOperatorRepository", distributionSystemOperatorRepository);
        Whitebox.setInternalState(service, "connectionRepository", connectionRepository);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkDuplicateBalanceResponsiblePartyDomainFailure() throws Exception {
        String key = "usef.example.com";
        BalanceResponsibleParty instance = createBalanceResponsibleParty(key);
        Mockito.when(balanceResponsiblePartyRepository.find(key)).thenReturn(instance);
        service.checkDuplicateBalanceResponsiblePartyDomain(key);
        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(1)).find(key);
    }

    @Test
    public void checkDuplicateBalanceResponsiblePartyDomainSuccess() throws Exception {
        String key = "usef.example.com";
        Mockito.when(balanceResponsiblePartyRepository.find(key)).thenReturn(null);
        service.checkDuplicateBalanceResponsiblePartyDomain(key);
        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(1)).find(key);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkExistingBalanceResponsiblePartyDomainFailure() throws Exception {
        String key = "usef.example.com";
        Mockito.when(balanceResponsiblePartyRepository.find(key)).thenReturn(null);
        service.checkExistingBalanceResponsiblePartyDomain(key);
    }

    @Test
    public void checkExistingBalanceResponsiblePartyDomainSuccess() throws Exception {
        String key = "usef.example.com";
        BalanceResponsibleParty instance = createBalanceResponsibleParty(key);
        Mockito.when(balanceResponsiblePartyRepository.find(key)).thenReturn(instance);
        service.checkExistingBalanceResponsiblePartyDomain(key);
        Mockito.verify(balanceResponsiblePartyRepository, Mockito.times(1)).find(key);
    }


    @Test (expected = BusinessValidationException.class)
    public void checkDuplicateDistributionSystemOperatorDomainFailure() throws Exception {
        String key = "usef.example.com";
        DistributionSystemOperator instance = createDistributionSystemOperator(key);
        Mockito.when(distributionSystemOperatorRepository.find(key)).thenReturn(instance);
        service.checkDuplicateDistributionSystemOperatorDomain(key);
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(1)).find(key);
    }

    @Test
    public void checkDuplicateDistributionSystemOperatorDomainSuccess() throws Exception {
        String key = "usef.example.com";
        Mockito.when(distributionSystemOperatorRepository.find(key)).thenReturn(null);
        service.checkDuplicateDistributionSystemOperatorDomain(key);
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(1)).find(key);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkExistingDistributionSystemOperatorDomainFailure() throws Exception {
        String key = "usef.example.com";
        Mockito.when(distributionSystemOperatorRepository.find(key)).thenReturn(null);
        service.checkExistingDistributionSystemOperatorDomain(key);
    }

    @Test
    public void checkExistingDistributionSystemOperatorDomainSuccess() throws Exception {
        String key = "usef.example.com";
        DistributionSystemOperator instance = createDistributionSystemOperator(key);
        Mockito.when(distributionSystemOperatorRepository.find(key)).thenReturn(instance);
        service.checkExistingDistributionSystemOperatorDomain(key);
        Mockito.verify(distributionSystemOperatorRepository, Mockito.times(1)).find(key);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkDuplicatempanMeterDataCoyDomainFailure() throws Exception {
        String key = "ean.0000000000001";
        Connection instance = createConnection(key);
        Mockito.when(connectionRepository.find(key)).thenReturn(instance);
        service.checkDuplicateConnection(key);
        Mockito.verify(connectionRepository, Mockito.times(1)).find(key);
    }

    @Test
    public void checkDuplicateMeterDataCompanyDomainSuccess() throws Exception {
        String key = "usef.example.com";
        Mockito.when(connectionRepository.find(key)).thenReturn(null);
        service.checkDuplicateConnection(key);
        Mockito.verify(connectionRepository, Mockito.times(1)).find(key);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkExistingMeterDataCompanyDomainFailure() throws Exception {
        String key = "usef.example.com";
        Mockito.when(connectionRepository.find(key)).thenReturn(null);
        service.checkExistingConnection(key);
    }

    @Test
    public void checkExistingMeterDataCompanyDomainSuccess() throws Exception {
        String key = "usef.example.com";
        Connection instance = createConnection(key);
        Mockito.when(connectionRepository.find(key)).thenReturn(instance);
        service.checkExistingConnection(key);
        Mockito.verify(connectionRepository, Mockito.times(1)).find(key);
    }


    private BalanceResponsibleParty createBalanceResponsibleParty(String key) {
        BalanceResponsibleParty instance = new BalanceResponsibleParty();
        instance.setDomain(key);
        return instance;
    }
    private DistributionSystemOperator createDistributionSystemOperator(String key) {
        DistributionSystemOperator instance = new DistributionSystemOperator();
        instance.setDomain(key);
        return instance;
    }

    private Connection createConnection(String key) {
        Connection instance = new Connection();
        instance.setEntityAddress(key);
        return instance;
    }

}
