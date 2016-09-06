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

package energy.usef.brp.workflow.settlement.send;

import static org.junit.Assert.assertNotNull;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;

import java.util.concurrent.TimeUnit;

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
 * Test class in charge of the unit tests related the {@link BrpSettlementMessageDispositionTimer} class.
 */
@RunWith(PowerMockRunner.class)
public class BrpSettlementMessageDispositionTimerTest {

    @Mock
    private PlanboardMessageRepository planboardMessageRepository;
    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    private ConfigBrp configBrp;

    private BrpSettlementMessageDispositionTimer brpSettlementMessageDispositionTimer;

    @Before
    public void init() throws Exception {
        brpSettlementMessageDispositionTimer = new BrpSettlementMessageDispositionTimer();
        Whitebox.setInternalState(brpSettlementMessageDispositionTimer, planboardMessageRepository);
        Whitebox.setInternalState(brpSettlementMessageDispositionTimer, schedulerHelperService);
        Whitebox.setInternalState(brpSettlementMessageDispositionTimer, configBrp);
        PowerMockito.when(configBrp.getProperty(ConfigBrpParam.BRP_SETTLEMENT_MESSAGE_DISPOSAL_TIME)).thenReturn("22:00");
    }

    @Test
    public void testTriggerIsRegistered() {
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        brpSettlementMessageDispositionTimer.registerTrigger();
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(
                Matchers.eq("SETTLEMENT_MESSAGE_DISPOSITION_TIMER"), runnableCaptor.capture(), Matchers.any(Long.class),
                Matchers.eq(TimeUnit.DAYS.toMillis(1)));

        WorkItemExecution runnable = runnableCaptor.getValue();
        assertNotNull(runnable);
        runnable.execute();
        Mockito.verify(planboardMessageRepository, Mockito.times(1)).updateOldSettlementMessageDisposition();

    }
}
