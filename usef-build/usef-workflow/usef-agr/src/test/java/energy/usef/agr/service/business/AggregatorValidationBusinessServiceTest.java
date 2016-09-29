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
package energy.usef.agr.service.business;

import energy.usef.agr.model.CommonReferenceOperator;
import energy.usef.agr.model.SynchronisationConnection;
import energy.usef.agr.repository.CommonReferenceOperatorRepository;
import energy.usef.agr.repository.SynchronisationConnectionRepository;
import energy.usef.core.exception.BusinessValidationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class AggregatorValidationBusinessServiceTest {

    AggregatorValidationBusinessService service;

    @Mock
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Mock
    SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Before
    public void init() {
        service = new AggregatorValidationBusinessService();
        Whitebox.setInternalState(service, "commonReferenceOperatorRepository", commonReferenceOperatorRepository);
        Whitebox.setInternalState(service, "synchronisationConnectionRepository", synchronisationConnectionRepository);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkDuplicateCommonReferenceOperatorDomainFailure() throws Exception {
        String domain = "usef.example.com";
        CommonReferenceOperator commonReferenceOperator = createCommonReferenceOperator(1L, domain);
        Mockito.when(commonReferenceOperatorRepository.findByDomain(domain)).thenReturn(commonReferenceOperator);
        service.checkDuplicateCommonReferenceOperatorDomain(domain);
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test
    public void checkDuplicateCommonReferenceOperatorDomainSuccess() throws Exception {
        String domain = "usef.example.com";
        Mockito.when(commonReferenceOperatorRepository.findByDomain(domain)).thenReturn(null);
        service.checkDuplicateCommonReferenceOperatorDomain(domain);
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkExistingCommonReferenceOperatorDomainFailure() throws Exception {
        String domain = "usef.example.com";
        Mockito.when(commonReferenceOperatorRepository.findByDomain(domain)).thenReturn(null);
        service.checkExistingCommonReferenceOperatorDomain(domain);
    }

    @Test
    public void checkExistingCommonReferenceOperatorDomainSuccess() throws Exception {
        String domain = "usef.example.com";
        CommonReferenceOperator commonReferenceOperator = createCommonReferenceOperator(1L, domain);
        Mockito.when(commonReferenceOperatorRepository.findByDomain(domain)).thenReturn(commonReferenceOperator);
        service.checkExistingCommonReferenceOperatorDomain(domain);
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(1)).findByDomain(domain);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkDuplicateSynchronisationCongestionPointFailure() throws Exception {
        String entityAddress = "ean.0000000000001";
        SynchronisationConnection synchronisationCongestionPoint = createSynchronisationConnection(1L, entityAddress);
        Mockito.when(synchronisationConnectionRepository.findByEntityAddress(entityAddress)).thenReturn(synchronisationCongestionPoint);
        service.checkDuplicateSynchronisationConnection(entityAddress);

    }
    @Test
    public void checkDuplicateSynchronisationCongestionPointSuccess() throws Exception {
        String entityAddress = "ean.0000000000001";
        Mockito.when(synchronisationConnectionRepository.findByEntityAddress(entityAddress)).thenReturn(null);
        service.checkDuplicateSynchronisationConnection(entityAddress);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkExistingSynchronisationCongestionPointFailure() throws Exception {
        String entityAddress = "ean.0000000000001";
        Mockito.when(synchronisationConnectionRepository.findByEntityAddress(entityAddress)).thenReturn(null);
        service.checkExistingSynchronisationConnection(entityAddress);
    }

    @Test
    public void checkExistingSynchronisationCongestionPointSuccess() throws Exception {
        String entityAddress = "ean.0000000000001";
        SynchronisationConnection synchronisationCongestionPoint = createSynchronisationConnection(1L, entityAddress);
        Mockito.when(synchronisationConnectionRepository.findByEntityAddress(entityAddress)).thenReturn(synchronisationCongestionPoint);
        service.checkExistingSynchronisationConnection(entityAddress);
        Mockito.verify(synchronisationConnectionRepository, Mockito.times(1)).findByEntityAddress(entityAddress);
    }


    private SynchronisationConnection createSynchronisationConnection(Long id, String entityAddress) {
        SynchronisationConnection c = new SynchronisationConnection();
        c.setId(id);
        c.setEntityAddress(entityAddress);
        return c;
    }

    private CommonReferenceOperator createCommonReferenceOperator(Long id, String domain) {
        CommonReferenceOperator c = new CommonReferenceOperator();
        c.setId(id);
        c.setDomain(domain);
        return c;
    }
}
