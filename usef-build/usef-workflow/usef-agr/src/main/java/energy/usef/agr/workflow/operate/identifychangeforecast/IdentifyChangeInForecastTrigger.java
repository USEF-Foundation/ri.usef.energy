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

package energy.usef.agr.workflow.operate.identifychangeforecast;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.service.helper.AbstractTriggerRegisterHelperService;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * Timed trigger to fire IdentifyChangeInForecastEvent.
 */
@Singleton
@Startup
public class IdentifyChangeInForecastTrigger extends AbstractTriggerRegisterHelperService {

    @Inject
    private Event<IdentifyChangeInForecastEvent> identifyChangeInForecastEventManager;

    @Inject
    private ConfigAgr configAgr;

    /**
     * {@inheritDoc}
     */
    @Override
    @PostConstruct
    public void registerTrigger() {
        getSchedulerHelperService().registerScheduledCall("AGRIdentifyChangeInForecast",
                () -> identifyChangeInForecastEventManager.fire(new IdentifyChangeInForecastEvent()),
                createInitialDelay(), createIntervalPeriod());
    }

    private Long createInitialDelay() {
        return TimeUnit.SECONDS
                .toMillis(Long.valueOf(configAgr
                        .getProperty(ConfigAgrParam.AGR_IDENTIFY_CHANGE_IN_FORECAST_INITIAL_DELAY_IN_SECONDS)));
    }

    private Long createIntervalPeriod() {
        return TimeUnit.SECONDS.toMillis(Long.valueOf(configAgr
                .getProperty(ConfigAgrParam.AGR_IDENTIFY_CHANGE_IN_FORECAST_INTERVAL_IN_SECONDS)));
    }

}
