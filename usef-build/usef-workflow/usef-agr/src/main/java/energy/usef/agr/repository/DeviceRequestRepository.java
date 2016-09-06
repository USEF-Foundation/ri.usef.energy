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

package energy.usef.agr.repository;

import energy.usef.agr.model.DeviceRequest;
import energy.usef.core.repository.BaseRepository;
import org.joda.time.LocalDate;

import javax.ejb.Stateless;

/**
 * Repository class for the {@link DeviceRequest} entity. This class provides methods to interact with the aggregator database.
 */
@Stateless
public class DeviceRequestRepository extends BaseRepository<DeviceRequest> {
    /**
     * Delete all {@link DeviceRequest}s for a certain date.
     *
     * @param period
     * @return the number of {@link DeviceRequest}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM DeviceRequest dr ");
        sql.append("WHERE dr.period = :period)");

        return entityManager.createQuery(sql.toString()).setParameter("period", period.toDateMidnight().toDate()).executeUpdate();
    }

}
