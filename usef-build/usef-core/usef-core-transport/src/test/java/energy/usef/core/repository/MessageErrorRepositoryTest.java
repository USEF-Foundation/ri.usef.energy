/*
 * Copyright 2015 USEF Foundation
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

import static org.powermock.reflect.Whitebox.setInternalState;
import energy.usef.core.model.Message;
import energy.usef.core.model.MessageError;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the OutgoingMessageErrorRepository class.
 */
@RunWith(PowerMockRunner.class)
public class MessageErrorRepositoryTest {

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private MessageErrorRepository repository;

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
        repository = new MessageErrorRepository();
        repository.setEntityManager(entityManager);
        setInternalState(repository, "entityManager", entityManager);
    }

    /**
     * Tests MessageService.storeMssagError method with null Message.
     */
    @Test(expected = PersistenceException.class)
    public void createOutgoingMessageErrorWithNullMessageTest() {
        MessageError messageError = createMessageError(null);
        repository.persist(messageError);
    }

    /*
     * Fills the required properties
     */
    public static MessageError createMessageError(Message message) {
        MessageError messageError = new MessageError();
        messageError.setMessage(message);
        return messageError;
    }
}
