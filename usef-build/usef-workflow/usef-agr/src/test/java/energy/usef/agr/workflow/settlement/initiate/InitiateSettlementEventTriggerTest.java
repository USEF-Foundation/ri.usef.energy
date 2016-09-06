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

package energy.usef.agr.workflow.settlement.initiate;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;
import energy.usef.core.util.DateTimeUtil;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.junit.Assert;
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
 * Test class in charge of the unit tests related to the {@link InitiateSettlementEventTrigger} class.
 */
@RunWith(PowerMockRunner.class)
public class InitiateSettlementEventTriggerTest {
    private static final String TASK_NAME = "Initiate Settlement";
    private InitiateSettlementEventTrigger initiateSettlementEventTrigger;
    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    protected ConfigAgr configAgr;
    @Mock
    private Event<InitiateSettlementEvent> eventManager;

    @Before
    public void setUp() throws Exception {
        initiateSettlementEventTrigger = new InitiateSettlementEventTrigger();
        Whitebox.setInternalState(initiateSettlementEventTrigger, schedulerHelperService);
        Whitebox.setInternalState(initiateSettlementEventTrigger, configAgr);
        Whitebox.setInternalState(initiateSettlementEventTrigger, eventManager);
        Mockito.when(configAgr.getProperty(ConfigAgrParam.AGR_INITIATE_SETTLEMENT_TIME_OF_DAY)).thenReturn("11:00");
    }

    @Test
    public void testRegisterTriggerWithDayOfMonthIsSettlementDay() throws Exception {
        LocalDate today = DateTimeUtil.getCurrentDate();
        Mockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIATE_SETTLEMENT_DAY_OF_MONTH))
                .thenReturn(today.getDayOfMonth());
        ArgumentCaptor<WorkItemExecution> taskCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        // invocation
        initiateSettlementEventTrigger.registerTrigger();
        // validation
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.eq(TASK_NAME),
                taskCaptor.capture(),
                Matchers.any(Long.class),
                Matchers.any(Long.class));
        WorkItemExecution task = taskCaptor.getValue();
        // invocation
        task.execute();
        // validation
        Mockito.verify(configAgr, Mockito.times(1)).getIntegerProperty(ConfigAgrParam.AGR_INITIATE_SETTLEMENT_DAY_OF_MONTH);
        Mockito.verify(configAgr, Mockito.times(1)).getProperty(ConfigAgrParam.AGR_INITIATE_SETTLEMENT_TIME_OF_DAY);
        Assert.assertNotNull(task);
        ArgumentCaptor<InitiateSettlementEvent> eventCaptor = ArgumentCaptor.forClass(InitiateSettlementEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());
        InitiateSettlementEvent firedEvent = eventCaptor.getValue();
        Assert.assertEquals(today, firedEvent.getPeriodInMonth());
    }

    @Test
    public void testRegisterTriggerWithDayOfMonthIsNotSettlementDay() throws Exception {
        LocalDate today = DateTimeUtil.getCurrentDate();
        Mockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIATE_SETTLEMENT_DAY_OF_MONTH))
                .thenReturn(today.plusDays(1).getDayOfMonth());
        ArgumentCaptor<WorkItemExecution> taskCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        // invocation
        initiateSettlementEventTrigger.registerTrigger();
        // validation
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.eq(TASK_NAME),
                taskCaptor.capture(),
                Matchers.any(Long.class),
                Matchers.any(Long.class));
        WorkItemExecution task = taskCaptor.getValue();
        // invocation
        task.execute();
        // validation
        Mockito.verify(configAgr, Mockito.times(1)).getIntegerProperty(ConfigAgrParam.AGR_INITIATE_SETTLEMENT_DAY_OF_MONTH);
        Mockito.verify(configAgr, Mockito.times(1)).getProperty(ConfigAgrParam.AGR_INITIATE_SETTLEMENT_TIME_OF_DAY);
        Assert.assertNotNull(task);
        ArgumentCaptor<InitiateSettlementEvent> eventCaptor = ArgumentCaptor.forClass(InitiateSettlementEvent.class);
        Mockito.verifyZeroInteractions(eventManager);

    }
}
