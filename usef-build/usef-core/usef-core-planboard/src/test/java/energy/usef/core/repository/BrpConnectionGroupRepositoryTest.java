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

package energy.usef.core.repository;

import static org.powermock.reflect.Whitebox.setInternalState;
import energy.usef.core.model.BrpConnectionGroup;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the BrpConnectionGroupRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class BrpConnectionGroupRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private BrpConnectionGroupRepository repository;

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
        repository = new BrpConnectionGroupRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    /**
     * Tests for BrpConnectionGroupRepository.findActiveBrpConnectionGroups method.
     */
    @Test
    public void testFindActiveBalanceResponsibleParties() {
        LocalDate dateTime = new LocalDate();
        List<BrpConnectionGroup> brpConnectionGroups = repository.findActiveBrpConnectionGroups(dateTime);
        Assert.assertEquals(1, brpConnectionGroups.size());
    }

    @Test
    public void testFindOrCreate() {
        entityManager.getTransaction().begin();
        Assert.assertNotNull(repository.findOrCreate("bestaatnogniet-domain.nl"));
        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
        Assert.assertNotNull(repository.findOrCreate("bestaatnogniet-domain.nl"));
        entityManager.getTransaction().commit();
    }
}
