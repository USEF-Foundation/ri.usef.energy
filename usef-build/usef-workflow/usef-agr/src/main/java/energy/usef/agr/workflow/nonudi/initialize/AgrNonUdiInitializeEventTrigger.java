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

package energy.usef.agr.workflow.nonudi.initialize;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.service.helper.AbstractTriggerRegisterHelperService;
import energy.usef.core.util.DateTimeUtil;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.Days;
import org.joda.time.LocalTime;

/**
 * Timed trigger to fire AgrNonUdiInitializeEvent.
 */
@Singleton
@Startup
public class AgrNonUdiInitializeEventTrigger extends AbstractTriggerRegisterHelperService {

    @Inject
    private Event<AgrNonUdiInitializeEvent> eventManager;

    @Inject
    private ConfigAgr configAgr;

    /**
     * {@inheritDoc}
     */
    @Override
    @PostConstruct
    public void registerTrigger() {

        // Only register trigger for non-udi aggregators
        if (!configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            return;
        }

        getSchedulerHelperService().registerScheduledCall(AgrWorkflowStep.AGR_INITIALIZE_NON_UDI_CLUSTERS.name(),
                () -> eventManager.fire(new AgrNonUdiInitializeEvent(DateTimeUtil.getCurrentDate())), createInitialDelay(),
                createIntervalPeriod());
    }

    private long createInitialDelay() {
        return DateTimeUtil.millisecondDelayUntilNextTime(
                new LocalTime(configAgr.getProperty(ConfigAgrParam.AGR_INITIALIZE_NON_UDI_TIME_OF_DAY)));
    }

    private long createIntervalPeriod() {
        return Days.days(1).toStandardDuration().getMillis();
    }

}
