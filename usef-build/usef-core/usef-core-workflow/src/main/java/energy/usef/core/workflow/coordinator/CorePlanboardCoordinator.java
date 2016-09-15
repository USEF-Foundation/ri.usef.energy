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

package energy.usef.core.workflow.coordinator;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.core.event.BackToPlanEvent;
import energy.usef.core.event.DayAheadClosureEvent;
import energy.usef.core.event.IntraDayClosureEvent;
import energy.usef.core.event.MoveToOperateEvent;
import energy.usef.core.event.RequestMoveToValidateEvent;
import energy.usef.core.event.StartValidateEvent;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.service.business.CorePlanboardBusinessService;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core coordinator class handling events and managing workflows related to the planboard.
 */
@Stateless
public class CorePlanboardCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorePlanboardCoordinator.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardService;

    @Inject
    private Event<StartValidateEvent> startValidateEventManager;

    /**
     * Process a {@link BackToPlanEvent}. This will update the phase of all the {@link PtuContainer} entities with the given period.
     * The new {@link PtuContainer#phase} will be 'Plan' .
     *
     * @param event {@link BackToPlanEvent}.
     */
    public void processBackToPlanEvent(@Observes BackToPlanEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        corePlanboardService.processBackToPlanEvent(event.getPeriod());
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Process a {@link RequestMoveToValidateEvent}. This will update the phase of all the {@link PtuContainer}
     * entities with the given period. The new {@link PtuContainer#phase} will be 'Validate' .
     *
     * @param event {@link RequestMoveToValidateEvent}.
     */
    @Asynchronous
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void processMoveToValidateEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) RequestMoveToValidateEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        boolean success = corePlanboardService.processMoveToValidateEvent(event.getPeriod());
        if (success) {
            startValidateEventManager.fire(new StartValidateEvent(event.getPeriod()));
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Process a {@link DayAheadClosureEvent}. This will update the phase of all the {@link PtuContainer} entities with the given
     * period. The new {@link PtuContainer#phase} will be 'DayAheadClosedValidate'.
     *
     * @param event {@link DayAheadClosureEvent}.
     */
    public void processDayAheadClosureEvent(@Observes DayAheadClosureEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        corePlanboardService.processDayAheadClosureEvent(event.getPeriod());
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Process a {@link IntraDayClosureEvent}. This will update the phase of all the {@link PtuContainer} entities with the given
     * period and ptu index. The new phase will be {@link PtuContainerState#IntraDayClosedValidate}.
     *
     * @param event {@link IntraDayClosureEvent}.
     */
    public void processIntraDayClosureEvent(@Observes IntraDayClosureEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        corePlanboardService.processIntraDayClosureEvent(event.getPeriod(), event.getPtuIndex());
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Process a {@link MoveToOperateEvent}. This will update the phase of all the {@link PtuContainer} entities with the given
     * period and given ptu index. The new phase will be {@link PtuContainerState#Operate} for the specified {@link PtuContainer}.
     * The previous {@link PtuContainer} will be set to {@link PtuContainerState#PendingSettlement}.
     *
     * @param event {@link MoveToOperateEvent}.
     */
    public void processMoveToOperateEvent(@Observes MoveToOperateEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        corePlanboardService.processMoveToOperateEvent(event.getPeriod(), event.getPtuIndex());
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

}
