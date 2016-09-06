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
import energy.usef.core.util.DateTimeUtil;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.Days;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class in charge of the triggering of the BRP Initiate Settlement workflow.
 */
@Singleton
@Startup
public class CollectSmartMeterDataEventTrigger {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectSmartMeterDataEventTrigger.class);

    @Inject
    private Event<CollectSmartMeterDataEvent> eventManager;

    @Inject
    private SchedulerHelperService schedulerHelperService;

    @Inject
    private ConfigBrp configBrp;

    /**
     * Registers a trigger in the {@link SchedulerHelperService} that will repeatedly call the workflow on a certain time.
     */
    @PostConstruct
    public void scheduleTrigger() {
        schedulerHelperService.registerScheduledCall("BrpInitiateSettlement", () -> {
            int dayOfMonth = DateTimeUtil.getCurrentDate().getDayOfMonth();
            int settlementDay = Integer.parseInt(configBrp
                    .getProperty(ConfigBrpParam.BRP_INITIATE_SETTLEMENT_DAY_OF_MONTH));
            LOGGER.debug("Checking day of month: {} with {} ", settlementDay, dayOfMonth);

            if (dayOfMonth == settlementDay) {
                eventManager.fire(new CollectSmartMeterDataEvent(null));
            }
        }, createInitialDelay(), createIntervalPeriod());
    }

    private long createInitialDelay() {
        return DateTimeUtil.millisecondDelayUntilNextTime(new LocalTime(configBrp
                .getProperty(ConfigBrpParam.BRP_INITIATE_SETTLEMENT_TIME)));
    }

    private long createIntervalPeriod() {
        return Days.days(1).toStandardDuration().getMillis();
    }
}
