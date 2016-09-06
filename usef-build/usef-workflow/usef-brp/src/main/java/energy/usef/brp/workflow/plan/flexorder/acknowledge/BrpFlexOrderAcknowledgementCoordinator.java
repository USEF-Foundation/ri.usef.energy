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

package energy.usef.brp.workflow.plan.flexorder.acknowledge;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.brp.service.business.BrpPlanboardBusinessService;
import energy.usef.brp.workflow.plan.connection.forecast.PrepareFlexRequestsEvent;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.PlanboardMessage;

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
 * Coordinator class in charge of the workflow related to the acknowledgement of flex orders (BRP side).
 */
@Singleton
public class BrpFlexOrderAcknowledgementCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpFlexOrderAcknowledgementCoordinator.class);

    @Inject
    private BrpPlanboardBusinessService brpPlanboardBusinessService;

    @Inject
    private Event<PrepareFlexRequestsEvent> eventManager;

    /**
     * Handle Event.
     *
     * @param event
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void handleEvent(@Observes FlexOrderAcknowledgementEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        // Updating plan-board
        LOGGER.debug("Updating planboard.");
        PlanboardMessage flexOrder = brpPlanboardBusinessService.updateFlexOrdersWithAcknowledgementStatus(
                event.getFlexOrderSequence(), event.getAcknowledgementStatus(), event.getAggregatorDomain());
        if (flexOrder == null) {
            LOGGER.warn("No Flex Order with sequence [{}] and aggregator domain [{}] has been found.",
                    event.getFlexOrderSequence(), event.getAggregatorDomain());
            return;
        }

        if (AcknowledgementStatus.ACCEPTED != event.getAcknowledgementStatus()) {
            // Flexibility order is rejected or no response received, creating a new request
            PrepareFlexRequestsEvent prepareForFlexRequestsEvent = new PrepareFlexRequestsEvent(flexOrder.getPeriod());
            LOGGER.debug("AcknowledgementStatus of the flex order was not ACCEPTED. Flex exchange will be triggered again.");
            eventManager.fire(prepareForFlexRequestsEvent);
        }

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

}
