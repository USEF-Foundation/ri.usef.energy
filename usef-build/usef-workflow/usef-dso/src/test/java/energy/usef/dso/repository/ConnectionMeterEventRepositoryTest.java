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
import energy.usef.core.model.Connection;
import energy.usef.dso.model.ConnectionMeterEvent;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConnectionMeterEventRepositoryTest {
    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private ConnectionMeterEventRepository repository;

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
        repository = new ConnectionMeterEventRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    @Test
    public void testFindConnectionMeterEventsForPeriod() throws Exception {
        List<ConnectionMeterEvent> meterEventsForPeriod = repository
                .findConnectionMeterEventsForPeriod(new LocalDate("2015-01-01"), new LocalDate("2015-01-31"));

        Assert.assertNotNull(meterEventsForPeriod);
        Assert.assertEquals(2, meterEventsForPeriod.size());
    }

    @Test
    public void testFindCollectionsNotRelatedToConnectionMeterEvents() throws Exception {
        List<Connection> connections = repository.findConnectionsNotRelatedToConnectionMeterEvents(new LocalDate("2015-01-31"),
                Arrays.asList("qbcy.com", "ancx.com"));

        Assert.assertEquals(1, connections.size());
        Assert.assertEquals("qbcy.com", connections.get(0).getEntityAddress());

        connections = repository.findConnectionsNotRelatedToConnectionMeterEvents(new LocalDate("2049-01-31"),
                Arrays.asList("qbcy.com", "ancx.com"));
        Assert.assertEquals(2, connections.size());

        connections = repository.findConnectionsNotRelatedToConnectionMeterEvents(new LocalDate("2099-01-31"),
                Arrays.asList("qbcy.com", "ancx.com"));
        Assert.assertEquals(0, connections.size());
    }

    @Test
    public void testFindConnectionForConnectionMeterEventsPeriod() {
        Connection connection = repository.findConnectionForConnectionMeterEventsPeriod("ancx.com", new LocalDate("2015-01-31"));
        Assert.assertNotNull(connection);
    }
}
