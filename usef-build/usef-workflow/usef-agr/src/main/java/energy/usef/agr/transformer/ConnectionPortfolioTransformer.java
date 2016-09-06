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

package energy.usef.agr.transformer;

import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.model.Udi;
import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.workflow.dto.USEFRoleDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transformer class for transforming Connection related objects.
 */
public class ConnectionPortfolioTransformer {

    private ConnectionPortfolioTransformer() {
        // private constructor
    }

    /**
     * Transforms the power containers per connection and the power containers per UDI into a list of {@link ConnectionPortfolioDto}
     * objects.
     *
     *
     * @param activeConnections {@link List} of active {@link Connection}'s.
     * @param connectionPowerContainers {@link Map} of {@link PowerContainer} per {@link Connection}.
     * @param udisPerConnection {@link List} of {@link Udi} per {@link Connection}.
     * @param udiPowerContainers power containers at UDI level.
     * @return {@link List} of {@link ConnectionPortfolioDto}.
     */
    public static List<ConnectionPortfolioDto> transformToDTO(List<Connection> activeConnections,
            Map<Connection, List<PowerContainer>> connectionPowerContainers,
            Map<Connection, List<Udi>> udisPerConnection, Map<Udi, List<PowerContainer>> udiPowerContainers) {
        List<ConnectionPortfolioDto> results = new ArrayList<>();
        //map connections
        activeConnections.forEach(c -> {
            ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto(c.getEntityAddress());
            results.add(connectionPortfolioDTO);
            List<PowerContainer> powerContainers = connectionPowerContainers.getOrDefault(c, new ArrayList<>());
            powerContainers.stream()
                    .forEach(pc -> connectionPortfolioDTO.getConnectionPowerPerPTU().put(pc.getTimeIndex(),
                            PowerContainerTransformer.transform(pc)));
            if (udisPerConnection != null && udisPerConnection.containsKey(c)) {
                udisPerConnection.get(c).forEach(udi -> {
                    UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto(udi.getEndpoint(), udi.getDtuSize(), udi.getProfile());
                    connectionPortfolioDTO.getUdis().add(udiPortfolioDto);
                    if (udiPowerContainers.containsKey(udi)) {
                        udiPowerContainers.get(udi).stream().forEach(pc -> udiPortfolioDto.getUdiPowerPerDTU()
                                .put(pc.getTimeIndex(), PowerContainerTransformer.transform(pc)));
                    }
                });
            }
        });

        return results;
    }

    /**
     * Transforms a map of Power Containers at connection group level to list of ConnectionGroupPortfolioDto.
     *
     * @param connectionGroupPowerContainers a {@link Map} with {@link ConnectionGroup} as key and {@link List} of {@link
     * PowerContainer} as value.
     * @return a {@link List} of {@link ConnectionGroupPortfolioDto}.
     */
    public static List<ConnectionGroupPortfolioDto> transformToDTO(
            Map<ConnectionGroup, List<PowerContainer>> connectionGroupPowerContainers) {
        List<ConnectionGroupPortfolioDto> connectionGroupPortfolio = new ArrayList<>();
        for (ConnectionGroup connectionGroup : connectionGroupPowerContainers.keySet()) {
            String usefIdentifier = connectionGroup.getUsefIdentifier();
            USEFRoleDto usefRole = determineRole(connectionGroup);
            ConnectionGroupPortfolioDto connectionGroupPortfolioDto = new ConnectionGroupPortfolioDto(usefIdentifier, usefRole);
            connectionGroupPortfolioDto.getConnectionGroupPowerPerPTU()
                    .putAll(connectionGroupPowerContainers.get(connectionGroup)
                            .stream()
                            .map(PowerContainerTransformer::transform)
                            .collect(Collectors.toMap(PowerContainerDto::getTimeIndex, Function.identity())));
            connectionGroupPortfolio.add(connectionGroupPortfolioDto);
        }
        return connectionGroupPortfolio;
    }

    private static USEFRoleDto determineRole(ConnectionGroup connectionGroup) {
        USEFRoleDto role = null;
        if (connectionGroup instanceof BrpConnectionGroup) {
            role = USEFRoleDto.BRP;
        } else if (connectionGroup instanceof CongestionPointConnectionGroup) {
            role = USEFRoleDto.DSO;
        } else if (connectionGroup instanceof AgrConnectionGroup) {
            role = USEFRoleDto.AGR;
        }
        return role;
    }

}
