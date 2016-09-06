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

import org.joda.time.LocalDate;

import energy.usef.core.model.PtuSettlement;

/**
 * Repository class in charge of the operations related to the {@link PtuSettlement} entity.
 */
public class PtuSettlementRepository extends BaseRepository<PtuSettlement> {

    /**
     * Delete all {@link PtuSettlement}s for a certain date.
     *
     * @param period
     * @return the number of {@link PtuSettlement}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PtuSettlement ps ");
        sql.append("WHERE ps.ptuContainer IN (SELECT pc FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate)");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
    }
}
