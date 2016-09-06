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

package energy.usef.cro.repository;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.cro.model.DistributionSystemOperator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the DistributionSystemOperatorRepository class (CRO).
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class DistributionSystemOperatorRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private DistributionSystemOperatorRepository repository;

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
        repository = new DistributionSystemOperatorRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    /**
     * Tests for DistributionSystemOperatorRepository.findByDomain method with existing domain.
     */
    @Test
    public void testFindByDomainExisting() {
        String domain = "usef-example.com";
        DistributionSystemOperator dso = repository.findByDomain(domain);
        assertNotNull(dso);
    }

    /**
     * Tests for DistributionSystemOperatorRepository.findByDomain method with non-existent domain.
     */
    @Test
    public void testFindByDomainNonExistent() {
        String domain = "example.com";
        DistributionSystemOperator dso = repository.findByDomain(domain);
        assertNull(dso);
    }

    /**
     * Tests for DistributionSystemOperatorRepository.findOrCreateByDomain method for existing domain.
     */
    @Test
    public void testFindOrCreateByDomainExisting() {
        String domain = "usef-example.com";
        DistributionSystemOperator dso = repository.findOrCreateByDomain(domain);
        assertNotNull(dso);
    }

    /**
     * Tests for DistributionSystemOperatorRepository.findOrCreateByDomain method for existing domain.
     */
    @Test
    public void testFindOrCreateByDomainNonExistent() {
        String domain = "example.com";
        DistributionSystemOperator dso = repository.findOrCreateByDomain(domain);
        assertNotNull(dso);
    }
}
