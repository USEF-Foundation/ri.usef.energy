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
import javax.persistence.PersistenceException;

import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import energy.usef.core.model.FlexOrderSettlement;

/**
 * Test class in charge of the unit tests related to the {@link FlexOrderSettlementRepository} class.
 */
public class FlexOrderSettlementRepositoryTest {

    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;

    private FlexOrderSettlementRepository repository;

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
        repository = new FlexOrderSettlementRepository();
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
    public void testIsEachFlexOrderReadyForSettlement() throws Exception {
        final int year = 2014;
        final int month = 11;
        Boolean ready = repository.isEachFlexOrderReadyForSettlement(year, month);
        Assert.assertTrue(ready);
    }

    @Test
    public void testIsEachFlexOrderReadyForSettlementIsFalse() throws Exception {
        final int year = 2015;
        final int month = 2;
        Boolean ready = repository.isEachFlexOrderReadyForSettlement(year, month);
        Assert.assertTrue(ready);
    }

    @Test
    public void testFindFlexOrderSettlementsForPeriod() {
        final LocalDate startDate = new LocalDate(2015, 1, 1);
        final LocalDate endDate = new LocalDate(2015, 1, 31);
        List<FlexOrderSettlement> flexOrderSettlements = repository.findFlexOrderSettlementsForPeriod(startDate, endDate,
                Optional.empty(), Optional.empty());
        Assert.assertNotNull(flexOrderSettlements);
        Assert.assertEquals(0, flexOrderSettlements.size());
    }

    @Test
    public void testFindFlexOrderSettlementsForPeriodWithResults() {
        final LocalDate startDate = new LocalDate(2015, 2, 1);
        final LocalDate endDate = new LocalDate(2015, 2, 28);
        String connectionGroup = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        String participantDomain = "usef-example2.com";
        List<FlexOrderSettlement> flexOrderSettlementsForPeriod = repository.findFlexOrderSettlementsForPeriod(startDate,
                endDate,
                Optional.of(connectionGroup),
                Optional.of(participantDomain));
        Assert.assertEquals(1, flexOrderSettlementsForPeriod.size());
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-31")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-31")));
    }

    @Test
    public void testCleanupNotAllowed() {
        try {
            repository.cleanup(new LocalDate("1999-12-30"));
        } catch (PersistenceException e) {
            Assert.assertEquals("org.hibernate.exception.ConstraintViolationException: could not execute statement", e.getMessage());
        }
    }

}
