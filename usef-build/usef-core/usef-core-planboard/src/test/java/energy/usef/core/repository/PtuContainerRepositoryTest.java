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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.PhaseType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.util.DateTimeUtil;

/**
 * JUnit test for the GridPointRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
@PrepareForTest(DateTimeUtil.class)
public class PtuContainerRepositoryTest {
    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private PtuContainerRepository repository;

    @Mock
    private Config config;

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
        repository = new PtuContainerRepository();
        setInternalState(repository, "entityManager", entityManager);
        Whitebox.setInternalState(repository, config);

        Mockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);

        PowerMockito.mockStatic(DateTimeUtil.class);
        PowerMockito.when(DateTimeUtil.getCurrentDateTime()).thenReturn(new LocalDate("2000-01-01").toLocalDateTime(
                LocalTime.MIDNIGHT));
        PowerMockito.when(DateTimeUtil.getCurrentDate()).thenReturn(new LocalDate("2000-01-01"));
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
    public void testFindPtuContainer() {
        PtuContainer ptuContainer = repository.findPtuContainer(new LocalDate("2014-11-20"), 1);
        Assert.assertNotNull(ptuContainer);
        Assert.assertEquals(1, ptuContainer.getPtuIndex().intValue());
        Assert.assertEquals(new LocalDate("2014-11-20"), ptuContainer.getPtuDate());
    }

    /**
     * Tests for PTUContainerRepository.getPTUContainer method.
     */
    @Test
    public void findPtuContainersMap() {
        LocalDate ptuDate = new LocalDate("2014-11-20");
        Map<Integer, PtuContainer> ptuContainerMap = repository.findPtuContainersMap(ptuDate);
        assertNotNull(ptuContainerMap);
        Assert.assertEquals(new LocalDate("2014-11-20"), ptuContainerMap.get(1).getPtuDate());
        Assert.assertEquals(1, ptuContainerMap.get(1).getPtuIndex().intValue());

        ptuDate = new LocalDate("2014-11-22");
        ptuContainerMap = repository.findPtuContainersMap(ptuDate);
        assertTrue(ptuContainerMap.isEmpty());
    }

    @Test
    public void testFindPtuContainersForDocumentSequence() {
        List<PtuContainer> ptuContainers = repository.findPtuContainersForDocumentSequence(20141215113500000l, PtuFlexOffer.class);
        Assert.assertNotNull("Did not expect a null list.", ptuContainers);
        Assert.assertEquals("Mismatch with the size of the list.", 1, ptuContainers.size());
    }

    @Test
    public void testSyntaxPtuContainersStateWithNoPtuIndex() {
        int result = repository.updatePtuContainersState(PtuContainerState.DayAheadClosedValidate, new LocalDate(2014, 11, 20),
                null);
        Assert.assertEquals(4, result);
    }

    @Test
    public void testSyntaxUpdatePtuContainersStateWithPtuIndex() {
        int result = repository.updatePtuContainersPhase(PhaseType.Plan, new LocalDate(2014, 11, 20), 1);
        Assert.assertEquals(1, result);
    }

    @Test
    public void testUpdatePtuContainersStateWithPtuIndex() {
        int result = repository.updatePtuContainersState(PtuContainerState.DayAheadClosedValidate, new LocalDate(2014, 11, 20), 1);
        Assert.assertEquals(1, result);
    }

    @Test
    public void testNoUpdatePtuContainersStateWithPtuIndex() {
        int result = repository.updatePtuContainersState(PtuContainerState.DayAheadClosedValidate, new LocalDate(2014, 11, 20), 4);
        Assert.assertEquals(0, result);
    }

    @Test
    public void testNoUpdatePtuContainersPhaseWithWrongDate() {
        int result = repository.updatePtuContainersState(PtuContainerState.DayAheadClosedValidate, new LocalDate(2014, 11, 19), 2);
        Assert.assertEquals(0, result);
    }

    @Test
    public void testGetPTUContainerWithPhases() {
        LocalDate ptuDate = new LocalDate("2014-11-20");

        List<PtuContainer> ptuContainers = repository.findPtuContainers(ptuDate, PhaseType.Plan, PhaseType.Validate);
        Assert.assertEquals(3, ptuContainers.size());

        ptuContainers = repository.findPtuContainers(ptuDate, PhaseType.Operate);
        Assert.assertEquals(0, ptuContainers.size());
    }

    @Test
    public void testFindInitializedDaysOfPlanboard() {
        List<LocalDate> dates = repository.findInitializedDaysOfPlanboard();
        Assert.assertNotNull(dates);
        Assert.assertEquals(2, dates.size());
        Assert.assertEquals(new LocalDate("2014-02-22"), dates.get(0));
        Assert.assertEquals(new LocalDate("2014-11-20"), dates.get(1));
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 8, repository.cleanup(new LocalDate("1999-12-31")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-31")));
    }

    @Test (expected = PersistenceException.class)
    public void testCleanupNotAllowed() {
        try {
            repository.cleanup(new LocalDate("1999-12-30"));
        } catch (PersistenceException e) {
            Assert.assertEquals("org.hibernate.exception.ConstraintViolationException: could not execute statement", e.getMessage());
            throw e;
        }
    }
}
