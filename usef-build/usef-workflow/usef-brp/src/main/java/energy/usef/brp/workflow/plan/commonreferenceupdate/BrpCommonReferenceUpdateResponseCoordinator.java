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

package energy.usef.brp.workflow.plan.commonreferenceupdate;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.brp.service.business.BrpBusinessService;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdate;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;

import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the base logic for handling the CommonReferenceUpdateResponse logic.
 * 
 * This coordinator is @Stateless because it should be able to handle multiple responses at the same time.
 */
@Stateless
public class BrpCommonReferenceUpdateResponseCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpCommonReferenceUpdateResponseCoordinator.class);

    @Inject
    private BrpBusinessService businessService;

    /**
     * Handle the logic, to execute a {@link CommonReferenceUpdate}s.
     * 
     * @param event
     */
    public void handleEvent(@Observes CommonReferenceUpdateResponseEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        if (DispositionAcceptedRejected.ACCEPTED.equals(event.getCommonReferenceUpdateResponse().getResult())) {
            businessService.updateConnectionStatusForCRO(event.getCommonReferenceUpdateResponse().getMessageMetadata()
                    .getSenderDomain());

            businessService.cleanSynchronization();
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }
}
