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
import energy.usef.core.model.ConnectionGroupState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.joda.time.LocalDate;

/**
 * This repository is used to manage {@link ConnectionGroupState}.
 */
@Stateless
public class ConnectionGroupStateRepository extends BaseRepository<ConnectionGroupState> {

    /**
     * Find connection group state.
     *
     * @param usefIdentifier USEF identifier
     * @param connectionEntityAddress connection entity address
     * @param period date
     * @return connection group state
     */
    public ConnectionGroupState findConnectionGroupState(String usefIdentifier, String connectionEntityAddress, LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cgs FROM ConnectionGroupState cgs ");
        sql.append("WHERE cgs.connectionGroup.usefIdentifier = :usefIdentifier ");
        sql.append("  AND cgs.connection.entityAddress = :connectionEntityAddress ");
        sql.append("  AND cgs.validFrom <= :date ");
        sql.append("  AND cgs.validUntil > :date ");

        TypedQuery<ConnectionGroupState> query = entityManager.createQuery(sql.toString(), ConnectionGroupState.class)
                .setParameter("usefIdentifier", usefIdentifier)
                .setParameter("connectionEntityAddress", connectionEntityAddress)
                .setParameter("date", period.toDateMidnight().toDate(), TemporalType.DATE);

        List<ConnectionGroupState> results = query.getResultList();
        if (results.size() == 1) {
            return results.get(0);
        }
        return null;
    }

    /**
     * Find connection group state.
     *
     * @param usefIdentifier USEF identifier
     * @param period date
     * @return connection group state
     */
    public List<ConnectionGroupState> findConnectionGroupStatesByUsefIdentifier(String usefIdentifier, LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cgs FROM ConnectionGroupState cgs ");
        sql.append("WHERE cgs.connectionGroup.usefIdentifier = :usefIdentifier ");
        sql.append("  AND cgs.validFrom <= :date ");
        sql.append("  AND cgs.validUntil > :date ");

        TypedQuery<ConnectionGroupState> query = entityManager.createQuery(sql.toString(), ConnectionGroupState.class)
                .setParameter("usefIdentifier", usefIdentifier)
                .setParameter("date", period.toDateMidnight().toDate(), TemporalType.DATE);

        return query.getResultList();
    }

    /**
     * Finds connection group states.
     *
     * @param connectionEntityAddress {@link String} entity address of the connection.
     * @param date {@link LocalDate} date of validity.
     * @return connection group state list
     */
    public List<ConnectionGroupState> findConnectionGroupStates(String connectionEntityAddress, LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cgs FROM ConnectionGroupState cgs ");
        sql.append("WHERE cgs.connection.entityAddress = :connectionEntityAddress ");
        sql.append("  AND cgs.validFrom <= :date ");
        sql.append("  AND cgs.validUntil > :date ");
        TypedQuery<ConnectionGroupState> query = entityManager.createQuery(sql.toString(), ConnectionGroupState.class);
        query.setParameter("connectionEntityAddress", connectionEntityAddress);
        query.setParameter("date", date.toDateMidnight().toDate(), TemporalType.DATE);
        return query.getResultList();
    }

    /**
     * Finds all the active connection groups at given moment.
     *
     * @param date {@link LocalDate}
     * @param connectionGroupType {@link Class} optional type of {@link ConnectionGroup} wanted
     * @return a {@link java.util.List} of {@link ConnectionGroupState}
     */
    @SuppressWarnings("unchecked")
    public List<ConnectionGroupState> findActiveConnectionGroupStatesOfType(LocalDate date, Class<? extends
            ConnectionGroup> connectionGroupType) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cgs ");
        sql.append("FROM ConnectionGroupState cgs ");
        sql.append("LEFT JOIN FETCH cgs.connectionGroup AS cg ");
        sql.append("WHERE cgs.validFrom <= :date ");
        sql.append("  AND cgs.validUntil > :date ");
        if (connectionGroupType != null) {
            sql.append("  AND TYPE(cg) = :connectionGroupType ");
        }
        Query query = entityManager.createQuery(sql.toString());
        query.setParameter("date", date.toDateMidnight().toDate(), TemporalType.DATE);
        if (connectionGroupType != null) {
            query.setParameter("connectionGroupType", connectionGroupType);
        }
        return query.getResultList();
    }

    /**
     * Find ending connectionGroupStates.
     *
     * @param endingDate date {@link LocalDate} period on which the Connection Group States are ending.
     * @param connectionGroupType {@link Class} type of ConnectionGroup one wants to retrieve.
     * @return a {@link List} of {@link ConnectionGroupState}.
     */
    public List<ConnectionGroupState> findEndingConnectionGroupStates(LocalDate endingDate,
            Class<? extends ConnectionGroup> connectionGroupType) {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT cgs ");
        sql.append("FROM ConnectionGroupState cgs ");
        sql.append("  LEFT JOIN FETCH cgs.connectionGroup AS cg ");
        sql.append("WHERE cgs.validUntil = :endingDate ");
        sql.append("  AND TYPE(cg) = :connectionGroupType");

        return getEntityManager().createQuery(sql.toString(), ConnectionGroupState.class)
                .setParameter("endingDate", endingDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("connectionGroupType", connectionGroupType)
                .getResultList();
    }

    /**
     * Finds the connections linked to a given list of connection group identifiers on a given period.
     *
     * @param connectionGroupIdentifiers {@link List} of {@link String} connection group usef identifiers.
     * @param period {@link LocalDate} period.
     * @return a {@link Map} of {@link ConnectionGroup} as key and {@link List} of {@link Connection} related to the connection
     * group.
     */
    public Map<ConnectionGroup, List<Connection>> findConnectionsWithConnectionGroups(List<String> connectionGroupIdentifiers,
            LocalDate period) {
        if (connectionGroupIdentifiers == null || connectionGroupIdentifiers.isEmpty()) {
            return new HashMap<>();
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cgs ");
        sql.append("FROM ConnectionGroupState cgs ");
        sql.append("  JOIN FETCH cgs.connectionGroup cg ");
        sql.append("  JOIN FETCH cgs.connection c ");
        sql.append("WHERE cgs.validFrom <= :date ");
        sql.append("  AND cgs.validUntil > :date ");
        sql.append("  AND cg.usefIdentifier IN :usefIdentifiers");
        List<ConnectionGroupState> connectionGroupStates = getEntityManager()
                .createQuery(sql.toString(), ConnectionGroupState.class)
                .setParameter("date", period.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("usefIdentifiers", connectionGroupIdentifiers)
                .getResultList();
        return connectionGroupStates.stream()
                .collect(Collectors.groupingBy(ConnectionGroupState::getConnectionGroup,
                        Collectors.mapping(ConnectionGroupState::getConnection, Collectors.toList())));
    }

    /**
     * Finds the active connection groups and their connections.
     *
     * @param startDate {@link LocalDate} start date of validity.
     * @param endDate {@link LocalDate} end date  of validity (inclusive).
     * @return a {@link Map} with the connection group as key ({@link ConnectionGroup}) and a {@link List} of {@link Connection}
     * as value.
     */
    public Map<ConnectionGroup, List<Connection>> findActiveConnectionGroupsWithConnections(LocalDate startDate,
            LocalDate endDate) {
        List<ConnectionGroupState> connectionGroupStates = findActiveConnectionGroupStates(startDate, endDate);
        return connectionGroupStates.stream()
                .collect(Collectors.groupingBy(ConnectionGroupState::getConnectionGroup,
                        Collectors.mapping(ConnectionGroupState::getConnection, Collectors.toList())));
    }

    /**
     * Finds the active connection and their related connection groups.
     *
     * @param period {@link LocalDate} period of validity.
     * @return a {@link Map} with the connection as key ({@link Connection}) and a {@link List} of {@link ConnectionGroup}
     * as value.
     */
    public Map<Connection, List<ConnectionGroup>> findActiveConnectionsWithConnectionGroups(LocalDate period) {
        List<ConnectionGroupState> activeConnectionGroupStates = findActiveConnectionGroupStates(period, period);
        return activeConnectionGroupStates.stream()
                .collect(Collectors.groupingBy(ConnectionGroupState::getConnection,
                        Collectors.mapping(ConnectionGroupState::getConnectionGroup, Collectors.toList())));
    }

    /**
     * Finds the list of connection group states that were valid during the entire timeframe specified by startDate  and endDate.
     *
     * @param startDate {@link LocalDate} start date of validity.
     * @param endDate {@link LocalDate} end date of validity (inclusive)
     * @return a {@link List} of {@link ConnectionGroupState}.
     */
    public List<ConnectionGroupState> findActiveConnectionGroupStates(LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cgs ");
        sql.append("FROM ConnectionGroupState cgs ");
        sql.append("  JOIN FETCH cgs.connectionGroup cg ");
        sql.append("  JOIN FETCH cgs.connection c ");
        sql.append("WHERE cgs.validFrom <= :startDate ");
        sql.append("  AND cgs.validUntil > :endDate ");
        return getEntityManager().createQuery(sql.toString(), ConnectionGroupState.class)
                .setParameter("startDate", startDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("endDate", endDate.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Finds the list of connection group states which have an overlapping validity with the given period.
     *
     * @param startDate {@link LocalDate} start date of validity (inclusive).
     * @param endDate {@link LocalDate} end date of validity (inclusive).
     * @return a {@link List} of {@link ConnectionGroupState}.
     */
    public List<ConnectionGroupState> findConnectionGroupStatesWithOverlappingValidity(LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cgs ");
        sql.append("FROM ConnectionGroupState cgs ");
        sql.append("  JOIN FETCH cgs.connectionGroup cg ");
        sql.append("  JOIN FETCH cgs.connection c ");
        sql.append("WHERE cgs.validFrom <= :endDate ");
        sql.append("  AND cgs.validUntil > :startDate ");
        // valid until of a CGS is an excluded upper bound, so it has to be strictly bigger than the start date.
        return getEntityManager().createQuery(sql.toString(), ConnectionGroupState.class)
                .setParameter("startDate", startDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("endDate", endDate.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Delete all {@link ConnectionGroupState} objects for a certain date.
     *
     * @param period
     * @return the number of {@link ConnectionGroupState} objects deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ConnectionGroupState cgs ");
        sql.append("WHERE cgs.validUntil = :validUntil)");

        return entityManager.createQuery(sql.toString()).setParameter("validUntil", period.toDateMidnight().plusDays(1).toDate()).executeUpdate();
    }
}
