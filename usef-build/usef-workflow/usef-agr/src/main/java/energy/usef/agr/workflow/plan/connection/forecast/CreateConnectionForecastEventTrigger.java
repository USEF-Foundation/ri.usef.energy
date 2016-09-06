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
 * Time trigger for the event {@link CreateConnectionForecastEvent}.
 */
@Singleton
@Startup
public class CreateConnectionForecastEventTrigger {

    @Inject
    private Event<CreateConnectionForecastEvent> eventManager;

    @Inject
    private SchedulerHelperService schedulerHelperService;

    @Inject
    private ConfigAgr configAgr;

    /**
     * Registers a trigger in the {@link SchedulerHelperService} that will repeatedly call the workflow on a certain time.
     */
    @PostConstruct
    public void scheduleTrigger() {
        schedulerHelperService.registerScheduledCall("AGRCreateConnectionForecastEvent",
                () -> eventManager.fire(new CreateConnectionForecastEvent()),
                createInitialDelay(), createIntervalPeriod());
    }

    /**
     * Delay is set 10 minutes after AGR_CONNECTION_FORECAST_TIME at which the CRO query takes place.
     * 
     * @return
     */
    private long createInitialDelay() {
        return DateTimeUtil.millisecondDelayUntilNextTime(new LocalTime(configAgr
                .getProperty(ConfigAgrParam.AGR_CONNECTION_FORECAST_TIME)));
    }

    private long createIntervalPeriod() {
        return Days.days(Integer.parseInt(configAgr.getProperty(ConfigAgrParam.AGR_CONNECTION_FORECAST_DAYS_INTERVAL)))
                .toStandardDuration()
                .getMillis();
    }

}
