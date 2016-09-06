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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.core.model.Connection;
import energy.usef.core.util.DateTimeUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the core ConnectionRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class ConnectionRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private ConnectionRepository repository;

    /**
     * Initialize test fixture.
     * 
     * @throws Exception
     */
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

    /**
     * Initialize the test cases.
     */
    @Before
    public void init() {
        repository = new ConnectionRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    /**
     * Test method for {@link ConnectionRepository#findActiveConnections(LocalDate, Optional)} (org.joda.time.LocalDate)}.
     */
    @Test
    public void testFindAllConnectionsLocalDateTimeWithoutConnections() {
        LocalDate date = DateTimeUtil.getCurrentDate();

        List<Connection> connectionList = repository.findActiveConnections(date, Optional.empty());
        assertNotNull(connectionList);
        assertFalse(connectionList.isEmpty());
    }

    /**
     * Test method for {@link ConnectionRepository#findActiveConnections(LocalDate, Optional)} (org.joda.time.LocalDate)}.
     */
    @Test
    public void testFindAllConnectionsLocalDateTimeWithConnections() {
        LocalDate date = DateTimeUtil.getCurrentDate();

        List<String> connectionEntityList = Arrays.asList("connection.test.com");

        List<Connection> connectionList = repository.findActiveConnections(date, Optional.of(connectionEntityList));
        assertNotNull(connectionList);
        assertFalse(connectionList.isEmpty());

        connectionEntityList = Arrays.asList("connection1.test.com");

        connectionList = repository.findActiveConnections(date, Optional.of(connectionEntityList));
        assertNotNull(connectionList);
        assertTrue(connectionList.isEmpty());
    }

    /**
     * Test method for
     * {@link ConnectionRepository#findConnectionCountByUsefIdentifier(java.lang.String, org.joda.time.LocalDate)}
     * .
     */
    @Test
    public void testFindConnectionCountByUsefIdentifier() {
        String usefIdentifier = "brp.test.com";

        Long connectionCount = repository.findConnectionCountByUsefIdentifier(usefIdentifier, DateTimeUtil.getCurrentDate());
        assertNotNull(connectionCount);
        assertEquals(1l, connectionCount.longValue());
    }

    /**
     * Test method for {@link ConnectionRepository#findOrCreate(java.lang.String)}.
     */
    @Test
    public void testFindOrCreate() {
        entityManager.getTransaction().begin();
        assertNotNull(repository.findOrCreate("non-existing-connection-id"));
        entityManager.getTransaction().commit();
    }

    /**
     * Test method for
     * {@link ConnectionRepository#findConnectionsForConnectionGroup(java.lang.String, org.joda.time.LocalDate)}
     * .
     */
    @Test
    public void testFindConnectionsForConnectionGroupStringLocalDateTime() {
        String usefIdentifier = "brp.test.com";
        LocalDate date = DateTimeUtil.getCurrentDate();

        List<Connection> connectionList = repository.findConnectionsForConnectionGroup(usefIdentifier, date);
        assertNotNull(connectionList);
        assertEquals(1, connectionList.size());
    }
}
