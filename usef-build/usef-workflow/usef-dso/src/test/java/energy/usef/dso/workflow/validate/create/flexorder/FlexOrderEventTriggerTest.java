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
 * Test class in charge of the unit tests related to the {@link FlexOrderEventTrigger}.
 */
@RunWith(PowerMockRunner.class)
public class FlexOrderEventTriggerTest {
    private FlexOrderEventTrigger trigger;

    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    private Event<FlexOrderEvent> eventManager;
    @Mock
    private ConfigDso configDso;

    @Before
    public void init() {
        trigger = new FlexOrderEventTrigger();
        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, eventManager);
        Whitebox.setInternalState(trigger, configDso);
    }

    @Test
    public void testRegisterTrigger() {
        ArgumentCaptor<WorkItemExecution> workItemExecutionArgumentCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        ArgumentCaptor<FlexOrderEvent> eventCaptor = ArgumentCaptor.forClass(FlexOrderEvent.class);

        Mockito.when(configDso.getProperty(Matchers.eq(ConfigDsoParam.DSO_FLEXORDER_INITIAL_DELAY_IN_SECONDS))).thenReturn("1");
        Mockito.when(configDso.getProperty(Matchers.eq(ConfigDsoParam.DSO_FLEXORDER_INTERVAL_IN_SECONDS))).thenReturn("60");

        trigger.registerTrigger();

        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.anyString(),
                workItemExecutionArgumentCaptor.capture(),
                Matchers.any(Long.class),
                Matchers.any(Long.class));
        WorkItemExecution workItemExecution = workItemExecutionArgumentCaptor.getValue();

        workItemExecution.execute();

        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());
        Mockito.verify(configDso, Mockito.times(1)).getProperty(ConfigDsoParam.DSO_FLEXORDER_INITIAL_DELAY_IN_SECONDS);
        Mockito.verify(configDso, Mockito.times(1)).getProperty(ConfigDsoParam.DSO_FLEXORDER_INTERVAL_IN_SECONDS);
    }
}
