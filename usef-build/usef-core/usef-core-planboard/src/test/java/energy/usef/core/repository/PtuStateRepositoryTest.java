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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuState;
import energy.usef.core.model.RegimeType;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.util.DateTimeUtil;

/**
 * JUnit test for the PtuStateRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class PtuStateRepositoryTest {
    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private PtuStateRepository repository;

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
        repository = new PtuStateRepository();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        setInternalState(repository, "entityManager", entityManager);
        setInternalState(repository, "sequenceGeneratorService", sequenceGeneratorService);

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
     * Tests PtuStateRepository.findPtuStates method.
     */
    @Test
    public void testFindPtuStates() {
        LocalDate ptuDate = DateTimeUtil.parseDate("2014-11-20");
        String usefIdentifier = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        List<PtuState> result = repository.findPtuStates(ptuDate, usefIdentifier);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getPtuContainer().getPtuIndex().intValue());
    }

    /**
     * Tests PtuStateRepository.findPtuStates method with regimes.
     */
    @Test
    public void testFindPtuStatesByRegimes() {
        LocalDate startDate = DateTimeUtil.parseDate("2013-11-20");
        LocalDate endDate = DateTimeUtil.parseDate("2020-11-20");

        List<PtuState> result = repository.findPtuStates(startDate, endDate, RegimeType.GREEN, RegimeType.ORANGE);
        assertEquals(5, result.size());

        result = repository.findPtuStates(startDate, endDate, RegimeType.ORANGE);
        assertEquals(0, result.size());
    }

    /**
     * Tests PtuStateRepository.findPtuStates method.
     */
    @Test
    public void testFindOrCreatePtuState() {
        LocalDate ptuDate = DateTimeUtil.parseDate("2014-11-20");
        int ptuIndex = 3;
        String usefIdentifier = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuDate(ptuDate);
        ptuContainer.setPtuIndex(ptuIndex);
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(usefIdentifier);
        PtuState result = repository.findOrCreatePtuState(ptuContainer, connectionGroup);
        assertNotNull(result);
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-30")));
    }

}
