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

package energy.usef.agr.workflow.step;

import energy.usef.agr.workflow.nonudi.dto.BalanceResponsiblePartyDto;
import energy.usef.agr.workflow.nonudi.dto.CongestionPointDto;
import energy.usef.agr.workflow.nonudi.dto.ConnectionDto;
import energy.usef.agr.workflow.nonudi.initialize.AgrInitializeNonUdiClustersParameter.IN;
import energy.usef.agr.workflow.nonudi.service.PowerMatcher;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrInitializeNonUdiClustersStub} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrInitializeNonUdiClustersStubTest {

    private AgrInitializeNonUdiClustersStub agrInitializeNonUdiClustersStub;

    @Mock
    private PowerMatcher powerMatcher;

    @Before
    public void init() {
        agrInitializeNonUdiClustersStub = new AgrInitializeNonUdiClustersStub();
        Whitebox.setInternalState(agrInitializeNonUdiClustersStub, powerMatcher);

        PowerMockito.when(powerMatcher.findAllBalanceResponsibleParties()).thenReturn(buildPowerMatcherBalanceResponseParties());
        PowerMockito.when(powerMatcher.findAllCongestionPoints()).thenReturn(buildPowerMatcherCongestionPoints());
        PowerMockito.when(powerMatcher.findAllConnections()).thenReturn(buildPowerMatcherConnections());
    }

    private List<CongestionPointDto> buildPowerMatcherCongestionPoints() {
        List<CongestionPointDto> congestionPointList = new ArrayList<>();

        congestionPointList.add(new CongestionPointDto("cp1"));
        congestionPointList.add(new CongestionPointDto("cp2"));

        return congestionPointList;
    }

    private List<BalanceResponsiblePartyDto> buildPowerMatcherBalanceResponseParties() {
        List<BalanceResponsiblePartyDto> brpList = new ArrayList<>();

        brpList.add(new BalanceResponsiblePartyDto("brp1"));
        brpList.add(new BalanceResponsiblePartyDto("brp2"));

        return brpList;
    }

    private List<ConnectionDto> buildPowerMatcherConnections() {
        List<ConnectionDto> connectionList = new ArrayList<>();

        ConnectionDto connectionDto = new ConnectionDto();
        connectionDto.setConnectionId("conn1");
        connectionDto.setBrpId("brp1");
        connectionDto.setCongestionPointId("cp1");
        connectionList.add(connectionDto);

        connectionDto = new ConnectionDto();
        connectionDto.setConnectionId("conn2");
        connectionDto.setBrpId("brp1");
        connectionDto.setCongestionPointId("cp2");
        connectionList.add(connectionDto);

        connectionDto = new ConnectionDto();
        connectionDto.setConnectionId("conn3");
        connectionDto.setBrpId("brp2");
        connectionDto.setCongestionPointId("cp1");
        connectionList.add(connectionDto);

        connectionDto = new ConnectionDto();
        connectionDto.setConnectionId("conn4");
        connectionDto.setBrpId("brp2");
        connectionDto.setCongestionPointId("cp2");
        connectionList.add(connectionDto);

        return connectionList;
    }

    private Map<String, List<String>> buildBalanceResponseParties() {
        Map<String, List<String>> brpListMap = new HashMap<>();

        brpListMap.put("brp1", buildConnectionList("conn1"));
        brpListMap.put("brp3", buildConnectionList("conn5"));

        return brpListMap;
    }

    private List<String> buildConnectionList(String... connectionIdList) {
        List<String> connectionList = new ArrayList<>();

        for (String connectionId : connectionIdList) {
            connectionList.add(connectionId);
        }

        return connectionList;
    }

    private Map<String, List<String>> buildCongestionPoints() {
        Map<String, List<String>> congestionPointListMap = new HashMap<>();

        congestionPointListMap.put("cp1", buildConnectionList("conn1"));
        congestionPointListMap.put("cp3", buildConnectionList("conn5"));

        return congestionPointListMap;
    }

    @Test
    /**
     * Testing the situation that the AgrInitializeNonUdiClustersStub is invoked with the following situation:
     *
     * PowerMatcher contains the following data:
     * <li>brp1 -> conn1, conn2</li>
     * <li>brp2 -> conn3, conn4</li>
     * <li>cp1 -> conn1, conn3</li>
     * <li>cp2 -> conn2, conn4</li>
     *
     * New situation
     * <li>brp1 -> conn1</li>
     * <li>brp3 -> conn5</li>
     * <li>cp1 -> conn1</li>
     * <li>cp3 -> conn5</li>
     */ public void testInvoke() {
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(IN.PERIOD.name(), DateTimeUtil.getCurrentDate());
        context.setValue(IN.BRP_CONNECTION_LIST_MAP.name(), buildBalanceResponseParties());
        context.setValue(IN.CP_CONNECTION_LIST_MAP.name(), buildCongestionPoints());

        WorkflowContext outContext = agrInitializeNonUdiClustersStub.invoke(context);

        Mockito.verify(powerMatcher, Mockito.times(3)).deleteConnection(Matchers.any(ConnectionDto.class));
        Mockito.verify(powerMatcher, Mockito.times(0)).deleteBalanceResponsibleParty(Matchers.anyString());
        Mockito.verify(powerMatcher, Mockito.times(0)).deleteCongestionPoint(Matchers.anyString());
        Mockito.verify(powerMatcher, Mockito.times(1)).createCongestionPoint(Matchers.any(CongestionPointDto.class));
        Mockito.verify(powerMatcher, Mockito.times(1))
                .createBalanceResponsibleParty(Matchers.any(BalanceResponsiblePartyDto.class));
        Mockito.verify(powerMatcher, Mockito.times(1)).createConnection(Matchers.any(ConnectionDto.class));
    }

}
