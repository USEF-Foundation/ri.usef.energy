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

package energy.usef.core.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.util.DateTimeUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the GridPointRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class ConnectionGroupStateRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private ConnectionGroupStateRepository repository;

    @BeforeClass
    public static void initTestFixture() throws Exception {
        // Get the entity manager for the tests.
        entityManagerFactory = Persistence.createEntityManagerFactory("test");
        entityManager = entityManagerFactory.createEntityManager();
    }

    /**
     * Cleans up the session.
     */
    @AfterClass
    public static void closeTestFixture() {
        entityManager.close();
        entityManagerFactory.close();
    }

    @Before
    public void init() {
        repository = new ConnectionGroupStateRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    /**
     * Tests for GridPointRepository.getGridPointByEntityAddress method.
     */
    @Test
    public void testFindByConnectionGroupAndConnection() {

        assertNotNull(repository.findConnectionGroupState("ea1.1992-02.com.otherexample:gridpoint.4f76ff19-a53b-49f5-99e9",
                "connection.test.com", new LocalDate()));
        assertNotNull(repository.findConnectionGroupState("brp.test.com", "connection.test.com", new LocalDate()));
        assertNull(repository.findConnectionGroupState("non.test.com", "connection.test.com", new LocalDate()));
        assertNull(repository.findConnectionGroupState("brp.test.com", "non.test.com", new LocalDate()));
    }

    @Test
    public void testFindActiveConnectionGroupStatesWithNullType() {
        List<ConnectionGroupState> connectionGroupStates = repository.findActiveConnectionGroupStatesOfType(
                DateTimeUtil.getCurrentDate(),
                null);
        assertNotNull(connectionGroupStates);
        assertEquals(2, connectionGroupStates.size());
    }

    @Test
    public void testFindActiveCongestionPointConnectionGroups() {
        List<ConnectionGroupState> connectionGroupStates = repository.findActiveConnectionGroupStatesOfType(
                DateTimeUtil.getCurrentDate(),
                CongestionPointConnectionGroup.class);
        assertNotNull(connectionGroupStates);
        assertEquals(1, connectionGroupStates.size());
    }

    @Test
    public void testFindConnectionGroupStatesByStartDateAndEndDate() {
        LocalDate startDate = DateTimeUtil.parseDate("2013-11-20");
        List<ConnectionGroupState> connectionGroupStates = repository.findActiveConnectionGroupStates(startDate,
                DateTimeUtil.getCurrentDate());
        assertEquals(2, connectionGroupStates.size());

        startDate = DateTimeUtil.parseDate("1960-01-01");
        connectionGroupStates = repository.findActiveConnectionGroupStates(startDate, DateTimeUtil.getCurrentDate());
        assertEquals(0, connectionGroupStates.size());
    }

    @Test
    public void testFindConnectionsWithConnectionGroups() {
        List<String> usefIdentifiers = Arrays.asList(
                "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6",
                "ea1.1992-02.com.otherexample:gridpoint.4f76ff19-a53b-49f5-99e9");
        Map<ConnectionGroup, List<Connection>> connectionsWithConnectionGroups = repository
                .findConnectionsWithConnectionGroups(usefIdentifiers, DateTimeUtil.parseDate("2015-06-24"));
        Assert.assertNotNull(connectionsWithConnectionGroups);
        Assert.assertEquals(1, connectionsWithConnectionGroups.entrySet().size());
        Assert.assertEquals(1,
                connectionsWithConnectionGroups
                        .get(new CongestionPointConnectionGroup("ea1.1992-02.com.otherexample:gridpoint.4f76ff19-a53b-49f5-99e9"))
                        .size());
        Assert.assertEquals(new Connection("connection.test.com"),
                connectionsWithConnectionGroups
                        .get(new CongestionPointConnectionGroup("ea1.1992-02.com.otherexample:gridpoint.4f76ff19-a53b-49f5-99e9"))
                        .get(0));
    }

    @Test
    public void testFindActiveConnectionGroupsWithConnections() {
        Map<ConnectionGroup, List<Connection>> connectionGroups = repository
                .findActiveConnectionGroupsWithConnections(DateTimeUtil.parseDate("2015-07-03"),
                        DateTimeUtil.parseDate("2015-07-03"));
        // verifications
        Assert.assertNotNull(connectionGroups);
        Assert.assertEquals(2, connectionGroups.keySet().size());
        Assert.assertEquals(1, connectionGroups.get(new BrpConnectionGroup("brp.test.com")).size());
        Assert.assertEquals(1, connectionGroups.get(
                new CongestionPointConnectionGroup("ea1.1992-02.com.otherexample:gridpoint.4f76ff19-a53b-49f5-99e9")).size());
    }

    @Test
    public void testFindActiveConnectionsWithConnectionGroups() {
        Map<Connection, List<ConnectionGroup>> connectionsMap = repository.findActiveConnectionsWithConnectionGroups(
                DateTimeUtil.parseDate("2015-07-03"));
        // verifications
        Assert.assertNotNull(connectionsMap);
        Assert.assertEquals(1, connectionsMap.size());
        Assert.assertEquals(2, connectionsMap.get(new Connection("connection.test.com")).size());
    }

    @Test
    public void testFindConnectionGroupStatesWithOverlappingValidity() {
        LocalDate startDate = DateTimeUtil.parseDate("2049-12-31");
        LocalDate endDate = DateTimeUtil.parseDate("2050-01-10");
        // invocation
        List<ConnectionGroupState> connectionGroupStates = repository
                .findConnectionGroupStatesWithOverlappingValidity(startDate, endDate);
        // verifications
        Assert.assertNotNull(connectionGroupStates);
        Assert.assertEquals(2, connectionGroupStates.size());
    }

}
