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

package energy.usef.cro.repository;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.cro.model.Aggregator;
import energy.usef.cro.model.CongestionPoint;
import energy.usef.cro.model.Connection;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the CongestionPointRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class CongestionPointRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private CongestionPointRepository repository;

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
        repository = new CongestionPointRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    /**
     * Tests for CongestionPointRepository.getCongestionPointByEntityAddress method.
     */
    @Test
    public void testGetCongestionPointByExistentEntityAddress() {
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        entityManager.getTransaction().begin();
        CongestionPoint congestionPoint = repository.getCongestionPointByEntityAddress(entityAddress);
        entityManager.getTransaction().commit();
        assertNotNull("Existent CongestionPoint", congestionPoint);
    }

    /**
     * Tests for CongestionPointRepository.getCongestionPointByEntityAddress method.
     */
    @Test
    public void testGetCongestionPointByNonExitentEntityAddress() {
        String entityAddress = "XX0.000-00.XXX.XXXXXXX:XXXXXXXXX.00000000-0000-0000-0000";
        entityManager.getTransaction().begin();
        CongestionPoint congestionPoint = repository.getCongestionPointByEntityAddress(entityAddress);
        entityManager.getTransaction().commit();
       assertNull("Noneistent CongestionPoint", congestionPoint);
    }

    /**
     * Test whether 1 specific {@link CongestionPoint} related to 2 specific {@link Connection} is returned with a given
     * entityAddress.
     */
    @Test
    public void testFindCongestionPointsByAGR() {
        String agrDomain = "tesla.com";
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        Map<CongestionPoint, Set<Connection>> results = repository.findConnectionsForCongestionPointsByAGR(agrDomain,
                entityAddress);
        assertNotNull(results);
        Assert.assertEquals(1, results.size());

        CongestionPoint congestionPoint = results.keySet().iterator().next();
        Assert.assertEquals(2, results.get(congestionPoint).size());
    }

    /**
     * Test whether all {@link CongestionPoint}'s are returned whitout a given EntityAddress.
     */
    @Test
    public void testFindCongestionPointsByAGRWithoutEntityAddress() {
        String agrDomain = "tesla.com";
        Map<CongestionPoint, Set<Connection>> results = repository.findConnectionsForCongestionPointsByAGR(agrDomain, null);
        assertNotNull(results);
        Assert.assertEquals(2, results.size());

        for (CongestionPoint congestionPoint : results.keySet()) {
            // congestionPoint with 1 connections who have an aggregator
            if (congestionPoint.getEntityAddress().equals("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6")) {
                Assert.assertEquals(2, results.get(congestionPoint).size());
                for (Connection connection : results.get(congestionPoint)) {
                    Assert.assertEquals(agrDomain, connection.getAggregator().getDomain());
                }
            } else if (congestionPoint.getEntityAddress()
                    .equals("ea1.1992-02.com.otherexample:gridpoint.4f76ff19-a53b-49f5-99e9")) {
                Assert.assertEquals(1, results.get(congestionPoint).size());
                for (Connection connection : results.get(congestionPoint)) {
                    Assert.assertEquals(agrDomain, connection.getAggregator().getDomain());
                }
            }
        }
    }

    /**
     * Test whether 1 specific {@link CongestionPoint} is returned with a given entityAddress.
     */
    @Test
    public void testFindCongestionPointsByDSO() {
        String dsoDomain = "usef-example.com";
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        Map<CongestionPoint, Map<Aggregator, Long>> results = repository.findAggregatorCountForCongestionPointsByDSO(dsoDomain,
                entityAddress);
        assertNotNull(results);
        Assert.assertEquals(1, results.size());
    }

    /**
     * Test whether all {@link CongestionPoint}'s are returned whitout a given EntityAddress.
     */
    @Test
    public void testFindCongestionPointsByDSOWithoutEntityAddress() {
        String dsoDomain = "usef-example.com";
        Map<CongestionPoint, Map<Aggregator, Long>> results = repository
                .findAggregatorCountForCongestionPointsByDSO(dsoDomain, null);
        assertNotNull(results);
        Assert.assertEquals(2, results.size());

        for (CongestionPoint congestionPoint : results.keySet()) {
            // congestionPoint with 2 connections who have an aggregator
            if (congestionPoint.getEntityAddress().equals("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6")) {
                Assert.assertEquals(3, results.get(congestionPoint).size());
                for (Entry<Aggregator, Long> entry : results.get(congestionPoint).entrySet()) {
                    if (entry.getKey() == null) {
                        Assert.assertEquals(2l, entry.getValue().longValue());
                        continue;
                    }
                    if (entry.getKey().getDomain().equals("tesla.com")) {
                        // tesla has 2 connections
                        Assert.assertEquals(new Long(2), entry.getValue());
                    }
                    if (entry.getKey().getDomain().equals("mijn-groene-energie.com")) {
                        // mijn-groene-energie.com has 1 connection
                        Assert.assertEquals(new Long(1), entry.getValue());
                    }
                }
            }
            // congestionpoint with 1 connection who has an aggregator
            if (congestionPoint.getEntityAddress().equals("ea1.1992-03.com.otherexample:gridpoint.4f76ff19-a53b-49f5-99e9")) {
                Assert.assertEquals(1, results.get(congestionPoint).size());
            }
        }
    }

    /**
     * Test whether an empty list is returned in case of no results.
     */
    @Test
    public void testFindCongestionPointsByDSOIncorrect() {
        String dsoDomain = "usef-doesnotexist.com";
        Map<CongestionPoint, Map<Aggregator, Long>> results = repository
                .findAggregatorCountForCongestionPointsByDSO(dsoDomain, null);
        assertNotNull(results);
    }

    @Test
    public void findCongestionPointForAgr() {
        String agrDomain = "tesla.com";
        List<CongestionPoint> result = repository.findCongestionPointsForAggregator(agrDomain);
        assertNotNull(result);
    }
}
