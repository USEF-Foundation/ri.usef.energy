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

package energy.usef.agr.workflow.plan.create.aplan;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.core.event.RequestMoveToValidateEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.service.business.CorePlanboardBusinessService;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregator coordinator class updating A-Plan plan board messages.
 */
@Singleton
public class AgrUpdateAPlanPlanboardCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrCreateAPlanPlanboardCoordinator.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private Event<RequestMoveToValidateEvent> moveToValidateEventManager;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * Handles the event {@link FinalizeAPlansEvent}.
     * 
     * @param event {@link FinalizeAPlansEvent}
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void handleEvent(@Observes FinalizeAPlansEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);

        corePlanboardBusinessService.finalizeAPlans(event.getPeriod());

        // now trigger the moveToValidateEvent to create D-prognoses and start trading with DSOs
        moveToValidateEventManager.fire(new RequestMoveToValidateEvent(event.getPeriod()));

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }
}
