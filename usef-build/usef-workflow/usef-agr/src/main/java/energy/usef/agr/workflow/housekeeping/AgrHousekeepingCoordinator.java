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

package energy.usef.agr.workflow.housekeeping;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.agr.service.business.AgrHousekeepingBusinessService;
import energy.usef.core.event.HousekeepingEvent;
import energy.usef.core.service.business.PlanboardHousekeepingBusinessService;
import energy.usef.core.service.business.TransportHousekeepingBusinessService;

/**
 * Coordinator for housekeeping in an aggregator implementation.
 */
public class AgrHousekeepingCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrHousekeepingCoordinator.class);

    @Inject
    private AgrHousekeepingBusinessService agrHousekeepingService;
    @Inject
    private PlanboardHousekeepingBusinessService planboardHousekeepingService;
    @Inject
    private TransportHousekeepingBusinessService transportHousekeepingService;
    @Inject
    private EventValidationService eventValidationService;

    /**
     * Handle a {@Link HousekeepingEvent}.
     *
     * @param {@Link HousekeepingEvent}
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void cleanDatabase(@Observes HousekeepingEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriod(event);

        agrHousekeepingService.cleanup(event.getPeriod());
        planboardHousekeepingService.cleanup(event.getPeriod());
        transportHousekeepingService.cleanup(event.getPeriod());
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }
}
