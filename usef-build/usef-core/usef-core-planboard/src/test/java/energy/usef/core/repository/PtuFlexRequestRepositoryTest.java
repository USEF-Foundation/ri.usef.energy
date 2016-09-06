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

import energy.usef.core.model.PtuFlexRequest;
import energy.usef.core.util.DateTimeUtil;

/**
 * JUnit test for the GridPointRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class PtuFlexRequestRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private PtuFlexRequestRepository repository;

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
        repository = new PtuFlexRequestRepository();
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
     * Tests for GridPointRepository.getGridPointByEntityAddress method.
     */
    @Test
    public void findLastFlexRequestDocumentWithDispositionRequested() {
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        PtuFlexRequest flexRequest = repository.findLastFlexRequestDocumentWithDispositionRequested(entityAddress,
                DateTimeUtil.parseDate("2014-11-20"), 10002L);
        Assert.assertNotNull(flexRequest);
        Assert.assertEquals(new Long(7L), flexRequest.getId());
    }

    @Test
    public void testFindPtuFlexRequestWithSequence() throws Exception {
        final String connectionGroupUsefIdentifier = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        final Long sequenceNumber = 10002l;
        final String participantDomain = "abc4.com";
        List<PtuFlexRequest> ptuFlexRequests = repository.findPtuFlexRequestWithSequence(connectionGroupUsefIdentifier,
                sequenceNumber, participantDomain);
        Assert.assertNotNull(ptuFlexRequests);
        Assert.assertEquals(3, ptuFlexRequests.size());
    }

    @Test
    public void testFindPtuPrognosisSequencesByFlexOfferSequences() {
        Long sequence = repository.findPtuPrognosisSequenceByFlexOfferSequence(20140222112000014L, "agr.usef-example.com");
        Assert.assertEquals(Long.valueOf("20140222112000010"), sequence);
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-30")));
    }

}
