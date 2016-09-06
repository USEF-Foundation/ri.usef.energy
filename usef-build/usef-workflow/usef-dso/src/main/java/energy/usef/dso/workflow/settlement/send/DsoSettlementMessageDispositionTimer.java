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

package energy.usef.dso.workflow.settlement.send;

import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.service.helper.AbstractTriggerRegisterHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.joda.time.LocalTime;

/**
 * Class which will register a scheduled task whose purpose is to check whether some settlement messages have not received a
 * response in time and hence should be marked as DISPUTED.
 */
@Singleton
@Startup
public class DsoSettlementMessageDispositionTimer extends AbstractTriggerRegisterHelperService {

    private static final String TASK_NAME = "SETTLEMENT_MESSAGE_DISPOSITION_TIMER";

    @Inject
    private PlanboardMessageRepository planboardMessageRepository;

    @Inject
    private ConfigDso configDso;

    /**
     * {@inheritDoc}
     */
    @Override
    @PostConstruct
    public void registerTrigger() {
        getSchedulerHelperService()
                .registerScheduledCall(TASK_NAME, planboardMessageRepository::updateOldSettlementMessageDisposition,
                        DateTimeUtil.millisecondDelayUntilNextTime(
                                new LocalTime(configDso.getProperty(ConfigDsoParam.DSO_SETTLEMENT_MESSAGE_DISPOSAL_TIME))),
                        TimeUnit.DAYS.toMillis(1));
    }
}
