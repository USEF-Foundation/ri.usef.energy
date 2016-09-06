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

package energy.usef.brp.workflow.plan.flexorder.place;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link FlexOrderEventTrigger} class.
 */
@RunWith(PowerMockRunner.class)
public class FlexOrderEventTriggerTest {

    private FlexOrderEventTrigger trigger;

    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    private Event<FlexOrderEvent> flexOrderEventManager;
    @Mock
    private ConfigBrp configBrp;

    @Before
    public void init() {
        trigger = new FlexOrderEventTrigger();
        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, flexOrderEventManager);
        Whitebox.setInternalState(trigger, configBrp);

        PowerMockito.when(configBrp.getProperty(ConfigBrpParam.BRP_FLEXORDER_INITIAL_DELAY_IN_SECONDS)).thenReturn("30");
        PowerMockito.when(configBrp.getProperty(ConfigBrpParam.BRP_FLEXORDER_INTERVAL_IN_SECONDS)).thenReturn("5");
    }

    @Test
    public void testRegisterTrigger() {
        ArgumentCaptor<WorkItemExecution> captor = ArgumentCaptor.forClass(WorkItemExecution.class);
        trigger.registerTrigger();
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.eq("BRPFlexOrder"),
                captor.capture(),
                Matchers.any(Long.class), Matchers.any(Long.class));

        WorkItemExecution workItemExecution = captor.getValue();
        workItemExecution.execute();

        Mockito.verify(flexOrderEventManager, Mockito.times(1)).fire(Matchers.any(FlexOrderEvent.class));
        Mockito.verify(configBrp, Mockito.times(1)).getProperty(ConfigBrpParam.BRP_FLEXORDER_INTERVAL_IN_SECONDS);
        Mockito.verify(configBrp, Mockito.times(1)).getProperty(ConfigBrpParam.BRP_FLEXORDER_INITIAL_DELAY_IN_SECONDS);

    }

}
