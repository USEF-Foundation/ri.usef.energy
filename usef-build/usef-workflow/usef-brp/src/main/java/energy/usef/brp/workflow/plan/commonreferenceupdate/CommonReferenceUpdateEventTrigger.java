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

package energy.usef.brp.workflow.plan.commonreferenceupdate;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.util.DateTimeUtil;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.LocalTime;

/**
 * Registers the trigger with our schedulerHeperService.
 */
@Startup
@Singleton
public class CommonReferenceUpdateEventTrigger {

    @Inject
    private SchedulerHelperService schedulerHelperService;

    @Inject
    private Event<CommonReferenceUpdateEvent> eventManager;

    @Inject
    protected ConfigBrp configBrp;

    /**
     * Register Trigger.
     */
    @PostConstruct
    public void registerTrigger() {
        schedulerHelperService.registerScheduledCall("CommonReferenceUpdateEvent",
                () -> eventManager.fire(new CommonReferenceUpdateEvent()),
                DateTimeUtil.millisecondDelayUntilNextTime(new LocalTime(configBrp
                        .getProperty(ConfigBrpParam.BRP_COMMON_REFERENCE_UPDATE_TIME_OF_DAY))), TimeUnit.DAYS.toMillis(1));
    }

}
