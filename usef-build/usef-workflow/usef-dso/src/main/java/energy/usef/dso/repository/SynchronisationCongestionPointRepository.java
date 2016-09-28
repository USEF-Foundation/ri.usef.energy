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

package energy.usef.dso.repository;

import energy.usef.core.repository.BaseRepository;
import energy.usef.dso.model.SynchronisationCongestionPoint;
import energy.usef.dso.model.SynchronisationCongestionPointStatus;
import energy.usef.dso.model.SynchronisationConnection;
import energy.usef.dso.model.SynchronisationConnectionStatusType;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

/**
 * Repository class for the {@link SynchronisationConnection} entity. This class provides methods to interact with the BRP database.
 */
@Stateless
public class SynchronisationCongestionPointRepository extends BaseRepository<SynchronisationCongestionPoint> {

    /**
     * Gets all the congestion points.
     *
     * @return a {@link java.util.List} of {@link SynchronisationCongestionPoint}.
     */
    public List<SynchronisationCongestionPoint> findAll() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cg ");
        sql.append("FROM SynchronisationCongestionPoint cg ");
        sql.append("  LEFT JOIN FETCH cg.statusses status ");
        sql.append("  LEFT JOIN FETCH status.commonReferenceOperator cro ");

        return entityManager.createQuery(sql.toString(), SynchronisationCongestionPoint.class).getResultList();
    }

    /**
     * Updates all CongestionPoints where the synchronisationTime is > as the lastModificationTime for this croDomain.
     *
     * @param congestionPointEntityAddress Congestion point entity address
     * @param croDomain CRO domain
     */
    public void updateCongestionPointStatusForCRO(String congestionPointEntityAddress, String croDomain) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" SELECT status ");
        queryBuilder.append(" FROM SynchronisationCongestionPointStatus status ");
        queryBuilder.append(" WHERE status.commonReferenceOperator.domain = :croDomain  ");
        queryBuilder.append("  AND status.synchronisationCongestionPoint.entityAddress = :entityAddress ");
        queryBuilder.append(
                "   AND status.synchronisationCongestionPoint.lastSynchronisationTime > status.synchronisationCongestionPoint"
                        + ".lastModificationTime  ");
        Query query = entityManager.createQuery(queryBuilder.toString(), SynchronisationCongestionPointStatus.class);
        query.setParameter("croDomain", croDomain);
        query.setParameter("entityAddress", congestionPointEntityAddress);
        @SuppressWarnings("unchecked")
        List<SynchronisationCongestionPointStatus> results = query.getResultList();
        for (SynchronisationCongestionPointStatus status : results) {
            if (status.getStatus() == SynchronisationConnectionStatusType.MODIFIED) {
                status.setStatus(SynchronisationConnectionStatusType.SYNCHRONIZED);
            }
            if (status.getStatus() == SynchronisationConnectionStatusType.DELETED) {
                status.setStatus(SynchronisationConnectionStatusType.DELETED_SYNCHRONIZED);
            }
        }
    }

    /**
     * Gets SynchronisationCongestionPoint entity by its entity address.
     *
     * @param entityAddress SynchronisationCongestionPoint entity address
     *
     * @return SynchronisationCongestionPoint entity
     */
    @SuppressWarnings("unchecked")
    public SynchronisationCongestionPoint findByEntityAddress(String entityAddress) {

        List<SynchronisationCongestionPoint> result = entityManager
                .createQuery(
                        "SELECT scp FROM SynchronisationCongestionPoint scp WHERE scp.entityAddress = :entityAddress")
                .setParameter("entityAddress", entityAddress).getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Deletes {@Link SynchronisationCongestionPoint} entity by its entity address.
     *
     * @param domain SynchronisationCongestionPoint entityAddress
     */
    @SuppressWarnings("unchecked")
    public void deleteByEntityAddress(String entityAddress) {
        SynchronisationCongestionPoint synchronisationCongestionPoint = findByEntityAddress(entityAddress);
        if (synchronisationCongestionPoint != null) {
            entityManager.remove(synchronisationCongestionPoint);
        }
    }

    /**
     * Deletes all {@link SynchronisationConnection} entities.
     */
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM SynchronisationCongestionPoint").executeUpdate();
    }
}
