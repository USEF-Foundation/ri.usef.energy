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

package energy.usef.brp.workflow.settlement.initiate;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.core.service.helper.SchedulerHelperService;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.Minutes;

/**
 * Event implementation for the finalization of the Initiate Settlement workflow.
 */
public class FinalizeUnfinishedInitiateSettlementEventTrigger {

    @Inject
    private Event<FinalizeUnfinishedInitiateSettlementEvent> eventManager;

    @Inject
    private SchedulerHelperService schedulerHelperService;

    @Inject
    private ConfigBrp configBrp;

    /**
     * Registers a trigger in the {@link SchedulerHelperService} that will repeatedly call the workflow on a certain time.
     */
    @PostConstruct
    public void scheduleTrigger() {
        schedulerHelperService.registerScheduledCall("BrpInitiateSettlement",
                () -> eventManager.fire(new FinalizeUnfinishedInitiateSettlementEvent()),
                0L, createIntervalPeriod());
    }

    private long createIntervalPeriod() {
        return Minutes
                .minutes(configBrp.getIntegerProperty(ConfigBrpParam.BRP_METER_DATA_QUERY_EXPIRATION_CHECK_INTERVAL_IN_MINUTES))
                .toStandardDuration().getMillis();
    }
}
