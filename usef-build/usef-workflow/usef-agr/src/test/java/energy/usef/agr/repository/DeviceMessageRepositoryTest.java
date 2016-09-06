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

import energy.usef.agr.model.DeviceMessage;
import energy.usef.agr.model.DeviceMessageStatus;
import org.joda.time.LocalDate;
import org.junit.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;

import static org.powermock.reflect.Whitebox.setInternalState;

public class DeviceMessageRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private DeviceMessageRepository repository;

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
        repository = new DeviceMessageRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
        entityManager.getTransaction().begin();
    }

    @After
    public void tearDown() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    @Test
    public void testFindAllDeviceMessages() {
        List<DeviceMessage> deviceMessages = repository.findDeviceMessages();
        Assert.assertNotNull(deviceMessages);
        Assert.assertEquals(4, deviceMessages.size());
    }

    @Test
    public void testFindDeviceMessages() throws Exception {
        List<DeviceMessage> deviceMessages = repository.findDeviceMessages("ean.673685900012623654",
                DeviceMessageStatus.NEW);
        Assert.assertNotNull(deviceMessages);
        Assert.assertEquals(3   , deviceMessages.size());
    }

    @Test
    public void testCleanup() {
        try {
            repository.cleanup(new LocalDate());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
