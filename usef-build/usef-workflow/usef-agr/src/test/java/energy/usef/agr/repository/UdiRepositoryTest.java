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

import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.*;

import energy.usef.agr.model.Udi;
import energy.usef.core.model.Connection;

/**
 * Test class in charge of the unit tests related to the {@link UdiRepository} class.
 */
public class UdiRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private UdiRepository repository;

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
        repository = new UdiRepository();
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
    public void testFindActiveUdisWithResults() {
        final LocalDate period = new LocalDate(2015, 1, 20);
        Map<String, Udi> activeUdis = repository.findActiveUdisMappedPerEndpoint(period);
        Assert.assertNotNull(activeUdis);
        Assert.assertEquals(2, activeUdis.size());
        Udi udi = activeUdis.values().iterator().next();
        Assert.assertEquals(2L, udi.getId().longValue());
        Assert.assertEquals("ean.12168590001263698.UDI", udi.getEndpoint());
    }

    @Test
    public void testFindActiveUdisWithoutResults() {
        final LocalDate period = new LocalDate(2015, 1, 18);
        Map<String, Udi> activeUdis = repository.findActiveUdisMappedPerEndpoint(period);
        Assert.assertNotNull(activeUdis);
        Assert.assertEquals(0, activeUdis.size());
    }

    @Test
    public void testFindByEndpoint() {
        final LocalDate period = new LocalDate(2015, 1, 20);
        Udi udi = repository.findByEndpoint("ean.12168590001263699.UDI", period);
        Assert.assertNotNull(udi);
        Assert.assertEquals(1, udi.getId().intValue());
        Assert.assertEquals(1, udi.getDtuSize().intValue());
    }

    @Test
    public void testFindActiveUdisPerConnectionByPeriod() {
        final LocalDate period = new LocalDate(2015, 1, 20);
        Map<Connection, List<Udi>> udisPerConnection = repository.findActiveUdisPerConnection(period);

        Assert.assertNotNull(udisPerConnection);
        Assert.assertEquals(1, udisPerConnection.size());
    }

    @Test
    public void testFindActiveUdisPerConnectionByPeriodAndConnectionList() {
        final LocalDate period = new LocalDate(2015, 1, 20);

        // test with connection entity address that does not exist
        List<String> connectionList = Arrays.asList("123");
        Map<Connection, List<Udi>> udisPerConnection = repository.findActiveUdisPerConnection(period, Optional.of(connectionList));

        Assert.assertNotNull(udisPerConnection);
        Assert.assertEquals(0, udisPerConnection.size());

        // test with connection entity address that does exist
        connectionList = Arrays.asList("ean.673685900012623654");
        udisPerConnection = repository.findActiveUdisPerConnection(period, Optional.of(connectionList));

        Assert.assertNotNull(udisPerConnection);
        Assert.assertEquals(1, udisPerConnection.size());
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("2015-11-30")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("2015-11-30")));
    }
}
