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

import energy.usef.core.model.ConnectionGroup;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;

/**
 * This repository is used to manage {@link ConnectionGroup}.
 */
@Stateless
public class ConnectionGroupRepository extends BaseRepository<ConnectionGroup> {

    /**
     * Finds all the {@link ConnectionGroup}s.
     *
     * @return a {@link List} of {@link ConnectionGroup}.
     */
    public List<ConnectionGroup> findAll() {
        return entityManager.createQuery("SELECT cg FROM ConnectionGroup cg ", ConnectionGroup.class).getResultList();
    }

    /**
     * Finds all the ConnectionGroups related to the connectionAdresses for a specific time.
     *
     * @param connectionAddresses
     * @param date {@link LocalDate} period of validity (usually a day).
     * @return a {@link List} of {@link ConnectionGroup}.
     */
    public List<ConnectionGroup> findConnectionGroupsWithConnections(List<String> connectionAddresses, LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cgs.connectionGroup FROM ConnectionGroupState cgs ");
        sql.append(" WHERE cgs.validFrom <= :date ");
        sql.append(" AND cgs.connection.entityAddress IN(:connectionAddresses) ");
        sql.append(" AND cgs.validUntil > :date ");
        return entityManager.createQuery(sql.toString(), ConnectionGroup.class)
                .setParameter("connectionAddresses", connectionAddresses)
                .setParameter("date", date.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Finds all the ConnectionGroups which had connections for that dateTime.
     *
     * @param date {@link LocalDate} period of validity (usually a day).
     * @return a {@link List} of {@link ConnectionGroup}.
     */
    public List<ConnectionGroup> findAllForDateTime(LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cgs.connectionGroup FROM ConnectionGroupState cgs ");
        sql.append("WHERE cgs.validFrom <= :date ");
        sql.append("  AND cgs.validUntil > :date ) ");
        return entityManager.createQuery(sql.toString(), ConnectionGroup.class)
                .setParameter("date", date.toDateMidnight().toDate())
                .getResultList();
    }
}
