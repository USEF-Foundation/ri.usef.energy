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
import energy.usef.cro.model.Aggregator;
import energy.usef.cro.model.CongestionPoint;
import energy.usef.cro.model.Connection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;

/**
 * Congestion Point Repository for CRO.
 */
@Stateless
public class CongestionPointRepository extends BaseRepository<CongestionPoint> {
    /**
     * Gets a CongestionPoint entity by an entity address.
     *
     * @param entityAddress entity address
     * @return CongestionPoint
     */
    @SuppressWarnings("unchecked")
    public CongestionPoint getCongestionPointByEntityAddress(String entityAddress) {

        List<CongestionPoint> result = entityManager.createQuery(
                "SELECT cp FROM CongestionPoint cp WHERE cp.entityAddress = :entityAddress")
                .setParameter("entityAddress", entityAddress)
                .getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Gets CongestionPoints related to a DSO, either all or for a specific entityAdress.
     *
     * @param dsoDomain - The domain of the requesting party
     * @param entityAddress - (optional) The congestionPoint entity address
     * @return Map - The Map with congestionPoints and there Aggregators, and their connectionCount.
     */
    @SuppressWarnings("unchecked")
    public Map<CongestionPoint, Map<Aggregator, Long>> findAggregatorCountForCongestionPointsByDSO(String dsoDomain,
            String entityAddress) {
        Map<String, Object> parameters = new HashMap<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cp, aggregator, count(connections) ");
        sql.append("FROM CongestionPoint cp ");
        sql.append("  LEFT JOIN cp.connections connections ");
        sql.append("  LEFT JOIN connections.aggregator aggregator ");
        sql.append("WHERE cp.distributionSystemOperator.domain = :dsoDomain ");

        parameters.put("dsoDomain", dsoDomain);

        if (StringUtils.isNotEmpty(entityAddress)) {
            sql.append("  AND cp.entityAddress = :entityAddress ");
            parameters.put("entityAddress", entityAddress);
        }

        sql.append("GROUP BY cp, aggregator ");

        Query query = entityManager.createQuery(sql.toString(), Object[].class);
        List<Object[]> resultList = addNamedParams(query, parameters).getResultList();

        // Map Results to Maps
        Map<CongestionPoint, Map<Aggregator, Long>> resultsMap = new HashMap<>();
        for (Object[] row : resultList) {
            CongestionPoint congestionPoint = (CongestionPoint) row[0];
            if (!resultsMap.containsKey(congestionPoint)) {
                resultsMap.put(congestionPoint, new HashMap<>());
            }
            resultsMap.get(congestionPoint).put((Aggregator) row[1], (Long) row[2]);
        }
        return resultsMap;
    }

    /**
     * Gets Connections and CongestionPoints related to an AGR, either all or for a specific entityAdress.
     *
     * @param agrDomain - The domain of the requesting party
     * @param entityAddress - (optional) The congestionPoint entity address
     * @return Map - The Map with connections and corresponding congestionPoints.
     */
    @SuppressWarnings("unchecked")
    public Map<CongestionPoint, Set<Connection>> findConnectionsForCongestionPointsByAGR(String agrDomain, String entityAddress) {
        Map<String, Object> parameters = new HashMap<>();

        String queryString = "SELECT connection FROM Connection connection "
                + " WHERE connection.aggregator.domain = :agrDomain AND connection.congestionPoint != null";
        parameters.put("agrDomain", agrDomain);

        if (StringUtils.isNotEmpty(entityAddress)) {
            queryString += " AND connection.congestionPoint.entityAddress = :entityAddress ";
            parameters.put("entityAddress", entityAddress);
        }

        Query query = entityManager.createQuery(queryString, Object[].class);
        List<Object> resultList = addNamedParams(query, parameters).getResultList();

        // Map Results to Maps
        Map<CongestionPoint, Set<Connection>> resultsMap = new HashMap<>();
        for (Object object : resultList) {
            Connection connection = (Connection) object;
            if (!resultsMap.containsKey(connection.getCongestionPoint())) {
                Set<Connection> set = new HashSet<>();
                set.add(connection);
                resultsMap.put(connection.getCongestionPoint(), set);
            } else {
                resultsMap.get(connection.getCongestionPoint()).add(connection);
            }
        }
        return resultsMap;
    }

    /**
     * Gets CongestionPoints related to an AGR.
     *
     * @param senderDomain
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<CongestionPoint> findCongestionPointsForAggregator(String senderDomain) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.congestionPoint ");
        sql.append("FROM Connection c ");
        sql.append("WHERE c.aggregator.domain = :aggregatorDomain");
        return entityManager.createQuery(sql.toString())
                .setParameter("aggregatorDomain", senderDomain)
                .getResultList();
    }
}
