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
import energy.usef.agr.workflow.nonudi.service.PowerMatcher;
import energy.usef.agr.workflow.nonudi.initialize.AgrInitializeNonUdiClustersParameter;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub implementation of the PBC which is in charge of initializing the Non-UDI cluster (eg PowerMatcher).
 * <p>
 * The PBC receives the following parameters as input to take the decision:
 * <ul>
 * <li>PERIOD: the period {@link org.joda.time.LocalDate} the cluster needs to be initialized.</li>
 * <li>BRP_CONNECTION_LIST_MAP: Map with brp_id {@link String} -> {@link List} of connection entity addresses {@link String}.</li>
 * <li>CP_CONNECTION_LIST_MAP : Map with congestion_point_id {@link String} -> {@link List} of connection entity addresses
 * {@link String}.</li>
 * </ul>
 * <p>
 * The step implementation of this PBC will retrieve the current information available in the PowerMatcher and
 * will only send new or delete existing BRP / Congestion Point / Connection data.
 * <p>
 * This PBC requires a PowerMatcher to be available and will try forever in case the PowerMatcher isn't responding.
 */
public class AgrInitializeNonUdiClustersStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrInitializeNonUdiClustersStub.class);

    @Inject
    private PowerMatcher powerMatcher;

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        // get the input parameters
        LocalDate period = context.get(AgrInitializeNonUdiClustersParameter.IN.PERIOD.name(), LocalDate.class);
        Map<String, List<String>> brpConnectionListMap = context.get(AgrInitializeNonUdiClustersParameter.IN.BRP_CONNECTION_LIST_MAP.name(), HashMap.class);
        Map<String, List<String>> cpConnectionListMap = context.get(AgrInitializeNonUdiClustersParameter.IN.CP_CONNECTION_LIST_MAP.name(), HashMap.class);

        //BRPs
        List<BalanceResponsiblePartyDto> powerMatcherBrpList = powerMatcher.findAllBalanceResponsibleParties();
        List<BalanceResponsiblePartyDto> usefBrpList = mapToBrpDto(brpConnectionListMap);

        List<BalanceResponsiblePartyDto> toBeCreatedBrpList = removeElementsFromList(usefBrpList, powerMatcherBrpList);

        //CPs
        List<CongestionPointDto> powerMatcherCpList = powerMatcher.findAllCongestionPoints();
        List<CongestionPointDto> usefCpList = mapToCpDto(cpConnectionListMap);

        List<CongestionPointDto> toBeCreatedCpList = removeElementsFromList(usefCpList, powerMatcherCpList);

        //Connections
        List<ConnectionDto> powerMatcherConnList = powerMatcher.findAllConnections();
        List<ConnectionDto> usefConnList = mapToConnectionDto(brpConnectionListMap, cpConnectionListMap);

        List<ConnectionDto> removedConnList = removeElementsFromList(powerMatcherConnList, usefConnList);
        List<ConnectionDto> toBeCreatedConnList = removeElementsFromList(usefConnList, powerMatcherConnList);

        LOGGER.debug("Removing {} connections.", removedConnList.size());
        removedConnList.forEach(powerMatcher::deleteConnection);

        LOGGER.debug("Creating {} connections, {} brp's and {} congestion points.",
                toBeCreatedConnList.size(), toBeCreatedBrpList.size(), toBeCreatedCpList.size());
        toBeCreatedBrpList.forEach(powerMatcher::createBalanceResponsibleParty);
        toBeCreatedCpList.forEach(powerMatcher::createCongestionPoint);
        toBeCreatedConnList.forEach(powerMatcher::createConnection);

        return context;
    }

    private List<ConnectionDto> mapToConnectionDto(Map<String, List<String>> brpConnectionListMap,
            Map<String, List<String>> cpConnectionListMap) {
        Map<String, String> connectionToCongestionPointMap = new HashMap<>();
        cpConnectionListMap.forEach(
                (cpId, connectionList) -> connectionList.forEach(connId -> connectionToCongestionPointMap.put(connId, cpId)));

        // create a list of ConnectionDto's
        List<ConnectionDto> connList = new ArrayList<>();
        brpConnectionListMap.forEach((brpId, connectionList) -> connectionList.forEach(connId -> {
            ConnectionDto connDto = new ConnectionDto();
            connDto.setConnectionId(connId);
            connDto.setBrpId(brpId);
            if (connectionToCongestionPointMap.containsKey(connId)) {
                connDto.setCongestionPointId(connectionToCongestionPointMap.get(connId));
            } else {
                connDto.setCongestionPointId(null);
            }
            connList.add(connDto);
        }));
        return connList;
    }

    private List<BalanceResponsiblePartyDto> mapToBrpDto(Map<String, List<String>> cpConnectionListMap) {
        return cpConnectionListMap.keySet().stream().map(BalanceResponsiblePartyDto::new).collect(Collectors.toList());
    }

    private List<CongestionPointDto> mapToCpDto(Map<String, List<String>> cpConnectionListMap) {
        return cpConnectionListMap.keySet().stream().map(CongestionPointDto::new).collect(Collectors.toList());
    }

    private <T> List<T> removeElementsFromList(List<T> baseList, List<T> toBeRemovedList) {
        List<T> result = new ArrayList<>(baseList);
        result.removeAll(toBeRemovedList);
        return result;
    }

}
