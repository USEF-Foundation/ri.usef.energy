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

package energy.usef.dso.workflow.plan.commonreferenceupdate;

import java.util.ArrayList;
import java.util.Collections;
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

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.dso.model.CommonReferenceOperator;
import energy.usef.dso.model.SynchronisationCongestionPoint;
import energy.usef.dso.model.SynchronisationCongestionPointStatus;
import energy.usef.dso.model.SynchronisationConnection;
import energy.usef.dso.model.SynchronisationConnectionStatusType;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;

@RunWith(PowerMockRunner.class)
public class DsoCommonReferenceUpdateCoordinatorTest {

    private DsoCommonReferenceUpdateCoordinator coordinator;

    @Mock
    private DsoPlanboardBusinessService businessService;

    @Mock
    private Config config;

    @Mock
    private JMSHelperService jmsService;

    @Before
    public void init() {
        coordinator = new DsoCommonReferenceUpdateCoordinator();
        Whitebox.setInternalState(coordinator, businessService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, jmsService);
    }

    @Test
    public void testHandleEventNoData() {
        Map<String, List<SynchronisationCongestionPoint>> connectionsPerCRO = new HashMap<>();

        Mockito.when(businessService.findConnectionsPerCRO()).thenReturn(connectionsPerCRO);
        // no data no result
        coordinator.handleEvent(new CommonReferenceUpdateEvent());

        Mockito.verify(config, Mockito.times(0)).getProperty(Mockito.any(ConfigParam.class));
        Mockito.verify(jmsService, Mockito.times(0)).sendMessageToOutQueue(Mockito.anyString());
    }

    @Test
    public void testHandleEventOneMessage() {
        Map<String, List<SynchronisationCongestionPoint>> connectionsPerCRO = new HashMap<>();
        List<SynchronisationCongestionPoint> congestionPoints = new ArrayList<>();
        SynchronisationCongestionPoint congestionPoint = new SynchronisationCongestionPoint();
        SynchronisationCongestionPointStatus status = new SynchronisationCongestionPointStatus();
        CommonReferenceOperator cro = new CommonReferenceOperator();
        cro.setDomain("cro.dummy.nl");
        status.setStatus(SynchronisationConnectionStatusType.MODIFIED);
        status.setCommonReferenceOperator(cro);
        congestionPoint.setStatusses(Collections.singletonList(status));
        congestionPoint.setEntityAddress("connectionAddress");
        congestionPoint.getConnections().add(new SynchronisationConnection());
        congestionPoints.add(congestionPoint);
        connectionsPerCRO.put("cro.dummy.nl", congestionPoints);

        Mockito.when(businessService.findConnectionsPerCRO()).thenReturn(connectionsPerCRO);

        coordinator.handleEvent(new CommonReferenceUpdateEvent());

        Mockito.verify(config, Mockito.times(1)).getProperty(Mockito.eq(ConfigParam.HOST_DOMAIN));
        Mockito.verify(jmsService, Mockito.times(1)).sendMessageToOutQueue(Mockito.anyString());

        Assert.assertNotNull(congestionPoint.getLastSynchronisationTime());
    }
}
