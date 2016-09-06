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

import energy.usef.agr.model.device.capability.IncreaseCapability;

import java.math.BigInteger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link IncreaseCapabilityRepository} class.
 */
public class IncreaseCapabilityRepositoryTest {

    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;
    private IncreaseCapabilityRepository repository;

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
        repository = new IncreaseCapabilityRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
    }

    @Test
    public void testSimpleFind() {
        final String id = "4b920da5-5f8d-46c3-9a8b-689441f1d36f";
        IncreaseCapability increaseCapability = repository.find(id);
        Assert.assertNotNull(increaseCapability);
        Assert.assertNotNull(increaseCapability.getUdiEvent());
        Assert.assertEquals(id, increaseCapability.getId());
        Assert.assertEquals("148c6c12-4f09-4d38-9d9d-463d6c23b36a", increaseCapability.getUdiEvent().getId());
        Assert.assertEquals(8, increaseCapability.getMaxDtus().intValue());
        Assert.assertEquals(BigInteger.valueOf(200L), increaseCapability.getPowerStep());
        Assert.assertEquals(BigInteger.valueOf(3000L), increaseCapability.getMaxPower());
        Assert.assertEquals(1, increaseCapability.getDurationMultiplier().intValue());
    }
}
