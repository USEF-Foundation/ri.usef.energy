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

package energy.usef.agr.workflow.plan.connection.forecast;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.util.DateTimeUtil;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.Days;
import org.joda.time.LocalTime;

/**
 * Class in charge of the triggering of the AGR Create N-Day-Ahead Forecasts workflow.
 */
@Singleton
@Startup
public class AgrCommonReferenceQueryEventTrigger {

    @Inject
    private Event<CommonReferenceQueryEvent> eventManager;

    @Inject
    private SchedulerHelperService schedulerHelperService;

    @Inject
    private ConfigAgr configAgr;

    /**
     * Registers a trigger in the {@link SchedulerHelperService} that will repeatedly call the workflow on a certain time.
     */
    @PostConstruct
    public void scheduleTrigger() {
        schedulerHelperService.registerScheduledCall("AGR_COMMON_REFERENCE_QUERY_EVENT",
                () -> eventManager.fire(new CommonReferenceQueryEvent()),
                createInitialDelay(), createIntervalPeriod());
    }

    private long createInitialDelay() {
        return DateTimeUtil.millisecondDelayUntilNextTime(new LocalTime(configAgr
                .getProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_TIME)));
    }

    private long createIntervalPeriod() {
        return Days.days(Integer.parseInt(configAgr.getProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL)))
                .toStandardDuration()
                .getMillis();
    }

}
