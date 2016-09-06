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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import energy.usef.core.util.DateTimeUtil;
import energy.usef.dso.model.GridSafetyAnalysis;

/**
 * Test class in charge of the unit tests related to the {@link GridSafetyAnalysisRepository} class.
 */
public class GridSafetyAnalysisRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private GridSafetyAnalysisRepository repository;

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
    public void before() {
        repository = new GridSafetyAnalysisRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
        entityManager.getTransaction().begin();
    }

    @After
    public void after() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    @Test
    public void testFindLatestGridSafetyAnalysis() {
        String congestionPointEntityAddress = "ean.123456789012345678";
        LocalDate ptuDate = new LocalDate(2014, 11, 28);

        List<GridSafetyAnalysis> gridSafetyAnalysis = repository
                .findGridSafetyAnalysisWithDispositionRequested(congestionPointEntityAddress, ptuDate);
        Assert.assertNotNull("Did not expect a null grid safety analysis list.", gridSafetyAnalysis);
    }

    @Test
    public void testFindGridSafetyAnalysisWithPrognosisList() {
        String congestionPointEntityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";
        LocalDate ptuDate = new LocalDate(2014, 11, 20);

        List<GridSafetyAnalysis> gridSafetyAnalysis = repository
                .findGridSafetyAnalysisWithDispositionRequested(congestionPointEntityAddress, ptuDate);
        Assert.assertTrue("Expect not empty prognosis list related to the fount Grid Safety Analysis table.", !gridSafetyAnalysis
                .get(0).getPrognoses().isEmpty());
    }

    @Test
    public void testFindGridSafetyAnalysisRelatedToFlexOffers() {
        List<GridSafetyAnalysis> gridSafetyAnalysis = repository.findGridSafetyAnalysisRelatedToFlexOffers(DateTimeUtil
                .parseDate("2014-11-19"));
        Assert.assertTrue(!gridSafetyAnalysis.isEmpty());
    }

    @Test
    public void testFindPreviousGridSafetyAnalysis() {
        String congestionPointEntityAddress = "ean.123456789012345678";
        LocalDate ptuDate = new LocalDate(2014, 11, 29);
        List<GridSafetyAnalysis> previousGridSafetyAnalysis = repository
                .findGridSafetyAnalysis(congestionPointEntityAddress, ptuDate);
        Assert.assertNotNull(previousGridSafetyAnalysis);

    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-29")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-29")));
    }

    @Test
    public void testDeletePreviousGridSafetyAnalysis() {
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";
        Assert.assertEquals("Expected no deleted objects", 0, repository.deletePreviousGridSafetyAnalysis(entityAddress, new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.deletePreviousGridSafetyAnalysis(entityAddress, new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.deletePreviousGridSafetyAnalysis(entityAddress, new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected deleted objects", 1, repository.deletePreviousGridSafetyAnalysis(entityAddress, new LocalDate("1999-12-29")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.deletePreviousGridSafetyAnalysis(entityAddress, new LocalDate("1999-12-29")));
    }

}
