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

import energy.usef.agr.model.DeviceMessage;
import energy.usef.agr.model.DeviceMessageStatus;
import energy.usef.core.repository.BaseRepository;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;

import javax.ejb.Stateless;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Repository class for the {@link DeviceMessage} entity. This class provides methods to interact with the aggregator database.
 */
@Stateless
public class DeviceMessageRepository extends BaseRepository<DeviceMessage> {

    /**
     * Find all the device messages.
     *
     * @return a {@link List} of {@link DeviceMessage}.
     */
    public List<DeviceMessage> findDeviceMessages() {
        return findDeviceMessages(null, null);
    }

    /**
     * Find the device messages having the data matching the method parameters.
     *
     * @param connectionEntityAddress {@link String} optional entity address of the connection.
     * @param deviceMessageStatus     {@link DeviceMessageStatus} optional status of the device message.
     * @return a {@link List} of {@link DeviceMessage}.
     */
    public List<DeviceMessage> findDeviceMessages(String connectionEntityAddress, DeviceMessageStatus deviceMessageStatus) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT dm FROM DeviceMessage dm ");
        sql.append("WHERE 1 = 1 ");
        if (StringUtils.isNotBlank(connectionEntityAddress)) {
            sql.append("  AND dm.udi.connection.entityAddress = :connectionEntityAddress ");
        }
        if (deviceMessageStatus != null) {
            sql.append("  AND dm.deviceMessageStatus = :status ");
        }

        TypedQuery<DeviceMessage> query = getEntityManager().createQuery(sql.toString(), DeviceMessage.class);
        if (StringUtils.isNotBlank(connectionEntityAddress)) {
            query.setParameter("connectionEntityAddress", connectionEntityAddress);
        }
        if (deviceMessageStatus != null) {
            query.setParameter("status", deviceMessageStatus);
        }
        return query.getResultList();
    }

    /**
     * Delete all {@link DeviceMessage}s for a certain date.
     *
     * @param period
     * @return the number of {@link DeviceMessage}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM DeviceMessage dm ");
        sql.append("WHERE dm.udi IN (SELECT u FROM Udi u WHERE u.validUntil = :validUntil)");

        return entityManager.createQuery(sql.toString()).setParameter("validUntil", period.toDateMidnight().plusDays(1).toDate()).executeUpdate();
    }
}
