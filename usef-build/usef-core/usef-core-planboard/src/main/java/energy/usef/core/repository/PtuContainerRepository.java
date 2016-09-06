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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.transaction.Transactional;

import org.joda.time.LocalDate;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.Document;
import energy.usef.core.model.PhaseType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.model.PtuState;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;

/**
 * PTUContainer Repository.
 */
@Stateless
public class PtuContainerRepository extends BaseRepository<PtuContainer> {

    public static final int PTU_OFFSET_IN_SECONDS = 30;

    @Inject
    private Config config;

    /**
     * Finds {@link PtuContainer} entity for given period and a given index. This method should only be called in a OPERATE
     * context and not in a loop. If in a loop, use {@link PtuContainerRepository#findPtuContainersMap(LocalDate)};
     *
     * @param period {@link LocalDate} period.
     * @param ptuIndex {@link Integer} The Ptu Index.
     * @return a {@link PtuContainer}.
     */
    public PtuContainer findPtuContainer(LocalDate period, Integer ptuIndex) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ptu ");
        sql.append("FROM PtuContainer ptu ");
        sql.append("WHERE ptu.ptuDate = :ptuDate AND ptu.ptuIndex = :ptuIndex ");
        List<PtuContainer> result = entityManager.createQuery(sql.toString(), PtuContainer.class)
                .setParameter("ptuDate", period.toDateMidnight().toDate(), TemporalType.DATE)
                .setParameter("ptuIndex", ptuIndex)
                .getResultList();
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Finds {@link PtuContainer} entities for given period.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link List} of {@link PtuContainer}s.
     */
    public List<PtuContainer> findPtuContainers(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ptu ");
        sql.append("FROM PtuContainer ptu ");
        sql.append("WHERE ptu.ptuDate = :ptuDate ");
        sql.append("ORDER BY ptu.ptuIndex ");
        return entityManager.createQuery(sql.toString(), PtuContainer.class)
                .setParameter("ptuDate", period.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }

    /**
     * Finds {@link PtuContainer} entities for given period.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link Map} linking the PTU Index to its {@link PtuContainer}.
     */
    @SuppressWarnings("unchecked")
    public Map<Integer, PtuContainer> findPtuContainersMap(LocalDate period) {
        List<PtuContainer> result = findPtuContainers(period);
        if (result.isEmpty()) {
            return new HashMap<>();
        }
        return result.stream().collect(Collectors.toMap(PtuContainer::getPtuIndex, Function.identity()));
    }
    /**
     * Find the PTU Containers for a given sequence number, for a given type of document.
     *
     * @param sequenceNumber {@link Long} sequence number
     * @param documentType {@link Class} entity class giving the type of document.
     * @return a {@link List} of {@link PtuContainer}s.
     */
    @SuppressWarnings("unchecked")
    public List<PtuContainer> findPtuContainersForDocumentSequence(Long sequenceNumber, Class<? extends Document> documentType) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT doc.ptuContainer ");
        sql.append("FROM Document doc ");
        sql.append("WHERE doc.sequence = :sequenceNumber AND TYPE(doc) = :documentType ");
        sql.append("ORDER BY doc.ptuContainer.ptuIndex");
        return getEntityManager().createQuery(sql.toString())
                .setParameter("sequenceNumber", sequenceNumber)
                .setParameter("documentType", documentType)
                .getResultList();
    }

    /**
     * Apply a bulk update to the PtuContainer entities.
     *
     * @param state {@link PtuContainerState} new state of the {@link PtuContainer}.
     * @param period {@link LocalDate} date of the {@link PtuContainer} that will be changed.
     * @param ptuIndex {@link Integer}. Optional filter to limit the update to a specific PTU index.
     * @return the number of records updated.
     */
    @SuppressWarnings("unchecked")
    public int updatePtuContainersState(PtuContainerState state, LocalDate period, Integer ptuIndex) {
        if (state == null) {
            throw new IllegalArgumentException("Cannot have a null PtuContainerState.");
        }
        if (period == null) {
            throw new IllegalArgumentException("Cannot have a null ptu period.");
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p ");
        sql.append("FROM PtuState p ");
        sql.append("WHERE p.ptuContainer.ptuDate = :period ");
        if (ptuIndex != null) {
            sql.append(" AND p.ptuContainer.ptuIndex = :ptuIndex ");
        }

        Query query = getEntityManager().createQuery(sql.toString()).setParameter("period", period.toDateMidnight().toDate());
        if (ptuIndex != null) {
            query = query.setParameter("ptuIndex", ptuIndex);
        }

        int i = 0;
        List<PtuState> resultList = query.getResultList();
        for (PtuState ptuState : resultList) {
            ptuState.setState(state);
            i++;
        }

        return i;
    }

    /**
     * Apply a bulk update to the PtuContainer entities.
     *
     * @param phase {@link PhaseType} new PhaseType of the {@link PtuContainer}.
     * @param period {@link LocalDate} date of the {@link PtuContainer} that will be changed.
     * @param ptuIndex {@link Integer}. Optional filter to limit the update to a specific PTU index.
     * @return the number of records updated.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public int updatePtuContainersPhase(PhaseType phase, LocalDate period, Integer ptuIndex) {
        boolean isPlanValidate = PhaseType.Plan.equals(phase) || PhaseType.Validate.equals(phase);

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE PtuContainer ");
        sql.append("SET phase = :phase ");
        sql.append("WHERE ptuDate = :period ");
        if (isPlanValidate && period.equals(DateTimeUtil.getCurrentDate())) {
            // only future ptus may be updated.
            sql.append(" AND ptuIndex > :currentPtu ");
        }
        if (ptuIndex != null) {
            sql.append(" AND ptuIndex = :ptuIndex ");
        }

        Query query = getEntityManager().createQuery(sql.toString())
                .setParameter("phase", phase)
                .setParameter("period", period.toDateMidnight().toDate());

        if (isPlanValidate && period.equals(DateTimeUtil.getCurrentDate())) {
            // only future ptus may be updated.
            int currentPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime().plusSeconds(PTU_OFFSET_IN_SECONDS),
                    config.getIntegerProperty(ConfigParam.PTU_DURATION));
            query = query.setParameter("currentPtu", currentPtuIndex);
        }
        if (ptuIndex != null) {
            query = query.setParameter("ptuIndex", ptuIndex);
        }
        return query.executeUpdate();
    }

    /**
     * Gets the ptuContainers for a certain day and phases.
     *
     * @param ptuDate PTU date
     * @param phases PTU phases
     * @return PTUContainer list
     */
    @SuppressWarnings("unchecked")
    public List<PtuContainer> findPtuContainers(LocalDate ptuDate, PhaseType... phases) {
        if (phases == null || phases.length == 0) {
            throw new IllegalArgumentException("Cannot have a null phase.");
        }
        StringBuilder inString = new StringBuilder();
        for (PhaseType phase : phases) {
            inString.append(inString.length() == 0 ? "" : ", ").append("'").append(phase.name()).append("'");
        }
        return entityManager
                .createQuery(
                        "SELECT pc FROM PtuContainer pc  WHERE pc.ptuDate = :ptuDate AND pc.phase IN (" + inString.toString() + ")")
                .setParameter("ptuDate", ptuDate.toDateMidnight().toDate())
                .getResultList();
    }

    /**
     * Initialises the existing ptuContainers at startup.
     *
     * @param period {@link LocalDate} date of the {@link PtuContainer} that will be changed.
     * @param ptuIndex {@link Integer}. Optional filter to limit the update to a specific PTU index.
     * @return the number of records updated.
     */
    public int initialisePtuContainers(LocalDate period, Integer ptuIndex) {
        StringBuilder settlementSql = new StringBuilder();
        settlementSql.append("UPDATE PtuContainer ");
        settlementSql.append("SET phase = :phase ");
        settlementSql.append("WHERE phase != :phase AND ((ptuDate = :period AND ptuIndex < :ptuIndex) OR ptuDate < :period)");

        int updateCount = setPhase(settlementSql.toString(), PhaseType.Settlement, period, ptuIndex);

        StringBuilder operateSql = new StringBuilder();
        operateSql.append("UPDATE PtuContainer ");
        operateSql.append("SET phase = :phase ");
        operateSql.append("WHERE phase != :phase AND ptuDate = :period AND ptuIndex = :ptuIndex");

        updateCount += setPhase(operateSql.toString(), PhaseType.Operate, period, ptuIndex);
        return updateCount;
    }

    private int setPhase(String sql, PhaseType phase, LocalDate period, Integer ptuIndex) {
        return getEntityManager().createQuery(sql)
                .setParameter("phase", phase)
                .setParameter("period", period.toDateMidnight().toDate())
                .setParameter("ptuIndex", ptuIndex).executeUpdate();
    }

    /**
     * Returns the list of dates (present and future, not past) for which the planboard has been initialized (i.e. for which ptu
     * containers exist).
     *
     * @return {@link List} of {@link LocalDate}.
     */
    public List<LocalDate> findInitializedDaysOfPlanboard() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ptu.ptuDate FROM PtuContainer ptu ");
        sql.append("WHERE ptu.ptuDate >= :today ");
        sql.append("ORDER BY ptu.ptuDate ");
        List<Date> dates = entityManager.createQuery(sql.toString(), Date.class)
                .setParameter("today", DateTimeUtil.getCurrentDate().toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
        return dates.stream().map(LocalDate::new).collect(Collectors.toList());
    }

    /**
     * Delete all {@link PtuContainer}s for a certain date.
     *
     * @param period
     * @return the number of {@link PtuContainer}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
   }
}
