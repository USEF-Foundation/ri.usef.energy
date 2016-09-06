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

import energy.usef.core.model.Connection;
import energy.usef.core.repository.BaseRepository;
import energy.usef.dso.model.ConnectionMeterEvent;

import java.util.List;

import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.joda.time.LocalDate;

/**
 * Repository for managing {@link ConnectionMeterEvent}'s.
 */
public class ConnectionMeterEventRepository extends BaseRepository<ConnectionMeterEvent> {

    /**
     * This method finds all ConnectionMeterEvents for a certain period that where active in that period.
     *
     * @param fromDate from date
     * @param endDate end date
     * 
     * @return connection meter event list
     */
    @SuppressWarnings("unchecked")
    public List<ConnectionMeterEvent> findConnectionMeterEventsForPeriod(LocalDate fromDate, LocalDate endDate) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT cme FROM ConnectionMeterEvent cme ");
        queryString.append(" WHERE cme.dateTime >= :fromDate ");
        // it is inclusive because i add a day to the endDate
        queryString.append("  AND cme.dateTime < :endDate ");

        Query query = getEntityManager().createQuery(queryString.toString());
        query.setParameter("fromDate", fromDate.toDateMidnight().toDateTime().toDate(), TemporalType.TIMESTAMP);
        query.setParameter("endDate", endDate.plusDays(1).toDateMidnight().toDateTime().toDate(), TemporalType.TIMESTAMP);

        return query.getResultList();
    }

    /**
     * Finds connections not related to ConnectionMeterEvents.
     * 
     * @param date date
     * @param connectionIncludeList connection include list
     * @return connection list
     */
    public List<Connection> findConnectionsNotRelatedToConnectionMeterEvents(LocalDate date, List<String> connectionIncludeList) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT distinct cgs.connection FROM ConnectionGroupState cgs");
        sql.append(" WHERE cgs.validFrom <= :startDate");
        sql.append(" AND cgs.validUntil >= :startDate");
        sql.append(" AND cgs.connection NOT IN ");
        sql.append("  (SELECT cme.connection FROM ConnectionMeterEvent cme WHERE cme.dateTime >= :startTime AND cme.dateTime < :endTime)");
        sql.append(" AND cgs.connection.entityAddress IN :connectionIncludeList");

        TypedQuery<Connection> query = entityManager.createQuery(sql.toString(), Connection.class);
        query.setParameter("startDate", date.toDateMidnight().toDate());
        query.setParameter("startTime", date.toDateMidnight().toDateTime().toDate(), TemporalType.TIMESTAMP);
        query.setParameter("endTime", date.plusDays(1).toDateMidnight().toDateTime().toDate(), TemporalType.TIMESTAMP);
        query.setParameter("connectionIncludeList", connectionIncludeList);
        return query.getResultList();
    }

    /**
     * This method finds a Connection for a given connection entity address and a certain period.
     *
     * @param entityAddress connection entity address
     * @param date date
     * 
     * @return connection if exists, null otherwise
     */
    @SuppressWarnings("unchecked")
    public Connection findConnectionForConnectionMeterEventsPeriod(String entityAddress, LocalDate date) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT cme.connection FROM ConnectionMeterEvent cme ");
        queryString.append(" WHERE cme.dateTime >= :fromTime ");
        queryString.append("  AND cme.dateTime < :endTime ");
        queryString.append("  AND cme.connection.entityAddress = :entityAddress ");

        Query query = getEntityManager().createQuery(queryString.toString());
        query.setParameter("fromTime", date.toDateMidnight().toDateTime().toDate(), TemporalType.TIMESTAMP);
        query.setParameter("endTime", date.plusDays(1).toDateMidnight().toDateTime().toDate(), TemporalType.TIMESTAMP);
        query.setParameter("entityAddress", entityAddress);

        List<Connection> connections = query.getResultList();
        if (connections.isEmpty()) {
            return null;
        }
        return connections.get(0);
    }
}
