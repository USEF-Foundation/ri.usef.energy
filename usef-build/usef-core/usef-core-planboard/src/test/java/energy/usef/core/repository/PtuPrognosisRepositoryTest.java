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

import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.util.DateTimeUtil;

/**
 * JUnit test for the PrognosisRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class PtuPrognosisRepositoryTest {
    /**
     *
     */
    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private PtuPrognosisRepository repository;

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
        repository = new PtuPrognosisRepository();
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
     * Tests PrognosisRepository.getBySequence method.
     */
    @Test
    public void testGetBySequence() {
        long prognosisSequence = 2014112611;
        Assert.assertNotNull(repository.findBySequence(prognosisSequence));
    }

    @Test
    public void testFindLastPrognosesForPeriodAndConnectionGroup() {
        List<PtuPrognosis> dPrognoses = repository
                .findLastPrognoses(DateTimeUtil.parseDate("2014-11-20"), Optional.of(PrognosisType.D_PROGNOSIS),
                        Optional.of(CONGESTION_POINT_ENTITY_ADDRESS), Optional.empty());

        Assert.assertNotNull(dPrognoses);
        Assert.assertEquals(3, dPrognoses.size());
    }

    @Test
    public void testFindLastPrognosisWithDocumentStatus() {
        List<PtuPrognosis> aplans = repository
                .findLastPrognoses(DateTimeUtil.parseDate("2014-11-20"), Optional.empty(), Optional.empty(),
                        Optional.of(DocumentStatus.RECEIVED));
        Assert.assertNotNull(aplans);
        Assert.assertEquals(2, aplans.size());
    }

    @Test
    public void testFindLastPrognosisWithPrognosisType() {
        List<PtuPrognosis> dPrognosis = repository
                .findLastPrognoses(DateTimeUtil.parseDate("2014-11-20"), Optional.of(PrognosisType.D_PROGNOSIS), Optional.empty(),
                        Optional.empty());
        Assert.assertNotNull(dPrognosis);
        Assert.assertEquals(3, dPrognosis.size());
    }

    @Test
    public void testFindPtuPrognosisForSequences() {
        List<PtuPrognosis> prognosis = repository.findPtuPrognosisForSequence(Long.valueOf("20140222112000010"),
                "agr.usef-example.com");

        Assert.assertEquals(1, prognosis.size());
    }

    @Test
    public void testFindPrognosesForSettlement() throws Exception {
        List<PtuPrognosis> plans = repository.findPrognosesForSettlement(new LocalDate("2014-11-01"), new LocalDate("2014-11-30"));
        Assert.assertNotNull(plans);
        Assert.assertEquals(2, plans.size());
    }

    @Test
    public void testFindPrognosesWithOrderInPeriod() {
        // variables
        final LocalDate period = new LocalDate(2014, 2, 22);
        // invocation
        List<PtuPrognosis> prognosesWithOrderInPeriod = repository.findPrognosesWithOrderInPeriod(period);
        // verifications
        Assert.assertEquals(2, prognosesWithOrderInPeriod.size());
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-30")));
    }

}
