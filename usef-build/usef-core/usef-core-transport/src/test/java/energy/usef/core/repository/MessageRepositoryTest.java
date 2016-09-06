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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.apache.commons.codec.digest.DigestUtils;
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

import energy.usef.core.model.Message;
import energy.usef.core.model.MessageDirection;
import energy.usef.core.model.MessageType;

/**
 * JUnit test for the IngoingMessageRepository class.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.persistence.*")
public class MessageRepositoryTest {

    /**
     * The factory that produces entity manager.
     */
    private static EntityManagerFactory entityManagerFactory;
    /**
     * The entity manager that persists and queries the DB.
     */
    private static EntityManager entityManager;

    private MessageRepository repository;

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
        repository = new MessageRepository();
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

    /**
     * Tests IngoingMessageRepository.getMessageResponseByConversationId method.
     *
     * @throws Exception
     */
    @Test
    public void getMessageResponseByConversationId() throws Exception {
        String conversationId = "conversationId";
        // SetUp
        Message messageIn = createMessage();
        messageIn.setDirection(MessageDirection.INBOUND);
        messageIn.setConversationId(conversationId);
        repository.persist(messageIn);

        Message messageOut = createMessage();
        messageOut.setDirection(MessageDirection.OUTBOUND);
        messageOut.setConversationId(conversationId);
        repository.persist(messageOut);

        Message foundMessage = repository.getMessageResponseByConversationId(messageIn.getConversationId());

        assertNotNull(foundMessage);
        assertEquals(messageIn.getId(), foundMessage.getId());

    }

    @Test
    public void testisMessageIdAlreadyUsed() {
        String conversationId = "conversationId";
        String messageId = "12345678-1234-1234-1234567890ab";
        // SetUp
        Message newMessage = createMessage();
        newMessage.setDirection(MessageDirection.INBOUND);
        newMessage.setConversationId(conversationId);
        newMessage.setMessageId(messageId);
        repository.persist(newMessage);

        boolean foundMessage = repository.isMessageIdAlreadyUsed(messageId);
        assertNotNull(foundMessage);
        assertTrue(foundMessage);
    }

    @Test
    public void testGetMessageByMessagId() {
        String conversationId = "conversationId";
        String messageId = "12345678-1234-1234-1234567890ac";
        // SetUp
        Message newMessage = createMessage();
        newMessage.setDirection(MessageDirection.OUTBOUND);
        newMessage.setConversationId(conversationId);
        newMessage.setMessageId(messageId);
        repository.persist(newMessage);
        Message fountMessage = repository.getMessageResponseByMessageId(messageId, MessageDirection.OUTBOUND);
        Assert.assertEquals(conversationId, fountMessage.getConversationId());
    }

    /**
     * Tests Message required messageType.
     */
    @Test(expected = PersistenceException.class)
    public void createIngoingMessageWithNullMessageTypeTest() {
        Message message = createMessage();
        message.setMessageType(null);
        repository.persist(message);
    }

    /**
     * Tests IngoingMessageService.createIngoingMessage method with null message id.
     */
    @Test(expected = PersistenceException.class)
    public void createIngoingMessageWithNullMessageIdTest() {
        Message message = createMessage();
        message.setMessageId(null);
        repository.persist(message);
    }

    /**
     * Tests IngoingMessageService.createIngoingMessage method with null hash content.
     */
    @Test(expected = PersistenceException.class)
    public void createIngoingMessageWithNullContentHashTest() {
        Message message = createMessage();
        message.setContentHash(null);
        repository.persist(message);
    }

    /**
     * Expects to have the first message of a conversation.
     */
    @Test
    public void testGetInitialMessageOfConversationSucceeds() {
        String conversationId = "12345678-1234-1234-1234-1234567890ab";
        String messageId = "12345678-1234-1234-1234-1234567890ab";
        // SetUp
        Message newMessage = createMessage();
        newMessage.setDirection(MessageDirection.OUTBOUND);
        newMessage.setConversationId(conversationId);
        newMessage.setMessageId(messageId);
        repository.persist(newMessage);

        // actual test
        Message message = repository.getInitialMessageOfConversation(conversationId);
        Assert.assertNotNull("Expected non null message.", message);
        Assert.assertEquals("Message ID mismatch.", messageId, message.getMessageId());
        Assert.assertEquals("Conversation ID mismatch.", conversationId, message.getConversationId());

    }

    /**
     * Expectes to have <code>null</code> as a result of the query.
     */
    @Test
    public void testGetInitialMessageOfConversationFailsOnEmptyConversation() {
        Message message = repository.getInitialMessageOfConversation("12345678-1234-1234-1234-1234567890ab");
        Assert.assertNull("Excpected a null message.", message);
    }

    @Test
    public void testHasEveryCommonReferenceQuerySentAResponseReceivedIsTrue() {
        boolean result = repository.hasEveryCommonReferenceQuerySentAResponseReceived(
                new LocalDate(2014, 11, 20).toDateTimeAtCurrentTime().toLocalDateTime());
        Assert.assertTrue(result);
    }

    @Test
    public void testHasEveryCommonReferenceQuerySentAResponseReceivedIsFalse() {
        boolean result = repository.hasEveryCommonReferenceQuerySentAResponseReceived(
                new LocalDate(2014, 11, 21).toDateTimeAtCurrentTime().toLocalDateTime());
        Assert.assertFalse(result);
    }

    /*
     * Fills the required properties
     */
    public static Message createMessage() {
        Message message = new Message();
        message.setCreationTime(new LocalDateTime());
        message.setXml("<xml/>");
        message.setMessageId(UUID.randomUUID().toString());
        message.setMessageType(MessageType.ROUTINE);
        message.setContentHash(DigestUtils.sha256("Hash of the message"));
        return message;
    }

    @Test
    public void testCleanup() {
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate()));
        Assert.assertEquals("Expected deleted objects", 2, repository.cleanup(new LocalDate("2014-11-20")));
        Assert.assertEquals("Expected no deleted objects", 0, repository.cleanup(new LocalDate("2014-11-20")));
    }

    @Test (expected = PersistenceException.class)
    public void testCleanupNotAllowed() {
        try {
            repository.cleanup(new LocalDate("2014-11-22"));
        } catch (PersistenceException e) {
            Assert.assertEquals("org.hibernate.exception.ConstraintViolationException: could not execute statement", e.getMessage());
            throw e;
        }
    }

}
