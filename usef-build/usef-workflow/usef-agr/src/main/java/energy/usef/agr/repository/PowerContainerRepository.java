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

package energy.usef.agr.repository;

import energy.usef.agr.model.ConnectionGroupPowerContainer;
import energy.usef.agr.model.ConnectionPowerContainer;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.model.Udi;
import energy.usef.agr.model.UdiPowerContainer;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.repository.BaseRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.joda.time.LocalDate;

/**
 * Repository class for the {@link PowerContainer} entity. This class provides methods to interact with the aggregator database.
 */
@Stateless
public class PowerContainerRepository extends BaseRepository<PowerContainer> {

    public static final String USEF_IDENTIFIER = "usefIdentifier";

    /**
     * Find all {@link PowerContainer}'s for a period on the {@link ConnectionGroup} Level.
     * Possible to filter for a specific {@link ConnectionGroup}.
     *
     * @param period                  the given period.
     * @param optionalConnectionGroup Optional {@link ConnectionGroup}
     * @return List of {@link PowerContainer} objects mapped per {@link ConnectionGroup}
     */
    public Map<ConnectionGroup, List<PowerContainer>> findConnectionGroupPowerContainers(LocalDate period,
            Optional<ConnectionGroup> optionalConnectionGroup) {
        StringBuilder queryString = new StringBuilder("SELECT cgpc FROM ConnectionGroupPowerContainer cgpc ");
        queryString.append(" WHERE cgpc.connectionGroup.usefIdentifier IN ");
        queryString.append(buildFindActiveConnectionGroups(optionalConnectionGroup));
        queryString.append(" AND cgpc.period = :period ");

        TypedQuery<PowerContainer> query = getEntityManager()
                .createQuery(queryString.toString(), PowerContainer.class)
                .setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE);

        if (optionalConnectionGroup.isPresent()) {
            query.setParameter(USEF_IDENTIFIER, optionalConnectionGroup.get().getUsefIdentifier());
        }

        return query.getResultList().stream().collect(Collectors.groupingBy(
                powerContainer -> ((ConnectionGroupPowerContainer) powerContainer).getConnectionGroup()));
    }

    /**
     * Finds all {@link PowerContainer}'s for a period and a list of connection entity addresses, grouped by Connection.
     *
     * @param period               The period for which this should be done.
     * @param connectionEntityList An {@link List} of connection entity addresses, null if not applicable.
     * @param connectionGroup      The {@link ConnectionGroup}, if applicable.
     * @return A {@link Map} with the {@link Connection} mapped to a list of {@link PowerContainer} objects.
     */
    public Map<Connection, List<PowerContainer>> findConnectionPowerContainers(LocalDate period,
            Optional<List<String>> connectionEntityList, Optional<ConnectionGroup> connectionGroup) {

        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT DISTINCT cpc FROM ConnectionPowerContainer cpc ");
        queryString.append("  JOIN FETCH cpc.connection conn, ConnectionGroupState cgs ");
        queryString.append("WHERE conn.entityAddress = cgs.connection.entityAddress ");
        queryString.append("  AND cpc.period = :period ");
        queryString.append("  AND cgs.validFrom <= :period AND cgs.validUntil > :period ");
        if (connectionEntityList.isPresent()) {
            queryString.append("AND conn.entityAddress IN :connectionList ");
        }
        if (connectionGroup.isPresent()) {
            queryString.append("AND cgs.connectionGroup.usefIdentifier = :usefIdentifier ");
        }

        TypedQuery<PowerContainer> query = getEntityManager().createQuery(queryString.toString(), PowerContainer.class)
                .setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE);

        if (connectionEntityList.isPresent()) {
            query.setParameter("connectionList", connectionEntityList.get());
        }
        if (connectionGroup.isPresent()) {
            query.setParameter("usefIdentifier", connectionGroup.get().getUsefIdentifier());
        }

        return query.getResultList().stream()
                .collect(Collectors.groupingBy(powerContainer -> ((ConnectionPowerContainer) powerContainer).getConnection()));
    }

    /**
     * Finds all {@link PowerContainer}'s for the UDI level for the current period and optional connection entity address list,
     * grouped by Udi.
     *
     * @param period               The period for which this should be done.
     * @param connectionEntityList A {@link List} of connection entity addresses, null if not applicable.
     * @param connectionGroup The {@link ConnectionGroup}, if applicable.
     * @return List of {@link PowerContainer} objects mapped per {@link Udi}.
     */
    public Map<Udi, List<PowerContainer>> findUdiPowerContainers(LocalDate period, Optional<List<String>> connectionEntityList
            , Optional<ConnectionGroup> connectionGroup) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT DISTINCT upc FROM UdiPowerContainer upc JOIN FETCH upc.udi udi ");
        queryString.append(" JOIN udi.connection conn, ConnectionGroupState cgs ");
        queryString.append("WHERE conn.entityAddress = cgs.connection.entityAddress ");
        queryString.append("AND upc.period = :period ");
        queryString.append("AND cgs.validFrom <= :period AND cgs.validUntil > :period ");
        if (connectionEntityList.isPresent()) {
            queryString.append("AND conn.entityAddress IN :connectionList ");
        }
        if (connectionGroup.isPresent()) {
            queryString.append("AND cgs.connectionGroup.usefIdentifier = :usefIdentifier ");
        }

        TypedQuery<PowerContainer> query = getEntityManager().createQuery(queryString.toString(), PowerContainer.class)
                .setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE);
        if (connectionEntityList.isPresent()) {
            query.setParameter("connectionList", connectionEntityList.get());
        }
        if (connectionGroup.isPresent()) {
            query.setParameter(USEF_IDENTIFIER, connectionGroup.get().getUsefIdentifier());
        }

        return query.getResultList().stream()
                .collect(Collectors.groupingBy(powerContainer -> ((UdiPowerContainer) powerContainer).getUdi()));
    }


    private String buildFindActiveConnectionGroups(Optional<ConnectionGroup> optionalConnectionGroup) {
        StringBuilder subquery = new StringBuilder(" ( ");
        subquery.append("SELECT DISTINCT cgs.connectionGroup.usefIdentifier FROM ConnectionGroupState cgs ");
        subquery.append(" WHERE cgs.validFrom <= :period ");
        subquery.append(" AND cgs.validUntil > :period ");
        if (optionalConnectionGroup.isPresent()) {
            subquery.append(" AND cgs.connectionGroup.usefIdentifier = :usefIdentifier ");
        }
        return subquery.append(" ) ").toString();
    }

    /**
     * Delete all {@link PowerContainer} objects for a certain date.
     *
     * @param period
     * @return the number of {@link PowerContainer} objects deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PowerContainer pc ");
        sql.append("WHERE pc.period = :period");

        return entityManager.createQuery(sql.toString()).setParameter("period", period.toDateMidnight().toDate()).executeUpdate();
    }
}
