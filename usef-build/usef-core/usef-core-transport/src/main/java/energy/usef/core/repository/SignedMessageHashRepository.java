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

import energy.usef.core.model.SignedMessageHash;

import javax.ejb.Stateless;

/**
 * Repository class for operations related to the {@link SignedMessageHash} entity.
 *
 */
@Stateless
public class SignedMessageHashRepository extends BaseRepository<SignedMessageHash> {

    /**
     * Default constructor.
     */
    public SignedMessageHashRepository() {
        super();
    }

    /**
     * Checks whether the hashed content of a signed message is already present in the database.
     *
     * @param hashedContent - byte array
     * @return <code>true</code> if the hash is present. Otherwise, <code>false</code>
     */
    public boolean isSignedMessageHashAlreadyPresent(byte[] hashedContent) {
        Long signedMessageHashes = (Long) entityManager
                .createQuery("SELECT COUNT(smh) FROM SignedMessageHash smh WHERE smh.hashedContent = :hashedContent")
                .setParameter("hashedContent", hashedContent)
                .getSingleResult();
        return signedMessageHashes != null && signedMessageHashes == 1;

    }

}
