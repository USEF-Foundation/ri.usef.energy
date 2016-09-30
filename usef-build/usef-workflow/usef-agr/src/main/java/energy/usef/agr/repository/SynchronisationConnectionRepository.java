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

import energy.usef.agr.model.SynchronisationConnection;
import energy.usef.agr.model.SynchronisationConnectionStatus;
import energy.usef.agr.model.SynchronisationConnectionStatusType;
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
     * Gets all the connections.
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
    @SuppressWarnings("unchecked")
    public void updateConnectionStatusForCRO(String croDomain) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" SELECT status ");
        queryBuilder.append(" FROM SynchronisationConnectionStatus status ");
        queryBuilder.append(" WHERE status.commonReferenceOperator.domain = :croDomain  ");
        queryBuilder
                .append("   AND status.synchronisationConnection.lastSynchronisationTime > status.synchronisationConnection.lastModificationTime  ");
        Query query = entityManager.createQuery(queryBuilder.toString());
        query.setParameter("croDomain", croDomain);
        List<SynchronisationConnectionStatus> results = query.getResultList();
        for (SynchronisationConnectionStatus status : results) {
            status.setStatus(SynchronisationConnectionStatusType.SYNCHRONIZED);
        }
    }

    /**
     * Deletes all {@link SynchronisationConnection} entities.
     */
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM SynchronisationConnectionStatus").executeUpdate();
        entityManager.createQuery("DELETE FROM SynchronisationConnection").executeUpdate();
    }

    /**
     * Gets Synchronisation Connection entity by its entity address.
     *
     * @param entityAddress SynchronisationConnection entity address
     *
     * @return SynchronisationConnection entity
     */
    @SuppressWarnings("unchecked")
    public SynchronisationConnection findByEntityAddress(String entityAddress) {

        List<SynchronisationConnection> result = entityManager
                .createQuery(
                        "SELECT sc FROM SynchronisationConnection sc WHERE sc.entityAddress = :entityAddress")
                .setParameter("entityAddress", entityAddress).getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Deletes {@Link SynchronisationConnection} entity by its entity address.
     *
     * @param domain commonReferenceOperator domain
     */
    @SuppressWarnings("unchecked")
    public void deleteByEntityAddress(String domain) {
        SynchronisationConnection synchronisationConnection = findByEntityAddress(domain);
        if (synchronisationConnection != null) {
            entityManager.remove(synchronisationConnection);
        }
    }
}
