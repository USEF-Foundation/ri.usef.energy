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
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.util.DateTimeUtil;

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
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the GridPointRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class ConnectionGroupRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private ConnectionGroupRepository repository;

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
        repository = new ConnectionGroupRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    /**
     * Tests for GridPointRepository.getGridPointByEntityAddress method.
     */
    @Test
    public void testGetGridPointByEntityAddress() {
        String entityAddress = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        ConnectionGroup connectionGroup = repository.find(entityAddress);
        assertNotNull(connectionGroup);
    }

    @Test
    public void testFindAllGridPoints() {
        List<ConnectionGroup> connectionGroups = repository.findAll();
        assertNotNull(connectionGroups);
        Assert.assertEquals(4, connectionGroups.size());
    }

    @Test
    public void testFindAllForDateTimeFindsSuccessfull() {
        LocalDate period = DateTimeUtil.getCurrentDate();
        List<ConnectionGroup> connectionGroups = repository.findAllForDateTime(period);
        Assert.assertEquals(2, connectionGroups.size());
    }

}
