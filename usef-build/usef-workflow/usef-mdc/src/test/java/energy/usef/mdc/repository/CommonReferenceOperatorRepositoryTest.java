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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.reflect.Whitebox.setInternalState;
import energy.usef.mdc.model.CommonReferenceOperator;

import java.util.List;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

public class CommonReferenceOperatorRepositoryTest {
    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private CommonReferenceOperatorRepository repository;

    @Test
    public void testFind() {
        CommonReferenceOperator commonReferenceOperator = repository.find("cro1.usef-example.com");
        assertNotNull(commonReferenceOperator);
        assertEquals("cro1.usef-example.com", commonReferenceOperator.getDomain());
    }

    @Test
    public void testFindAll() {
        List<CommonReferenceOperator> commonReferenceOperators = repository.findAll();
        assertEquals(3, commonReferenceOperators.size());
        Stream.of("cro1.usef-example.com", "cro2.usef-example.com")
                .forEach(cro -> commonReferenceOperators.stream()
                        .map(CommonReferenceOperator::getDomain)
                        .anyMatch(domain -> domain.equals(cro)));
    }

    @Test
    public void testFindOrCreateIsCreatingWhenNotFound() {
        entityManager = PowerMockito.mock(EntityManager.class);
        Whitebox.setInternalState(repository, entityManager);
        repository.findOrCreate("cro999.usef-example.com");

        ArgumentCaptor<CommonReferenceOperator> croCaptor = ArgumentCaptor.forClass(CommonReferenceOperator.class);
        Mockito.verify(entityManager, Mockito.times(1)).find(CommonReferenceOperator.class, "cro999.usef-example.com");
        Mockito.verify(entityManager, Mockito.times(1)).persist(croCaptor.capture());
        CommonReferenceOperator createdCro = croCaptor.getValue();
        assertNotNull(createdCro);
        assertEquals("cro999.usef-example.com", createdCro.getDomain());
    }

    @Test
    public void testFindOrCreateIsNotCreatingWhenFound() {
        entityManager = PowerMockito.mock(EntityManager.class);
        Whitebox.setInternalState(repository, entityManager);

        // stubbing
        PowerMockito.when(entityManager.find(CommonReferenceOperator.class, "cro999.usef-example.com"))
                .thenReturn(new CommonReferenceOperator("cro999.usef-example.com"));
        repository.findOrCreate("cro999.usef-example.com");

        Mockito.verify(entityManager, Mockito.times(1)).find(CommonReferenceOperator.class, "cro999.usef-example.com");
        Mockito.verify(entityManager, Mockito.times(0)).persist(Matchers.any(CommonReferenceOperator.class));
    }

    @BeforeClass
    public static void initTestFixture() throws Exception {
        // Get the entity manager for the tests.
        entityManagerFactory = Persistence.createEntityManagerFactory("test");
        entityManager = entityManagerFactory.createEntityManager();
    }

    @Before
    public void setUp() throws Exception {
        repository = new CommonReferenceOperatorRepository();

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
