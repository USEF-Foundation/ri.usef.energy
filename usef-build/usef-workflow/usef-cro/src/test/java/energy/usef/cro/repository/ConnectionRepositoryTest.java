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

package energy.usef.cro.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.cro.model.Connection;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the ConnectionRepository class (CRO).
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class ConnectionRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private ConnectionRepository repository;

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
        repository = new ConnectionRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    /**
     * Tests for ConnectionRepository.findCongestionPointByEntityAddress method.
     */
    @Test
    public void testFindAll() {
        List<Connection> connectionList = repository.findAll();
        assertNotNull(connectionList);
        assertEquals("Size of List", 10, connectionList.size());
    }

    /**
     * Tests for ConnectionRepository.findCongestionPointByEntityAddress method.
     */
    @Test
    public void testFindCongestionPointByExistentEntityAddress() {
        String entityAddress = "ean.871685900012636543";
        Connection connection = repository.findConnectionByEntityAddress(entityAddress);
        assertNotNull("Existent Connection", connection);
    }

    /**
     * Tests for ConnectionRepository.findCongestionPointByEntityAddress method.
     */
    @Test
    public void testFindCongestionPointByNonexistentEntityAddress() {
        String entityAddress = "XXX.000000000000000000";
        Connection connection = repository.findConnectionByEntityAddress(entityAddress);
        assertNull("Nonexistent Connection", connection);
    }
    @Test
    public void testFindConnectionsForAggregator() {
        String senderDomain = "tesla.com";
        List<Connection> connections = repository.findConnectionsForAggregator(senderDomain);
        assertEquals(1, connections.size());
    }

    @Test
    public void testFindConnectionsForBrp() {
        String senderDomain = "brp.test.com";
        List<Connection> connections = repository.findConnectionsForBRP(senderDomain);
        assertEquals(1, connections.size());
    }

    @Test
    public void testFindConnectionsForMdc() {
        List<Connection> connections = repository.findConnectionsForMDC();
        assertEquals(8, connections.size());
    }

}
