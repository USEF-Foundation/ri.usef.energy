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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.agr.model.Element;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for the {@link ElementRepository}.
 */
public class ElementRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private ElementRepository repository;

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
        repository = new ElementRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    @Test
    public void testFind() {
        Element element = repository.find("UUID_1");
        assertNotNull(element);
        assertEquals("ean.100000000123", element.getConnectionEntityAddress());
        assertEquals(2, element.getElementDtuData().size());
    }

    @Test
    public void testFindActiveElements() {
        List<Element> activeElementsForPeriod = repository.findActiveElementsForPeriod(new LocalDate("2000-01-20"));
        assertEquals(1, activeElementsForPeriod.size());
        assertEquals("UUID_2", activeElementsForPeriod.get(0).getId());
    }

    @Test
    public void testDeleteAll() {
        entityManager.getTransaction().begin();
        repository.deleteAllElements();
        assertEquals(0, repository.getEntityManager().createQuery("SELECT e FROM Element e").getResultList().size());
        entityManager.getTransaction().rollback();
    }
}
