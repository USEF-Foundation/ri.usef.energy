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

import javax.ejb.Stateless;

import org.joda.time.LocalDate;

import energy.usef.core.repository.BaseRepository;
import energy.usef.dso.model.PrognosisUpdateDeviation;

/**
 * Repository class in charge of the {@link PrognosisUpdateDeviation} entities.
 */
@Stateless
public class PrognosisUpdateDeviationRepository extends BaseRepository<PrognosisUpdateDeviation> {
    /**
     * Delete all {@link PrognosisUpdateDeviation}s for a certain date.
     *
     * @param period
     * @return the number of {@link PrognosisUpdateDeviation}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PrognosisUpdateDeviation pud WHERE pud.ptuDate = :ptuDate");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
    }

}
