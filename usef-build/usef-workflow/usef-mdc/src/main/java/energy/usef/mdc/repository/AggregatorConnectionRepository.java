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

package energy.usef.mdc.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;

import energy.usef.core.repository.BaseRepository;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.mdc.model.AggregatorConnection;

/**
 * Repository class for the {@link AggregatorConnection} entity.
 */
@Stateless
public class AggregatorConnectionRepository extends BaseRepository<AggregatorConnection> {

    /**
     * Finds the list of active {@link AggregatorConnection} for the given aggregator at a given time.
     *
     * @param aggregatorDomain {@link String} aggregator domain.
     * @param date {@link org.joda.time.LocalDate} validity date (will be evaluated as current date if <code>null</code>
     *            ).
     * @return a {@link java.util.List} of {@link AggregatorConnection} entities.
     */
    public List<AggregatorConnection> findActiveAggregatorConnectionsForAggregator(String aggregatorDomain,
            LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ac FROM AggregatorConnection ac ");
        sql.append("WHERE ac.aggregator.domain = :aggregatorDomain ");
        sql.append("  AND ac.validFrom <= :date ");
        sql.append("  AND (ac.validUntil is null OR ac.validUntil > :date) ");
        return getEntityManager().createQuery(sql.toString(), AggregatorConnection.class)
                .setParameter("aggregatorDomain", aggregatorDomain)
                .setParameter("date", date == null ?
                        DateTimeUtil.getCurrentDate().toDateMidnight().toDate() :
                        date.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Finds the active {@link AggregatorConnection} for the given connection at a given time.
     *
     * @param connectionEntityAddress {@link java.lang.String} entity address of the connection.
     * @param date {@link org.joda.time.LocalDate} validity date (will be evaluated as current date if <code>null</code>
     *            ).
     * @return a unique {@link AggregatorConnection} or <code>null</code> if not in the database.
     */
    public AggregatorConnection findActiveAggregatorConnectionForConnection(String connectionEntityAddress,
            LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ac FROM AggregatorConnection ac ");
        sql.append("WHERE ac.connection.entityAddress = :connectionEntityAddress ");
        sql.append("  AND ac.validFrom <= :date ");
        sql.append("  AND (ac.validUntil is null OR ac.validUntil > :date) ");
        List<AggregatorConnection> resultList = getEntityManager().createQuery(sql.toString(),
                AggregatorConnection.class)
                .setParameter("connectionEntityAddress", connectionEntityAddress)
                .setParameter("date", date == null ?
                        DateTimeUtil.getCurrentDate().toDateMidnight().toDate() :
                        date.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }
        return resultList.get(0);
    }

    /**
     * Finds the list of {@link AggregatorConnection} for the give Common Reference Operator domain.
     *
     * @param croDomain {@link java.lang.String} domain name of the Common Reference Operator.
     * @param date {@link org.joda.time.LocalDate} validity date (will be evaluated as current date if <code>null</code>
     *            ).
     * @return a {@link java.util.List} of {@link AggregatorConnection}.
     */
    public List<AggregatorConnection> findActiveAggregatorConnectionsForCommonReferenceOperator(String croDomain,
            LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ac FROM AggregatorConnection ac ");
        sql.append("WHERE ac.commonReferenceOperator.domain = :croDomain ");
        sql.append("  AND ac.validFrom <= :date ");
        sql.append("  AND (ac.validUntil is null OR ac.validUntil > :date) ");
        return getEntityManager().createQuery(sql.toString(), AggregatorConnection.class)
                .setParameter("croDomain", croDomain)
                .setParameter("date", date == null ?
                        DateTimeUtil.getCurrentDate().toDateMidnight().toDate() :
                        date.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Find the list of aggregator entity addresses and domains valid on a given date for a list of connections.
     *
     * @param date
     * @param connectionEntityAddressList
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> findAggregatorForEachConnection(LocalDate date, List<String> connectionEntityAddressList) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ac.connection.entityAddress, ac.aggregator.domain ");
        sql.append(" FROM AggregatorConnection ac ");
        sql.append("WHERE ac.connection.entityAddress IN (:connectionEntityAddressList) ");
        sql.append("  AND ac.validFrom <= :date ");
        sql.append("  AND (ac.validUntil is null OR ac.validUntil > :date) ");
        Query query = getEntityManager().createQuery(sql.toString());
        query.setParameter("date", date.toDateMidnight().toDate());
        query.setParameter("connectionEntityAddressList", connectionEntityAddressList);
        return ((List<Object[]>) query.getResultList())
                .stream().collect(Collectors.toMap(record -> (String) record[0], record -> (String) record[1]));
    }

    /**
     * Delete all {@link AggregatorConnection}s for a given period.
     *
     * @param period
     * @return the number of {@link AggregatorConnection}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM AggregatorConnection ac WHERE ac.validUntil = :validUntil");

        return entityManager.createQuery(sql.toString()).setParameter("validUntil", period.toDateMidnight().toDate()).executeUpdate();
    }
}
