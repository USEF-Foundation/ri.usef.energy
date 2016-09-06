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

import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.util.DateTimeUtil;

/**
 * JUnit test for the GridPointRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class PtuFlexOrderRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private PtuFlexOrderRepository repository;

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
        repository = new PtuFlexOrderRepository();
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
     * Tests for GridPointRepository.findFlexOrdersBySequence method.
     */
    @Test
    public void findFlexOrdersBySequence() {
        Long sequence = 234579012L;
        List<PtuFlexOrder> results = repository.findFlexOrdersBySequence(sequence);
        Assert.assertTrue(results.size() > 0);

        results = repository.findFlexOrdersBySequence(sequence, AcknowledgementStatus.SENT);
        Assert.assertTrue(results.size() > 0);

        results = repository.findFlexOrdersBySequence(sequence, AcknowledgementStatus.NO_RESPONSE);
        Assert.assertTrue(results.size() == 0);
    }

    /**
     * Tests for GridPointRepository.findFlexOrdersByDates method.
     */
    @Test
    public void findFlexOrdersByDates() {
        LocalDate startDate = DateTimeUtil.parseDate("2013-11-20");
        LocalDate endDate = DateTimeUtil.parseDate("2014-11-20");
        List<PtuFlexOrder> results = repository.findFlexOrdersByDates(startDate, endDate);
        Assert.assertTrue(results.size() > 0);

        endDate = DateTimeUtil.parseDate("2014-11-01");
        results = repository.findFlexOrdersByDates(startDate, endDate);
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testFindAcknowledgedFlexOrdersForMonthInYear() {
        LocalDate workingDate = new LocalDate("2014-11-20");
        List<PtuFlexOrder> results = repository.findAcknowledgedFlexOrdersForMonthInYear(workingDate);
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());

        workingDate = new LocalDate("2014-11-15");
        results = repository.findAcknowledgedFlexOrdersForMonthInYear(workingDate);
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());

        workingDate = new LocalDate("2013-11-20");
        results = repository.findAcknowledgedFlexOrdersForMonthInYear(workingDate);
        Assert.assertNotNull(results);
        Assert.assertEquals(0, results.size());

        workingDate = new LocalDate("2014-12-20");
        results = repository.findAcknowledgedFlexOrdersForMonthInYear(workingDate);
        Assert.assertNotNull(results);
        Assert.assertEquals(0, results.size());
    }

    @Test
    public void testFindFlexOrderByDateAndUsefIdentifier() {
        LocalDate workingDate = new LocalDate(2014, 11, 20);
        List<PtuFlexOrder> results = repository.findAcceptedFlexOrdersByDateAndUsefIdentifier(
                Optional.of("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6"),
                workingDate);

        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-30")));
    }

}
