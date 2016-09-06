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

package energy.usef.dso.workflow.plan.commonreferenceupdate;

import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;
import energy.usef.dso.config.ConfigDso;

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

@RunWith(PowerMockRunner.class)
public class CommonReferenceUpdateEventTriggerTest {

    private CommonReferenceUpdateEventTrigger commonReferenceUpdateEventTrigger;

    @Mock
    private SchedulerHelperService schedulerHelperService;

    @Mock
    private Event<CommonReferenceUpdateEvent> eventManager;

    @Mock
    private ConfigDso configDso;

    @Before
    public void init() {
        commonReferenceUpdateEventTrigger = new CommonReferenceUpdateEventTrigger();
        Whitebox.setInternalState(commonReferenceUpdateEventTrigger, schedulerHelperService);
        Whitebox.setInternalState(commonReferenceUpdateEventTrigger, eventManager);
        Whitebox.setInternalState(commonReferenceUpdateEventTrigger, configDso);
    }

    @Test
    public void testRegisterTrigger() {

        commonReferenceUpdateEventTrigger.registerTrigger();

        ArgumentCaptor<WorkItemExecution> runnable = ArgumentCaptor.forClass(WorkItemExecution.class);

        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.eq("CommonReferenceUpdateEvent"),
                runnable.capture(), Matchers.anyLong(), Matchers.eq(86400000L));

        runnable.getValue().execute();

        Mockito.verify(eventManager, Mockito.times(1)).fire(Mockito.any(CommonReferenceUpdateEvent.class));
    }

}
