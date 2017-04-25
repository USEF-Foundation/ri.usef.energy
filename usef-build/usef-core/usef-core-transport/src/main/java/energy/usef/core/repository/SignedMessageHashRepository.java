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

import java.sql.Date;

import javax.ejb.Stateless;

import org.joda.time.LocalDate;

import energy.usef.core.model.SignedMessageHash;

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

    /**
     * Delete all {@link SignedMessageHash}s for a certain date.
     *
     * @param period
     * @return the number of {@link SignedMessageHash}s deleted.
     */
    public int cleanup(LocalDate period) {
        LocalDate endDate = period.plusDays(1);

        Date start = new Date(period.toDateMidnight().getMillis());
        Date end = new Date(endDate.toDateMidnight().getMillis());

        String sql = "DELETE FROM SignedMessageHash o WHERE o.creationTime >= :start AND o.creationTime < :end";
        return entityManager.createQuery(sql).setParameter("start", start).setParameter("end", end).executeUpdate();
    }
}
