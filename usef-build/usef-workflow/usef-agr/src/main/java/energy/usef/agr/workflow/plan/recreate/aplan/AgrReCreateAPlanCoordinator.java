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

package energy.usef.agr.workflow.plan.recreate.aplan;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.agr.workflow.plan.create.aplan.CreateAPlanEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;

import java.util.List;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class in charge of the workflow responsible of the re-creation of A-Plans.
 */
@Singleton
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class AgrReCreateAPlanCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrReCreateAPlanCoordinator.class);

    @Inject
    private Event<CreateAPlanEvent> createAPlanEventManager;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * Handles a {@link ReCreateAPlanEvent}.
     * <p>
     * For all A-Plans flagged as 'TO_BE_RECREATED' in the database, the method will select the distinct associated BRPs and fire
     * one {@link CreateAPlanEvent} per for each.
     *
     * @param event {@link ReCreateAPlanEvent}.
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void handleEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) ReCreateAPlanEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);

        invokeCreateAPlanEvent(event.getPeriod());

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void invokeCreateAPlanEvent(LocalDate period) {
        // fetch the a-plans for the period specified in the event with status 'TO_BE_RECREATED'.
        LOGGER.debug("Fetching A-Plans for period [{}] with status [{}].", period, DocumentStatus.TO_BE_RECREATED);

        List<PlanboardMessage> aPlans = corePlanboardBusinessService
                .findPlanboardMessages(DocumentType.A_PLAN, period, DocumentStatus.TO_BE_RECREATED);
        if (aPlans == null || aPlans.isEmpty()) {
            LOGGER.debug("No A-Plan needs to be re-created.");
            return;
        }

        // for each distinct connection group of all the A-Plans to be re-created.
        aPlans.stream().map(aPlan -> aPlan.getConnectionGroup().getUsefIdentifier()).distinct().forEach(connectionGroup -> {
            LOGGER.debug("Firing a CreateAPlanEvent for period [{}] and BRP [{}]", period, connectionGroup);
            createAPlanEventManager.fire(new CreateAPlanEvent(period, connectionGroup));
        });

        // change the status of each A-Plan
        aPlans.forEach(aPlan -> aPlan.setDocumentStatus(DocumentStatus.ARCHIVED));
    }
}
