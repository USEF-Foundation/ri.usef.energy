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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;

import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.model.PtuState;
import energy.usef.core.model.RegimeType;
import energy.usef.core.service.business.SequenceGeneratorService;

/**
 * This repository is used to manage {@link PtuState}.
 */
@Stateless
public class PtuStateRepository extends BaseRepository<PtuState> {

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;
    /**
     * Finds the PTU States.
     * 
     * @param ptuDate PTU date
     * @param usefIdentifier USEF identifier
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<PtuState> findPtuStates(LocalDate ptuDate, String usefIdentifier) {
        List<PtuState> results = entityManager
                .createQuery("SELECT p FROM PtuState p WHERE p.ptuContainer.ptuDate = :ptuDate AND " +
                        "p.connectionGroup.usefIdentifier = :usefIdentifier")
                .setParameter("ptuDate", ptuDate.toDateMidnight().toDate())
                .setParameter("usefIdentifier", usefIdentifier)
                .getResultList();
        if (results == null || results.isEmpty()) {
            results = new ArrayList<>();
        }
        return results;
    }

    /**
     * Finds the PTU States.
     * 
     * @param startDate start Date
     * @param endDate end Date
     * @param regimes
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<PtuState> findPtuStates(LocalDate startDate, LocalDate endDate, RegimeType... regimes) {
        List<PtuState> results = entityManager
                .createQuery(
                        "SELECT p FROM PtuState p WHERE p.ptuContainer.ptuDate >= :startDate AND p.ptuContainer.ptuDate <= :endDate AND p.regime IN :regimes")
                .setParameter("startDate", startDate.toDateMidnight().toDate())
                .setParameter("endDate", endDate.toDateMidnight().toDate())
                .setParameter("regimes", Arrays.asList(regimes))
                .getResultList();
        if (results == null || results.isEmpty()) {
            results = new ArrayList<>();
        }
        return results;
    }

    /**
     * Finds a PTU state and create if it does not exist.
     * 
     * @param ptuContainer PTU Container
     * @param connectionGroup Connection Group
     * @return PtuState
     */
    @SuppressWarnings("unchecked")
    public PtuState findOrCreatePtuState(PtuContainer ptuContainer, ConnectionGroup connectionGroup) {
        List<PtuState> results = entityManager
                .createQuery("SELECT p FROM PtuState p WHERE p.ptuContainer.ptuDate = :ptuDate AND " +
                        "p.ptuContainer.ptuIndex = :ptuIndex AND p.connectionGroup.usefIdentifier = :usefIdentifier")
                .setParameter("ptuDate", ptuContainer.getPtuDate().toDateMidnight().toDate())
                .setParameter("ptuIndex", ptuContainer.getPtuIndex())
                .setParameter("usefIdentifier", connectionGroup.getUsefIdentifier())
                .getResultList();
        if (results == null || results.isEmpty()) {
            PtuState ptuState = new PtuState();
            ptuState.setPtuContainer(ptuContainer);
            ptuState.setConnectionGroup(connectionGroup);
            ptuState.setSequence(sequenceGeneratorService.next());
            ptuState.setState(PtuContainerState.PlanValidate);
            ptuState.setRegime(RegimeType.GREEN);
            persist(ptuState);
            return ptuState;
        }
        return results.get(0);
    }

    /**
     * Delete all {@link PtuState}s for a certain date.
     *
     * @param period
     * @return the number of {@link PtuState}s deleted.
     */
    public int cleanup(LocalDate period) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM PtuState ps ");
        sql.append("WHERE ps.ptuContainer IN (SELECT pc FROM PtuContainer pc WHERE pc.ptuDate = :ptuDate)");

        return entityManager.createQuery(sql.toString()).setParameter("ptuDate", period.toDateMidnight().toDate()).executeUpdate();
    }

}
