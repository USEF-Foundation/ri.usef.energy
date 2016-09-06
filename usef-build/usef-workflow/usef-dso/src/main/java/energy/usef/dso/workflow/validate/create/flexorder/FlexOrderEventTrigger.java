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

package energy.usef.dso.workflow.validate.create.flexorder;

import energy.usef.core.service.helper.AbstractTriggerRegisterHelperService;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * Timed trigger to fire FlexOfferEvent.
 */
@Singleton
@Startup
public class FlexOrderEventTrigger extends AbstractTriggerRegisterHelperService {

    @Inject
    private Event<FlexOrderEvent> flexOrderEvents;

    @Inject
    private ConfigDso configDso;

    /**
     * {@inheritDoc}
     */
    @Override
    @PostConstruct
    public void registerTrigger() {
        getSchedulerHelperService().registerScheduledCall("DSOFlexOrder",
                () -> flexOrderEvents.fire(new FlexOrderEvent()),
                createInitialDelay(), createIntervalPeriod());
    }

    private Long createInitialDelay() {
        return TimeUnit.SECONDS
                .toMillis(Long.valueOf(configDso.getProperty(ConfigDsoParam.DSO_FLEXORDER_INITIAL_DELAY_IN_SECONDS)));
    }

    private Long createIntervalPeriod() {
        return TimeUnit.SECONDS.toMillis(Long.valueOf(configDso.getProperty(ConfigDsoParam.DSO_FLEXORDER_INTERVAL_IN_SECONDS)));
    }

}
