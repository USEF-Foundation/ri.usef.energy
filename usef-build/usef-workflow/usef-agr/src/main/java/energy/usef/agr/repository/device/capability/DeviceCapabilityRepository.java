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

package energy.usef.agr.repository.device.capability;

import org.joda.time.LocalDate;

import energy.usef.agr.model.device.capability.DeviceCapability;
import energy.usef.core.repository.BaseRepository;

/**
 *
 */
public class DeviceCapabilityRepository extends BaseRepository<DeviceCapability> {
    /**
     * Delete all {@link DeviceCapability}s for a certain date.
     *
     * @param period
     * @return the number of {@link DeviceCapability}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM DeviceCapability dc ");
        sql.append("WHERE dc.udiEvent IN (SELECT ue FROM UdiEvent ue WHERE ue.period = :period)");

        return entityManager.createQuery(sql.toString()).setParameter("period", period.toDateMidnight().toDate()).executeUpdate();
    }

}
