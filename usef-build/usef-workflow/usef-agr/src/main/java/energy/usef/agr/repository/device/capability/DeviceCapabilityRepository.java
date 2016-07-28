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
