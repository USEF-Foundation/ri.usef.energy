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

package energy.usef.agr.workflow.validate.flexoffer;

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
 * Test class in charge of the unit tests related to the {@link FlexOfferEventTrigger}.
 */
@RunWith(PowerMockRunner.class)
public class FlexOfferEventTriggerTest {
    private FlexOfferEventTrigger trigger;

    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    private Event<FlexOfferEvent> eventManager;
    @Mock
    private ConfigAgr configAgr;

    @Before
    public void init() {
        trigger = new FlexOfferEventTrigger();
        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, eventManager);
        Whitebox.setInternalState(trigger, configAgr);
    }

    @Test
    public void testRegisterTrigger() {
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        ArgumentCaptor<FlexOfferEvent> eventCaptor = ArgumentCaptor.forClass(FlexOfferEvent.class);

        Mockito.when(configAgr.getProperty(Matchers.eq(ConfigAgrParam.AGR_FLEXOFFER_INITIAL_DELAY_IN_SECONDS))).thenReturn("1");
        Mockito.when(configAgr.getProperty(Matchers.eq(ConfigAgrParam.AGR_FLEXOFFER_INTERVAL_IN_SECONDS))).thenReturn("60");

        trigger.registerTrigger();

        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.anyString(),
                runnableCaptor.capture(),
                Matchers.any(Long.class),
                Matchers.any(Long.class));
        WorkItemExecution workItemExecution = runnableCaptor.getValue();

        workItemExecution.execute();

        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());
        Mockito.verify(configAgr, Mockito.times(1)).getProperty(ConfigAgrParam.AGR_FLEXOFFER_INITIAL_DELAY_IN_SECONDS);
        Mockito.verify(configAgr, Mockito.times(1)).getProperty(ConfigAgrParam.AGR_FLEXOFFER_INTERVAL_IN_SECONDS);

    }
}
