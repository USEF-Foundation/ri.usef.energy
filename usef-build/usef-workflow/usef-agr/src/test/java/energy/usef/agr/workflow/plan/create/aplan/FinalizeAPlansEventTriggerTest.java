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

package energy.usef.agr.workflow.plan.create.aplan;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;

import javax.enterprise.event.Event;

import org.joda.time.Days;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * JUnit class in charge of the tests related to the triggering of the UpdateAPlanEvent.
 */
@RunWith(PowerMockRunner.class)
public class FinalizeAPlansEventTriggerTest {

    @Mock
    private SchedulerHelperService schedulerHelperService;

    @Mock
    private Event<FinalizeAPlansEvent> eventManager;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private Config config;

    private FinalizeAPlansEventTrigger trigger;

    @Before
    public void init() {
        trigger = new FinalizeAPlansEventTrigger();

        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, eventManager);
        Whitebox.setInternalState(trigger, config);
        Whitebox.setInternalState(trigger, configAgr);

        PowerMockito.when(config.getProperty(ConfigParam.DAY_AHEAD_GATE_CLOSURE_TIME)).thenReturn("5:00");
        PowerMockito.when(config.getProperty(ConfigParam.PTU_DURATION)).thenReturn("15");
        PowerMockito.when(configAgr.getProperty(ConfigAgrParam.AGR_FINALIZE_A_PLAN_PTUS)).thenReturn("4");
    }

    @Test
    public void testEventTriggerIsCorrectlyRegistered() {
        trigger.scheduleTrigger();
        Long interval = Days.ONE.toStandardDuration().getMillis();
        ArgumentCaptor<WorkItemExecution> captor = ArgumentCaptor.forClass(WorkItemExecution.class);
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.anyString(), captor.capture(),
                Matchers.any(Long.class),
                Matchers.eq(interval));

        WorkItemExecution workItemExecution = captor.getValue();
        Assert.assertNotNull("Scheduled task is empty", workItemExecution);

        workItemExecution.execute();
        Mockito.verify(eventManager, Mockito.times(1)).fire(Matchers.any(FinalizeAPlansEvent.class));

    }
}
