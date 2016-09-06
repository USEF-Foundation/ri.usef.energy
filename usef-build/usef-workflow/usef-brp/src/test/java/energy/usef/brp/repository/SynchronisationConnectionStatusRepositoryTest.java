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

package energy.usef.brp.repository;

import static org.junit.Assert.assertEquals;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.brp.model.SynchronisationConnectionStatusType;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class in charge of the unit tests related to the {@link SynchronisationConnectionStatusRepository} class.
 */
@RunWith(PowerMockRunner.class)
public class SynchronisationConnectionStatusRepositoryTest {

    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;
    private SynchronisationConnectionStatusRepository repository;

    /**
     * Initialize the session.
     */
    @Before
    public void init() throws Exception {
        repository = new SynchronisationConnectionStatusRepository();

        // Get the entity manager for the tests.
        entityManagerFactory = Persistence.createEntityManagerFactory("test");
        entityManager = entityManagerFactory.createEntityManager();

        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    /**
     * Clean up the session.
     */
    @AfterClass
    public static void closeTestFixture() {
        entityManager.close();
    }

    @Test
    public void testCountSynchronisationConnectionStatusWithStatus() throws Exception {
        long count = repository.countSynchronisationConnectionStatusWithStatus(SynchronisationConnectionStatusType.MODIFIED);

        Assert.assertEquals(4l, count);
    }

    @Test
    public void testDeleteAll() throws Exception {
        entityManager.getTransaction().begin();
        repository.deleteAll();

        long count = repository.countSynchronisationConnectionStatusWithStatus(SynchronisationConnectionStatusType.MODIFIED);
        assertEquals(0l, count);

        entityManager.getTransaction().rollback();
    }
}
