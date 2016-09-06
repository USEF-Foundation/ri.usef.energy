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

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.joda.time.LocalDate;

import energy.usef.core.repository.BaseRepository;
import energy.usef.dso.model.NonAggregatorForecast;

/**
 * Repository class for the DSO NonAggregatorForecast.
 */
@Stateless
public class NonAggregatorForecastRepository extends BaseRepository<NonAggregatorForecast> {
    /**
     * Gets last NonAggregatorForecast.
     */
    @SuppressWarnings("unchecked")
    public NonAggregatorForecast getLastNonAggregatorForecast() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT naf ");
        sql.append("FROM NonAggregatorForecast naf ");
        sql.append("ORDER BY naf.creationDate DESC ");

        List<NonAggregatorForecast> result = entityManager
                .createQuery(sql.toString())
                .setMaxResults(1)
                .getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Gets last NonAggregatorForecasts.
     *
     * @param ptuDate        start date
     * @param usefIdentifier
     * @return last non aggregator forecasts
     */
    @SuppressWarnings("unchecked")
    public List<NonAggregatorForecast> getLastNonAggregatorForecasts(LocalDate ptuDate, Optional<String> usefIdentifier) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT naf2 ");
        sql.append("FROM NonAggregatorForecast naf2 ");
        sql.append("WHERE naf2.creationDate IN (");
        sql.append(" SELECT max(naf.creationDate) ");
        sql.append(" FROM NonAggregatorForecast naf ");
        sql.append(" WHERE naf.ptuContainer.ptuDate = :ptuDate ");
        if (usefIdentifier.isPresent()) {
            sql.append(" AND naf.connectionGroup.usefIdentifier = :usefIdentifier");
        }
        sql.append(" GROUP BY naf.connectionGroup ");
        sql.append(")");
        if (usefIdentifier.isPresent()) {
            sql.append(" AND naf2.connectionGroup.usefIdentifier = :usefIdentifier");
        }

        Query query = entityManager
                .createQuery(sql.toString())
                .setParameter("ptuDate", ptuDate.toDateMidnight().toDate());
        if (usefIdentifier.isPresent()) {
            query.setParameter("usefIdentifier", usefIdentifier.get());
        }
        return query.getResultList();
    }

    /**
     * Gets last NonAggregatorForecast.
     *
     * @param ptuDate  ptu date
     * @param ptuIndex ptu index
     * @return last non aggregator forecasts
     */
    @SuppressWarnings("unchecked")
    public List<NonAggregatorForecast> getLastNonAggregatorForecasts(LocalDate ptuDate, Integer ptuIndex) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT naf2 ");
        sql.append("FROM NonAggregatorForecast naf2 ");
        sql.append("WHERE naf2.creationDate IN (");
        sql.append(" SELECT max(naf.creationDate) ");
        sql.append(" FROM NonAggregatorForecast naf ");
        sql.append(" WHERE naf.ptuContainer.ptuDate = :ptuDate ");
        sql.append("  AND naf.ptuContainer.ptuIndex = :ptuIndex ");
        sql.append(" GROUP BY naf.connectionGroup ");
        sql.append(")");
        return entityManager
                .createQuery(sql.toString())
                .setParameter("ptuDate", ptuDate.toDateMidnight().toDate())
                .setParameter("ptuIndex", ptuIndex)
                .getResultList();
    }

    /**
     * Delete all {@link NonAggregatorForecast} objects for a certain date.
     *
     * @param period
     * @return the number of {@link NonAggregatorForecast} objects deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM NonAggregatorForecast naf ");
        sql.append("WHERE naf.ptuContainer IN (SELECT pc FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate)");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
    }
}
