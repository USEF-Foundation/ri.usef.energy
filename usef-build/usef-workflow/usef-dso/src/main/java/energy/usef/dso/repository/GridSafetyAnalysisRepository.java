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

import javax.ejb.Stateless;

import org.joda.time.LocalDate;

import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.repository.BaseRepository;
import energy.usef.dso.model.GridSafetyAnalysis;

/**
 * Repository class for the GridSafetyAnalysis entity.
 */
@Stateless
public class GridSafetyAnalysisRepository extends BaseRepository<GridSafetyAnalysis> {

    /**
     * Finds the {@link GridSafetyAnalysis} entities for the given parameters with disposition requested for at least one of the
     * ptu's.
     *
     * @param congestionPointEntityAddress {@link String} related Congestion Point entity address.
     * @param ptuDate {@link LocalDate} Period of the PTU.
     * @return a {@link List} of {@link GridSafetyAnalysis}.
     */
    @SuppressWarnings("unchecked")
    public List<GridSafetyAnalysis> findGridSafetyAnalysisWithDispositionRequested(String congestionPointEntityAddress,
            LocalDate ptuDate) {

        StringBuilder subqueryExists = new StringBuilder();
        subqueryExists.append("SELECT 1 ");
        subqueryExists.append("FROM GridSafetyAnalysis gsa2 ");
        subqueryExists.append("WHERE gsa2.connectionGroup.usefIdentifier = :entityAddress ");
        subqueryExists.append("  AND gsa2.ptuContainer.ptuDate = :ptuDate ");
        subqueryExists.append("  AND gsa2.disposition = :dispositionRequested ");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT gsa ");
        sql.append("FROM GridSafetyAnalysis gsa ");
        sql.append("WHERE gsa.connectionGroup.usefIdentifier = :entityAddress ");
        sql.append("  AND gsa.ptuContainer.ptuDate = :ptuDate ");
        sql.append("  AND EXISTS (").append(subqueryExists).append(") ");
        sql.append("ORDER BY gsa.ptuContainer.ptuIndex");

        return getEntityManager().createQuery(sql.toString())
                .setParameter("entityAddress", congestionPointEntityAddress)
                .setParameter("ptuDate", ptuDate.toDateMidnight().toDate())
                .setParameter("dispositionRequested", DispositionAvailableRequested.REQUESTED)
                .getResultList();
    }

    /**
     * Finds the {@link GridSafetyAnalysis} entities for a given day.
     *
     * @param congestionPointEntityAddress {@link String} related Congestion Point entity address.
     * @param ptuDate {@link LocalDate} Period of the PTU.
     * @return a {@link List} of {@link GridSafetyAnalysis}.
     */
    @SuppressWarnings("unchecked")
    public List<GridSafetyAnalysis> findGridSafetyAnalysis(String congestionPointEntityAddress, LocalDate ptuDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT gsa ");
        sql.append("FROM GridSafetyAnalysis gsa ");
        sql.append("WHERE gsa.connectionGroup.usefIdentifier = :entityAddress ");
        sql.append("  AND gsa.ptuContainer.ptuDate = :ptuDate ");
        sql.append("ORDER BY gsa.ptuContainer.ptuIndex");

        return getEntityManager().createQuery(sql.toString())
                .setParameter("entityAddress", congestionPointEntityAddress)
                .setParameter("ptuDate", ptuDate.toDateMidnight().toDate())
                .getResultList();
    }

    /**
     * Finds the {@link GridSafetyAnalysis} entities used in calculations to find optimal flex offer combinations required
     * to place flex orders.
     *
     * @param currentDate current date
     * @return a {@link List} of {@link GridSafetyAnalysis}.
     */
    @SuppressWarnings("unchecked")
    public List<GridSafetyAnalysis> findGridSafetyAnalysisRelatedToFlexOffers(LocalDate currentDate) {
        // Include the GridSafetyAnalysis only if there are FlexOffers that are ACCEPTED for the ConnectionGroup on that date.
        StringBuilder subqueryInPtuDates = new StringBuilder();
        subqueryInPtuDates.append("SELECT pbm1.period FROM PlanboardMessage pbm1 ");
        subqueryInPtuDates.append("WHERE pbm1.documentType = :documentType ");
        subqueryInPtuDates.append("AND pbm1.documentStatus = :acceptedDocumentStatus ");
        subqueryInPtuDates.append("AND pbm1.connectionGroup.usefIdentifier = gsa.connectionGroup.usefIdentifier ");

        // Exclude the GridSafetyAnalysis if there are FlexOffers that are already PROCESSED for the ConnectionGroup on that date.
        StringBuilder subqueryNotInPtuDates = new StringBuilder();
        subqueryNotInPtuDates.append("SELECT pbm2.period FROM PlanboardMessage pbm2 ");
        subqueryNotInPtuDates.append("WHERE pbm2.documentType = :documentType ");
        subqueryNotInPtuDates.append("AND pbm2.documentStatus = :processedDocumentStatus ");
        subqueryNotInPtuDates.append("AND pbm2.connectionGroup.usefIdentifier = gsa.connectionGroup.usefIdentifier ");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT gsa ");
        sql.append("FROM GridSafetyAnalysis gsa ");
        sql.append("WHERE gsa.ptuContainer.ptuDate IN (").append(subqueryInPtuDates).append(") ");
        sql.append("AND gsa.ptuContainer.ptuDate NOT IN (").append(subqueryNotInPtuDates).append(") ");
        sql.append("AND gsa.ptuContainer.ptuDate >= :currentDate ");

        return getEntityManager().createQuery(sql.toString())
                .setParameter("documentType", DocumentType.FLEX_OFFER)
                .setParameter("acceptedDocumentStatus", DocumentStatus.ACCEPTED)
                .setParameter("processedDocumentStatus", DocumentStatus.PROCESSED)
                .setParameter("currentDate", currentDate.toDateMidnight().toDate())
                .getResultList();
    }

    /**
     * Delete all {@link GridSafetyAnalysis} objects for a certain date.
     *
     * @param period
     * @return the number of {@link GridSafetyAnalysis} objects deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM GridSafetyAnalysis gsa ");
        sql.append("WHERE gsa.ptuContainer IN (SELECT pc FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate)");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
    }

    /**
     * Delete all {@link GridSafetyAnalysis} objects for a certain date and congestion point.
     *
     * @param entityAddress
     * @param period
     */
    public int deletePreviousGridSafetyAnalysis(String entityAddress, LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM GridSafetyAnalysis gsa ");
        sql.append("WHERE gsa.connectionGroup.usefIdentifier = :entityAddress ");
        sql.append("AND gsa.ptuContainer IN (SELECT pc FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate)");

        return getEntityManager().createQuery(sql.toString())
                .setParameter("entityAddress", entityAddress)
                .setParameter("ptuDate", period.toDateMidnight().toDate())
                .executeUpdate();
    }
}
