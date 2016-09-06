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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;

/**
 * JUnit test for the PlanboardMessageRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class PlanboardMessageRepositoryTest {
    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private PlanboardMessageRepository repository;
    private ConnectionGroupRepository connectionGroupRepository;

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
        repository = new PlanboardMessageRepository();
        setInternalState(repository, "entityManager", entityManager);

        connectionGroupRepository = new ConnectionGroupRepository();
        setInternalState(connectionGroupRepository, "entityManager", entityManager);

        // clear the entity manager to avoid unexpected results
        repository.getEntityManager().clear();
        connectionGroupRepository.getEntityManager().clear();
        entityManager.getTransaction().begin();
    }

    @After
    public void after() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    @Test
    public void testFindLastAPlanPlanboardMessages() {
        LocalDate period = new LocalDate(2015, 1, 20);
        List<PlanboardMessage> results = repository.findLastAPlanPlanboardMessages(period);
        assertTrue(results.size() == 6);
    }

    @Test
    public void testFindNewRequests() {
        List<PlanboardMessage> results = repository.findPlanboardMessages(DocumentType.FLEX_REQUEST, DocumentStatus.SENT);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindNewRequestsOlderThan() {
        List<PlanboardMessage> results = repository.findPlanboardMessagesOlderThan(new LocalDateTime("2014-11-20T01:00"),
                DocumentType.METER_DATA_QUERY_USAGE, DocumentStatus.SENT);
        assertEquals(1, results.size());
    }

    @Test
    public void testFindPlanboardMessagesWithOriginSequence() {
        List<PlanboardMessage> planboardMessages = repository.findPlanboardMessagesWithOriginSequence(2222221l,
                DocumentType.FLEX_OFFER, "usef-example.com");
        assertNotNull(planboardMessages);
        assertEquals(2, planboardMessages.size());
    }

    @Test
    public void testFindFlexOrdersRelatedToPrognosis() {
        List<PlanboardMessage> orders = repository.findFlexOrdersRelatedToPrognosis(1111111l, "usef-example.com");
        assertNotNull(orders);
        assertEquals(2, orders.size());
    }

    @Test
    public void testFindPlanboardMessages() {
        List<PlanboardMessage> messages = repository.findPlanboardMessages(DocumentType.FLEX_ORDER, "usef-example.com",
                "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6", null);
        assertNotNull(messages);
        assertEquals(2, messages.size());
    }

    /**
     * Tests PlanboardMessageRepository.findPlanboardMessages method.
     */
    @Test
    public void testFindPlanboardMessagesBySequence() {
        Long sequence = 3333331L;
        List<PlanboardMessage> result = repository.findPlanboardMessages(sequence, DocumentType.FLEX_OFFER, "usef-example.com");
        assertTrue(result.size() > 0);

        result = repository.findPlanboardMessages(sequence, DocumentType.FLEX_ORDER, "usef-example.com");
        assertTrue(result.size() == 0);
    }

    @Test
    public void testFindMaxPlanboardMessageSequence() {
        Long maxSequence = repository.findMaxPlanboardMessageSequence(DocumentType.D_PROGNOSIS, "dso.usef-example.com",
                new LocalDate(2015, 3, 31), "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6", null);
        assertEquals(3333331, maxSequence.longValue());

        maxSequence = repository.findMaxPlanboardMessageSequence(DocumentType.D_PROGNOSIS, "dso.usef-example.com",
                new LocalDate(2015, 3, 31),
                "ea.not.existing", null);
        assertEquals(0, maxSequence.longValue());
    }

    public void testFindPlanboardMessagesWithPeriod() {
        List<PlanboardMessage> messages = repository.findPlanboardMessages(DocumentType.FLEX_ORDER, "usef-example.com",
                "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6", null, new LocalDate(2015, 1, 13),
                new LocalDate(2015, 1, 17));
        assertNotNull(messages);
        assertEquals(1, messages.size());
    }

    @Test
    public void testUpdateOldSettlementMessageDisposition() {
        int updatedRecords = repository.updateOldSettlementMessageDisposition();
        assertEquals(1, updatedRecords);
    }

    @Test
    public void testFindSinglePlanboardMessage() throws Exception {
        PlanboardMessage planboardMessage = repository.findSinglePlanboardMessage(4444442L, DocumentType.FLEX_ORDER,
                "usef-example.com");
        assertNotNull(planboardMessage);
    }

    @Test
    public void testFindSinglePlanboardMessageByDate() throws Exception {
        PlanboardMessage planboardMessage = repository
                .findSinglePlanboardMessage(new LocalDate("2015-01-20"), DocumentType.METER_DATA_QUERY_USAGE,
                        "mdc.usef-example.com");
        assertNotNull(planboardMessage);
    }

    @Test
    public void testFindPrognosisRelevantForDateAndUsefIdentifierForAggregator() {
        List<PlanboardMessage> planboardMessages = repository
                .findPrognosisRelevantForDateByUsefIdentifier(new LocalDate(2015, 3, 31), null, null);
        assertEquals(planboardMessages.size(), 7);
        assertTrue(planboardMessages.get(0).getDocumentType().equals(DocumentType.D_PROGNOSIS));
    }

    @Test
    public void testFindPrognosisRelevantForDate() {
        List<PlanboardMessage> planboardMessages = repository.findPrognosisRelevantForDate(new LocalDate(2015, 3, 31),
                "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6");
        assertEquals(3, planboardMessages.size());
        assertTrue(planboardMessages.get(0).getDocumentType().equals(DocumentType.D_PROGNOSIS));
    }

    @Test
    public void testFindPrognosisRelevantForDateAndUsefIdentifierForDso() {
        String connectionGroupIdentifier = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        List<PlanboardMessage> planboardMessages = repository
                .findPrognosisRelevantForDateByUsefIdentifier(new LocalDate(2015, 3, 31), connectionGroupIdentifier,
                        "dso.usef-example.com");
        assertEquals(2, planboardMessages.size());
    }

    @Test
    public void testFindPrognosisRelevantForDateAndUsefIdentifierForBrp() {
        String connectionGroupIdentifier = "brp.test.com";
        List<PlanboardMessage> planboardMessages = repository
                .findPrognosisRelevantForDateByUsefIdentifier(new LocalDate(2015, 3, 31), connectionGroupIdentifier,
                        "agr.usef-example.com");
        assertEquals(1, planboardMessages.size());
    }

    @Test
    public void testFindPlanboardMessagesByMultipleDocumentStatusses() {
        List<PlanboardMessage> planboardMessages = repository.findPlanboardMessages(4444442L, DocumentType.FLEX_ORDER,
                "usef-example.com",
                "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6", DocumentStatus.SENT, DocumentStatus.ACCEPTED);

        assertFalse(planboardMessages.isEmpty());

    }

    @Test
    public void testFindOrderableFlexOffers() {
        List<PlanboardMessage> planboardMessages = repository.findOrderableFlexOffers();
        // flex offer is in the past
        assertEquals(0, planboardMessages.size());
    }

    @Test
    public void testFindAPlanRelatedToFlexOffer() {
        PlanboardMessage aPlan = repository.findAPlanRelatedToFlexOffer(77777122L, "agr3.usef-example.com");
        assertNotNull(aPlan);
    }

    @Test
    public void testFindAcceptedPrognosisMessages() {
        List<PlanboardMessage> acceptedPrognosisMessages = repository.findAcceptedPlanboardMessagesForConnectionGroup(
                DocumentType.A_PLAN, new LocalDate("2015-03-31"), "brp.test.com");
        Assert.assertNotNull(acceptedPrognosisMessages);
        Assert.assertEquals(3, acceptedPrognosisMessages.size());
    }

    @Test
    public void testFindSinglePlanboardMessageForCongestionPoint() throws Exception {
        String connectionGroupIdentifier = "ea1.1992-01.com.example:gridpoint.4f76ff19-a53b-49f5-84e6";
        List<PlanboardMessage> planboardMessages = repository.findPlanboardMessages(4444442L, connectionGroupIdentifier,
                DocumentType.FLEX_ORDER);
        assertEquals(planboardMessages.size(), 1);
    }

    @Test
    public void testFindPlanboardMessages1() {
        List<PlanboardMessage> messages = repository.findPlanboardMessages(DocumentType.A_PLAN, new LocalDate(2015, 3, 31),
                new LocalDate(2015, 4, 1), DocumentStatus.ACCEPTED);
        Assert.assertNotNull(messages);
        Assert.assertEquals(3, messages.size());
    }
    @Test
    public void testFindPlanboardMessages2() {
        List<PlanboardMessage> messages = repository.findPlanboardMessages(DocumentType.A_PLAN, new LocalDate(2015, 3, 31),
                DocumentStatus.ACCEPTED);
        Assert.assertNotNull(messages);
        Assert.assertEquals(3, messages.size());
    }

    @Test
    public void testFindPlanboardMessages3() {
        List<PlanboardMessage> messages = repository.findPlanboardMessages(DocumentType.A_PLAN, "brp.usef-example.com", "brp.test.com", DocumentStatus.ACCEPTED, new LocalDate(2015, 3, 31), new LocalDate(2015, 4, 1));
        Assert.assertNotNull(messages);
        Assert.assertEquals(1, messages.size());
    }

    @Test
    public void testFindAcceptedPlanboardMessagesForConnectionGroup() {
        List<PlanboardMessage> messages = repository.findAcceptedPlanboardMessagesForConnectionGroup(DocumentType.A_PLAN,
                new LocalDate(2015, 3, 31), "brp.test.com");
        Assert.assertNotNull(messages);
        Assert.assertEquals(3, messages.size());
    }

    @Test
    public void testFindPlanboardMessagesWithOriginSequence2() {
        PlanboardMessage planboardMessage = repository.findPlanboardMessagesWithOriginSequence(3333332L,
                DocumentType.FLEX_ORDER, DocumentStatus.SENT);
        assertNotNull(planboardMessage);
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 1, repository.cleanup(new LocalDate("1999-12-31")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("1999-12-31")));
    }

    @Test (expected = PersistenceException.class)
    public void testCleanupNotAllowed() {
        try {
            repository.cleanup(new LocalDate("1999-12-29"));
        } catch (PersistenceException e) {
            Assert.assertEquals("org.hibernate.exception.ConstraintViolationException: could not execute statement", e.getMessage());
            throw e;
        }
    }
}
