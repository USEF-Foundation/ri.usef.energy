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

package energy.usef.mdc.workflow;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;
import energy.usef.mdc.config.ConfigMdc;

import java.util.concurrent.TimeUnit;

import javax.enterprise.event.Event;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link MdcCommonReferenceQueryEventTrigger} class.
 */
@RunWith(PowerMockRunner.class)
public class MdcCommonReferenceQueryEventTriggerTest {

    private MdcCommonReferenceQueryEventTrigger trigger;

    @Mock
    private ConfigMdc configMdc;
    @Mock
    private Event<CommonReferenceQueryEvent> commonReferenceQueryEventManager;
    @Mock
    private SchedulerHelperService schedulerHelperService;

    @Before
    public void setUp() throws Exception {
        trigger = new MdcCommonReferenceQueryEventTrigger();
        Whitebox.setInternalState(trigger, configMdc);
        Whitebox.setInternalState(trigger, commonReferenceQueryEventManager);
        Whitebox.setInternalState(trigger, schedulerHelperService);
    }

    @Test
    public void testRegisterTriggerIsSuccessful() {
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        trigger.registerTrigger();
        Mockito.verify(schedulerHelperService, Mockito.times(1))
                .registerScheduledCall(eq("MDC_COMMON_REFERENCE_QUERY_EVENT"), runnableCaptor.capture(), any(Long.class),
                        eq(TimeUnit.DAYS.toMillis(1)));
        WorkItemExecution scheduledTask = runnableCaptor.getValue();
        Assert.assertNotNull(scheduledTask);
        scheduledTask.execute();
        Mockito.verify(commonReferenceQueryEventManager, Mockito.times(1)).fire(any(CommonReferenceQueryEvent.class));
    }
}
