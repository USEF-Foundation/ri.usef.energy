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

package energy.usef.agr.workflow.nonudi.operate;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.service.helper.AbstractTriggerRegisterHelperService;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * Class in charge of triggering the 'Retrieve ADS Goal Realization' workflow for a non-UDI aggregator.
 */
@Singleton
@Startup
public class AgrNonUdiRetrieveAdsGoalRealizationEventTrigger extends AbstractTriggerRegisterHelperService {

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private Event<AgrNonUdiRetrieveAdsGoalRealizationEvent> agrNonUdiRetrieveAdsGoalRealizationEventManager;

    /**
     * {@inheritDoc}
     */
    @Override
    @PostConstruct
    public void registerTrigger() {
        if (!configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            return;
        }
        Long firstDelayAndInterval = createIntervalPeriod();
        getSchedulerHelperService().registerScheduledCall(
                AgrWorkflowStep.AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION.name(),
                () -> agrNonUdiRetrieveAdsGoalRealizationEventManager.fire(new AgrNonUdiRetrieveAdsGoalRealizationEvent()),
                firstDelayAndInterval,
                firstDelayAndInterval);
    }

    private Long createIntervalPeriod() {
        return TimeUnit.MINUTES.toMillis(Long.valueOf(configAgr.getIntegerProperty(
                ConfigAgrParam.AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION_INTERVAL_IN_MINUTES)));
    }
}
