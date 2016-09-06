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

package energy.usef.agr.workflow.operate.netdemand;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_DETERMINE_NET_DEMANDS;
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
 * Timed trigger to fire DetermineNetDemandEvent.
 */
@Singleton
@Startup
public class DetermineNetDemandEventTrigger extends AbstractTriggerRegisterHelperService {

    @Inject
    private Event<DetermineNetDemandEvent> determineNetDemandEvents;

    @Inject
    private ConfigAgr configAgr;

    /**
     * {@inheritDoc}
     */
    @Override
    @PostConstruct
    public void registerTrigger() {
        // Only register trigger for udi aggregators
        if (configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            return;
        }

        getSchedulerHelperService().registerScheduledCall(AGR_DETERMINE_NET_DEMANDS.name(),
                () -> determineNetDemandEvents.fire(new DetermineNetDemandEvent()),
                createInitialDelay(), createIntervalPeriod());
    }

    private Long createInitialDelay() {
        return TimeUnit.SECONDS
                .toMillis(Long.valueOf(configAgr.getProperty(ConfigAgrParam.AGR_DETERMINE_NETDEMANDS_INITIAL_DELAY_IN_SECONDS)));
    }

    private Long createIntervalPeriod() {
        return TimeUnit.SECONDS.toMillis(Long.valueOf(configAgr
                .getProperty(ConfigAgrParam.AGR_DETERMINE_NETDEMANDS_INTERVAL_IN_SECONDS)));
    }

}
