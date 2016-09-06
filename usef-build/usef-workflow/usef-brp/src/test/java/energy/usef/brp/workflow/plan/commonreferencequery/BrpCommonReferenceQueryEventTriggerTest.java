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

package energy.usef.brp.workflow.plan.commonreferencequery;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.brp.workflow.plan.connection.forecast.CommonReferenceQueryEvent;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;

import javax.enterprise.event.Event;

import org.joda.time.Days;
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
 * 
 */
@RunWith(PowerMockRunner.class)
public class BrpCommonReferenceQueryEventTriggerTest {

    @Mock
    private SchedulerHelperService schedulerHelperService;

    @Mock
    private Event<CommonReferenceQueryEvent> eventManager;

    @Mock
    private ConfigBrp configBrp;

    private BrpCommonReferenceQueryEventTrigger trigger;

    @Before
    public void init() {
        trigger = new BrpCommonReferenceQueryEventTrigger();
        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, eventManager);
        Whitebox.setInternalState(trigger, configBrp);

        PowerMockito.when(configBrp.getIntegerProperty(ConfigBrpParam.BRP_INITIALIZE_PLANBOARD_DAYS_INTERVAL)).thenReturn(1);
        PowerMockito.when(configBrp.getProperty(ConfigBrpParam.BRP_INITIALIZE_PLANBOARD_TIME)).thenReturn("12:00");
    }

    @Test
    public void testEventTriggerIsCorrectlyRegistered() {
        trigger.registerTrigger();
        Long interval = Days.ONE.toStandardDuration().getMillis();
        ArgumentCaptor<WorkItemExecution> captor = ArgumentCaptor.forClass(WorkItemExecution.class);
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.anyString(), captor.capture(),
                Matchers.any(Long.class),
                Matchers.eq(interval));

        WorkItemExecution registeredRunnable = captor.getValue();
        Assert.assertNotNull("Scheduled task is empty", registeredRunnable);

        registeredRunnable.execute();
        Mockito.verify(eventManager, Mockito.times(1)).fire(Matchers.any(CommonReferenceQueryEvent.class));

    }
}
