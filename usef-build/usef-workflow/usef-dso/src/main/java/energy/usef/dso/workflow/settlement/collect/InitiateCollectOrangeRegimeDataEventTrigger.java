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

package energy.usef.dso.workflow.settlement.collect;

import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;

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
 * Class in charge of the triggering of the DSO InitiateCollectOrangeRegimeDataEvent.
 */
@Singleton
@Startup
public class InitiateCollectOrangeRegimeDataEventTrigger {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateCollectOrangeRegimeDataEventTrigger.class);

    @Inject
    private Event<InitiateCollectOrangeRegimeDataEvent> eventManager;

    @Inject
    private SchedulerHelperService schedulerHelperService;

    @Inject
    private ConfigDso configDso;

    /**
     * Registers a trigger in the {@link SchedulerHelperService} that will repeatedly call the event on a certain time.
     */
    @PostConstruct
    public void scheduleTrigger() {
        schedulerHelperService.registerScheduledCall("InitiateCollectOrangeRegimeDataEvent", () -> {
            int dayOfMonth = DateTimeUtil.getCurrentDate().getDayOfMonth();
            int initiateCollectOrangeRegimeDataDay = Integer.parseInt(configDso
                    .getProperty(ConfigDsoParam.DSO_INITIATE_COLLECT_ORANGE_REGIME_DATA_DAY_OF_MONTH));
            LOGGER.debug("Checking day of month: {} with {} ", initiateCollectOrangeRegimeDataDay, dayOfMonth);

            if (dayOfMonth == initiateCollectOrangeRegimeDataDay) {
                eventManager.fire(new InitiateCollectOrangeRegimeDataEvent());
            }
        }, createInitialDelay(), createIntervalPeriod());
    }

    private long createInitialDelay() {
        return DateTimeUtil.millisecondDelayUntilNextTime(new LocalTime(configDso
                .getProperty(ConfigDsoParam.DSO_INITIATE_COLLECT_ORANGE_REGIME_DATA_TIME)));
    }

    private long createIntervalPeriod() {
        return Days.days(1).toStandardDuration().getMillis();
    }

}
