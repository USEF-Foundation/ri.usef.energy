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

import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.repository.CongestionPointConnectionGroupRepository;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link AggregatorOnConnectionGroupStateRepository} class.
 */
public class AggregatorOnConnectionGroupStateRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private AggregatorOnConnectionGroupStateRepository repository;
    private CongestionPointConnectionGroupRepository congestionPointConnectionGroupRepository;
    private static LocalDate localdateTime = new LocalDate();

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
        repository = new AggregatorOnConnectionGroupStateRepository();
        congestionPointConnectionGroupRepository = new CongestionPointConnectionGroupRepository();

        setInternalState(repository, "entityManager", entityManager);
        setInternalState(congestionPointConnectionGroupRepository, "entityManager", entityManager);
        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
        congestionPointConnectionGroupRepository.getEntityManager().clear();

    }

    /**
     * Tests AggregatorRepository.getAggregatorsByCongestionPointAddress method.
     */
    @Test
    public void testGetAggregatorsByCongestionPointAddress() {
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";
        List<Aggregator> list = repository.getAggregatorsByCongestionPointAddress(entityAddress, DateTimeUtil.getCurrentDate());
        Assert.assertEquals(2, list.size());
    }

    /**
     * Tests AggregatorRepository.findStateForAggregatorOnConnectionGroupTest method. AggregatorOnConnectionGroupState [1971..null>
     */
    @Test
    public void testFindStateForAggregatorOnConnectionGroup1() {
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";
        CongestionPointConnectionGroup congestionPoint = congestionPointConnectionGroupRepository.find(entityAddress);
        Aggregator aggregator = new Aggregator();
        aggregator.setDomain("agr1.usef-example.com");
        AggregatorOnConnectionGroupState aggregatorOnConnectionGroupState = repository.findStateForAggregatorOnConnectionGroup(
                aggregator, congestionPoint, localdateTime);
        Assert.assertNotNull(aggregatorOnConnectionGroupState);
    }

    /**
     * Tests AggregatorRepository.findStateForAggregatorOnConnectionGroupTest method.
     * <p>
     * AggregatorOnConnectionGroupState [1971..2020>
     */
    @Test
    public void testFindStateForAggregatorOnConnectionGroup2() {
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";
        CongestionPointConnectionGroup congestionPoint = congestionPointConnectionGroupRepository.find(entityAddress);
        Aggregator aggregator = new Aggregator();
        aggregator.setDomain("agr2.usef-example.com");
        AggregatorOnConnectionGroupState aggregatorOnConnectionGroupState = repository.findStateForAggregatorOnConnectionGroup(
                aggregator, congestionPoint, localdateTime);
        Assert.assertNotNull(aggregatorOnConnectionGroupState);
    }

    /**
     * Tests AggregatorRepository.findStateForAggregatorOnConnectionGroupTest method.
     * <p>
     * AggregatorOnConnectionGroupState [1971..1990>
     */
    @Test
    public void testFindStateForAggregatorOnConnectionGroup3() {
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";
        CongestionPointConnectionGroup congestionPoint = congestionPointConnectionGroupRepository.find(entityAddress);
        Aggregator aggregator = new Aggregator();
        aggregator.setDomain("agr3.usef-example.com");
        AggregatorOnConnectionGroupState aggregatorOnConnectionGroupState = repository.findStateForAggregatorOnConnectionGroup(
                aggregator, congestionPoint, localdateTime);
        Assert.assertNull(aggregatorOnConnectionGroupState);
    }

    /**
     * Tests AggregatorRepository.findStateForAggregatorOnConnectionGroupTest method.
     * <p>
     * AggregatorOnConnectionGroupState [2020..2021>
     */
    @Test
    public void testFindStateForAggregatorOnConnectionGroup4() {
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";
        CongestionPointConnectionGroup congestionPoint = congestionPointConnectionGroupRepository.find(entityAddress);
        Aggregator aggregator = new Aggregator();
        aggregator.setDomain("agr4.usef-example.com");
        AggregatorOnConnectionGroupState aggregatorOnConnectionGroupState = repository.findStateForAggregatorOnConnectionGroup(
                aggregator, congestionPoint, localdateTime);
        Assert.assertNull(aggregatorOnConnectionGroupState);
    }

    /**
     * Tests AggregatorRepository.findStateForAggregatorOnConnectionGroupTest method.
     * <p>
     * AggregatorOnConnectionGroupState [2020..2021>
     */
    @Test
    public void testFindStateForAggregatorOnConnectionGroup5() {
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7";
        CongestionPointConnectionGroup congestionPoint = congestionPointConnectionGroupRepository.find(entityAddress);
        Aggregator aggregator = new Aggregator();
        aggregator.setDomain("agr5.usef-example.com");
        AggregatorOnConnectionGroupState aggregatorOnConnectionGroupState = repository.findStateForAggregatorOnConnectionGroup(
                aggregator, congestionPoint, localdateTime);
        Assert.assertNull(aggregatorOnConnectionGroupState);
    }

    @Test
    public void testFindConnectionGroupsWithAggregators() {
        Map<CongestionPointConnectionGroup, List<AggregatorOnConnectionGroupState>> connectionGroupsWithAggregators = repository
                .findConnectionGroupsWithAggregators(new LocalDate("2015-07-03"));
        Assert.assertNotNull(connectionGroupsWithAggregators);
        Assert.assertEquals(1, connectionGroupsWithAggregators.keySet().size());
        Assert.assertEquals(2, connectionGroupsWithAggregators
                .get(new CongestionPointConnectionGroup("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7")).size());
    }

    @Test
    public void testCountActiveAggregatorsForCongestionPointOnDay() {
        long result = repository.countActiveAggregatorsForCongestionPointOnDay(
                "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7", new LocalDate("2015-07-03"));

        Assert.assertEquals(2L, result);
    }

    @Test
    public void testFindAggregatorsWithOverlappingActivityForPeriod() {
        final LocalDate startDate = new LocalDate("1969-12-30");
        final LocalDate endDate = new LocalDate("1970-01-02");
        List<AggregatorOnConnectionGroupState> states = repository
                .findAggregatorsWithOverlappingActivityForPeriod(startDate, endDate);
        Assert.assertNotNull(states);
        Assert.assertEquals(3, states.size());
    }

    @Test
    public void testFindAggregatorOnConnectionGroupStateByCongestionPointAddress() {
        List<AggregatorOnConnectionGroupState> aggregatorOnConnectionGroupStateByCongestionPointAddress = repository
                .findAggregatorOnConnectionGroupStateByCongestionPointAddress(
                        "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7", new LocalDate("2015-07-03"));

        Assert.assertNotNull(aggregatorOnConnectionGroupStateByCongestionPointAddress);
        Assert.assertEquals(2, aggregatorOnConnectionGroupStateByCongestionPointAddress.size());
    }

    @Test
    public void testFindEndingAggregatorOnConnectionGroupStates() {
        List<AggregatorOnConnectionGroupState> endingAggregatorOnConnectionGroupStates = repository
                .findEndingAggregatorOnConnectionGroupStates("ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e7",
                        new LocalDate("2050-01-01"));

        Assert.assertNotNull(endingAggregatorOnConnectionGroupStates);
        Assert.assertEquals(1, endingAggregatorOnConnectionGroupStates.size());
    }
}
