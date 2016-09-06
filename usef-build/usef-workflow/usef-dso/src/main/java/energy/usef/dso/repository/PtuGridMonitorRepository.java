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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;

import org.joda.time.LocalDate;

import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.repository.BaseRepository;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.dso.model.PtuGridMonitor;

/**
 * Repository class in charge of the management of the {@link PtuGridMonitor} entities.
 */
@Stateless
public class PtuGridMonitorRepository extends BaseRepository<PtuGridMonitor> {

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;
    /**
     * Store the actual power at this PTU. If the power already exists, store the median of the current and the new value instead.
     *
     * @param ptuContainer
     * @param actualPower
     * @param connectionGroup
     */
    @SuppressWarnings("unchecked")
    public void setActualPower(PtuContainer ptuContainer, Long actualPower, ConnectionGroup connectionGroup) {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ptugm ");
        sql.append("FROM PtuGridMonitor ptugm ");
        sql.append("WHERE ptugm.ptuContainer.ptuDate = :ptuDate ");
        sql.append("AND ptugm.ptuContainer.ptuIndex = :ptuIndex ");
        sql.append("AND ptugm.connectionGroup.usefIdentifier = :congestionPoint ");

        PtuGridMonitor ptuGridMonitor;

        List<PtuGridMonitor> ptuGridMonitorList = (List<PtuGridMonitor>) getEntityManager().createQuery(sql.toString())
                .setParameter("ptuDate", ptuContainer.getPtuDate().toDateMidnight().toDate())
                .setParameter("ptuIndex", ptuContainer.getPtuIndex())
                .setParameter("congestionPoint", connectionGroup.getUsefIdentifier())
                .getResultList();

        if (ptuGridMonitorList.isEmpty()) {
            ptuGridMonitor = new PtuGridMonitor();
            ptuGridMonitor.setPtuContainer(ptuContainer);
            ptuGridMonitor.setSequence(sequenceGeneratorService.next());
            ptuGridMonitor.setConnectionGroup(connectionGroup);
            ptuGridMonitor.setActualPower(actualPower);
        } else {
            ptuGridMonitor = ptuGridMonitorList.get(0);
            ptuGridMonitor.setActualPower((actualPower + ptuGridMonitor.getActualPower()) / 2);
        }
        persist(ptuGridMonitor);
    }

    /**
     * Retreive the actual power at this PTU.
     *
     * @param ptuContainer
     * @param connectionGroup
     * @return long
     */
    @SuppressWarnings("unchecked")
    public long getActualPower(PtuContainer ptuContainer, ConnectionGroup connectionGroup) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ptugm ");
        sql.append("FROM PtuGridMonitor ptugm ");
        sql.append("WHERE ptugm.ptuContainer.ptuDate = :ptuDate ");
        sql.append("AND ptugm.ptuContainer.ptuIndex = :ptuIndex ");
        sql.append("AND ptugm.connectionGroup.usefIdentifier = :congestionPoint ");

        List<PtuGridMonitor> ptuGridMonitorList = (List<PtuGridMonitor>) getEntityManager().createQuery(sql.toString())
                .setParameter("ptuDate", ptuContainer.getPtuDate().toDateMidnight().toDate())
                .setParameter("ptuIndex", ptuContainer.getPtuIndex())
                .setParameter("congestionPoint", connectionGroup.getUsefIdentifier())
                .getResultList();

        if (ptuGridMonitorList == null || ptuGridMonitorList.isEmpty()) {
            return 0;
        }
        return ptuGridMonitorList.get(0).getActualPower();
    }

    /**
     * Find Ptu Grid Monitors.
     *
     * @param startDate PTU start day
     * @param endDate PTU end day
     * @return Ptu Grid Monitors
     */
    public List<PtuGridMonitor> findPtuGridMonitorsByDates(LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT gm ");
        sql.append("FROM PtuGridMonitor gm ");
        sql.append("WHERE gm.ptuContainer.ptuDate >= :startDate ");
        sql.append(" AND gm.ptuContainer.ptuDate <= :endDate ");
        Query query = entityManager.createQuery(sql.toString())
                .setParameter("startDate", startDate.toDateMidnight().toDate())
                .setParameter("endDate", endDate.toDateMidnight().toDate());

        @SuppressWarnings("unchecked")
        List<PtuGridMonitor> results = query.getResultList();
        if (results == null) {
            results = new ArrayList<>();
        }
        return results;
    }

    /**
     * Retreive the limited power at this PTU.
     *
     * @param ptuContainer {@link PtuContainer} the specified PTU (not nullable).
     * @param connectionGroup {@link ConnectionGroup} a congestion point (not nullable).
     * @return an {@link Optional} of {@link Long} limited power for the given PTU and the given congestion point.
     */
    public Optional<Long> findLimitedPower(PtuContainer ptuContainer, ConnectionGroup connectionGroup) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ptugm.limitedPower ");
        sql.append("FROM PtuGridMonitor ptugm ");
        sql.append("WHERE ptugm.ptuContainer.ptuDate = :ptuDate ");
        sql.append("  AND ptugm.ptuContainer.ptuIndex = :ptuIndex ");
        sql.append("  AND ptugm.connectionGroup.usefIdentifier = :congestionPoint ");
        sql.append("  AND ptugm.limitedPower is not null ");

        List<Long> ptuLimitedPowers = getEntityManager().createQuery(sql.toString(), Long.class)
                .setParameter("ptuDate", ptuContainer.getPtuDate().toDateMidnight().toDate())
                .setParameter("ptuIndex", ptuContainer.getPtuIndex())
                .setParameter("congestionPoint", connectionGroup.getUsefIdentifier())
                .getResultList();
        return ptuLimitedPowers.stream().findAny();
    }

    /**
     * Store the limited power at this PTU.
     *
     * @param ptuContainer
     * @param limitedPower
     * @param connectionGroup
     */
    public void setLimitedPower(PtuContainer ptuContainer, Long limitedPower, ConnectionGroup connectionGroup) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ptugm ");
        sql.append("FROM PtuGridMonitor ptugm ");
        sql.append("WHERE ptugm.ptuContainer.ptuDate = :ptuDate ");
        sql.append("AND ptugm.ptuContainer.ptuIndex = :ptuIndex ");
        sql.append("AND ptugm.connectionGroup.usefIdentifier = :congestionPoint ");

        PtuGridMonitor ptuGridMonitor;

        @SuppressWarnings("unchecked")
        List<PtuGridMonitor> ptuGridMonitorList = (List<PtuGridMonitor>) getEntityManager().createQuery(sql.toString())
                .setParameter("ptuDate", ptuContainer.getPtuDate().toDateMidnight().toDate())
                .setParameter("ptuIndex", ptuContainer.getPtuIndex())
                .setParameter("congestionPoint", connectionGroup.getUsefIdentifier())
                .getResultList();

        if (ptuGridMonitorList.isEmpty()) {
            ptuGridMonitor = new PtuGridMonitor();
            ptuGridMonitor.setPtuContainer(ptuContainer);
            ptuGridMonitor.setSequence(sequenceGeneratorService.next());
            ptuGridMonitor.setConnectionGroup(connectionGroup);
            ptuGridMonitor.setLimitedPower(limitedPower);
        } else {
            ptuGridMonitor = ptuGridMonitorList.get(0);
            ptuGridMonitor.setLimitedPower(limitedPower);
        }
        persist(ptuGridMonitor);
    }

    /**
     * Delete all {@link PtuGridMonitor} objects for a certain date.
     *
     * @param period
     * @return the number of {@link PtuGridMonitor} objects deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PtuGridMonitor pgm ");
        sql.append("WHERE pgm.ptuContainer IN (SELECT pc FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate)");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
    }


}
