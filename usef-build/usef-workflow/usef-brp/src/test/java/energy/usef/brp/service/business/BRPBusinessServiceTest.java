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
import energy.usef.brp.model.SynchronisationConnectionStatusType;
import energy.usef.brp.repository.CommonReferenceOperatorRepository;
import energy.usef.brp.repository.MeterDataCompanyRepository;
import energy.usef.brp.repository.SynchronisationConnectionRepository;
import energy.usef.brp.repository.SynchronisationConnectionStatusRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class BRPBusinessServiceTest {

    private BrpBusinessService service;

    @Mock
    private SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Mock
    private SynchronisationConnectionStatusRepository synchronisationConnectionStatusRepository;

    @Mock
    private CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Mock
    private MeterDataCompanyRepository meterDataCompanyRepository;

    @Before
    public void init() {
        service = new BrpBusinessService();
        Whitebox.setInternalState(service, synchronisationConnectionRepository);
        Whitebox.setInternalState(service, synchronisationConnectionStatusRepository);
        Whitebox.setInternalState(service, commonReferenceOperatorRepository);
        Whitebox.setInternalState(service, meterDataCompanyRepository);
    }

    @Test
    public void testFindConnectionsPerCRO() {
        Mockito.when(synchronisationConnectionRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(new ArrayList<>());

        Assert.assertTrue(service.findConnectionsPerCRO().isEmpty());

        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(buildCROList());

        Assert.assertTrue(service.findConnectionsPerCRO().isEmpty());

        Mockito.when(synchronisationConnectionRepository.findAll()).thenReturn(buildConnectionList());

        Map<String, List<SynchronisationConnection>> result = service.findConnectionsPerCRO();

        Assert.assertTrue(!result.isEmpty());

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(2, result.get(result.keySet().iterator().next()).size());

        Mockito.verify(synchronisationConnectionRepository, Mockito.times(3)).findAll();
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(3)).findAll();
    }

    @Test
    public void testUpdateConnectionStatusForCRO() {
        service.updateConnectionStatusForCRO("cro.dummy.nl");
        Mockito.verify(synchronisationConnectionRepository, Mockito.times(1)).updateConnectionStatusForCRO("cro.dummy.nl");
    }

    @Test
    public void testFindAllMDCs() {
        service.findAllMDCs();
        Mockito.verify(meterDataCompanyRepository, Mockito.times(1)).findAll();
    }

    @Test
    public void testCleanSynchronization() {
        Mockito.when(
                synchronisationConnectionStatusRepository
                        .countSynchronisationConnectionStatusWithStatus(SynchronisationConnectionStatusType.MODIFIED))
                .thenReturn(1L);
        service.cleanSynchronization();
        // no calls done to repositories

        Mockito.when(
                synchronisationConnectionStatusRepository
                        .countSynchronisationConnectionStatusWithStatus(SynchronisationConnectionStatusType.MODIFIED))
                .thenReturn(0L);
        service.cleanSynchronization();
        // only 1 time, first call should have no interactions
        Mockito.verify(synchronisationConnectionStatusRepository, Mockito.times(1)).deleteAll();
        Mockito.verify(synchronisationConnectionRepository, Mockito.times(1)).deleteAll();
    }

    /**
     * @return
     */
    private List<SynchronisationConnection> buildConnectionList() {
        List<SynchronisationConnection> connections = new ArrayList<>();
        SynchronisationConnection connection = new SynchronisationConnection();
        connections.add(connection);
        SynchronisationConnection connection2 = new SynchronisationConnection();
        connections.add(connection2);
        return connections;
    }

    /**
     * @return
     */
    private List<CommonReferenceOperator> buildCROList() {
        List<CommonReferenceOperator> cros = new ArrayList<>();
        CommonReferenceOperator cro = new CommonReferenceOperator();
        cro.setDomain("random." + new Random().nextInt());
        cros.add(cro);
        return cros;
    }
}
