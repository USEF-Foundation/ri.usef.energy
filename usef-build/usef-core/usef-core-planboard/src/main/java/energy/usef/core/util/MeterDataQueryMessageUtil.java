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

package energy.usef.core.util;

import energy.usef.core.data.xml.bean.message.Connections;
import energy.usef.core.data.xml.bean.message.MeterDataQuery;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class to help to build Meter Data Query messages.
 */
public class MeterDataQueryMessageUtil {

    private MeterDataQueryMessageUtil() {
        // empty constructor to prevent instantiation.
    }

    /**
     * Populate the Connections and Connection elements in the MeterDataQuery from the map containing the congestion points to
     * their list of connections.
     *
     * @param meterDataQuery {@link MeterDataQuery} message to fill in.
     * @param connectionGroupsWithConnections {@link Map} of {@link ConnectionGroup} to {@link List} of {@link Connection}.
     */
    public static void populateConnectionsInConnectionGroups(MeterDataQuery meterDataQuery,
            Map<ConnectionGroup, List<Connection>> connectionGroupsWithConnections) {
        // convert the map of ConnectionGroup > Connections to a map of String > Strings.
        Map<String, List<String>> map = connectionGroupsWithConnections.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getUsefIdentifier(),
                        entry -> entry.getValue().stream().map(Connection::getEntityAddress).collect(Collectors.toList())));
        // for each meter data query Connections element group, add all the connections.
        for (Connections meterDataQueryConnections : meterDataQuery.getConnections()) {
            if (map.get(meterDataQueryConnections.getParent()) == null) {
                continue;
            }
            meterDataQueryConnections.getConnection().addAll(map.get(meterDataQueryConnections.getParent()));
        }
    }

}
