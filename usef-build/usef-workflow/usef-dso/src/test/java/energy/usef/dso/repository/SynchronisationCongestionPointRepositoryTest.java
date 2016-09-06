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
import energy.usef.dso.model.SynchronisationCongestionPoint;
import energy.usef.dso.model.SynchronisationConnectionStatusType;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class in charge of the unit testing of the {@link SynchronisationConnectionRepository}.
 */
public class SynchronisationCongestionPointRepositoryTest {
    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private SynchronisationCongestionPointRepository repository;

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
        repository = new SynchronisationCongestionPointRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    @Test
    public void testFindAll() {
        List<SynchronisationCongestionPoint> synchronisationCongestionPoints = repository.findAll();
        Assert.assertEquals(6, synchronisationCongestionPoints.size());
    }

    @Test
    public void testUpdateConnectionStatusForCRO() {
        entityManager.getTransaction().begin();
        repository.updateCongestionPointStatusForCRO("ea1.1234-1234-1231241", "status-demo.com");
        entityManager.getTransaction().commit();
        Assert.assertEquals(SynchronisationConnectionStatusType.SYNCHRONIZED, repository.find(1L)
                .findStatusForCRO("status-demo.com"));

    }
}
