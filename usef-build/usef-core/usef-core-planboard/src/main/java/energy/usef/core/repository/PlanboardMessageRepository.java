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
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import energy.usef.core.exception.TechnicalException;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.util.DateTimeUtil;

/**
 * Repository class for the {@link PlanboardMessage} entity.
 */
@Stateless
public class PlanboardMessageRepository extends BaseRepository<PlanboardMessage> {

    /**
     * Finds last planboard messages corresponding to A-Plans.
     *
     * @param period period
     * @return last planboard messages corresponding to A-Plans
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findLastAPlanPlanboardMessages(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pbm1 ");
        sql.append(" FROM PlanboardMessage pbm1");
        sql.append(" WHERE pbm1.sequence IN ");
        sql.append(" (SELECT MAX(pbm.sequence)");
        sql.append("   FROM PlanboardMessage pbm ");
        sql.append("  WHERE pbm.documentStatus <> :rejected ");
        sql.append("    AND pbm.period = :period ");
        sql.append("    AND pbm.documentType = :documentType ");
        sql.append("  GROUP BY pbm.participantDomain)");

        List<PlanboardMessage> result = entityManager
                .createQuery(sql.toString())
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("period", period.toDateMidnight().toDate())
                .setParameter("documentType", DocumentType.A_PLAN)
                .getResultList();

        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    /**
     * Finds plan board messages.
     *
     * @param sequence          corresponding document sequence
     * @param documentType      document type
     * @param participantDomain - The participantDomain which is being communicated with.
     * @return plan board messages
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessages(Long sequence, DocumentType documentType, String participantDomain) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pbm ");
        sql.append("FROM PlanboardMessage pbm ");
        sql.append("WHERE pbm.documentStatus <> :rejected ");
        sql.append("  AND pbm.sequence = :sequence ");
        sql.append("  AND pbm.documentType = :documentType ");
        sql.append("  AND pbm.participantDomain = :participantDomain ");

        List<PlanboardMessage> result = entityManager.createQuery(sql.toString())
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("sequence", sequence)
                .setParameter("documentType", documentType)
                .setParameter("participantDomain", participantDomain)
                .getResultList();
        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    /**
     * Finds a single plan board messages.
     *
     * @param sequence          corresponding document sequence
     * @param documentType      document type
     * @param participantDomain - The participantDomain which is being communicated with.
     * @return a single planboard message or <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public PlanboardMessage findSinglePlanboardMessage(Long sequence, DocumentType documentType, String participantDomain) {
        List<PlanboardMessage> result = findPlanboardMessages(sequence, documentType, participantDomain);
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Finds plan board messages.
     *
     * @param sequence        corresponding document sequence
     * @param congestionPoint congestion point
     * @param documentType    document type
     * @return plan board messages
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessages(Long sequence, String congestionPoint, DocumentType documentType) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT pbm ");
        sql.append(" FROM PlanboardMessage pbm ");
        sql.append(" WHERE pbm.documentStatus <> :rejected ");
        sql.append(" AND pbm.sequence = :sequence ");
        sql.append(" AND pbm.documentType = :documentType ");
        sql.append(" AND pbm.connectionGroup.usefIdentifier = :congestionPoint ");
        List<PlanboardMessage> result = entityManager.createQuery(sql.toString())
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("sequence", sequence)
                .setParameter("documentType", documentType)
                .setParameter("congestionPoint", congestionPoint)
                .getResultList();
        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    /**
     * Finds a single plan board message by period.
     *
     * @param period            The period
     * @param documentType      document type
     * @param participantDomain - The participantDomain which is being communicated with.
     * @return a single planboard message or <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public PlanboardMessage findSinglePlanboardMessage(LocalDate period, DocumentType documentType, String participantDomain) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pbm ");
        sql.append("FROM PlanboardMessage pbm ");
        sql.append("WHERE pbm.documentStatus <> :rejected");
        sql.append("  AND pbm.period = :period");
        sql.append("  AND pbm.documentType = :documentType ");
        sql.append("  AND pbm.participantDomain = :participantDomain ");

        List<PlanboardMessage> result = entityManager.createQuery(sql.toString())
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("period", period.toDateMidnight().toDate())
                .setParameter("documentType", documentType)
                .setParameter("participantDomain", participantDomain)
                .getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Finds a single planboard message given its business key: usef identifier of the connection group, participant domain name
     * and sequence number of the document.
     *
     * @return a {@link PlanboardMessage}.
     */
    public PlanboardMessage findSinglePlanboardMessage(String usefIdentifier, Long sequenceNumber, String participantDomain) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm FROM PlanboardMessage pm ");
        sql.append("WHERE pm.sequence = :sequenceNumber ");
        sql.append("  AND pm.participantDomain = :participantDomain ");
        sql.append("  AND pm.connectionGroup.usefIdentifier = :usefIdentifier ");
        List<PlanboardMessage> planboardMessages = getEntityManager().createQuery(sql.toString(), PlanboardMessage.class)
                .setParameter("sequenceNumber", sequenceNumber)
                .setParameter("usefIdentifier", usefIdentifier)
                .setParameter("participantDomain", participantDomain)
                .getResultList();
        if (planboardMessages.size() != 1) {
            throw new TechnicalException(
                    "Multiple planboard messages have the same business key (usefIdentifier = [" + usefIdentifier +
                            "], sequenceNumber = [" + sequenceNumber + "], participantDomain = [" + participantDomain + "])");
        }
        return planboardMessages.get(0);
    }

    /**
     * This method finds {@link PlanboardMessage} based on {@link DocumentType} and {@link DocumentStatus}.
     *
     * @param documentType   The type of document, like request, offer or order.
     * @param documentStatus The status of document, like new, submitted or rejected.
     * @return The list of {@link PlanboardMessage} which have a specific {@link DocumentType} and {@link DocumentStatus}.
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessages(DocumentType documentType, DocumentStatus documentStatus) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT pbm ");
        sql.append(" FROM PlanboardMessage pbm ");
        sql.append(" WHERE pbm.documentStatus <> :rejected ");
        sql.append(" AND pbm.documentStatus = :documentStatus ");
        sql.append(" AND pbm.documentType = :documentType ");
        Query query = entityManager.createQuery(sql.toString());
        query.setParameter("rejected", DocumentStatus.REJECTED);
        query.setParameter("documentStatus", documentStatus);
        query.setParameter("documentType", documentType);
        return query.getResultList();
    }

    /**
     * This method finds {@link PlanboardMessage} based on {@link DocumentType} and {@link DocumentStatus}.
     *
     * @param localDateTime  The LocalDateTime the message should be before.
     * @param documentType   The type of document, like request, offer or order.
     * @param documentStatus The status of document, like new, submitted or rejected.
     * @return The list of {@link PlanboardMessage} which have a specific {@link DocumentType} and {@link DocumentStatus}.
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessagesOlderThan(LocalDateTime localDateTime, DocumentType documentType,
            DocumentStatus documentStatus) {
        Query query = entityManager.createQuery("SELECT pbm FROM PlanboardMessage pbm WHERE pbm.documentType = :documentType "
                + "AND pbm.documentStatus = :documentStatus AND pbm.creationDateTime < :localDateTime");
        query.setParameter("documentStatus", documentStatus);
        query.setParameter("documentType", documentType);
        query.setParameter("localDateTime", localDateTime.toDateTime().toDate());
        return query.getResultList();
    }

    /**
     * This method finds {@link PlanboardMessage} based on {@link DocumentType} and {@link DocumentStatus} within a time frame
     * (startDate - endDate).
     *
     * @param documentType   The type of document, like request, offer or order.
     * @param period         The date of the planboard message
     * @param documentStatus The status of document, like new, submitted or rejected.
     * @return The list of {@link PlanboardMessage} which have a specific {@link DocumentType} and {@link DocumentStatus}.
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessages(DocumentType documentType, LocalDate period,
            DocumentStatus documentStatus) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE pm.documentType = :documentType ");
        sql.append(" AND pm.documentStatus <> :rejected ");
        sql.append(" AND pm.period = :period ");
        if (documentStatus != null) {
            sql.append(" AND pm.documentStatus = :documentStatus ");
        }

        Query query = getEntityManager().createQuery(sql.toString())
                .setParameter("documentType", documentType)
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("period", period.toDateMidnight().toDate());

        if (documentStatus != null) {
            query.setParameter("documentStatus", documentStatus);
        }
        return query.getResultList();

    }

    /**
     * Finds all the planboard messages of a certain {@link DocumentType} for a given participant Optionally, a specific gridpoint
     * and the {@link DocumentStatus} of the document can be specified.
     *
     * @param documentType      {@link DocumentType} mandatory document type.
     * @param participantDomain {@link String} mandatory participant domain.
     * @param usefIdentifier    {@link String} optional usefIdentifier of the {@link ConnectionGroup} related to the message.
     * @param documentStatus    {@link DocumentStatus} optional document status.
     * @return a {@link List} of {@link PlanboardMessage}.
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessages(DocumentType documentType, String participantDomain, String usefIdentifier,
            DocumentStatus documentStatus) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE pm.documentType = :documentType ");
        sql.append(" AND pm.documentStatus <> :rejected ");
        sql.append(" AND pm.participantDomain = :participantDomain ");
        if (usefIdentifier != null) {
            sql.append(" AND pm.connectionGroup.usefIdentifier= :usefIdentifier ");
        }
        if (documentStatus != null) {
            sql.append(" AND pm.documentStatus = :documentStatus ");
        }
        sql.append("ORDER BY pm.sequence DESC ");

        Query query = getEntityManager().createQuery(sql.toString())
                .setParameter("documentType", documentType)
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("participantDomain", participantDomain);
        if (usefIdentifier != null) {
            query.setParameter("usefIdentifier", usefIdentifier);
        }
        if (documentStatus != null) {
            query.setParameter("documentStatus", documentStatus);
        }
        return query.getResultList();

    }

    /**
     * Finds maximal planboard message sequence of a certain {@link DocumentType} for a given participant optionally, a specific
     * USEF identifier and the {@link DocumentStatus} of the planboard message can be specified.
     *
     * @param documentType      {@link DocumentType} mandatory document type.
     * @param participantDomain {@link String} mandatory participant domain.
     * @param period            {@link LocalDate} mandatory period.
     * @param usefIdentifier    {@link String} optional usefIdentifier of the {@link ConnectionGroup} related to the message.
     * @param documentStatus    {@link DocumentStatus} optional document status.
     * @return a {@link Long} maximal planboard message sequence, if there is no record in the table 0 value is returned .
     */
    public Long findMaxPlanboardMessageSequence(DocumentType documentType, String participantDomain, LocalDate period,
            String usefIdentifier,
            DocumentStatus documentStatus) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT MAX(pm.sequence) ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE pm.documentType = :documentType ");
        sql.append(" AND pm.documentStatus <> :rejected ");
        sql.append(" AND pm.participantDomain = :participantDomain ");
        sql.append(" AND pm.period = :period ");
        if (usefIdentifier != null) {
            sql.append(" AND pm.connectionGroup.usefIdentifier= :usefIdentifier ");
        }
        if (documentStatus != null) {
            sql.append(" AND pm.documentStatus = :documentStatus ");
        }

        Query query = getEntityManager().createQuery(sql.toString())
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("documentType", documentType)
                .setParameter("participantDomain", participantDomain)
                .setParameter("period", period.toDateMidnight().toDate());
        if (usefIdentifier != null) {
            query.setParameter("usefIdentifier", usefIdentifier);
        }
        if (documentStatus != null) {
            query.setParameter("documentStatus", documentStatus);
        }

        if (query.getResultList().isEmpty() || query.getResultList().get(0) == null) {
            return 0L;
        }

        return (Long) query.getResultList().get(0);
    }

    /**
     * Finds all the planboard messages of a certain {@link DocumentType} for a given participant. Optionally, a specific gridpoint
     * and the {@link DocumentStatus} of the document can be specified. Optionally, a period can be defined.
     *
     * @param documentType      {@link DocumentType} mandatory document type.
     * @param participantDomain {@link String} mandatory participant domain.
     * @param usefIdentifier    {@link String} optional entity address of the congestion point related to the message.
     * @param documentStatus    {@link DocumentStatus} optional document status.
     * @param validFrom         {@link LocalDate} optional starting date of the period.
     * @param validUntil        {@link LocalDate} optional ending date of the period.
     * @return a {@link List} of {@link PlanboardMessage}.
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessages(DocumentType documentType, String participantDomain,
            String usefIdentifier,
            DocumentStatus documentStatus, LocalDate validFrom, LocalDate validUntil) {
        StringBuilder sql = createSqlStringForFindPlanboardMessage(participantDomain, usefIdentifier, documentStatus, validFrom,
                validUntil);

        Query query = buildQueryForFindPlanboardMessage(documentType, participantDomain, usefIdentifier, documentStatus, validFrom,
                validUntil, sql);
        return query.getResultList();

    }

    /**
     * Finds all the {@link PlanboardMessage} entities with the given document type, for a given period and related to a given
     * connection group.
     *
     * @param documentType   {@link DocumentType} of the message.
     * @param period         {@link LocalDate} period of the message.
     * @param usefIdentifier {@link String} USEF identifier of the connection group of the message.
     * @return a {@link List} of {@link PlanboardMessage}.
     */
    public List<PlanboardMessage> findAcceptedPlanboardMessagesForConnectionGroup(DocumentType documentType, LocalDate period,
            String usefIdentifier) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm FROM PlanboardMessage pm ");
        sql.append("WHERE pm.documentType = :documentType ");
        sql.append("  AND pm.period = :period ");
        sql.append("  AND pm.documentStatus = :documentStatus ");
        sql.append("  AND pm.connectionGroup.usefIdentifier = :usefIdentifier ");
        sql.append("ORDER BY pm.participantDomain, pm.sequence");
        return getEntityManager().createQuery(sql.toString(), PlanboardMessage.class)
                .setParameter("documentType", documentType)
                .setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("usefIdentifier", usefIdentifier)
                .setParameter("documentStatus", DocumentStatus.ACCEPTED)
                .getResultList();
    }

    private Query buildQueryForFindPlanboardMessage(DocumentType documentType, String participantDomain, String usefIdentifier,
            DocumentStatus documentStatus, LocalDate validFrom, LocalDate validUntil, StringBuilder sql) {
        Query query = getEntityManager().createQuery(sql.toString())
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("documentType", documentType);
        if (participantDomain != null) {
            query.setParameter("participantDomain", participantDomain);
        }
        if (usefIdentifier != null) {
            query.setParameter("usefIdentifier", usefIdentifier);
        }
        if (documentStatus != null) {
            query.setParameter("documentStatus", documentStatus);
        }
        if (validFrom != null) {
            query.setParameter("validFrom", validFrom.toDateMidnight().toDate());
        }
        if (validUntil != null) {
            query.setParameter("validUntil", validUntil.toDateMidnight().toDate());
        }
        return query;
    }

    private StringBuilder createSqlStringForFindPlanboardMessage(String participantDomain, String usefIdentifier,
            DocumentStatus documentStatus, LocalDate validFrom, LocalDate validUntil) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE pm.documentStatus <> :rejected ");
        sql.append(" AND pm.documentType = :documentType ");
        if (participantDomain != null) {
            sql.append(" AND pm.participantDomain = :participantDomain ");
        }
        if (usefIdentifier != null) {
            sql.append(" AND pm.connectionGroup.usefIdentifier = :usefIdentifier ");
        }
        if (documentStatus != null) {
            sql.append(" AND pm.documentStatus = :documentStatus ");
        }
        if (validFrom != null) {
            sql.append(" AND pm.period >= :validFrom ");
        }
        if (validUntil != null) {
            sql.append(" AND pm.period <= :validUntil ");
        }
        sql.append("ORDER BY pm.sequence DESC ");
        return sql;
    }

    /**
     * Find all the planboard messages of a given type for a given participant with the specified origin sequence number.
     *
     * @param originSequence    {@link Long} Origin Sequence Number.
     * @param documentType      {@link DocumentType} document type of the planboard message.
     * @param participantDomain {@link String} participant domain.
     * @return a {@link List} of {@link PlanboardMessage}.
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessagesWithOriginSequence(Long originSequence, DocumentType documentType,
            String participantDomain) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE pm.documentStatus <> :rejected ");
        sql.append(" AND pm.originSequence = :originSequence ");
        sql.append(" AND pm.documentType = :documentType ");
        sql.append(" AND pm.participantDomain = :participantDomain ");

        return getEntityManager().createQuery(sql.toString())
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("originSequence", originSequence)
                .setParameter("documentType", documentType)
                .setParameter("participantDomain", participantDomain)
                .getResultList();
    }

    /**
     * Find all the planboard messages of a given type for a given document status with the specified origin sequence number.
     *
     * @param originSequence {@link Long} origin Sequence Number.
     * @param documentType   {@link DocumentType} document type of the planboard message.
     * @param documentStatus {@link DocumentStatus} document status.
     * @return the {@link PlanboardMessage} matching the requested parameters.
     */
    @SuppressWarnings("unchecked")
    public PlanboardMessage findPlanboardMessagesWithOriginSequence(Long originSequence, DocumentType documentType,
            DocumentStatus documentStatus) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE pm.originSequence = :originSequence ");
        sql.append(" AND pm.documentType = :documentType ");
        sql.append(" AND pm.documentStatus = :documentStatus ");

        List<PlanboardMessage> resultList = getEntityManager().createQuery(sql.toString())
                .setParameter("originSequence", originSequence)
                .setParameter("documentType", documentType)
                .setParameter("documentStatus", documentStatus)
                .getResultList();
        if (resultList.size() == 1) {
            return resultList.get(0);
        }
        return null;
    }

    /**
     * Find all the planboard messages of a given type for a given document status with a time period (startDate until endDate).
     *
     * @param documentType   {@link DocumentType} document type of the planboard message.
     * @param startDate      {@link LocalDate} starting date of the period.
     * @param endDate        {@link LocalDate} end date of the period.
     * @param documentStatus {@link DocumentStatus} optional document status.
     * @return the {@link PlanboardMessage} matching the requested parameters.
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessages(DocumentType documentType, LocalDate startDate, LocalDate endDate,
            DocumentStatus documentStatus) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE pm.documentType = :documentType ");
        sql.append(" AND pm.documentStatus <> :rejected ");
        sql.append(" AND pm.period BETWEEN :startDate AND :endDate ");
        if (documentStatus != null) {
            sql.append(" AND pm.documentStatus = :documentStatus ");
        }

        Query query = getEntityManager().createQuery(sql.toString())
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("documentType", documentType)
                .setParameter("startDate", startDate.toDateMidnight().toDate())
                .setParameter("endDate", endDate.toDateMidnight().toDate());
        if (documentStatus != null) {
            query.setParameter("documentStatus", documentStatus);
        }
        return query.getResultList();

    }

    /**
     * This method finds rejected {@link PlanboardMessage} based on {@link DocumentType} and {@link DocumentStatus} within a time
     * frame
     * (startDate - endDate).
     *
     * @param documentType The type of document, like request, offer or order.
     * @param period       The date of the planboard message
     * @return The list of {@link PlanboardMessage} which have a specific {@link DocumentType} and {@link DocumentStatus}.
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findRejectedPlanboardMessages(DocumentType documentType, LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE pm.documentType = :documentType ");
        sql.append(" AND pm.documentStatus = :rejected ");
        sql.append(" AND pm.period = :period ");

        Query query = getEntityManager().createQuery(sql.toString())
                .setParameter("documentType", documentType)
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("period", period.toDateMidnight().toDate());

        return query.getResultList();

    }

    /**
     * Finds A-Plans related to the flex offer.
     *
     * @param flexOfferSequenceNumber flex offer sequence number
     * @param participantDomain       participant domain
     * @return A-Plans related to the flex offer
     */
    public PlanboardMessage findAPlanRelatedToFlexOffer(Long flexOfferSequenceNumber, String participantDomain) {
        StringBuilder offers = new StringBuilder();
        offers.append("SELECT offers.originSequence ");
        offers.append("FROM PlanboardMessage offers ");
        offers.append(" WHERE offers.documentStatus <> :rejected ");
        offers.append(" AND offers.participantDomain = :participantDomain ");
        offers.append(" AND offers.documentType = :flexOfferType ");
        offers.append(" AND offers.sequence = :flexOfferSequenceNumber ");

        StringBuilder requests = new StringBuilder();
        requests.append("SELECT requests.originSequence ");
        requests.append("FROM PlanboardMessage requests ");
        requests.append(" WHERE requests.documentStatus <> :rejected ");
        requests.append(" AND requests.participantDomain = :participantDomain ");
        requests.append(" AND requests.documentType = :flexRequestType ");
        requests.append(" AND requests.sequence IN (").append(offers).append(")");

        StringBuilder aPlans = new StringBuilder();
        aPlans.append("SELECT prognoses ");
        aPlans.append("FROM PlanboardMessage prognoses ");
        aPlans.append("WHERE prognoses.documentStatus <> :rejected ");
        aPlans.append(" AND prognoses.participantDomain = :participantDomain ");
        aPlans.append(" AND prognoses.documentType = :aPlanType ");
        aPlans.append(" AND prognoses.sequence IN (").append(requests).append(")");

        @SuppressWarnings("unchecked")
        List<PlanboardMessage> result = getEntityManager().createQuery(aPlans.toString())
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("flexOfferSequenceNumber", flexOfferSequenceNumber)
                .setParameter("participantDomain", participantDomain)
                .setParameter("flexOfferType", DocumentType.FLEX_OFFER)
                .setParameter("flexRequestType", DocumentType.FLEX_REQUEST)
                .setParameter("aPlanType", DocumentType.A_PLAN)
                .getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Find all the flex orders related to prognosis with sequence number and participain domain.
     *
     * @param prognosisSequenceNumber the sequence number of the prognosis
     * @param participantDomain       the participant domain
     * @return a {@link List} of {@link PlanboardMessage} objects matching the requested parameters.
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findFlexOrdersRelatedToPrognosis(Long prognosisSequenceNumber, String participantDomain) {

        StringBuilder prognoses = new StringBuilder();
        prognoses.append("SELECT prognoses.sequence ");
        prognoses.append("FROM PlanboardMessage prognoses ");
        prognoses.append("WHERE prognoses.documentStatus <> :rejected ");
        prognoses.append(" AND prognoses.participantDomain = :participantDomain ");
        prognoses.append(" AND (prognoses.documentType = :dPrognosisType OR prognoses.documentType = :aPlanType) ");
        prognoses.append(" AND prognoses.sequence = :sequence ");

        StringBuilder requests = new StringBuilder();
        requests.append("SELECT requests.sequence ");
        requests.append("FROM PlanboardMessage requests ");
        requests.append("WHERE requests.documentStatus <> :rejected ");
        requests.append(" AND requests.participantDomain = :participantDomain ");
        requests.append(" AND requests.documentType = :flexRequestType ");
        requests.append(" AND requests.originSequence IN (").append(prognoses).append(")");

        StringBuilder offers = new StringBuilder();
        offers.append("SELECT offers.sequence ");
        offers.append("FROM PlanboardMessage offers ");
        offers.append("WHERE offers.documentStatus <> :rejected ");
        offers.append(" AND offers.participantDomain = :participantDomain ");
        offers.append(" AND offers.documentType = :flexOfferType ");
        offers.append(" AND offers.originSequence IN (").append(requests).append(")");

        StringBuilder orders = new StringBuilder();
        orders.append("SELECT orders ");
        orders.append("FROM PlanboardMessage orders ");
        orders.append("WHERE orders.documentStatus <> :rejected ");
        orders.append(" AND orders.participantDomain = :participantDomain ");
        orders.append(" AND orders.documentType = :flexOrderType ");
        orders.append(" AND orders.originSequence IN (").append(offers).append(")");

        return getEntityManager().createQuery(orders.toString())
                .setParameter("rejected", DocumentStatus.REJECTED)
                .setParameter("sequence", prognosisSequenceNumber)
                .setParameter("participantDomain", participantDomain)
                .setParameter("flexOrderType", DocumentType.FLEX_ORDER)
                .setParameter("flexOfferType", DocumentType.FLEX_OFFER)
                .setParameter("flexRequestType", DocumentType.FLEX_REQUEST)
                .setParameter("dPrognosisType", DocumentType.D_PROGNOSIS)
                .setParameter("aPlanType", DocumentType.A_PLAN)
                .getResultList();
    }

    /**
     * Update the {@link PlanboardMessage} entities of type {@link DocumentType#FLEX_ORDER_SETTLEMENT} with a passed expiration date
     * and a document status {@link DocumentStatus#SENT}.
     *
     * @return the number of updated entities.
     */
    public int updateOldSettlementMessageDisposition() {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE PlanboardMessage ");
        sql.append("SET documentStatus = :disposition ");
        sql.append("WHERE expirationDate < :now ");
        sql.append(" AND documentStatus = :sent ");
        sql.append(" AND documentType = :flexOrderSettlement ");
        return getEntityManager().createQuery(sql.toString())
                .setParameter("disposition", DocumentStatus.DISPUTED)
                .setParameter("sent", DocumentStatus.SENT)
                .setParameter("flexOrderSettlement", DocumentType.FLEX_ORDER_SETTLEMENT)
                .setParameter("now", DateTimeUtil.getCurrentDateTime().toDateTime().toDate(), TemporalType.TIMESTAMP)
                .executeUpdate();
    }

    /**
     * Find Prognosis for period for connectionGroupIdentifier and/or ParticipantDomain.
     *
     * @param date                      period ({@link LocalDate})
     * @param connectionGroupIdentifier (optional)
     * @param participantDomain         (optional)
     * @return A {@link List} of {@link PlanboardMessage} objects
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPrognosisRelevantForDateByUsefIdentifier(LocalDate date, String connectionGroupIdentifier,
            String participantDomain) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE ( pm.documentType = 'A_PLAN' OR pm.documentType = 'D_PROGNOSIS' )");
        sql.append(" AND pm.period = :date ");
        sql.append(" AND pm.documentStatus IN (:statuses)  ");
        if (participantDomain != null) {
            sql.append(" AND pm.participantDomain = :participantDomain ");
        }
        if (connectionGroupIdentifier != null) {
            sql.append(" AND pm.connectionGroup.usefIdentifier = :connectionGroupIdentifier ");
        }

        Query query = getEntityManager().createQuery(sql.toString());
        query.setParameter("date", date.toDateMidnight().toDate(), TemporalType.DATE);
        query.setParameter("statuses", Arrays.asList(DocumentStatus.ACCEPTED, DocumentStatus.ARCHIVED, DocumentStatus.FINAL));
        if (participantDomain != null) {
            query.setParameter("participantDomain", participantDomain);
        }
        if (connectionGroupIdentifier != null) {
            query.setParameter("connectionGroupIdentifier", connectionGroupIdentifier);
        }
        return query.getResultList();
    }

    /**
     * Find Prognosis for period for connectionGroupIdentifier.
     *
     * @param date                      the period ({@link LocalDate})
     * @param connectionGroupIdentifier (optional)
     * @return A {@link List} of {@link PlanboardMessage} objects
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPrognosisRelevantForDate(LocalDate date, String connectionGroupIdentifier) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT pm ");
        sql.append("FROM PlanboardMessage pm ");
        sql.append("WHERE ( pm.documentType = 'A_PLAN' OR pm.documentType = 'D_PROGNOSIS' )");
        sql.append(" AND documentStatus <> :rejected ");
        sql.append(" AND pm.period = :date ");
        sql.append(" AND pm.connectionGroup.usefIdentifier = :connectionGroupIdentifier ");

        Query query = getEntityManager().createQuery(sql.toString());
        query.setParameter("rejected", DocumentStatus.REJECTED);
        query.setParameter("date", date.toDateMidnight().toDate());
        query.setParameter("connectionGroupIdentifier", connectionGroupIdentifier);
        return query.getResultList();
    }

    /**
     * Find planboard messages.
     *
     * @param sequence          sequence number
     * @param documentType      the documenttype ({@link DocumentType})
     * @param participantDomain the domain name of the participant
     * @param congestionPoint   congestion point entity address
     * @param documentStatusses one or more document statusses ({@link DocumentStatus})
     * @return A {@link List} of {@link PlanboardMessage} objects
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessages(Long sequence, DocumentType documentType, String participantDomain,
            String congestionPoint, DocumentStatus... documentStatusses) {

        List<PlanboardMessage> result = entityManager.createQuery(
                "SELECT pbm FROM PlanboardMessage pbm WHERE " + "pbm.sequence = :sequence "
                        + " AND pbm.documentType = :documentType"
                        + " AND pbm.participantDomain = :participantDomain"
                        + " AND pbm.connectionGroup.usefIdentifier = :congestionPoint"
                        + " AND pbm.documentStatus IN ( :documentStatusList )")

                .setParameter("sequence", sequence)
                .setParameter("documentType", documentType)
                .setParameter("participantDomain", participantDomain)
                .setParameter("congestionPoint", congestionPoint)
                .setParameter("documentStatusList", Arrays.asList(documentStatusses))

                .getResultList();
        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    /**
     * Finds accepted flex offers to place flex orders.
     *
     * @return flex offer message list
     */
    public List<PlanboardMessage> findOrderableFlexOffers() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT flexOffer FROM PlanboardMessage flexOffer ");
        sql.append("WHERE flexOffer.documentType = :flexOfferDocumentType ");
        sql.append("  AND flexOffer.documentStatus IN :flexOfferDocumentStatus ");
        sql.append("  AND flexOffer.period >= :today ");
        sql.append("  AND flexOffer.sequence NOT IN (");
        sql.append("    SELECT forder.originSequence FROM PlanboardMessage forder ");
        sql.append("    WHERE forder.participantDomain = flexOffer.participantDomain ");
        sql.append("      AND forder.documentType = :flexOrderDocumentType ");
        sql.append("      AND forder.documentStatus in :flexOrderStatuses ");
        sql.append("      AND forder.period = flexOffer.period ) ");
        return getEntityManager().createQuery(sql.toString(), PlanboardMessage.class)
                .setParameter("flexOfferDocumentType", DocumentType.FLEX_OFFER)
                .setParameter("flexOfferDocumentStatus", Arrays.asList(DocumentStatus.ACCEPTED))
                .setParameter("flexOrderDocumentType", DocumentType.FLEX_ORDER)
                .setParameter("flexOrderStatuses", Arrays.asList(DocumentStatus.ACCEPTED, DocumentStatus.SENT))
                .setParameter("today", DateTimeUtil.getCurrentDate().toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();

    }

    /**
     * Delete all {@link PlanboardMessage}s created for a specific period.
     *
     * @param period
     * @return the number of {@link PlanboardMessage}s deleted.
     */
    public int cleanup(LocalDate period) {
        String sql = "DELETE FROM PlanboardMessage pm WHERE pm.period = :period";

        return entityManager.createQuery(sql).setParameter("period", period.toDateMidnight().toDate()).executeUpdate();
    }
}
