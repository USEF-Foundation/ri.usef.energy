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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.hibernate.PropertyValueException;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import energy.usef.dso.model.PrognosisUpdateDeviation;

import java.math.BigInteger;
import java.util.Date;

/**
 * Test class in charge of the unit tests related to the {@link PrognosisUpdateDeviationRepository} class.
 */
public class PrognosisUpdateDeviationRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private PrognosisUpdateDeviationRepository repository;

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
        repository = new PrognosisUpdateDeviationRepository();
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
    public void testSimpleFind() {
        PrognosisUpdateDeviation prognosisUpdateDeviation = repository.find(-1l);
        Assert.assertNotNull("Did not expect a null result.", prognosisUpdateDeviation);
        Assert.assertEquals("ID value mismatch.", -1l, prognosisUpdateDeviation.getId().longValue());
        Assert.assertEquals("Prognosis sequence value mismatch.", 20150116141712042l, prognosisUpdateDeviation.getPrognosisSequence()
                .longValue());
        Assert.assertEquals("Aggregator domain value mismatch.", "agr.usef-example.com", prognosisUpdateDeviation.getAggregatorDomain());
        Assert.assertEquals("Ordered power value mismatch.", 1000l, prognosisUpdateDeviation.getOrderedPower().longValue());
        Assert.assertEquals("Prognosed power value mismatch.", 750l, prognosisUpdateDeviation.getPrognosedPower().longValue());
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("2014-11-20")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("2014-11-20")));
    }


    // Explicit test for mandatory PtuDate after bugfix.
    @Test (expected = PersistenceException.class)
    public void testMandatoryPtuDate() {
        PrognosisUpdateDeviation prognosisUpdateDeviation = new PrognosisUpdateDeviation();
        prognosisUpdateDeviation.setAggregatorDomain("usef-example.com");
        prognosisUpdateDeviation.setPrognosisSequence(-11L);
        prognosisUpdateDeviation.setOrderedPower(BigInteger.ONE);
        prognosisUpdateDeviation.setPreviousPrognosedPower(BigInteger.ONE);
        prognosisUpdateDeviation.setPrognosedPower(BigInteger.ONE);
        prognosisUpdateDeviation.setPtuIndex(1);
        entityManager.persist(prognosisUpdateDeviation);
    }

    // Explicit test for mandatory PtuIndex after bugfix.
    @Test (expected = PersistenceException.class)
    public void testMandatoryPtuIndex() {
        PrognosisUpdateDeviation prognosisUpdateDeviation = new PrognosisUpdateDeviation();
        prognosisUpdateDeviation.setAggregatorDomain("usef-example.com");
        prognosisUpdateDeviation.setPrognosisSequence(-11L);
        prognosisUpdateDeviation.setOrderedPower(BigInteger.ONE);
        prognosisUpdateDeviation.setPreviousPrognosedPower(BigInteger.ONE);
        prognosisUpdateDeviation.setPrognosedPower(BigInteger.ONE);
        prognosisUpdateDeviation.setPtuDate(new Date());
        entityManager.persist(prognosisUpdateDeviation);
    }

}
