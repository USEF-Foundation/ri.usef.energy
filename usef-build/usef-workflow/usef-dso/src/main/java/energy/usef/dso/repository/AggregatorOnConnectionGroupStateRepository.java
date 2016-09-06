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

import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.repository.BaseRepository;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;

/**
 * Repository class for the {@link AggregatorOnConnectionGroupState} entity.
 */
@Stateless
public class AggregatorOnConnectionGroupStateRepository extends BaseRepository<AggregatorOnConnectionGroupState> {

    /**
     * Get Aggregators by CongestionPointAddress.
     *
     * @param congestionPointAddress {@link String} entity address of the congestion point.
     * @param dateTime {@link org.joda.time.LocalDateTime} validity moment of the {@link AggregatorOnConnectionGroupState}.
     * @return list of {@link Aggregator}.
     */
    public List<Aggregator> getAggregatorsByCongestionPointAddress(String congestionPointAddress, LocalDate dateTime) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT state.aggregator ");
        sql.append("FROM AggregatorOnConnectionGroupState state ");
        sql.append("WHERE state.congestionPointConnectionGroup.usefIdentifier = :congestionPointEntityAddress ");
        sql.append(" AND state.validFrom <= :date ");
        sql.append(" AND state.validUntil > :date ");
        return entityManager.createQuery(sql.toString(), Aggregator.class)
                .setParameter("congestionPointEntityAddress", congestionPointAddress)
                .setParameter("date", dateTime.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Find the {@link AggregatorOnConnectionGroupState}s for this congestionPoint in a moment in time.
     *
     * @param congestionPointAddress {@link String} entity address of the congestion point.
     * @param date {@link org.joda.time.LocalDate} validity moment of the states.
     * @return list of AggregatorOnConnectionGroupState elements
     */
    public List<AggregatorOnConnectionGroupState> findAggregatorOnConnectionGroupStateByCongestionPointAddress(
            String congestionPointAddress, LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT state ");
        sql.append("FROM AggregatorOnConnectionGroupState state ");
        sql.append("WHERE state.congestionPointConnectionGroup.usefIdentifier = :congestionPointEntityAddress ");
        sql.append(" AND state.validFrom <= :date ");
        sql.append(" AND state.validUntil > :date ");
        return entityManager.createQuery(sql.toString(), AggregatorOnConnectionGroupState.class)
                .setParameter("congestionPointEntityAddress", congestionPointAddress)
                .setParameter("date", date.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Counts the {@link AggregatorOnConnectionGroupState}s for this congestionPoint in a moment in time.
     *
     * @param entityAddress The congestion Point Entity Address.
     * @param date The day.
     * @return the number of active aggregators for congestion point on given moment in time.
     */
    public long countActiveAggregatorsForCongestionPointOnDay(String entityAddress, LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT ( DISTINCT state.aggregator.domain) ");
        sql.append("FROM AggregatorOnConnectionGroupState state ");
        sql.append("WHERE state.congestionPointConnectionGroup.usefIdentifier = :congestionPointEntityAddress ");
        sql.append(" AND state.validFrom <= :date ");
        sql.append(" AND state.validUntil > :date ");
        return entityManager.createQuery(sql.toString(), Long.class)
                .setParameter("congestionPointEntityAddress", entityAddress)
                .setParameter("date", date.toDateMidnight().toDate(), TemporalType.DATE)
                .getSingleResult();
    }

    /**
     * Finds the Aggregators on a congestion point.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link Map} with the congestion point as key ({@link CongestionPointConnectionGroup}) and a {@link List} of
     * {@link AggregatorOnConnectionGroupState} as value.
     */
    public Map<CongestionPointConnectionGroup, List<AggregatorOnConnectionGroupState>> findConnectionGroupsWithAggregators(
            LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT state ");
        sql.append("FROM AggregatorOnConnectionGroupState state ");
        sql.append("WHERE state.validFrom <= :date ");
        sql.append("  AND state.validUntil > :date ");
        List<AggregatorOnConnectionGroupState> states = getEntityManager().createQuery(sql.toString(),
                AggregatorOnConnectionGroupState.class)
                .setParameter("date", period.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
        return states.stream().collect(Collectors.groupingBy(AggregatorOnConnectionGroupState::getCongestionPointConnectionGroup));
    }

    /**
     * Find State for an AggregatorOnConnectionGroup.
     *
     * @param aggregator {@link Aggregator} non-nullable aggregator.
     * @param congestionPoint {@link CongestionPointConnectionGroup} the congestion point.
     * @param date {@link org.joda.time.LocalDateTime} validity moment.
     * @return an {@link AggregatorOnConnectionGroupState} or <code>null</code>
     */
    public AggregatorOnConnectionGroupState findStateForAggregatorOnConnectionGroup(Aggregator aggregator,
            CongestionPointConnectionGroup congestionPoint, LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT state ");
        sql.append("FROM AggregatorOnConnectionGroupState state ");
        sql.append("WHERE state.congestionPointConnectionGroup = :congestionPoint ");
        sql.append(" AND state.aggregator.domain = :aggregatorDomain");
        sql.append(" AND state.validFrom <= :date");
        sql.append(" AND state.validUntil > :date");

        List<AggregatorOnConnectionGroupState> states = getEntityManager().createQuery(sql.toString(),
                AggregatorOnConnectionGroupState.class)
                .setParameter("aggregatorDomain", aggregator.getDomain())
                .setParameter("congestionPoint", congestionPoint)
                .setParameter("date", date.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
        if (states == null || states.isEmpty()) {
            return null;
        }
        return states.get(0);
    }

    /**
     * Finds all the {@link AggregatorOnConnectionGroupState} entities with a validUntil date equal to the specified date.
     *
     * @param usefIdentifier {@link String} USEF identifier of the {@link ConnectionGroup}.
     * @param initializationDate {@link LocalDate} date.
     * @return a {@link List} of {@link AggregatorOnConnectionGroupState}.
     */
    public List<AggregatorOnConnectionGroupState> findEndingAggregatorOnConnectionGroupStates(String usefIdentifier,
            LocalDate initializationDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT state ");
        sql.append("FROM AggregatorOnConnectionGroupState state ");
        sql.append("WHERE state.congestionPointConnectionGroup.usefIdentifier = :usefIdentifier ");
        sql.append("  AND state.validUntil = :date ");
        return getEntityManager().createQuery(sql.toString(), AggregatorOnConnectionGroupState.class)
                .setParameter("usefIdentifier", usefIdentifier)
                .setParameter("date", initializationDate.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Finds the {@link AggregatorOnConnectionGroupState} entities with an overlap over the given period.
     *
     * @param startDate {@link LocalDate} start date of the period.
     * @param endDate {@link LocalDate} end date of the period (inclusive).
     * @return a {@link List} of {@link AggregatorOnConnectionGroupState}.
     */
    public List<AggregatorOnConnectionGroupState> findAggregatorsWithOverlappingActivityForPeriod(LocalDate startDate,
            LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT state ");
        sql.append("FROM AggregatorOnConnectionGroupState state ");
        sql.append("  JOIN FETCH state.aggregator a ");
        sql.append("  JOIN FETCH state.congestionPointConnectionGroup c ");
        sql.append("WHERE state.validFrom <= :endDate ");
        sql.append("  AND state.validUntil > :startDate ");
        // valid until of a state is an excluded upper bound, so it has to be strictly bigger than the start date.
        return getEntityManager().createQuery(sql.toString(), AggregatorOnConnectionGroupState.class)
                .setParameter("startDate", startDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("endDate", endDate.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

}
