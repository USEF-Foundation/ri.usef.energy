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

import java.util.List;
import java.util.Optional;

import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.joda.time.LocalDate;

import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.FlexOrderSettlement;
import energy.usef.core.model.PlanboardMessage;

/**
 * Repository class in charge of the operations related to the {@link FlexOrderSettlement} entities.
 */
public class FlexOrderSettlementRepository extends BaseRepository<FlexOrderSettlement> {

    /**
     * Checks if each accepted flex order in the given month of the given year has a related settlement item in the database.
     *
     * @param year {@link Integer} year.
     * @param month {@link Integer} month.
     * @return <code>true</code> if each ACCEPTED flex order planboard message has a related FlexOrderSettlement entity;
     * <code>false</code> otherwise.
     */
    public Boolean isEachFlexOrderReadyForSettlement(int year, int month) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE pm.documentType = :flexOrderType ");
        sql.append("  AND YEAR(pm.period) = :year ");
        sql.append("  AND MONTH(pm.period) = :month ");
        sql.append("  AND pm.documentStatus = :acceptedStatus ");
        sql.append("  AND pm NOT IN (");
        sql.append("    SELECT fos.flexOrder ");
        sql.append("    FROM FlexOrderSettlement fos ");
        sql.append("    WHERE MONTH(fos.period) = :month ");
        sql.append("      AND YEAR(fos.period) = :year ) ");
        List<PlanboardMessage> result = getEntityManager().createQuery(sql.toString(), PlanboardMessage.class)
                .setParameter("year", year)
                .setParameter("month", month)
                .setParameter("acceptedStatus", DocumentStatus.ACCEPTED)
                .setParameter("flexOrderType", DocumentType.FLEX_ORDER)
                .getResultList();
        return result.isEmpty();
    }

    /**
     * Finds the Flex Order Settlement entities for the given period, ordered by participant domain, connection group and period.
     *
     * @param startDate {@link LocalDate} start date of the period (inclusive).
     * @param endDate {@link LocalDate} end date of the period (inclusive).
     * @param connectionGroup {@link Optional} USEF identifier of the connection group of the flex order settlement.
     * @param participantDomain {@link Optional} participant domain of the flex order settlement.
     * @return a {@link List} of {@link FlexOrderSettlement} entities.
     */
    public List<FlexOrderSettlement> findFlexOrderSettlementsForPeriod(LocalDate startDate, LocalDate endDate,
            Optional<String> connectionGroup, Optional<String> participantDomain) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fos ");
        sql.append("FROM FlexOrderSettlement fos ");
        sql.append("WHERE fos.period >= :startDate AND fos.period <= :endDate ");
        connectionGroup.ifPresent(usefIdentifier -> sql.append("  AND fos.connectionGroup.usefIdentifier = :usefIdentifier"));
        participantDomain.ifPresent(domain -> sql.append("  AND fos.flexOrder.participantDomain = :participantDomain "));
        sql.append("ORDER BY fos.flexOrder.participantDomain, fos.flexOrder.connectionGroup, fos.period ");

        TypedQuery<FlexOrderSettlement> query = getEntityManager().createQuery(sql.toString(), FlexOrderSettlement.class);
        query.setParameter("startDate", startDate.toDateMidnight().toDate(), TemporalType.DATE);
        query.setParameter("endDate", endDate.toDateMidnight().toDate(), TemporalType.DATE);
        connectionGroup.ifPresent(usefIdentifier -> query.setParameter("usefIdentifier", usefIdentifier));
        participantDomain.ifPresent(domain -> query.setParameter("participantDomain", domain));
        return query.getResultList();
    }

    /**
     * Delete all {@link FlexOrderSettlement}s for a certain date.
     *
     * @param period
     * @return the number of {@link FlexOrderSettlement}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM FlexOrderSettlement fos WHERE fos.period = :period");

        return entityManager.createQuery(sql.toString()).setParameter("period", period.toDateMidnight().toDate()).executeUpdate();
    }

}
