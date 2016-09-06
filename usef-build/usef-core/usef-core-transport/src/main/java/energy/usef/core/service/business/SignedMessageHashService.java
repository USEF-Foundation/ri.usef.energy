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

package energy.usef.core.service.business;

import energy.usef.core.model.SignedMessageHash;
import energy.usef.core.repository.SignedMessageHashRepository;
import energy.usef.core.util.DateTimeUtil;

import java.util.Arrays;

import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Business Service class in charge of the operations related to the {@link SignedMessageHash} entity.
 *
 */
@Stateless
public class SignedMessageHashService {

    @Inject
    private SignedMessageHashRepository repository;

    /**
     * Checks whether a byte array with the hash of a incoming signed message is not in the database already.
     *
     * @param hashedContent - byte array.
     * @return <code>true</code> if the hash is already there. <code>false</code> otherwise.
     */
    public boolean isSignedMessageHashAlreadyPresent(byte[] hashedContent) {
        return repository.isSignedMessageHashAlreadyPresent(hashedContent);
    }

    /**
     * Creates a new {@link SignedMessageHash} entity in the database.
     *
     * @param hashedContent - the byte array with the hashe content one wants to store.
     */
    public void createSignedMessageHash(byte[] hashedContent) {
        if (hashedContent == null) {
            throw new IllegalArgumentException("Cannot create a new entry for a null hashed content");
        }
        SignedMessageHash signedMessageHash = new SignedMessageHash();
        signedMessageHash.setCreationTime(DateTimeUtil.getCurrentDateTime());
        signedMessageHash.setHashedContent(Arrays.copyOf(hashedContent, hashedContent.length));
        repository.persist(signedMessageHash);
    }
}
