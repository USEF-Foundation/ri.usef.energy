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

package energy.usef.dso.workflow.plan.connection.forecast;

import java.util.List;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.validate.gridsafetyanalysis.GridSafetyAnalysisEvent;

/**
 * DSO Non Aggreagator Connection Forecast workflow, Plan board sub-flow workflow coordinator.
 */
@Singleton
public class DsoDPrognosisCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoDPrognosisCoordinator.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private Event<GridSafetyAnalysisEvent> eventManager;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    /**
     * {@inheritDoc}
     */
    @Lock(LockType.WRITE)
    public void invokeWorkflow(Prognosis prognosis, Message savedMessage) {
        List<PtuPrognosis> existingPtuPrognoses = corePlanboardBusinessService
                .findLastPrognoses(prognosis.getPeriod(), PrognosisType.D_PROGNOSIS, prognosis.getCongestionPoint());

        if (isPrognosisNewPrognosis(prognosis, existingPtuPrognoses)) {
            LOGGER.info("New prognosis received.");
        } else {
            LOGGER.info("Updated prognosis received.");
            dsoPlanboardBusinessService.handleUpdatedPrognosis(prognosis, existingPtuPrognoses);
        }
        corePlanboardBusinessService.storePrognosis(prognosis.getCongestionPoint(), prognosis, DocumentType.D_PROGNOSIS,
                DocumentStatus.ACCEPTED, prognosis.getMessageMetadata().getSenderDomain(), savedMessage, false);

        triggerGridSafetyAnalysisWorkflow(prognosis.getPeriod(), prognosis.getCongestionPoint());
    }

    private boolean isPrognosisNewPrognosis(Prognosis dprognosis, List<PtuPrognosis> prognoses) {
        if (prognoses == null || prognoses.isEmpty()) {
            return true;
        }
        for (PtuPrognosis prognosis : prognoses) {
            if (dprognosis.getMessageMetadata().getSenderDomain().equals(prognosis.getParticipantDomain())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fires an event to trigger the Grid Safety Analysis workflow.
     *
     * @param analysisDate {@link LocalDate} period of the grid safety analysis to run.
     */
    private void triggerGridSafetyAnalysisWorkflow(LocalDate analysisDate, String congestionPointEntityAddress) {
        // count distinct aggregators
        List<Aggregator> activeAggregators = dsoPlanboardBusinessService.getAggregatorsByEntityAddress(
                congestionPointEntityAddress, analysisDate);

        List<PlanboardMessage> acceptedPrognosisMessages = corePlanboardBusinessService.findAcceptedPrognosisMessages(
                DocumentType.D_PROGNOSIS, analysisDate, congestionPointEntityAddress);

        if (acceptedPrognosisMessages.size() < activeAggregators.size()) {
            LOGGER.debug("Accepted prognoses amount=[{}], aggregators count=[{}].", acceptedPrognosisMessages.size(),
                    activeAggregators.size());
            LOGGER.info("Did not receive all the d-prognoses yet. Grid Safety Analysis will not be triggered.");
            return;
        }
        eventManager.fire(new GridSafetyAnalysisEvent(congestionPointEntityAddress, analysisDate));
    }
}
