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

import energy.usef.agr.model.IncreaseRequest;

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

public class IncreaseRequestRepositoryTest {
    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;
    private IncreaseRequestRepository repository;

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
        repository = new IncreaseRequestRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    @Test
    public void testSimpleFind() {
        String id = "14834256-1a15-444e-a95e-9d4bffd1f946";
        IncreaseRequest increaseRequest = repository.find(id);
        Assert.assertNotNull(increaseRequest);
        Assert.assertEquals("14834256-1a15-444e-a95e-9d4bffd1f946", increaseRequest.getId());
        Assert.assertEquals(new LocalDate(2015, 6, 11), increaseRequest.getPeriod());
        Assert.assertNotNull(increaseRequest.getDeviceMessage());
        Assert.assertEquals("8664cebc-e8a9-4fd2-ac29-cbe81a879469", increaseRequest.getEventId());
        Assert.assertEquals(1, increaseRequest.getStartDtu().intValue());
        Assert.assertEquals(48, increaseRequest.getEndDtu().intValue());
        Assert.assertEquals(BigInteger.valueOf(1000L), increaseRequest.getPower());
    }
}
