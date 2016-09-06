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
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link MeterDataQueryMessageUtil} class.
 */
public class MeterDataQueryMessageUtilTest {

    @Test
    public void testPopulateConnectionsInConnectionGroups() throws Exception {
        MeterDataQuery meterDataQuery = buildMeterDataQuery();
        Map<ConnectionGroup, List<Connection>> connectionsMap = buildConnectionsMap();

        meterDataQuery.getConnections().stream().forEach(connections -> Assert.assertTrue(connections.getConnection().isEmpty()));
        meterDataQuery.getConnections().stream().forEach(connections -> Assert.assertNotNull(connections.getParent()));

        MeterDataQueryMessageUtil.populateConnectionsInConnectionGroups(meterDataQuery, connectionsMap);
        meterDataQuery.getConnections().stream().forEach(connections -> Assert.assertEquals(2, connections.getConnection().size()));
        long group1Count = meterDataQuery.getConnections().stream()
                .filter(connections -> connections.getParent().equals("ean.111111111111"))
                .findFirst().get().getConnection().stream()
                .filter(connection -> connection.equals("ean.100000000001") || connection.equals("ean.100000000002")).count();
        long group2Count = meterDataQuery.getConnections().stream()
                .filter(connections -> connections.getParent().equals("ean.222222222222"))
                .findFirst().get().getConnection().stream()
                .filter(connection -> connection.equals("ean.200000000001") || connection.equals("ean.200000000002")).count();
        Assert.assertEquals(2, group1Count);
        Assert.assertEquals(2, group2Count);
    }

    private MeterDataQuery buildMeterDataQuery() {
        MeterDataQuery meterDataQuery = new MeterDataQuery();
        Connections connections1 = new Connections();
        connections1.setParent("ean.111111111111");
        Connections connections2 = new Connections();
        connections2.setParent("ean.222222222222");
        meterDataQuery.getConnections().addAll(Arrays.asList(connections1, connections2));
        return meterDataQuery;
    }

    private Map<ConnectionGroup, List<Connection>> buildConnectionsMap() {
        ConnectionGroup connectionGroup1 = new CongestionPointConnectionGroup("ean.111111111111");
        ConnectionGroup connectionGroup2 = new CongestionPointConnectionGroup("ean.222222222222");
        Connection connection1 = new Connection("ean.100000000001");
        Connection connection2 = new Connection("ean.100000000002");
        Connection connection3 = new Connection("ean.200000000001");
        Connection connection4 = new Connection("ean.200000000002");

        Map<ConnectionGroup, List<Connection>> connectionsMap = new HashMap<>();
        connectionsMap.put(connectionGroup1, new ArrayList<>());
        connectionsMap.put(connectionGroup2, new ArrayList<>());
        connectionsMap.get(connectionGroup1).addAll(Arrays.asList(connection1, connection2));
        connectionsMap.get(connectionGroup2).addAll(Arrays.asList(connection3, connection4));
        return connectionsMap;
    }
}
