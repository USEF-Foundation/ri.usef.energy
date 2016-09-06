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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.joda.time.LocalDate;

import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.PtuFlexRequest;

/**
 * Repository class for the {@link PtuFlexRequest} entity. This class is in charge of database operations on the tables related to
 * the flex requests.
 */
@Stateless
public class PtuFlexRequestRepository extends BaseRepository<PtuFlexRequest> {

    /**
     * Find the latest flex request document with a {@link DispositionAvailableRequested#REQUESTED} disposition.
     *
     * @param usefIdentifier {@link String} usefIdentifier of the {@link ConnectionGroup} related to the flex request.
     * @param period         {@link LocalDate} day period of the flex request.
     * @param sequence       {@link Long} sequence of the flex request message.
     * @return the latest {@link PtuFlexRequest} element of the planboard for the given parameter.
     */
    public PtuFlexRequest findLastFlexRequestDocumentWithDispositionRequested(String usefIdentifier, LocalDate period,
            Long sequence) {
        TypedQuery<PtuFlexRequest> query = entityManager.createQuery(
                "" + " SELECT fr FROM PtuFlexRequest fr " + " WHERE fr.ptuContainer.ptuDate = :period "
                        + " AND fr.connectionGroup.usefIdentifier= :usefIdentifier " + " AND fr.disposition = :disposition "
                        + " AND fr.sequence = :sequence "
                        + " AND fr.ptuContainer.ptuIndex =  (SELECT MAX(subFR.ptuContainer.ptuIndex) FROM PtuFlexRequest subFR "
                        + " WHERE subFR.ptuContainer.ptuDate = :period "
                        + " AND subFR.connectionGroup.usefIdentifier = :usefIdentifier " + " AND subFR.disposition = :disposition "
                        + " AND subFR.sequence = :sequence)", PtuFlexRequest.class);

        query.setParameter("disposition", DispositionAvailableRequested.REQUESTED);
        query.setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE);
        query.setParameter("usefIdentifier", usefIdentifier);
        query.setParameter("sequence", sequence);
        List<PtuFlexRequest> results = query.getResultList();
        if (results.size() == 1) {
            return results.get(0);
        }
        return null;
    }

    /**
     * Finds the {@link PtuFlexRequest} with the given sequence number for a given participant. The size of the resulting list
     * should match the number of PTUs for the period of those PtuFlexRequest.
     *
     * @param connectionGroupUsefIdentifier {@link String} usef identifier of the connection group.
     * @param flexRequestSequenceNumber     {@link Long} sequence number.
     * @param participantDomain             {@link String} participant domain name.
     * @return a {@link List} of {@link PtuFlexRequest}.
     */
    public List<PtuFlexRequest> findPtuFlexRequestWithSequence(String connectionGroupUsefIdentifier, Long flexRequestSequenceNumber,
            String participantDomain) {
        if (flexRequestSequenceNumber == null || participantDomain == null) {
            return new ArrayList<>();
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pfr ");
        sql.append("FROM PtuFlexRequest pfr ");
        sql.append("WHERE pfr.participantDomain = :participantDomain ");
        sql.append("  AND pfr.sequence = :sequence ");
        sql.append("  AND pfr.connectionGroup.usefIdentifier = :usefIdentifier ");
        sql.append("ORDER BY pfr.ptuContainer.ptuIndex ");

        return getEntityManager().createQuery(sql.toString(), PtuFlexRequest.class)
                .setParameter("sequence", flexRequestSequenceNumber)
                .setParameter("participantDomain", participantDomain)
                .setParameter("usefIdentifier", connectionGroupUsefIdentifier)
                .getResultList();
    }

    /**
     * Find list of PtuPrognosisSequences based on list of FlexOfferSequences.
     *
     * @param ptuFlexOfferSequence
     * @param participantDomain
     * @return
     */
    @SuppressWarnings("unchecked")
    public Long findPtuPrognosisSequenceByFlexOfferSequence(Long ptuFlexOfferSequence, String participantDomain) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fr.prognosisSequence FROM PtuFlexRequest fr WHERE fr.sequence = "
                + "( SELECT DISTINCT fo.flexRequestSequence FROM PtuFlexOffer fo WHERE fo.sequence = :ptuFlexOfferSequence "
                + " AND fo.participantDomain = :participantDomain )");
        List results = entityManager.createQuery(sql.toString())
                .setMaxResults(1)
                .setParameter("ptuFlexOfferSequence", ptuFlexOfferSequence)
                .setParameter("participantDomain", participantDomain)
                .getResultList();
        //should always be available, otherwise there is a data issue.
        return (Long) results.get(0);
    }

    /**
     * Finds all flex requests candidate to settlement process (period of the flex request within the interval defined by the two
     * given dates and status ACCEPTED).
     *
     * @param startDate {@link LocalDate} start date of the settlement (inclusive).
     * @param endDate {@link LocalDate} end date of the settlement (inclusive).
     * @return a {@link List} of {@link PtuFlexRequest}.
     */
    public List<PtuFlexRequest> findFlexRequestsForSettlement(LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT request ");
        sql.append("FROM PtuFlexRequest request, PlanboardMessage pm ");
        sql.append("WHERE request.ptuContainer.ptuDate >= :startDate ");
        sql.append("  AND request.ptuContainer.ptuDate <= :endDate ");
        sql.append("  AND pm.period = request.ptuContainer.ptuDate ");
        sql.append("  AND pm.participantDomain = request.participantDomain ");
        sql.append("  AND pm.sequence = request.sequence ");
        sql.append("  AND pm.connectionGroup = request.connectionGroup ");
        sql.append("  AND pm.documentStatus IN (:statuses) ");
        sql.append("ORDER BY request.ptuContainer.ptuDate, request.participantDomain, request.ptuContainer.ptuIndex ");
        return getEntityManager().createQuery(sql.toString(), PtuFlexRequest.class)
                .setParameter("startDate", startDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("endDate", endDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("statuses",
                        Arrays.asList(DocumentStatus.ACCEPTED, DocumentStatus.RECEIVED_OFFER, DocumentStatus.RECEIVED_EMPTY_OFFER,
                                DocumentStatus.PROCESSED))
                .getResultList();
    }
    /**
     * Delete all {@link PtuFlexRequest}s for a certain date.
     *
     * @param period
     * @return the number of {@link PtuFlexRequest}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PtuFlexRequest pfr ");
        sql.append("WHERE pfr.ptuContainer IN (SELECT pc FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate)");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
    }
}
