/*
 * Copyright 2015 USEF Foundation
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

package energy.usef.agr.repository;

import energy.usef.agr.model.Udi;
import energy.usef.core.model.Connection;
import energy.usef.core.repository.BaseRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.joda.time.LocalDate;

/**
 * Repository class for the {@link Udi} entity. This class provides methods to interact with the aggregator database.
 */
@Stateless
public class UdiRepository extends BaseRepository<Udi> {

    /**
     * Finds the Udis which are part of the portfolio on the given period.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link List} of {@link Udi} entities.
     */
    public Map<String, Udi> findActiveUdisMappedPerEndpoint(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT udi ");
        sql.append("FROM Udi udi ");
        sql.append("WHERE udi.connection IN ( ");
        sql.append("  SELECT DISTINCT cgs.connection ");
        sql.append("  FROM ConnectionGroupState cgs ");
        sql.append("  WHERE cgs.validFrom <= :period AND cgs.validUntil > :period )");

        return getEntityManager().createQuery(sql.toString(), Udi.class)
                .setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList()
                .stream().collect(Collectors.toMap(Udi::getEndpoint, Function.identity()));
    }

    /**
     * Finds the Udis which are part of the portfolio on the given period.
     * Grouped per Connection.
     *
     * @param period the given period.
     * @return List of {@link Udi} mapped per {@link Connection}.
     */
    public Map<Connection, List<Udi>> findActiveUdisPerConnection(LocalDate period) {
        return findActiveUdisPerConnection(period, Optional.empty());
    }

    /**
     * Finds the Udis which are part of the portfolio on the given period.
     * Grouped per Connection.
     *
     * @param period the given period.
     * @param connectionEntityList An optional {@link List} of connection entity addresses. Null if not applicable.
     * @return List of {@link Udi} mapped per {@link Connection}.
     */
    public Map<Connection, List<Udi>> findActiveUdisPerConnection(LocalDate period, Optional<List<String>> connectionEntityList) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT udi FROM Udi udi ");
        sql.append(" JOIN udi.connection conn, ConnectionGroupState cgs ");
        sql.append("WHERE conn.entityAddress = cgs.connection.entityAddress ");
        sql.append("  AND cgs.validFrom <= :period AND cgs.validUntil > :period ");
        if (connectionEntityList.isPresent()) {
            sql.append("  AND conn.entityAddress IN :connectionList ");
        }

        TypedQuery<Udi> query = getEntityManager().createQuery(sql.toString(), Udi.class)
                .setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE);

        if (connectionEntityList.isPresent()) {
            query.setParameter("connectionList", connectionEntityList.get());
        }

        return query.getResultList().stream().collect(Collectors.groupingBy(Udi::getConnection));
    }

    /**
     * Find UDI by connection.
     *
     * @param entityAddress the entity address of the connection.
     * @return a {@link List} of {@link Udi} objects for the given entity address.
     */
    @SuppressWarnings("unchecked")
    public List<Udi> findUdiByConnection(String entityAddress) {
        return entityManager.createQuery("SELECT u FROM Udi u WHERE u.connection.entityAddress = :entityAddress ").setParameter(
                "entityAddress", entityAddress).getResultList();
    }

    /**
     * Finds a Udi by its endpoint.
     *
     * @param udiEndpoint {@link String} the endpoint of the Udi.
     * @return a {@link Udi} entity or <code>null</code> if none or multiple exist.
     */
    public Udi findByEndpoint(String udiEndpoint) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT udi ");
        sql.append("FROM Udi udi ");
        sql.append("WHERE udi.endpoint = :endpoint ");
        List<Udi> udis = getEntityManager().createQuery(sql.toString(), Udi.class)
                .setParameter("endpoint", udiEndpoint)
                .getResultList();
        if (udis.size() != 1) {
            return null;
        }
        return udis.get(0);
    }
}
