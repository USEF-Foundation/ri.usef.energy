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

import static org.powermock.reflect.Whitebox.setInternalState;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import energy.usef.dso.model.SynchronisationCongestionPoint;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for class {@link SynchronisationConnectionRepository}.
 */
@RunWith(PowerMockRunner.class)
public class SynchronisationConnectionRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private SynchronisationConnectionRepository repository;

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
        repository = new SynchronisationConnectionRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    @Test
    public void testDeleteAll() {
        entityManager.getTransaction().begin();

        // make sure there are SynchronisationConnection records before deleting
        long count = repository.getEntityManager().createQuery("SELECT s FROM SynchronisationConnection s").getResultList().size();
        Assert.assertNotEquals(0, count);

        repository.deleteAll();

        // assert that all records have been removed after deleting
        count = repository.getEntityManager().createQuery("SELECT s FROM SynchronisationConnection s").getResultList().size();
        Assert.assertEquals(0, count);

        entityManager.getTransaction().rollback();
    }

    @Test
    public void testDeleteFor() {
        entityManager.getTransaction().begin();

        // make sure there are SynchronisationConnection records before deleting
        long count = repository.getEntityManager().createQuery("SELECT s FROM SynchronisationConnection s").getResultList().size();
        Assert.assertNotEquals(0, count);


        repository.deleteFor(null);

        // assert that all records have been removed after deleting
        count = repository.getEntityManager().createQuery("SELECT s FROM SynchronisationConnection s").getResultList().size();
        Assert.assertEquals(2, count);

        entityManager.getTransaction().rollback();
    }

}
