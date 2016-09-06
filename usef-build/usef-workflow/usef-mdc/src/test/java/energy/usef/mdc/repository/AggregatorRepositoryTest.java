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

import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.mdc.model.Aggregator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AggregatorRepository} class.
 */
public class AggregatorRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private AggregatorRepository repository;

    @Test
    public void testFind() {
        Aggregator aggregator = repository.find("agr1.usef-example.com");
        Assert.assertNotNull(aggregator);
        Assert.assertEquals("agr1.usef-example.com", aggregator.getDomain());
    }

    @Test
    public void testFindOrCreateIsCreatingWhenNotFound() {
        entityManager = PowerMockito.mock(EntityManager.class);
        Whitebox.setInternalState(repository, entityManager);
        repository.findOrCreate("agr.usef-example.com");

        ArgumentCaptor<Aggregator> aggregatorCaptor = ArgumentCaptor.forClass(Aggregator.class);
        Mockito.verify(entityManager, Mockito.times(1)).find(Aggregator.class, "agr.usef-example.com");
        Mockito.verify(entityManager, Mockito.times(1)).persist(aggregatorCaptor.capture());
        Aggregator createdAggregator = aggregatorCaptor.getValue();
        Assert.assertNotNull(createdAggregator);
        Assert.assertEquals("agr.usef-example.com", createdAggregator.getDomain());
    }

    @Test
    public void testFindOrCreateIsNotCreatingWhenFound() {
        entityManager = PowerMockito.mock(EntityManager.class);
        Whitebox.setInternalState(repository, entityManager);

        // stubbing
        PowerMockito.when(entityManager.find(Aggregator.class, "agr.usef-example.com"))
                .thenReturn(new Aggregator("agr.usef-example.com"));
        repository.findOrCreate("agr.usef-example.com");

        Mockito.verify(entityManager, Mockito.times(1)).find(Aggregator.class, "agr.usef-example.com");
        Mockito.verify(entityManager, Mockito.times(0)).persist(Matchers.any(Aggregator.class));
    }

    @BeforeClass
    public static void initTestFixture() throws Exception {
        // Get the entity manager for the tests.
        entityManagerFactory = Persistence.createEntityManagerFactory("test");
        entityManager = entityManagerFactory.createEntityManager();
    }

    @Before
    public void setUp() throws Exception {
        repository = new AggregatorRepository();

        setInternalState(repository, "entityManager", entityManager);
        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    /**
     * Cleans up the session.
     */
    @AfterClass
    public static void closeTestFixture() {
        entityManager.close();
        entityManagerFactory.close();
    }
}
