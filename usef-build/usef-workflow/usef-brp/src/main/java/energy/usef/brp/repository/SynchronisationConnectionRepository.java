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

package energy.usef.brp.repository;

import energy.usef.brp.model.SynchronisationConnection;
import energy.usef.brp.model.SynchronisationConnectionStatusType;
import energy.usef.core.repository.BaseRepository;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

/**
 * Repository class for the {@link SynchronisationConnection} entity. This class provides methods to interact with the BRP database.
 */
@Stateless
public class SynchronisationConnectionRepository extends BaseRepository<SynchronisationConnection> {

    /**
     * Gets all the connection's.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<SynchronisationConnection> findAll() {
        return entityManager.createQuery("SELECT connection FROM SynchronisationConnection connection ")
                .getResultList();
    }

    /**
     * Updates all ConnectionStatusses where the synchronisationTime is > as the lastModificationTime for this croDomain.
     * 
     * @param croDomain
     */
    public void updateConnectionStatusForCRO(String croDomain) {
        Query query = entityManager.createQuery(
                "UPDATE SynchronisationConnectionStatus scs SET scs.status = :status " +
                        " WHERE scs.commonReferenceOperator = " +
                        "       (SELECT cro FROM CommonReferenceOperator cro " +
                        "         WHERE cro.domain = :croDomain) " +
                        " AND scs.synchronisationConnection IN " +
                        "       (SELECT sc FROM SynchronisationConnection sc " +
                        "         WHERE sc.lastSynchronisationTime > sc.lastModificationTime) ");
        query.setParameter("status", SynchronisationConnectionStatusType.SYNCHRONIZED);
        query.setParameter("croDomain", croDomain);
        query.executeUpdate();
    }

    /**
     * Deletes all {@link SynchronisationConnection} entities.
     */
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM SynchronisationConnection").executeUpdate();
    }
}
