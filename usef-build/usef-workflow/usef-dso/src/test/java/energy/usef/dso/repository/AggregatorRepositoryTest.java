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

package energy.usef.dso.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.reflect.Whitebox.setInternalState;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import energy.usef.core.repository.ConnectionGroupRepository;
import energy.usef.dso.model.Aggregator;

/**
 * JUnit test for the AggregatorRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class AggregatorRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private AggregatorRepository repository;
    private ConnectionGroupRepository connectionGroupRepository;

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
        repository = new AggregatorRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();

        connectionGroupRepository = new ConnectionGroupRepository();
        setInternalState(connectionGroupRepository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        connectionGroupRepository.getEntityManager().clear();
    }

    @Test
    public void testSimpleFind() {
        Aggregator agr = repository.find("agr1.usef-example.com");
        assertNotNull("Did not expect a null agr.", agr);
        assertEquals("agr1.usef-example.com", agr.getDomain());
    }

    @Test
    public void testFindOrCreate() {
        EntityManager mockedEntityManager = PowerMockito.mock(EntityManager.class);
        setInternalState(repository, "entityManager", mockedEntityManager);
        final String DOMAIN_NAME = "agr999.usef-example.com";
        PowerMockito.when(mockedEntityManager.find(Aggregator.class, DOMAIN_NAME)).thenReturn(null);

        Aggregator aggregator = repository.findOrCreate(DOMAIN_NAME);
        Mockito.verify(mockedEntityManager, Mockito.times(1)).persist(Matchers.any(Aggregator.class));
        assertNotNull(aggregator);
        assertEquals(DOMAIN_NAME, aggregator.getDomain());
    }

}
