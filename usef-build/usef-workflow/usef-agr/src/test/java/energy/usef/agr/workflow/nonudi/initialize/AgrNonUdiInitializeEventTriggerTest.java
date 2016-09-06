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
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;

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
 * Test class in charge of the unit tests related to the {@link AgrNonUdiInitializeEventTrigger}.
 */
@RunWith(PowerMockRunner.class)
public class AgrNonUdiInitializeEventTriggerTest {
    private AgrNonUdiInitializeEventTrigger trigger;

    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    private Event<AgrNonUdiInitializeEvent> eventManager;
    @Mock
    private ConfigAgr configAgr;

    @Before
    public void init() {
        trigger = new AgrNonUdiInitializeEventTrigger();
        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, eventManager);
        Whitebox.setInternalState(trigger, configAgr);
    }

    @Test
    public void testRegisterTriggerNonUdi() {
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        ArgumentCaptor<AgrNonUdiInitializeEvent> eventCaptor = ArgumentCaptor.forClass(AgrNonUdiInitializeEvent.class);

        Mockito.when(configAgr.getBooleanProperty(Matchers.eq(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR))).thenReturn(true);
        Mockito.when(configAgr.getProperty(Matchers.eq(ConfigAgrParam.AGR_INITIALIZE_NON_UDI_TIME_OF_DAY))).thenReturn("00:01");

        trigger.registerTrigger();

        Mockito.verify(schedulerHelperService, Mockito.times(1))
                .registerScheduledCall(Matchers.anyString(), runnableCaptor.capture(), Matchers.any(Long.class),
                        Matchers.any(Long.class));
        WorkItemExecution workItemExecution = runnableCaptor.getValue();

        workItemExecution.execute();

        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());
        Mockito.verify(configAgr, Mockito.times(1)).getProperty(ConfigAgrParam.AGR_INITIALIZE_NON_UDI_TIME_OF_DAY);
    }

    @Test
    public void testRegisterTriggerUdi() {
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        ArgumentCaptor<AgrNonUdiInitializeEvent> eventCaptor = ArgumentCaptor.forClass(AgrNonUdiInitializeEvent.class);

        Mockito.when(configAgr.getBooleanProperty(Matchers.eq(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR))).thenReturn(false);
        Mockito.when(configAgr.getProperty(Matchers.eq(ConfigAgrParam.AGR_INITIALIZE_NON_UDI_TIME_OF_DAY))).thenReturn("00:01");

        trigger.registerTrigger();

        Mockito.verify(schedulerHelperService, Mockito.times(0))
                .registerScheduledCall(Matchers.anyString(), runnableCaptor.capture(), Matchers.any(Long.class),
                        Matchers.any(Long.class));

        Mockito.verify(configAgr, Mockito.times(1)).getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR);
    }
}


