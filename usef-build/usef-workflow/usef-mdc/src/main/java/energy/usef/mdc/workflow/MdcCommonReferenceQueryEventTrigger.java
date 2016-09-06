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

package energy.usef.mdc.workflow;

import energy.usef.core.service.helper.AbstractTriggerRegisterHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.mdc.config.ConfigMdc;
import energy.usef.mdc.config.ConfigMdcParam;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.LocalTime;

/**
 * This class will register a scheduled task which will fire a new {@link CommonReferenceQueryEvent} and which will run every day at
 * a given time.
 */
@Singleton
@Startup
public class MdcCommonReferenceQueryEventTrigger extends AbstractTriggerRegisterHelperService {

    @Inject
    private ConfigMdc configMdc;

    @Inject
    private Event<CommonReferenceQueryEvent> commonReferenceQueryEventManager;

    /**
     * Registers a new trigger in charge of firing a new {@link CommonReferenceQueryEvent} for the MDC role every day at the time
     * specified by the MDC_COMMON_REFERENCE_QUERY_TIME property.
     */
    @Override
    @PostConstruct
    public void registerTrigger() {
        getSchedulerHelperService().registerScheduledCall("MDC_COMMON_REFERENCE_QUERY_EVENT",
                () -> commonReferenceQueryEventManager.fire(new CommonReferenceQueryEvent()),
                DateTimeUtil.millisecondDelayUntilNextTime(
                        new LocalTime(configMdc.getProperty(ConfigMdcParam.MDC_COMMON_REFERENCE_QUERY_TIME))),
                TimeUnit.DAYS.toMillis(1));
    }
}
