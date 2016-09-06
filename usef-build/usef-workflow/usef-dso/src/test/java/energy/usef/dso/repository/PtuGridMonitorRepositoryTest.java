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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.dso.model.PtuGridMonitor;

/**
 * Test class in charge of the unit tests related to the {@link PtuGridMonitorRepository} class.
 */
public class PtuGridMonitorRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private PtuGridMonitorRepository repository;

    private PtuContainerRepository ptuContainerRepository;

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
        repository = new PtuGridMonitorRepository();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        setInternalState(repository, "entityManager", entityManager);
        setInternalState(repository, "sequenceGeneratorService", sequenceGeneratorService);

        ptuContainerRepository = new PtuContainerRepository();
        setInternalState(ptuContainerRepository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
        ptuContainerRepository.getEntityManager().clear();
        entityManager.getTransaction().begin();
    }

    @After
    public void after() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    @Test
    public void testSetActualPower() {
        String congestionPointEntityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";

        PtuContainer ptuContainer = ptuContainerRepository.findPtuContainer(new LocalDate(
                "2014-11-19"), 1);
        CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier(congestionPointEntityAddress);

        assertNotNull(ptuContainer);

        long power = repository.getActualPower(ptuContainer, congestionPoint);
        assertEquals(1250L, power);

        repository.setActualPower(ptuContainer, 1000L, congestionPoint);

        power = repository.getActualPower(ptuContainer, congestionPoint);
        assertEquals(1125L, power);
    }

    @Test
    public void testSetActualPowerEmptyGridMonitor() {
        String congestionPointEntityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e8";

        PtuContainer ptuContainer = ptuContainerRepository.findPtuContainer(new LocalDate("2014-11-19"), 1);
        CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier(congestionPointEntityAddress);

        repository.setActualPower(ptuContainer, 1000L, congestionPoint);

        long power = repository.getActualPower(ptuContainer, congestionPoint);
        assertEquals(1000L, power);
    }

    @Test
    public void findPtuGridMonitorsByDates() {
        LocalDate startDate = DateTimeUtil.parseDate("2014-11-19");
        LocalDate endDate = DateTimeUtil.parseDate("2014-12-20");
        List<PtuGridMonitor> results = repository.findPtuGridMonitorsByDates(startDate, endDate);
        assertTrue(results.size() > 0);

        startDate = DateTimeUtil.parseDate("2014-12-01");
        results = repository.findPtuGridMonitorsByDates(startDate, endDate);
        assertTrue(results.size() == 0);
    }

    @Test
    public void testFindLimitedPower() {
        String congestionPointEntityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";

        PtuContainer ptuContainer = ptuContainerRepository.findPtuContainer(new LocalDate("2014-11-19"), 1);
        CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier(congestionPointEntityAddress);

        Optional<Long> limitedPower = repository.findLimitedPower(ptuContainer, congestionPoint);
        Assert.assertEquals(1500l, limitedPower.get().longValue());
    }

    @Test
    public void testSetLimitedPower() {
        String congestionPointEntityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";

        PtuContainer ptuContainer = ptuContainerRepository.findPtuContainer(new LocalDate("2014-11-19"), 1);
        CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier(congestionPointEntityAddress);

        assertNotNull(ptuContainer);

        Optional<Long> power = repository.findLimitedPower(ptuContainer, congestionPoint);
        assertEquals(1500L, power.get().longValue());

        repository.setLimitedPower(ptuContainer, 2000L, congestionPoint);

        power = repository.findLimitedPower(ptuContainer, congestionPoint);
        assertEquals(2000L, power.get().longValue());
    }

    @Test
    public void testSetLimitedPowerEmptyGridMonitor() {
        String congestionPointEntityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e8";

        PtuContainer ptuContainer = ptuContainerRepository.findPtuContainer(new LocalDate("2014-11-19"), 1);
        CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier(congestionPointEntityAddress);

        assertNotNull(ptuContainer);

        Optional<Long> power = repository.findLimitedPower(ptuContainer, congestionPoint);
        assertTrue(!power.isPresent());

        repository.setLimitedPower(ptuContainer, 2000L, congestionPoint);

        power = repository.findLimitedPower(ptuContainer, congestionPoint);
        assertEquals(2000L, power.get().longValue());
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-30")));
    }
}
