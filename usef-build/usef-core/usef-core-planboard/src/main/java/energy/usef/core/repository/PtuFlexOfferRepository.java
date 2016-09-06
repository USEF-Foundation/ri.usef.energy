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

import static java.util.stream.Collectors.groupingBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import org.joda.time.LocalDate;

import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.Exchange;
import energy.usef.core.model.PtuFlexOffer;

/**
 * Repository class for the {@link PtuFlexOffer} entity. This class is in charge of database operations on the tables related to
 * the
 * flex offers.
 */
@Stateless
public class PtuFlexOfferRepository extends BaseRepository<PtuFlexOffer> {

    /**
     * Find single PtuFlexOffer based on given parameters.
     * <p>
     *
     * @param flexOfferSequenceNumber {@link Long} sequence number of the {@link PtuFlexOffer}s
     * @param participantDomain       {@link String} domain name of the corresponding participant.
     * @return {@link Map<Integer, PtuFlexOffer>} PtuFLexOfferMap per ptu index.
     */
    public Map<Integer, PtuFlexOffer> findPtuFlexOffer(@NotNull Long flexOfferSequenceNumber, @NotNull String participantDomain) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fo ");
        sql.append("FROM PtuFlexOffer fo ");
        sql.append("WHERE fo.sequence = :sequenceNumber ");
        sql.append("  AND fo.participantDomain = :participantDomain ");
        return getEntityManager().createQuery(sql.toString(), PtuFlexOffer.class)
                .setParameter("sequenceNumber", flexOfferSequenceNumber)
                .setParameter("participantDomain", participantDomain)
                .getResultList()
                .stream().collect(Collectors.toMap(fo -> fo.getPtuContainer().getPtuIndex(), Function.identity()));
    }

    /**
     * Finds the {@link PtuFlexOffer} entities linked to each {@link AcknowledgementStatus#ACCEPTED}
     * {@link energy.usef.core.model.PtuFlexOrder} for the given period.
     *
     * @param startDate {@link LocalDate} starting date of the period.
     * @param endDate   {@link LocalDate} ending date of the period.
     * @return a {@link Map} of the offers grouped by:
     * <ol>
     * <li>Participant Domain ({@link String})</li>
     * <li>Flex Offer Sequence number {@link Long})</li>
     * <li>Period {@link LocalDate})</li>
     * <li>PTU index ({@link Integer})</li>
     * </ol>
     */
    public Map<String, Map<Long, Map<LocalDate, Map<Integer, PtuFlexOffer>>>>
    findPtuFlexOffersOfAcceptedOrdersForPeriodByDomainSequencePeriodAndPtuIndex(
            LocalDate startDate, LocalDate endDate) {
        StringBuilder flexOfferSequences = new StringBuilder();
        flexOfferSequences.append("SELECT orders.flexOfferSequence FROM PtuFlexOrder orders ");
        flexOfferSequences.append("WHERE orders.ptuContainer.ptuDate >= :startDate ");
        flexOfferSequences.append("  AND orders.ptuContainer.ptuDate <= :endDate ");
        flexOfferSequences.append("  AND orders.acknowledgementStatus = :acknowledgementStatus ");

        StringBuilder flexOffers = new StringBuilder();
        flexOffers.append("SELECT offers FROM PtuFlexOffer offers ");
        flexOffers.append("WHERE offers.sequence IN (").append(flexOfferSequences).append(") ");

        return getEntityManager().createQuery(flexOffers.toString(), PtuFlexOffer.class)
                .setParameter("startDate", startDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("endDate", endDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("acknowledgementStatus", AcknowledgementStatus.ACCEPTED).getResultList().stream().collect(
                        groupingBy(Exchange::getParticipantDomain, groupingBy(ptuFlexOffer -> ptuFlexOffer.getSequence(),
                                groupingBy(ptuFlexOffer -> ptuFlexOffer.getPtuContainer().getPtuDate(), Collectors
                                        .toMap(ptuFlexOffer -> ptuFlexOffer.getPtuContainer().getPtuIndex(),
                                                Function.identity())))));
    }

    /**
     * Finds the {@link PtuFlexOffer} entities linked to each {@link AcknowledgementStatus#ACCEPTED}
     * {@link energy.usef.core.model.PtuFlexOrder} for the given period.
     *
     * @param startDate {@link LocalDate} starting date of the period.
     * @param endDate   {@link LocalDate} ending date of the period.
     * @return a {@link Map} of the offers grouped by:
     * <ol>
     * <li>Participant Domain ({@link String})</li>
     * <li>Connection Group USEF Identifier {@link String})</li>
     * <li>Period {@link LocalDate})</li>
     * <li>PTU index ({@link Integer})</li>
     * </ol>
     */
    public Map<String, Map<String, Map<LocalDate, Map<Integer, List<PtuFlexOffer>>>>> findPtuFlexOffersOfAcceptedOrdersForPeriod(
            LocalDate startDate, LocalDate endDate) {
        StringBuilder flexOfferSequences = new StringBuilder();
        flexOfferSequences.append("SELECT orders.flexOfferSequence FROM PtuFlexOrder orders ");
        flexOfferSequences.append("WHERE orders.ptuContainer.ptuDate >= :startDate ");
        flexOfferSequences.append("  AND orders.ptuContainer.ptuDate <= :endDate ");
        flexOfferSequences.append("  AND orders.acknowledgementStatus = :acknowledgementStatus ");

        StringBuilder flexOffers = new StringBuilder();
        flexOffers.append("SELECT offers FROM PtuFlexOffer offers ");
        flexOffers.append("WHERE offers.sequence IN (").append(flexOfferSequences).append(") ");

        return getEntityManager().createQuery(flexOffers.toString(), PtuFlexOffer.class)
                .setParameter("startDate", startDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("endDate", endDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("acknowledgementStatus", AcknowledgementStatus.ACCEPTED)
                .getResultList()
                .stream()
                .collect(groupingBy(Exchange::getParticipantDomain,
                        groupingBy(ptuFlexOffer -> ptuFlexOffer.getConnectionGroup().getUsefIdentifier(),
                                groupingBy(ptuFlexOffer -> ptuFlexOffer.getPtuContainer().getPtuDate(),
                                        Collectors.groupingBy(ptuFlexOffer -> ptuFlexOffer.getPtuContainer().getPtuIndex())))));
    }

    /**
     * Find the {@link PtuFlexOffer} with the given sequence number for a give participant and a given ptu.
     *
     * @param sequenceNumber    {@link Long} sequence number of the flex offer.
     * @param participantDomain {@link String} domain of the participant related to the flex offer.
     * @param ptuIndex          {@link Integer} index of the ptu for the flex offer.
     * @return a {@link PtuFlexOffer} or <code>null</code> if no record exists.
     */
    public PtuFlexOffer findPtuFlexOfferWithSequence(Long sequenceNumber, String participantDomain, Integer ptuIndex) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fo ");
        sql.append("FROM PtuFlexOffer fo ");
        sql.append("WHERE fo.sequence = :sequenceNumber ");
        sql.append(" AND fo.participantDomain = :participantDomain ");
        sql.append(" AND fo.ptuContainer.ptuIndex = :ptuIndex ");

        List<PtuFlexOffer> resultList = getEntityManager().createQuery(sql.toString(), PtuFlexOffer.class)
                .setParameter("sequenceNumber", sequenceNumber)
                .setParameter("participantDomain", participantDomain)
                .setParameter("ptuIndex", ptuIndex)
                .getResultList();
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }
        return resultList.get(0);
    }

    /**
     * Finds the {@link PtuFlexOffer} entities relevant for settlement for a given period.
     *
     * @param startDate {@link LocalDate} start date of the settlement period (inclusive).
     * @param endDate   {@link LocalDate} end date of the settlement period (inclusive).
     * @return a {@link List} of {@link PtuFlexOffer}.
     */
    public List<PtuFlexOffer> findFlexOffersForSettlement(LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT offer ");
        sql.append("FROM PtuFlexOffer offer, PlanboardMessage pm ");
        sql.append("WHERE offer.ptuContainer.ptuDate >= :startDate ");
        sql.append("  AND offer.ptuContainer.ptuDate <= :endDate ");
        sql.append("  AND pm.period = offer.ptuContainer.ptuDate ");
        sql.append("  AND pm.participantDomain = offer.participantDomain ");
        sql.append("  AND pm.sequence = offer.sequence ");
        sql.append("  AND pm.connectionGroup = offer.connectionGroup ");
        sql.append("  AND pm.documentStatus IN (:statuses) ");
        sql.append("ORDER BY offer.ptuContainer.ptuDate, offer.participantDomain, offer.ptuContainer.ptuIndex ");
        return getEntityManager().createQuery(sql.toString(), PtuFlexOffer.class)
                .setParameter("startDate", startDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("endDate", endDate.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("statuses", Arrays.asList(DocumentStatus.ACCEPTED, DocumentStatus.PROCESSED))
                .getResultList();
    }

    /**
     * Finds all the flex offers with are referenced by a flex order during the given period.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link LocalDate} of all the {@link PtuFlexOffer} entities.
     */
    public List<PtuFlexOffer> findFlexOffersWithOrderInPeriod(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT foffer ");
        sql.append("FROM PtuFlexOffer foffer, PtuFlexOrder forder ");
        sql.append("WHERE foffer.ptuContainer.ptuDate = :period ");
        sql.append("  AND forder.ptuContainer.ptuDate = :period ");
        sql.append("  AND forder.participantDomain = foffer.participantDomain ");
        sql.append("  AND forder.flexOfferSequence = foffer.sequence ");
        sql.append("  AND forder.ptuContainer.ptuIndex = foffer.ptuContainer.ptuIndex ");

        return getEntityManager().createQuery(sql.toString(), PtuFlexOffer.class)
                .setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Finds all the placed flex offers for a period (i.e. valid, active and non-revoked).
     *
     * @param period {@link LocalDate} period.
     * @return a {@link List} of {@link PtuFlexOffer}, ordered by participant domain, sequence number and ptu index.
     */
    public List<PtuFlexOffer> findPlacedFlexOffers(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT foffer ");
        sql.append("FROM PtuFlexOffer foffer, PlanboardMessage pm ");
        sql.append("WHERE foffer.ptuContainer.ptuDate = :period ");
        sql.append("  AND pm.period = :period ");
        sql.append("  AND pm.sequence = foffer.sequence ");
        sql.append("  AND pm.participantDomain = foffer.participantDomain ");
        sql.append("  AND pm.documentStatus IN (:statuses) ");
        sql.append("ORDER BY foffer.participantDomain, foffer.sequence, foffer.ptuContainer.ptuIndex ");
        return getEntityManager().createQuery(sql.toString(), PtuFlexOffer.class)
                .setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("statuses", Arrays.asList(DocumentStatus.ACCEPTED, DocumentStatus.SENT))
                .getResultList();
    }

    /**
     * Delete all {@link PtuFlexOffer}s for a certain date.
     *
     * @param period
     * @return the number of {@link PtuFlexOffer}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PtuFlexOffer pfo ");
        sql.append("WHERE pfo.ptuContainer IN (SELECT pc FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate)");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
    }
}
