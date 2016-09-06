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
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;

import java.util.concurrent.TimeUnit;

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
 * Test class in charge of the unit tests related the {@link DsoSettlementMessageDispositionTimer} class.
 */
@RunWith(PowerMockRunner.class)
public class DsoSettlementMessageDispositionTimerTest {

    @Mock
    private PlanboardMessageRepository planboardMessageRepository;
    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    private ConfigDso configDso;

    private DsoSettlementMessageDispositionTimer dsoSettlementMessageDispositionTimer;

    @Before
    public void init() {
        dsoSettlementMessageDispositionTimer = new DsoSettlementMessageDispositionTimer();
        Whitebox.setInternalState(dsoSettlementMessageDispositionTimer, schedulerHelperService);
        Whitebox.setInternalState(dsoSettlementMessageDispositionTimer, planboardMessageRepository);
        Whitebox.setInternalState(dsoSettlementMessageDispositionTimer, configDso);
        PowerMockito.when(configDso.getProperty(ConfigDsoParam.DSO_SETTLEMENT_MESSAGE_DISPOSAL_TIME)).thenReturn("22:00");
    }

    @Test
    public void testTriggerIsRegistered() {
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        dsoSettlementMessageDispositionTimer.registerTrigger();
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(
                Matchers.eq("SETTLEMENT_MESSAGE_DISPOSITION_TIMER"), runnableCaptor.capture(), Matchers.any(Long.class),
                Matchers.eq(TimeUnit.DAYS.toMillis(1)));

        WorkItemExecution workItemExecution = runnableCaptor.getValue();
        Assert.assertNotNull(workItemExecution);
        workItemExecution.execute();
        Mockito.verify(planboardMessageRepository, Mockito.times(1)).updateOldSettlementMessageDisposition();
    }
}
