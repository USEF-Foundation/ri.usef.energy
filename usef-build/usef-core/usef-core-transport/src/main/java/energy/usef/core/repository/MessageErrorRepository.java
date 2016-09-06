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

import java.sql.Date;

import javax.ejb.Stateless;

import org.joda.time.LocalDate;

import energy.usef.core.model.MessageError;

/**
 * Repository class for message errors.
 */
@Stateless
public class MessageErrorRepository extends BaseRepository<MessageError> {
    /**
     * Delete all {@link MessageError}s for a certain date.
     *
     * @param period
     * @return the number of {@link MessageError}s deleted.
     */
    public int cleanup(LocalDate period) {
        LocalDate endDate = period.plusDays(1);

        Date start = new Date(period.toDateMidnight().getMillis());
        Date end = new Date(endDate.toDateMidnight().getMillis());

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM MessageError me ");
        sql.append("WHERE me.message IN (SELECT m FROM Message m WHERE m.creationTime >= :start AND m.creationTime < :end)");

        return entityManager.createQuery(sql.toString()).setParameter("start", start).setParameter("end", end).executeUpdate();
    }

}
