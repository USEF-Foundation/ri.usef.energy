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

package energy.usef.agr.repository;

import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.agr.model.ShiftRequest;

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
 * Test class in charge of the unit tests related to the {@link ShiftRequestRepository} class.
 */
public class ShiftRequestRepositoryTest {

    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;
    private ShiftRequestRepository repository;

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
    public void setUp() {
        repository = new ShiftRequestRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    @Test
    public void testSimpleFind() {
        String id = "b52f1a3d-c3b9-4212-b940-0325773cffe3";
        ShiftRequest shiftRequest = repository.find(id);
        Assert.assertNotNull(shiftRequest);
        Assert.assertEquals("b52f1a3d-c3b9-4212-b940-0325773cffe3", shiftRequest.getId());
        Assert.assertEquals(new LocalDate(2015, 6, 11), shiftRequest.getPeriod());
        Assert.assertNotNull(shiftRequest.getDeviceMessage());
        Assert.assertEquals("dc533490-68e8-11e5-a837-0800200c9a66", shiftRequest.getEventId());
        Assert.assertEquals(49, shiftRequest.getStartDtu().intValue());
    }
}
