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

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
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
 * Class in charge of the triggering of the UpdateAPlanEvent.
 */
@Singleton
@Startup
public class FinalizeAPlansEventTrigger {

    public static final int ONE_DAY = 1;

    @Inject
    private SchedulerHelperService schedulerHelperService;

    @Inject
    private Event<FinalizeAPlansEvent> eventManager;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private Config config;

    /**
     * Register Finalize A-Plan trigger in the {@link SchedulerHelperService} to invoke the workflow every day at a certain time.
     */
    @PostConstruct
    public void scheduleTrigger() {
        schedulerHelperService.registerScheduledCall("FinalizeAPlansEvent",
                () -> eventManager.fire(new FinalizeAPlansEvent(DateTimeUtil.getCurrentDate().plusDays(1))),
                createInitialDelay(), createIntervalPeriod());
    }

    private long createInitialDelay() {
        LocalTime localTime = new LocalTime(config.getProperty(ConfigParam.DAY_AHEAD_GATE_CLOSURE_TIME));
        localTime = localTime.minusMinutes(Integer.parseInt(configAgr.getProperty(ConfigAgrParam.AGR_FINALIZE_A_PLAN_PTUS))
                * Integer.parseInt(config.getProperty(ConfigParam.PTU_DURATION)));
        return DateTimeUtil.millisecondDelayUntilNextTime(localTime);
    }

    private long createIntervalPeriod() {
        return Days.days(ONE_DAY).toStandardDuration().getMillis();
    }

}
