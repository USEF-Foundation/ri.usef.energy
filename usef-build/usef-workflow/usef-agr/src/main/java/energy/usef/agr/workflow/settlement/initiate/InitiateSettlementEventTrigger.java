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

package energy.usef.agr.workflow.settlement.initiate;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.util.DateTimeUtil;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers a trigger with the schedulerHelperService.
 */
@Singleton
@Startup
public class InitiateSettlementEventTrigger {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateSettlementEventTrigger.class);

    @Inject
    private SchedulerHelperService schedulerHelperService;

    @Inject
    protected ConfigAgr configAgr;

    @Inject
    private Event<InitiateSettlementEvent> eventManager;

    /**
     * The actual trigger registration.
     */
    @PostConstruct
    public void registerTrigger() {
        schedulerHelperService.registerScheduledCall("Initiate Settlement", () -> {
            int dayOfMonth = DateTimeUtil.getCurrentDate().getDayOfMonth();
            int settlementDay = configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIATE_SETTLEMENT_DAY_OF_MONTH);
            LOGGER.debug("Checking day of month: settlement day is {} while today is {} ", settlementDay, dayOfMonth);

            if (dayOfMonth == settlementDay) {
                eventManager.fire(new InitiateSettlementEvent());
            }
        }, DateTimeUtil.millisecondDelayUntilNextTime(new LocalTime(configAgr
                .getProperty(ConfigAgrParam.AGR_INITIATE_SETTLEMENT_TIME_OF_DAY))), TimeUnit.DAYS.toMillis(1));
    }
}
