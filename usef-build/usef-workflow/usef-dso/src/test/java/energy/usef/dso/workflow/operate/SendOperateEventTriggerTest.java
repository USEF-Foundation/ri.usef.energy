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

package energy.usef.dso.workflow.operate;

import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;

import javax.enterprise.event.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit test for the {@link SendOperateEventTrigger} class.
 */
@RunWith(PowerMockRunner.class)
public class SendOperateEventTriggerTest {

    private SendOperateEventTrigger trigger;

    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    private Event<SendOperateEvent> eventManager;
    @Mock
    private ConfigDso configDso;

    @Before
    public void init() {
        trigger = new SendOperateEventTrigger();
        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, eventManager);
        Whitebox.setInternalState(trigger, configDso);
    }

    @Test
    public void testRegisterTriggerNonUdi() {
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        ArgumentCaptor<SendOperateEvent> eventCaptor = ArgumentCaptor.forClass(SendOperateEvent.class);

        Mockito.when(configDso.getProperty(Matchers.eq(ConfigDsoParam.DSO_OPERATE_INITIAL_DELAY_IN_SECONDS))).thenReturn("100");
        Mockito.when(configDso.getProperty(Matchers.eq(ConfigDsoParam.DSO_OPERATE_INTERVAL_IN_SECONDS))).thenReturn("100");

        trigger.registerTrigger();

        Mockito.verify(schedulerHelperService, Mockito.times(1))
                .registerScheduledCall(Matchers.anyString(), runnableCaptor.capture(), Matchers.any(Long.class),
                        Matchers.any(Long.class));
        WorkItemExecution workItemExecution = runnableCaptor.getValue();

        workItemExecution.execute();

        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());
        Mockito.verify(configDso, Mockito.times(1)).getProperty(ConfigDsoParam.DSO_OPERATE_INITIAL_DELAY_IN_SECONDS);
        Mockito.verify(configDso, Mockito.times(1)).getProperty(ConfigDsoParam.DSO_OPERATE_INTERVAL_IN_SECONDS);
    }
}
