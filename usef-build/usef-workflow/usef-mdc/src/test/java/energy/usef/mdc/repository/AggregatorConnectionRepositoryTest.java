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

package energy.usef.mdc.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import energy.usef.mdc.model.AggregatorConnection;

/**
 * Test class in charge of the unit tests related to the {@link AggregatorConnectionRepository} class.
 */
public class AggregatorConnectionRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private AggregatorConnectionRepository repository;

    @Test
    public void testFind() {
        AggregatorConnection state = repository.find(-1l);
        assertNotNull(state);
        assertEquals(-1l, state.getId().longValue());
        assertEquals("agr1.usef-example.com", state.getAggregator().getDomain());
        assertEquals("ean.1111111111", state.getConnection().getEntityAddress());
        assertEquals(new LocalDate(1970, 1, 1), state.getValidFrom());
        assertEquals(new LocalDate(1990, 1, 1), state.getValidUntil());
    }

    @Test
    public void testFindActiveAggregatorOnConnectionStatesForAggregator() throws Exception {
        List<AggregatorConnection> states = repository.findActiveAggregatorConnectionsForAggregator("agr1.usef-example.com",
                new LocalDate(1980, 6, 1));
        assertEquals(1, states.size());
        AggregatorConnection state = states.get(0);
        assertNotNull(state);
        assertEquals("agr1.usef-example.com", state.getAggregator().getDomain());
        assertEquals("ean.1111111111", state.getConnection().getEntityAddress());
        assertEquals(new LocalDate(1970, 1, 1), state.getValidFrom());
        assertEquals(new LocalDate(1990, 1, 1), state.getValidUntil());
    }

    @Test
    public void testFindActiveAggregatorOnConnectionStateForConnection() throws Exception {
        AggregatorConnection state = repository.findActiveAggregatorConnectionForConnection("ean.2222222222", null);
        assertNotNull(state);
        assertEquals("agr1.usef-example.com", state.getAggregator().getDomain());
        assertEquals("ean.2222222222", state.getConnection().getEntityAddress());
    }

    @Test
    public void testFindActiveAggregatorOnConnectionStateForConnectionReturnsNothing() throws Exception {
        AggregatorConnection state = repository.findActiveAggregatorConnectionForConnection("ean.2222222222",
                new LocalDate(1980, 6, 1));
        assertNull(state);
    }

    @Test
    public void testFindActiveAggregatorOnConnectionStatesForCommonReferenceOperator() throws Exception {
        List<AggregatorConnection> states = repository.findActiveAggregatorConnectionsForCommonReferenceOperator(
                "cro1.usef-example.com", null);
        assertNotNull(states);
        assertEquals(1, states.size());
    }

    @Test
    public void testfindAggregatorForEachConnection() throws Exception {
        Map<String, String> aggregatorForEachConnection = repository.findAggregatorForEachConnection(new LocalDate(), Arrays
                .asList("ean.1111111111", "ean.2222222222", "ean.3333333333"));
        assertNotNull(aggregatorForEachConnection);
        assertEquals(2, aggregatorForEachConnection.size());
        assertNull(aggregatorForEachConnection.get("ean.1111111111"));
        assertEquals("agr1.usef-example.com", aggregatorForEachConnection.get("ean.2222222222"));
        assertEquals("agr1.usef-example.com", aggregatorForEachConnection.get("ean.3333333333"));
    }

    @BeforeClass
    public static void initTestFixture() throws Exception {
        // Get the entity manager for the tests.
        entityManagerFactory = Persistence.createEntityManagerFactory("test");
        entityManager = entityManagerFactory.createEntityManager();
    }

    @Before
    public void before() throws Exception {
        repository = new AggregatorConnectionRepository();

        setInternalState(repository, "entityManager", entityManager);
        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
        entityManager.getTransaction().begin();
    }

    @After
    public void after() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    /**
     * Cleans up the session.
     */
    @AfterClass
    public static void closeTestFixture() {
        entityManager.close();
        entityManagerFactory.close();
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1990-01-01")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1990-01-01")));
    }
}
