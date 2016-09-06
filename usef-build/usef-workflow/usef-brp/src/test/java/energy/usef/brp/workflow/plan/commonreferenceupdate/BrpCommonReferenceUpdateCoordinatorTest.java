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

package energy.usef.brp.workflow.plan.commonreferenceupdate;

import energy.usef.brp.model.SynchronisationConnection;
import energy.usef.brp.service.business.BrpBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.service.helper.JMSHelperService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class BrpCommonReferenceUpdateCoordinatorTest {

    private BrpCommonReferenceUpdateCoordinator coordinator;

    @Mock
    private BrpBusinessService businessService;

    @Mock
    private Config config;

    @Mock
    private JMSHelperService jmsService;

    @Before
    public void init() {
        coordinator = new BrpCommonReferenceUpdateCoordinator();
        Whitebox.setInternalState(coordinator, businessService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, jmsService);
    }

    @Test
    public void testHandleEventNoData() {
        Map<String, List<SynchronisationConnection>> connectionsPerCRO = new HashMap<>();

        Mockito.when(businessService.findConnectionsPerCRO()).thenReturn(connectionsPerCRO);
        // no data no result
        coordinator.handleEvent(new CommonReferenceUpdateEvent());

        Mockito.verify(config, Mockito.times(0)).getProperty(Mockito.any(ConfigParam.class));
        Mockito.verify(jmsService, Mockito.times(0)).sendMessageToOutQueue(Mockito.anyString());
    }

    @Test
    public void testHandleEventOneMessage() {
        Map<String, List<SynchronisationConnection>> connectionsPerCRO = new HashMap<>();
        List<SynchronisationConnection> connections = new ArrayList<>();
        SynchronisationConnection connection = new SynchronisationConnection();
        connection.setEntityAddress("connectionAddress");
        connections.add(connection);
        connectionsPerCRO.put("cro.dummy.nl", connections);

        Mockito.when(businessService.findConnectionsPerCRO()).thenReturn(connectionsPerCRO);
        // no data no result
        coordinator.handleEvent(new CommonReferenceUpdateEvent());

        Mockito.verify(config, Mockito.times(1)).getProperty(ConfigParam.HOST_DOMAIN);
        Mockito.verify(jmsService, Mockito.times(1)).sendMessageToOutQueue(Mockito.anyString());

        Assert.assertNotNull(connection.getLastSynchronisationTime());
    }
}
