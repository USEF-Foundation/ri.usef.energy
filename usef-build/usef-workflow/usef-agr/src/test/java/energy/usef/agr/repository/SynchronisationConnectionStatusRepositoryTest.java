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

import static energy.usef.agr.model.SynchronisationConnectionStatusType.MODIFIED;
import static energy.usef.agr.model.SynchronisationConnectionStatusType.SYNCHRONIZED;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import energy.usef.agr.model.CommonReferenceOperator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link SynchronisationConnectionStatusRepository} class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class SynchronisationConnectionStatusRepositoryTest {
    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private SynchronisationConnectionStatusRepository repository;
    private CommonReferenceOperatorRepository aRepository;

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

    @Before
    public void setUp() throws Exception {
        repository = new SynchronisationConnectionStatusRepository();
        Whitebox.setInternalState(repository, entityManager);
    }

    @Test
    public void testCountSynchronisationConnectionStatusWithStatus() throws Exception {
        Long modifiedConnections = repository
                .countSynchronisationConnectionStatusWithStatus(MODIFIED);
        Assert.assertEquals(4, modifiedConnections.longValue());
    }

    @Test
    public void testDeleteAll() throws Exception {
        entityManager.getTransaction().begin();
        repository.deleteAll();
        Long modifiedConnections = repository
                .countSynchronisationConnectionStatusWithStatus(MODIFIED);
        Long deletedConnections = repository
                .countSynchronisationConnectionStatusWithStatus(SYNCHRONIZED);
        Assert.assertEquals(0, modifiedConnections.longValue());
        Assert.assertEquals(0, deletedConnections.longValue());
        entityManager.getTransaction().rollback();
    }
}
