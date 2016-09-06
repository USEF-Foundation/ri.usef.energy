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
import java.util.Map;

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

import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.util.DateTimeUtil;

/**
 * Test class in charge of the unit tests related to the {@link PtuFlexOfferRepository}.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class PtuFlexOfferRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private PtuFlexOfferRepository repository;

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
    public void init() {
        repository = new PtuFlexOfferRepository();
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
    public void testFindFlexOfferSucceeds() {
        Map<Integer, PtuFlexOffer> offers = repository.findPtuFlexOffer(20141215113500000l, "agr1.usef-example.com");
        Assert.assertFalse(offers.isEmpty());
        Assert.assertNotNull("Did not expect a null flex offer.", offers);
    }

    @Test
    public void testFindFlexOfferReturnsNull() {
        Map<Integer, PtuFlexOffer> offers = repository.findPtuFlexOffer(20141215113400000l, "agr1.usef-example.com");
        Assert.assertNotNull("Did not expect a null flex offer.", offers);
        Assert.assertTrue("Did expect an empty list of flex offers.", offers.isEmpty());
    }

    @Test
    public void testFindFlexOfferBasedOnFlexOrderSucceeds() {
        Map<Integer, PtuFlexOffer> ptuFO = repository
                .findPtuFlexOffer(201412221525l, "agr1.usef-example.com");
        Assert.assertNotNull("PtuFlexOffer should not be null", ptuFO.get(1));

    }

    @Test
    public void testFindFlexOfferBasedOnFlexOrderReturnsNull() {
        Map<Integer, PtuFlexOffer> ptuFO = repository
                .findPtuFlexOffer(20141215113500000l, "flexoffer.usef-example.com");
        Assert.assertNull(ptuFO.get(1));
    }

    @Test
    public void testFindPtuFlexOffersOfAcceptedOrdersForPeriod() {
        //variables
        final LocalDate startDate = new LocalDate(2014, 11, 1);
        final LocalDate endDate = new LocalDate(2014, 11, 30);
        // actual invocation
        Map<String, Map<String, Map<LocalDate, Map<Integer, List<PtuFlexOffer>>>>> ptuFlexOffersMap = repository
                .findPtuFlexOffersOfAcceptedOrdersForPeriod(startDate, endDate);
        // validations
        Assert.assertNotNull(ptuFlexOffersMap);
        Assert.assertEquals(1, ptuFlexOffersMap.size());
        Assert.assertEquals(1, ptuFlexOffersMap.get("agr1.usef-example.com").size());
        Assert.assertEquals(1,
                ptuFlexOffersMap.get("agr1.usef-example.com").get("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6")
                        .size());
        // 2 ptus
        Assert.assertEquals(2,
                ptuFlexOffersMap.get("agr1.usef-example.com").get("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6")
                        .get(new LocalDate(2014, 11, 20)).size());
        // values for PTU 2 and 3
        Assert.assertNotNull(
                ptuFlexOffersMap.get("agr1.usef-example.com").get("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6")
                        .get(new LocalDate(2014, 11, 20)).get(2).get(0));
        Assert.assertNotNull(
                ptuFlexOffersMap.get("agr1.usef-example.com").get("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6")
                        .get(new LocalDate(2014, 11, 20)).get(3).get(0));
    }

    @Test
    public void testFindPtuFlexOffersOfAcceptedOrdersForPeriodByDomainSequencePeriodAndPtuIndex() {
        //variables
        final LocalDate startDate = new LocalDate(2014, 11, 1);
        final LocalDate endDate = new LocalDate(2014, 11, 30);
        // actual invocation
        Map<String, Map<Long, Map<LocalDate, Map<Integer, PtuFlexOffer>>>> ptuFlexOffersMap = repository
                .findPtuFlexOffersOfAcceptedOrdersForPeriodByDomainSequencePeriodAndPtuIndex(startDate, endDate);
        // validations
        Assert.assertNotNull(ptuFlexOffersMap);
        Assert.assertEquals(1, ptuFlexOffersMap.size());
        Assert.assertEquals(2, ptuFlexOffersMap.get("agr1.usef-example.com").size());
        Assert.assertEquals(1, ptuFlexOffersMap.get("agr1.usef-example.com").get(20141215113500000l).size());
        // 1 ptu
        Assert.assertEquals(1,
                ptuFlexOffersMap.get("agr1.usef-example.com").get(20141215113500000l).get(new LocalDate(2014, 11, 20)).size());
        // values for PTU 2
        Assert.assertNotNull(
                ptuFlexOffersMap.get("agr1.usef-example.com").get(20141215113500000l).get(new LocalDate(2014, 11, 20)).get(2));
    }

    @Test
    public void testFindFlexOffersWithOrderInPeriod() {
        // variables
        final LocalDate period = DateTimeUtil.parseDate("2014-11-20");
        // invocation
        List<PtuFlexOffer> flexOffersWithOrderInPeriod = repository.findFlexOffersWithOrderInPeriod(period);
        // verification
        Assert.assertNotNull(flexOffersWithOrderInPeriod);
        Assert.assertEquals(2, flexOffersWithOrderInPeriod.size());
    }

    @Test
    public void testFindPlacedFlexOffers() {
        // variables and constants
        final LocalDate period = new LocalDate(2014, 11, 20);
        // actual invocation
        List<PtuFlexOffer> placedFlexOffers = repository.findPlacedFlexOffers(period);
        // verifications and assertions
        Assert.assertNotNull(placedFlexOffers);
        Assert.assertEquals(1, placedFlexOffers.size());
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-30")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-30")));
    }

}
