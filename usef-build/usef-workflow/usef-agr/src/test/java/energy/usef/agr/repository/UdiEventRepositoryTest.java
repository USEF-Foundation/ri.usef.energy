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

import energy.usef.agr.model.UdiEvent;
import energy.usef.agr.model.UdiEventType;
import energy.usef.core.util.DateTimeUtil;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link UdiEventRepository} class.
 */
public class UdiEventRepositoryTest {

    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;
    private UdiEventRepository repository;

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
    public void before() {
        repository = new UdiEventRepository();
        setInternalState(repository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
        entityManager.getTransaction().begin();
    }

    @After
    public void after() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    @Test
    public void testSimpleFind() {
        String id = "2033689c-b040-437e-a052-11e0c14fdbac";
        UdiEvent udiEvent = repository.find(id);
        Assert.assertNotNull(udiEvent);
        Assert.assertEquals(id, udiEvent.getId());
        Assert.assertEquals(DateTimeUtil.parseDate("2015-10-05"), udiEvent.getPeriod());
        Assert.assertEquals(1, udiEvent.getUdi().getId().intValue());
        Assert.assertEquals(3, udiEvent.getDeviceCapabilities().size());
        Assert.assertEquals(UdiEventType.CONSUMPTION, udiEvent.getUdiEventType());
        Assert.assertEquals(1, udiEvent.getStartDtu().intValue());
        Assert.assertEquals(4, udiEvent.getEndDtu().intValue());
        Assert.assertNull(udiEvent.getDeviceSelector());
        Assert.assertNull(udiEvent.getFinishBeforeDtu());
        Assert.assertNull(udiEvent.getStartAfterDtu());
    }

    @Test
    public void testFindUdiEventsForPeriod() {
        final LocalDate period = new LocalDate(2015,10,5);
        List<UdiEvent> udiEvents = repository.findUdiEventsForPeriod(period);
        Assert.assertNotNull(udiEvents);
        Assert.assertEquals(2, udiEvents.size());
    }

    @Test
    public void testFindUdiEventsForPeriodWithNoResults() {
        final LocalDate period = new LocalDate(2015,10,6);
        List<UdiEvent> udiEvents = repository.findUdiEventsForPeriod(period);
        Assert.assertNotNull(udiEvents);
        Assert.assertEquals(0, udiEvents.size());
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("2015-12-01")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("2015-12-01")));
    }

}
