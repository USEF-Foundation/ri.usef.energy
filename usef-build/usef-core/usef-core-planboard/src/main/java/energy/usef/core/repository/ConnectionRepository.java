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

import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.persistence.LockModeType;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.joda.time.LocalDate;

/**
 * This repository is used to manage {@link Connection}.
 */
@Stateless
public class ConnectionRepository extends BaseRepository<Connection> {

    /**
     * Finds the list of connections for a moment in time.
     *
     * @param date period
     * @param connectionEntityList (Optional) list of connection entity addresses
     * @return
     */
    public List<Connection> findActiveConnections(LocalDate date, Optional<List<String>> connectionEntityList) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT distinct cgs.connection FROM ConnectionGroupState cgs ");
        sql.append(" WHERE cgs.validFrom <= :date ");
        sql.append(" AND cgs.validUntil > :date ");
        if (connectionEntityList.isPresent()) {
            sql.append(" AND cgs.connection.entityAddress IN :connectionList ");
        }
        TypedQuery<Connection> query = entityManager.createQuery(sql.toString(), Connection.class);
        query.setParameter("date", date.toDateMidnight().toDate());
        if (connectionEntityList.isPresent()) {
            query.setParameter("connectionList", connectionEntityList.get());
        }
        return query.getResultList();
    }

    /**
     * Finds the connection count for a specified usef identifier at a given moment.
     *
     * @param usefIdentifier {@link String} usef identifier.
     * @param period {@link LocalDate} validity period.
     * @return a {@link Long}.
     */
    public Long findConnectionCountByUsefIdentifier(String usefIdentifier, LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(cgs.connection) ");
        sql.append("FROM ConnectionGroupState cgs ");
        sql.append("WHERE cgs.validFrom <= :date ");
        sql.append(" AND cgs.connectionGroup.usefIdentifier = :usefIdentifier ");
        sql.append(" AND cgs.validUntil > :date ");
        return entityManager.createQuery(sql.toString(), Long.class)
                .setParameter("usefIdentifier", usefIdentifier)
                .setParameter("date", period.toDateMidnight().toDate(), TemporalType.DATE)
                .getSingleResult();
    }

    /**
     * Creates or finds the {@link Connection} in a seperate transaction.
     *
     * @param connectionEntityAddress Connection entity address
     * @return Connection
     */
    @Transactional(value = TxType.REQUIRES_NEW)
    public Connection findOrCreate(String connectionEntityAddress) {
        Connection connection = getEntityManager().find(super.clazz, connectionEntityAddress, LockModeType.PESSIMISTIC_WRITE);
        if (connection == null) {
            connection = new Connection();
            connection.setEntityAddress(connectionEntityAddress);
            persist(connection);
        }
        return connection;
    }

    /**
     * Find {@link Connection} per {@link ConnectionGroup}.
     *
     * @param usefIdentifier {@link String} usef identifier of the Connection group.
     * @param date {@link org.joda.time.LocalDateTime} validity time.
     * @return a {@link java.util.List} of {@link Connection}s.
     */
    public List<Connection> findConnectionsForConnectionGroup(String usefIdentifier, LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cgs.connection FROM ConnectionGroupState cgs ");
        sql.append(" WHERE cgs.validFrom <= :date ");
        sql.append(" AND cgs.connectionGroup.usefIdentifier = :usefIdentifier");
        sql.append(" AND cgs.validUntil > :date ");
        return entityManager.createQuery(sql.toString(), Connection.class)
                .setParameter("date", date.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("usefIdentifier", usefIdentifier)
                .getResultList();

    }

}
