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

import static javax.persistence.TemporalType.TIMESTAMP;

import java.sql.Date;
import java.util.List;

import javax.ejb.Stateless;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.model.Message;
import energy.usef.core.model.MessageDirection;

/**
 * Repository class for the messages.
 */
@Stateless
public class MessageRepository extends BaseRepository<Message> {

    private static final String COMMON_REFERENCE_QUERY = "CommonReferenceQuery ";
    private static final String COMMON_REFERENCE_QUERY_RESPONSE = "CommonReferenceQueryResponse";

    /**
     * Gets an ingoing message entity corresponding to an response message.
     *
     * @param conversationId conversation Id
     * @return ingoing message entity corresponding to an response message
     */
    @SuppressWarnings("unchecked")
    public Message getMessageResponseByConversationId(String conversationId) {

        List<Message> result = entityManager
                .createQuery("SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.direction = :direction")
                .setParameter("conversationId", conversationId)
                .setParameter("direction", MessageDirection.INBOUND).getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Gets a message entity by message id.
     *
     * @param messageId message Id
     * @param direction direction
     * @return ingoing message entity corresponding to an response message
     */
    @SuppressWarnings("unchecked")
    public Message getMessageResponseByMessageId(String messageId, MessageDirection direction) {

        List<Message> result = entityManager
                .createQuery("SELECT m FROM Message m WHERE m.messageId = :messageId AND m.direction = :direction")
                .setParameter("messageId", messageId)
                .setParameter("direction", direction).getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Check if a message Id has been used before.
     *
     * @param messageId - UUID {@link String}
     * @return true if the messageId is already in use
     */
    public boolean isMessageIdAlreadyUsed(String messageId) {
        Long messageCount = (Long) entityManager.createQuery("SELECT COUNT(m) FROM Message m WHERE m.messageId = :messageId")
                .setParameter("messageId", messageId)
                .getSingleResult();
        return messageCount != null && messageCount >= 1;
    }

    /**
     * Gets the first message of a conversation based on the conversation ID.
     *
     * @param conversationId {@link String} required Conversation ID
     * @return a {@link Message} or <code>null</code> if not present in the database.
     */
    @SuppressWarnings("unchecked")
    public Message getInitialMessageOfConversation(String conversationId) {
        if (conversationId == null) {
            return null;
        }
        String query = "SELECT m "
                + "FROM Message m "
                + "WHERE m.conversationId = :conversationId AND m.direction = :direction "
                + "ORDER BY m.creationTime ASC";
        List<Message> result = entityManager.createQuery(query)
                .setParameter("conversationId", conversationId)
                .setParameter("direction", MessageDirection.OUTBOUND).getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Checks whether each outbound {@link CommonReferenceQuery} message has a related inbound {@link
     * CommonReferenceQueryResponse} for the given period.
     *
     * @param period {@link LocalDate} period.
     * @return <code>true</code> if every query has a response.
     */
    public boolean hasEveryCommonReferenceQuerySentAResponseReceived(LocalDateTime period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT queries FROM Message queries WHERE queries.conversationId NOT IN (");
        sql.append("  SELECT responses.conversationId FROM Message responses ");
        sql.append("  WHERE responses.xml LIKE :responseType ");
        sql.append("    AND YEAR(responses.creationTime) = :year ");
        sql.append("    AND MONTH(responses.creationTime) = :month ");
        sql.append("    AND DAY(responses.creationTime) = :day ");
        sql.append("  )");
        sql.append("  AND queries.xml LIKE :queryType ");
        sql.append("  AND YEAR(queries.creationTime) = :year ");
        sql.append("  AND MONTH(queries.creationTime) = :month ");
        sql.append("  AND DAY(queries.creationTime) = :day ");
        List<Message> messages = getEntityManager().createQuery(sql.toString(), Message.class)
                .setParameter("year", period.toLocalDate().getYear())
                .setParameter("month", period.toLocalDate().getMonthOfYear())
                .setParameter("day", period.toLocalDate().getDayOfMonth())
                .setParameter("queryType", "%" + COMMON_REFERENCE_QUERY + "%")
                .setParameter("responseType", "%" + COMMON_REFERENCE_QUERY_RESPONSE + "%")
                .getResultList();
        return messages.isEmpty();
    }

    /**
     * Delete all {@link Message}s created on a certain date.
     *
     * @param period
     * @return the number of {@link Message}s deleted.
     */
    public int cleanup(LocalDate period) {
        LocalDate endDate = period.plusDays(1);

        Date start = new Date(period.toDateMidnight().getMillis());
        Date end = new Date(endDate.toDateMidnight().getMillis());
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM Message m WHERE m.creationTime >= :start AND m.creationTime < :end");

        return entityManager.createQuery(sql.toString()).setParameter("start", start, TIMESTAMP).setParameter("end", end, TIMESTAMP)
                .executeUpdate();
    }
}
