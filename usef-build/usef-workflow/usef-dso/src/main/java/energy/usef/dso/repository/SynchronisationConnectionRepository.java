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
import energy.usef.dso.model.SynchronisationConnection;

import javax.ejb.Stateless;
import javax.persistence.Query;
import java.util.List;

/**
 * Repository class for the {@link SynchronisationConnection} entity. This class provides methods to interact with the BRP database.
 */
@Stateless
public class SynchronisationConnectionRepository extends BaseRepository<SynchronisationConnection> {

    /**
     * Deletes all {@link SynchronisationConnection} entities.
     */
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM SynchronisationConnection").executeUpdate();
    }

    /**
     * Deletes all the {@link SynchronisationConnection} objects for a {@Link synchronisationCongestionPoint}.
     *
     * @param synchronisationCongestionPoint
     */
    public void deleteFor (SynchronisationCongestionPoint synchronisationCongestionPoint) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("DELETE FROM SynchronisationConnection sc ");
        queryBuilder.append("WHERE sc.congestionPoint = :congestionPoint");

        Query query = entityManager.createQuery(queryBuilder.toString());
        query.setParameter("congestionPoint", synchronisationCongestionPoint).executeUpdate();
    }

    /**
     * Gets Synchronisation Connection entity by its entity address.
     *
     * @param entityAddresses a list of SynchronisationConnection entity addresses
     *
     * @return SynchronisationConnection entityList
     */
    @SuppressWarnings("unchecked")
    public List<SynchronisationConnection> findByEntityAddresses(List<String> entityAddresses) {

        return entityManager
                .createQuery(
                        "SELECT sc FROM SynchronisationConnection sc WHERE sc.entityAddress IN :entityAddresses")
                .setParameter("entityAddresses", entityAddresses).getResultList();
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
}
