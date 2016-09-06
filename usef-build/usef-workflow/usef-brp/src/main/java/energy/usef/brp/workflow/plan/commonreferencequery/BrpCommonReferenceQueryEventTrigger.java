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

package energy.usef.brp.workflow.plan.commonreferencequery;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.brp.workflow.plan.connection.forecast.CommonReferenceQueryEvent;
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
 * Class in charge of registering a scheduled task to fire {@link CommonReferenceQueryEvent}.
 */
@Singleton
@Startup
public class BrpCommonReferenceQueryEventTrigger extends AbstractTriggerRegisterHelperService {

    @Inject
    private Event<CommonReferenceQueryEvent> eventManager;

    @Inject
    private ConfigBrp configBrp;

    /**
     * {@inheritDoc}
     */
    @Override
    @PostConstruct
    public void registerTrigger() {
        getSchedulerHelperService().registerScheduledCall("BRP_COMMON_REFERENCE_QUERY_EVENT",
                () -> eventManager.fire(new CommonReferenceQueryEvent()),
                createInitialDelay(), createIntervalPeriod());
    }

    private long createInitialDelay() {
        return DateTimeUtil.millisecondDelayUntilNextTime(new LocalTime(configBrp
                .getProperty(ConfigBrpParam.BRP_INITIALIZE_PLANBOARD_TIME)));
    }

    private long createIntervalPeriod() {
        return Days.days(configBrp.getIntegerProperty(ConfigBrpParam.BRP_INITIALIZE_PLANBOARD_DAYS_INTERVAL))
                .toStandardDuration()
                .getMillis();
    }

}
