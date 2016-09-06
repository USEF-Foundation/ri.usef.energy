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

package energy.usef.agr.workflow.plan.commonreferenceupdate;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;

import javax.enterprise.event.Event;

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

@RunWith(PowerMockRunner.class)
public class CommonReferenceUpdateEventTriggerTest {
    private static final String WORKFLOW_NAME = "CommonReferenceUpdateEvent";
    private static final Long MILLISECONDS_PER_DAY = 86400000L;

    private CommonReferenceUpdateEventTrigger commonReferenceUpdateEventTrigger;
    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    private Event<CommonReferenceUpdateEvent> eventManager;
    @Mock
    private ConfigAgr configAgr;

    @Before
    public void init() {
        commonReferenceUpdateEventTrigger = new CommonReferenceUpdateEventTrigger();
        Whitebox.setInternalState(commonReferenceUpdateEventTrigger, schedulerHelperService);
        Whitebox.setInternalState(commonReferenceUpdateEventTrigger, eventManager);
        Whitebox.setInternalState(commonReferenceUpdateEventTrigger, configAgr);
        Mockito.when(configAgr.getProperty(Matchers.eq(ConfigAgrParam.AGR_COMMON_REFERENCE_UPDATE_TIME))).thenReturn("11:00");
    }

    @Test
    public void testRegisterTrigger() throws Exception {
        ArgumentCaptor<WorkItemExecution> taskCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        // actual invocation
        commonReferenceUpdateEventTrigger.registerTrigger();
        // assertions and verification
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.eq(WORKFLOW_NAME),
                taskCaptor.capture(), Matchers.any(Long.class), Matchers.eq(MILLISECONDS_PER_DAY));
        WorkItemExecution workItemExecution = taskCaptor.getValue();
        Assert.assertNotNull(workItemExecution);
        workItemExecution.execute();
        ArgumentCaptor<CommonReferenceUpdateEvent> eventCaptor = ArgumentCaptor.forClass(CommonReferenceUpdateEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());
        CommonReferenceUpdateEvent commonReferenceUpdateEvent = eventCaptor.getValue();
        Assert.assertNotNull(commonReferenceUpdateEvent);
        Assert.assertEquals("CommonReferenceUpdateEvent[]", commonReferenceUpdateEvent.toString());
    }
}
