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
import energy.usef.dso.model.CommonReferenceOperator;
import energy.usef.dso.model.SynchronisationCongestionPoint;
import energy.usef.dso.model.SynchronisationConnection;
import energy.usef.dso.repository.CommonReferenceOperatorRepository;
import energy.usef.dso.repository.SynchronisationCongestionPointRepository;
import energy.usef.dso.repository.SynchronisationConnectionRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import static org.junit.Assert.*;

import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

@RunWith(PowerMockRunner.class)
public class DistributionSystemOperatorValidationBusinessServiceTest {

    DistributionSystemOperatorValidationBusinessService service;

    @Mock
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Mock
    SynchronisationCongestionPointRepository synchronisationCongestionPointRepository;

    @Mock
    SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Before
    public void init() {
        service = new DistributionSystemOperatorValidationBusinessService();
        Whitebox.setInternalState(service, "commonReferenceOperatorRepository", commonReferenceOperatorRepository);
        Whitebox.setInternalState(service, "synchronisationCongestionPointRepository", synchronisationCongestionPointRepository);
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
        String entityAddress = "ea1.2007-11.net.usef.energy:1-0";
        SynchronisationCongestionPoint synchronisationCongestionPoint = createSynchronisationCongestionPoint(1L, entityAddress);
        Mockito.when(synchronisationCongestionPointRepository.findByEntityAddress(entityAddress)).thenReturn(synchronisationCongestionPoint);
        service.checkDuplicateSynchronisationCongestionPoint(entityAddress);

    }
    @Test
    public void checkDuplicateSynchronisationCongestionPointSuccess() throws Exception {
        String entityAddress = "ea1.2007-11.net.usef.energy:1-0";
        Mockito.when(synchronisationCongestionPointRepository.findByEntityAddress(entityAddress)).thenReturn(null);
        service.checkDuplicateSynchronisationCongestionPoint(entityAddress);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkExistingSynchronisationCongestionPointFailure() throws Exception {
        String entityAddress = "ea1.2007-11.net.usef.energy:1-0";
        Mockito.when(synchronisationCongestionPointRepository.findByEntityAddress(entityAddress)).thenReturn(null);
        service.checkExistingSynchronisationCongestionPoint(entityAddress);
    }

    @Test
    public void checkExistingSynchronisationCongestionPointSuccess() throws Exception {
        String entityAddress = "ea1.2007-11.net.usef.energy:1-0";
        SynchronisationCongestionPoint synchronisationCongestionPoint = createSynchronisationCongestionPoint(1L, entityAddress);
        Mockito.when(synchronisationCongestionPointRepository.findByEntityAddress(entityAddress)).thenReturn(synchronisationCongestionPoint);
        service.checkExistingSynchronisationCongestionPoint(entityAddress);
        Mockito.verify(synchronisationCongestionPointRepository, Mockito.times(1)).findByEntityAddress(entityAddress);
    }

    @Test (expected = BusinessValidationException.class)
    public void checkDuplicateSynchronisationConnectionsFailure() throws Exception {
        List<SynchronisationConnection> connections = new ArrayList<>();
        connections.add(createSynchronisationConnection(1L, "ean.0000000000000"));
        connections.add(createSynchronisationConnection(2L, "ean.0000000000004"));

        List<String> addresses = new ArrayList<>();
        addresses.add("ean.0000000000001");
        addresses.add("ean.0000000000002");
        addresses.add("ean.0000000000003");
        addresses.add("ean.0000000000004");

        Mockito.when(synchronisationConnectionRepository.findByEntityAddresses(addresses)).thenReturn(connections);
        service.checkDuplicateSynchronisationConnections(addresses);
    }

    @Test
    public void checkDuplicateSynchronisationConnectionsSuccess() throws Exception {
        List<SynchronisationConnection> connections = new ArrayList<>();

        List<String> addresses = new ArrayList<>();
        addresses.add("ean.0000000000001");
        addresses.add("ean.0000000000002");
        addresses.add("ean.0000000000003");
        addresses.add("ean.0000000000004");

        Mockito.when(synchronisationConnectionRepository.findByEntityAddresses(addresses)).thenReturn(connections);
        service.checkDuplicateSynchronisationConnections(addresses);
        Mockito.verify(synchronisationConnectionRepository, Mockito.times(1)).findByEntityAddresses(addresses);
    }

    private SynchronisationConnection createSynchronisationConnection(Long id, String entityAddress) {
        SynchronisationConnection c = new SynchronisationConnection();
        c.setId(id);
        c.setEntityAddress(entityAddress);
        return c;
    }

    private SynchronisationCongestionPoint createSynchronisationCongestionPoint(Long id, String entityAddress) {
        SynchronisationCongestionPoint c = new SynchronisationCongestionPoint();
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
