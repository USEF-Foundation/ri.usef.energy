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

import energy.usef.agr.model.ReduceRequest;

import java.math.BigInteger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ReduceRequestRepositoryTest {
    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;
    private ReduceRequestRepository repository;

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
        repository = new ReduceRequestRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    @Test
    public void testSimpleFind() {
        String id = "fff81edc-afb2-4acc-bc7e-b35237674b15";
        ReduceRequest reduceRequest = repository.find(id);
        Assert.assertNotNull(reduceRequest);
        Assert.assertEquals("fff81edc-afb2-4acc-bc7e-b35237674b15", reduceRequest.getId());
        Assert.assertEquals(new LocalDate(2015, 6, 11), reduceRequest.getPeriod());
        Assert.assertNotNull(reduceRequest.getDeviceMessage());
        Assert.assertEquals("b22d7a5b-a4f7-4962-b0f6-d6ea8258af31", reduceRequest.getEventId());
        Assert.assertEquals(49, reduceRequest.getStartDtu().intValue());
        Assert.assertEquals(96, reduceRequest.getEndDtu().intValue());
        Assert.assertEquals(BigInteger.valueOf(-1000L), reduceRequest.getPower());
    }
}
