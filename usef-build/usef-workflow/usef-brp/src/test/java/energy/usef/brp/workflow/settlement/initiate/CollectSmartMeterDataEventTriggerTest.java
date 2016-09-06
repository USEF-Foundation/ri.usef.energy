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

package energy.usef.brp.workflow.settlement.initiate;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;
import energy.usef.core.util.DateTimeUtil;

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

@RunWith(PowerMockRunner.class)
public class CollectSmartMeterDataEventTriggerTest {

    @Mock
    private Event<CollectSmartMeterDataEvent> eventManager;

    @Mock
    private SchedulerHelperService schedulerHelperService;

    @Mock
    private ConfigBrp configBrp;

    private CollectSmartMeterDataEventTrigger trigger;

    @Before
    public void setUp() throws Exception {
        trigger = new CollectSmartMeterDataEventTrigger();
        Whitebox.setInternalState(trigger, eventManager);
        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, configBrp);

    }

    /**
     * Expect no event to be fired since day is not settlement day
     * 
     * @throws Exception
     */
    @Test
    public void testScheduleTriggerWithSettlementDayNotToday() throws Exception {
        PowerMockito.when(configBrp.getProperty(ConfigBrpParam.BRP_INITIATE_SETTLEMENT_DAY_OF_MONTH)).thenReturn("32");

        ArgumentCaptor<WorkItemExecution> WorkItemExecutionCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        trigger.scheduleTrigger();
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.eq("BrpInitiateSettlement"),
                WorkItemExecutionCaptor.capture(),
                Matchers.any(Long.class), Matchers.any(Long.class));

        WorkItemExecution WorkItemExecution = WorkItemExecutionCaptor.getValue();
        WorkItemExecution.execute();

        Mockito.verifyZeroInteractions(eventManager);
        Mockito.verify(configBrp, Mockito.times(1)).getProperty(ConfigBrpParam.BRP_INITIATE_SETTLEMENT_TIME);

    }

    @Test
    public void testScheduleTriggerWithSettlementDayToday() throws Exception {
        int today = DateTimeUtil.getCurrentDate().getDayOfMonth();
        PowerMockito.when(configBrp.getProperty(ConfigBrpParam.BRP_INITIATE_SETTLEMENT_DAY_OF_MONTH)).thenReturn(
                String.valueOf(today));
        ArgumentCaptor<WorkItemExecution> WorkItemExecutionCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        trigger.scheduleTrigger();
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.eq("BrpInitiateSettlement"),
                WorkItemExecutionCaptor.capture(),
                Matchers.any(Long.class), Matchers.any(Long.class));

        WorkItemExecution WorkItemExecution = WorkItemExecutionCaptor.getValue();
        WorkItemExecution.execute();

        Mockito.verify(eventManager, Mockito.times(1)).fire(Matchers.any(CollectSmartMeterDataEvent.class));
        Mockito.verify(configBrp, Mockito.times(1)).getProperty(ConfigBrpParam.BRP_INITIATE_SETTLEMENT_TIME);
    }
}
