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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;

import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuPrognosis;

/**
 * Repository class for the {@link PtuPrognosis} entity. This class is in charge of database operations on the tables related to
 * the
 * prognosises.
 */
@Stateless
public class PtuPrognosisRepository extends BaseRepository<PtuPrognosis> {

    /**
     * Returns the last prognoses of a specified period and prognosis type and optional usefIdentifier and documentstatus.
     *
     * @param period the period {@link LocalDate}
     * @param type (Optional) {@link PrognosisType}
     * @param usefIdentifier (Optional) usefIdentifier {@link String}
     * @param documentStatus (Optional) {@link DocumentStatus}
     * @return A {@link List} of {@link PtuPrognosis} objects.
     */
    @SuppressWarnings("unchecked")
    public List<PtuPrognosis> findLastPrognoses(LocalDate period, Optional<PrognosisType> type, Optional<String> usefIdentifier,
            Optional<DocumentStatus> documentStatus) {
        StringBuilder subselect = new StringBuilder();
        subselect.append("SELECT MAX(p.sequence) ");
        subselect.append("FROM PtuPrognosis p ");
        subselect.append("WHERE p.ptuContainer.ptuDate = :period ");
        if (type.isPresent()) {
            subselect.append("AND p.type = :type ");
        }
        subselect.append("GROUP BY p.participantDomain, p.connectionGroup ");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p2 ");
        sql.append("FROM PtuPrognosis p2 ");
        if (documentStatus.isPresent()) {
            sql.append(", PlanboardMessage pm ");
        }
        sql.append("WHERE p2.sequence IN (").append(subselect).append(")");
        sql.append("AND p2.ptuContainer.ptuDate = :period ");
        if (type.isPresent()) {
            sql.append("AND p2.type = :type ");
        }
        if (usefIdentifier.isPresent()) {
            sql.append("AND p2.connectionGroup.usefIdentifier = :usefIdentifier ");
        }
        if (documentStatus.isPresent()) {
            sql.append("AND pm.period = p2.ptuContainer.ptuDate ");
            sql.append("AND pm.participantDomain = p2.participantDomain ");
            sql.append("AND pm.sequence = p2.sequence ");
            sql.append("AND pm.connectionGroup = p2.connectionGroup ");
            sql.append("AND pm.documentStatus = :documentStatus ");
        }

        Query query = getEntityManager().createQuery(sql.toString());
        query.setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE);
        if (type.isPresent()) {
            query.setParameter("type", type.get());
        }
        if (usefIdentifier.isPresent()) {
            query.setParameter("usefIdentifier", usefIdentifier.get());
        }
        if (documentStatus.isPresent()) {
            query.setParameter("documentStatus", documentStatus.get());
        }

        return query.getResultList();
    }

    /**
     * Find all the Prognosis information needed for initiating the settlement. Prognoses with a period within the interval
     * defined by the variables and with status {@link DocumentStatus#ACCEPTED}, {@link DocumentStatus#FINAL} and {@link
     * DocumentStatus#ARCHIVED} will be retrieved.
     *
     * @param startDate {@link LocalDate} start date of the settlement (inclusive).
     * @param endDate {@link LocalDate} end date of the settlement (inclusive).
     * @return a {@link List} of {@link PtuPrognosis} ordered by period, participant domain and ptu index.
     */
    public List<PtuPrognosis> findPrognosesForSettlement(LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT prognosis ");
        sql.append("FROM PtuPrognosis prognosis, PlanboardMessage pm ");
        sql.append("WHERE prognosis.ptuContainer.ptuDate >= :startDate ");
        sql.append("  AND prognosis.ptuContainer.ptuDate <= :endDate ");
        sql.append("  AND pm.period = prognosis.ptuContainer.ptuDate ");
        sql.append("  AND pm.participantDomain = prognosis.participantDomain ");
        sql.append("  AND pm.sequence = prognosis.sequence ");
        sql.append("  AND pm.connectionGroup = prognosis.connectionGroup ");
        sql.append("  AND pm.documentStatus IN (:documentStatuses) ");
        sql.append("ORDER BY prognosis.ptuContainer.ptuDate, prognosis.participantDomain, prognosis.ptuContainer.ptuIndex ");
        return getEntityManager().createQuery(sql.toString(), PtuPrognosis.class)
                .setParameter("startDate", startDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("endDate", endDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("documentStatuses",
                        Arrays.asList(DocumentStatus.ACCEPTED, DocumentStatus.FINAL, DocumentStatus.ARCHIVED))
                .getResultList();
    }

    /**
     * Finds the prognosis by its sequence.
     *
     * @param prognosisSequence the sequence of the prognosis.
     * @return A {@link List} of {@link PtuPrognosis} objects
     */
    @SuppressWarnings("unchecked")
    public List<PtuPrognosis> findBySequence(long prognosisSequence) {
        return entityManager
                .createQuery("SELECT p FROM PtuPrognosis p WHERE p.sequence = :sequence")
                .setParameter("sequence", prognosisSequence)
                .getResultList();
    }

    /**
     * Finds ptuPrognosis by sequences of {@link PtuFlexOffer}.
     *
     * @param prognosisSequence the sequence number of the prognosis {@link Long}
     * @param participantDomain participant domain
     * @return ptuPrognosis list
     */
    @SuppressWarnings("unchecked")
    public List<PtuPrognosis> findPtuPrognosisForSequence(Long prognosisSequence, String participantDomain) {
        StringBuilder sql = new StringBuilder();
        sql.append(
                "SELECT p FROM PtuPrognosis p WHERE p.sequence = :prognosisSequence AND p.participantDomain = :participantDomain ");
        return entityManager.createQuery(sql.toString())
                .setParameter("prognosisSequence", prognosisSequence)
                .setParameter("participantDomain", participantDomain)
                .getResultList();

    }

    /**
     * Finds all the distinct prognoses offers that are linked to a flex order during the given period.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link LocalDate} of all the {@link PtuPrognosis} entities.
     */
    public List<PtuPrognosis> findPrognosesWithOrderInPeriod(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT prognosis ");
        sql.append("FROM PtuPrognosis prognosis, PtuFlexRequest frequest, PtuFlexOffer foffer, PtuFlexOrder forder ");
        sql.append("WHERE prognosis.ptuContainer.ptuDate = :period ");
        sql.append("  AND frequest.ptuContainer.ptuDate = :period ");
        sql.append("  AND foffer.ptuContainer.ptuDate = :period ");
        sql.append("  AND forder.ptuContainer.ptuDate = :period ");
        sql.append("  AND forder.participantDomain = foffer.participantDomain ");
        sql.append("  AND foffer.participantDomain = frequest.participantDomain ");
        sql.append("  AND frequest.participantDomain = prognosis.participantDomain ");
        sql.append("  AND forder.flexOfferSequence = foffer.sequence ");
        sql.append("  AND foffer.flexRequestSequence = frequest.sequence ");
        sql.append("  AND frequest.prognosisSequence = prognosis.sequence ");
        sql.append("  AND forder.ptuContainer.ptuIndex = foffer.ptuContainer.ptuIndex ");
        sql.append("  AND foffer.ptuContainer.ptuIndex = frequest.ptuContainer.ptuIndex ");
        sql.append("  AND frequest.ptuContainer.ptuIndex = prognosis.ptuContainer.ptuIndex ");
        return getEntityManager().createQuery(sql.toString(), PtuPrognosis.class)
                .setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Delete all {@link PtuPrognosis}s for a certain date.
     *
     * @param period
     * @return the number of {@link PtuPrognosis}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PtuPrognosis pp ");
        sql.append("WHERE pp.ptuContainer IN (SELECT pc FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate)");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
    }
}
