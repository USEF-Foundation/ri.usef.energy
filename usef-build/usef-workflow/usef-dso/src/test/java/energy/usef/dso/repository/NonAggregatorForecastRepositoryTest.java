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

import java.text.ParseException;
import java.util.List;
import java.util.Optional;

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
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import energy.usef.core.util.DateTimeUtil;
import energy.usef.dso.model.NonAggregatorForecast;

/**
 * JUnit test for the {@Link NonAggregatorForecastRepository} class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class NonAggregatorForecastRepositoryTest {
    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private NonAggregatorForecastRepository repository;

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
        repository = new NonAggregatorForecastRepository();
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
    /**
     * Tests NonAggregatorForecastRepository.getLastNonAggregatorForecast method.
     */
    @Test
    public void testGetLastNonAggregatorForecast() {
        NonAggregatorForecast last = repository.getLastNonAggregatorForecast();
        Assert.assertEquals(new Long(20), last.getPower());
    }

    @Test
    public void testGetLastNonAggregatorForecasts() throws ParseException {
        LocalDate startDate = DateTimeUtil.parseDate("2014-11-20");
        List<NonAggregatorForecast> list = repository.getLastNonAggregatorForecasts(startDate, Optional.empty());
        Assert.assertTrue(!list.isEmpty());

        startDate = DateTimeUtil.parseDate("2014-11-20");
        list = repository.getLastNonAggregatorForecasts(startDate, Optional.empty());
        Assert.assertTrue(!list.isEmpty());

        startDate = DateTimeUtil.parseDate("2015-10-20");
        list = repository.getLastNonAggregatorForecasts(startDate, Optional.empty());
        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void testGetLastNonAggregatorForecastsForPtu() {
        LocalDate ptuDate = DateTimeUtil.parseDate("2014-11-20");
        Integer ptuIndex = 1;
        List<NonAggregatorForecast> list = repository.getLastNonAggregatorForecasts(ptuDate, ptuIndex);
        Assert.assertNotNull(list);
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-30")));
    }
}
