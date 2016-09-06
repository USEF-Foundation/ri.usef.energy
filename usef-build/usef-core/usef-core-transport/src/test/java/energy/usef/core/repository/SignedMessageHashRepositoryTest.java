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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.setInternalState;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import energy.usef.core.model.SignedMessageHash;
import energy.usef.core.util.DateTimeUtil;

public class SignedMessageHashRepositoryTest {
    private static final String HELLO_USEF_CONTENT = "Hello USEF content";

    /** The factory that produces entity manager. */
    private static EntityManagerFactory entityManagerFactory;
    /** The entity manager that persists and queries the DB. */
    private static EntityManager entityManager;

    private SignedMessageHashRepository repository;

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
        repository = new SignedMessageHashRepository();
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
    public void testIsSignedContentHashAlreadyPresent() {

        SignedMessageHash signedMessageHash = new SignedMessageHash();
        signedMessageHash.setCreationTime(DateTimeUtil.getCurrentDateTime());
        signedMessageHash.setHashedContent(DigestUtils.sha256(HELLO_USEF_CONTENT));
        repository.persist(signedMessageHash);
        repository.getEntityManager().getTransaction().commit();

        boolean foundMessage = repository.isSignedMessageHashAlreadyPresent(DigestUtils.sha256(HELLO_USEF_CONTENT));
        assertTrue(foundMessage);

        // cleanup
        repository.getEntityManager().getTransaction().begin();
        repository.delete(signedMessageHash);
        repository.getEntityManager().getTransaction().commit();
    }

    @Test
    public void testIsSignedContentHashNotPresent() {
        boolean foundMessage = repository.isSignedMessageHashAlreadyPresent(DigestUtils.sha256(HELLO_USEF_CONTENT));
        assertFalse(foundMessage);
    }
}
