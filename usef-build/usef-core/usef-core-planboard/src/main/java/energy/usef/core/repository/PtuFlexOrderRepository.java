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

import static javax.persistence.TemporalType.DATE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.joda.time.LocalDate;

import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.PtuFlexOrder;

/**
 * Repository class for the {@link PtuFlexOrder} entity. This class is in charge of database operations on the tables related to the
 * flex orders.
 */
@Stateless
public class PtuFlexOrderRepository extends BaseRepository<PtuFlexOrder> {
    /**
     * Find flexibility Orders.
     *
     * @param sequence sequence number
     * @param acknowledgementStatus acknowledgementStatus
     * @return flexibility Orders
     */
    public List<PtuFlexOrder> findFlexOrdersBySequence(Long sequence, AcknowledgementStatus acknowledgementStatus) {
        Query query = entityManager.createQuery("SELECT fo FROM PtuFlexOrder fo WHERE fo.sequence = :sequence " + (
                acknowledgementStatus != null ?
                        "and fo.acknowledgementStatus = :acknowledgementStatus" :
                        ""));
        query.setParameter("sequence", sequence);
        if (acknowledgementStatus != null) {
            query.setParameter("acknowledgementStatus", acknowledgementStatus);
        }
        @SuppressWarnings("unchecked")
        List<PtuFlexOrder> results = query.getResultList();
        if (results == null) {
            results = new ArrayList<>();
        }
        return results;
    }

    /**
     * Find flexibility Orders.
     *
     * @param sequence sequence number
     * @return flexibility Orders
     */
    public List<PtuFlexOrder> findFlexOrdersBySequence(Long sequence) {
        return findFlexOrdersBySequence(sequence, null);
    }

    /**
     * Find flexibility Orders.
     *
     * @param startDate PTU start day
     * @param endDate PTU end day
     * @return flexibility Orders
     */
    public List<PtuFlexOrder> findFlexOrdersByDates(LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fo FROM PtuFlexOrder fo ");
        sql.append("WHERE fo.ptuContainer.ptuDate >= :startDate ");
        sql.append("  AND fo.ptuContainer.ptuDate <= :endDate ");
        sql.append("  AND fo.acknowledgementStatus = :acknowledgementStatus ");
        sql.append("ORDER BY fo.participantDomain, fo.ptuContainer.ptuDate, fo.ptuContainer.ptuIndex, ")
                .append("fo.connectionGroup.usefIdentifier, fo.sequence ");
        Query query = entityManager.createQuery(sql.toString());

        query.setParameter("acknowledgementStatus", AcknowledgementStatus.ACCEPTED);
        query.setParameter("startDate", startDate.toDateMidnight().toDate());
        query.setParameter("endDate", endDate.toDateMidnight().toDate());

        @SuppressWarnings("unchecked")
        List<PtuFlexOrder> results = query.getResultList();
        if (results == null) {
            results = new ArrayList<>();
        }
        return results;
    }

    /**
     * Find the Accepted {@link PtuFlexOrder}'s for the requested month and year.
     *
     * @param workingDate - The date with requested month and year
     * @return
     */
    public List<PtuFlexOrder> findAcknowledgedFlexOrdersForMonthInYear(LocalDate workingDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fo ");
        sql.append("FROM PtuFlexOrder fo ");
        sql.append("WHERE fo.acknowledgementStatus = :acknowledgementStatus ");
        sql.append("  AND fo.ptuContainer.ptuDate >= :firstDay ");
        sql.append("  AND fo.ptuContainer.ptuDate <= :lastDay ");
        Query query = entityManager.createQuery(sql.toString(), PtuFlexOrder.class);

        query.setParameter("acknowledgementStatus", AcknowledgementStatus.ACCEPTED);
        query.setParameter("firstDay", workingDate.withDayOfMonth(1).toDateMidnight().toDate(), DATE);
        query.setParameter("lastDay", workingDate.plusMonths(1).withDayOfMonth(1).minusDays(1).toDateMidnight().toDate(), DATE);

        return query.getResultList();
    }

    /**
     * Find flexibility Orders.
     *
     * @param startDate PTU start day
     * @param endDate PTU end day
     * @return flexibility Orders
     */
    public List<PtuFlexOrder> findFlexOrdersForSettlement(LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fo FROM PtuFlexOrder fo ");
        sql.append("WHERE fo.ptuContainer.ptuDate >= :startDate ");
        sql.append("  AND fo.ptuContainer.ptuDate <= :endDate ");
        sql.append("  AND fo.acknowledgementStatus IN (:statuses) ");
        sql.append("ORDER BY fo.ptuContainer.ptuDate, fo.participantDomain, fo.ptuContainer.ptuIndex, ")
                .append("fo.connectionGroup.usefIdentifier, fo.sequence ");
        Query query = entityManager.createQuery(sql.toString(), PtuFlexOrder.class);

        query.setParameter("statuses", Arrays.asList(AcknowledgementStatus.ACCEPTED, AcknowledgementStatus.PROCESSED));
        query.setParameter("startDate", startDate.toDateMidnight().toDate());
        query.setParameter("endDate", endDate.toDateMidnight().toDate());

        List<PtuFlexOrder> results = query.getResultList();
        if (results == null) {
            results = new ArrayList<>();
        }
        return results;
    }

    /**
     * Find and return all accepted {@link PtuFlexOrder}s for an usefIdentifier on a certain date that are not processed yet.
     *
     * @param usefIdentifier
     * @param ptuDate
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<PtuFlexOrder> findAcceptedFlexOrdersByDateAndUsefIdentifier(Optional<String> usefIdentifier, LocalDate ptuDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fo FROM PtuFlexOrder fo ");
        sql.append("WHERE fo.ptuContainer.ptuDate = :ptuDate ");
        sql.append(" AND fo.acknowledgementStatus = :acknowledgementStatus ");
        usefIdentifier.ifPresent(usefIdentifierValue -> sql.append("  AND fo.connectionGroup.usefIdentifier = :usefIdentifier"));

        Query query = entityManager.createQuery(sql.toString())
                .setParameter("ptuDate", ptuDate.toDateMidnight().toDate())
                .setParameter("acknowledgementStatus", AcknowledgementStatus.ACCEPTED);
        usefIdentifier.ifPresent(usefIdentifierValue -> query.setParameter("usefIdentifier", usefIdentifierValue));
        return query.getResultList();
    }

    /**
     * Delete all {@link PtuFlexOrder}s for a certain date.
     *
     * @param period
     * @return the number of {@link PtuFlexOrder}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PtuFlexOrder pfo ");
        sql.append("WHERE pfo.ptuContainer IN (SELECT pc FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate)");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
    }
}
