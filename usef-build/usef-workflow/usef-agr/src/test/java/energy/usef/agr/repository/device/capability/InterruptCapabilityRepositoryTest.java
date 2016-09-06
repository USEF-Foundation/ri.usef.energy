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

package energy.usef.agr.repository.device.capability;

import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.agr.model.device.capability.InterruptCapability;
import energy.usef.agr.model.device.capability.InterruptCapabilityType;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link InterruptCapabilityRepository} class.
 */
public class InterruptCapabilityRepositoryTest {
    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;
    private InterruptCapabilityRepository repository;

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
        repository = new InterruptCapabilityRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    @Test
    public void testSimpleFind() {
        final String id = "bccc1e59-3b08-4c1a-ba42-631d73e24a12";
        InterruptCapability interruptCapability = repository.find(id);
        Assert.assertNotNull(interruptCapability);
        Assert.assertNotNull(interruptCapability.getUdiEvent());
        Assert.assertEquals(id, interruptCapability.getId());
        Assert.assertEquals("2033689c-b040-437e-a052-11e0c14fdbac", interruptCapability.getUdiEvent().getId());
        Assert.assertEquals(InterruptCapabilityType.FULL, interruptCapability.getType());
        Assert.assertEquals(8, interruptCapability.getMaxDtus().intValue());
    }
}
