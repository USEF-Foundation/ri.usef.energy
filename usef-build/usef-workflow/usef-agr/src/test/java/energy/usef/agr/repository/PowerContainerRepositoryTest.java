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

package energy.usef.agr.repository;

import static junit.framework.TestCase.assertEquals;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.model.Udi;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

/**
 * Test class in charge of the unit testing of the {@link PowerContainerRepository}.
 */
public class PowerContainerRepositoryTest {
    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private PowerContainerRepository repository;

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
    public void before() {
        repository = new PowerContainerRepository();
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

    @Test
    public void testFindAllPowerContainers() {
        LocalDate testDate = new LocalDate("2015-01-20");

        Map<ConnectionGroup, List<PowerContainer>> allPowerContainers = repository.findConnectionGroupPowerContainers(testDate,
                Optional.empty());
        Assert.assertEquals(1, allPowerContainers.size());
        Assert.assertEquals(1, allPowerContainers.entrySet().stream().findFirst().get().getValue().size());

        Map<Connection, List<PowerContainer>> connectionContainers = repository.findConnectionPowerContainers(testDate,
                Optional.empty(), Optional.empty());
        Assert.assertEquals(1, connectionContainers.size());
        Assert.assertEquals(1, connectionContainers.entrySet().stream().findFirst().get().getValue().size());

        Map<Udi, List<PowerContainer>> udisPerConnection = repository.findUdiPowerContainers(testDate, Optional.empty(), Optional.empty());
        Assert.assertEquals(1, udisPerConnection.size());

        Assert.assertEquals(1, udisPerConnection.size());
        Assert.assertEquals(1, udisPerConnection.values().iterator().next().size());
    }

    @Test
    public void testFindConnectionPowerContainers() {
        repository.findConnectionPowerContainers(new LocalDate("2015-01-20"), Optional.empty(), Optional.empty());
    }

    @Test
    public void testFindUdiPowerContainers() {
        Map<Udi, List<PowerContainer>> udiPowerContainers = repository
                .findUdiPowerContainers(new LocalDate("2015-01-20"), Optional.empty(), Optional.empty());

        assertEquals(1, udiPowerContainers.values().size());

        assertEquals("Should have 1 UDI! The other has no powerContainer.", 1, udiPowerContainers.size());

        List<PowerContainer> powerContainers = udiPowerContainers.entrySet()
                .stream().filter(entry -> entry.getKey().getId() == 1).map(Map.Entry::getValue).findFirst().get();
        assertEquals("udi 1 should only have the power containers of today", 1,
                powerContainers.size());
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 3, repository.cleanup(new LocalDate("2015-01-20")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("2015-01-20")));
    }
}
