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

package energy.usef.cro.repository;

import energy.usef.core.repository.BaseRepository;
import energy.usef.cro.model.Connection;

import java.util.List;

/**
 * Connection Repository for CRO.
 */
public class ConnectionRepository extends BaseRepository<Connection> {

    /**
     * Finds a Connection entity by an entity address.
     *
     * @param entityAddress entity address
     * @return Connection
     */
    @SuppressWarnings("unchecked")
    public Connection findConnectionByEntityAddress(String entityAddress) {

        List<Connection> result = entityManager
                .createQuery("SELECT c FROM Connection c WHERE c.entityAddress = :entityAddress")
                .setParameter("entityAddress", entityAddress).getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Finds all the {@link Connection}s.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Connection> findAll() {
        return entityManager.createQuery("SELECT c FROM Connection c").getResultList();
    }

    /**
     * Finds the {@link Connection}s for the AGR domain.
     *
     * @param senderDomain
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Connection> findConnectionsForAggregator(String senderDomain) {
        return entityManager
                .createQuery("SELECT c FROM Connection c WHERE c.aggregator.domain = :aggregatorDomain"
                        + " AND c.balanceResponsibleParty.domain != null")
                .setParameter("aggregatorDomain", senderDomain).getResultList();
    }

    /**
     * Finds the {@link Connection}s for the BRP domain.
     *
     * @param brpDomain
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Connection> findConnectionsForBRP(String brpDomain) {
        return entityManager
                .createQuery("SELECT c FROM Connection c WHERE c.balanceResponsibleParty.domain = :brpDomain"
                        + " AND c.aggregator.domain != null")
                .setParameter("brpDomain", brpDomain).getResultList();
    }

    /**
     * Finds the {@link Connection}s for the MDC domain.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Connection> findConnectionsForMDC() {
        return entityManager
                .createQuery("SELECT c FROM Connection c WHERE c.aggregator.domain != null").getResultList();
    }

}
