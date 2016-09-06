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

package energy.usef.dso.workflow.plan.connection.forecast;

import energy.usef.core.config.Config;
import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;

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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the event triggering in the DSO Non-Aggregator Connection Forecast workflow.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Config.class })
public class DsoCommonReferenceQueryEventTriggerTest {

    @Mock
    private SchedulerHelperService schedulerHelperService;

    @Mock
    private Event<CommonReferenceQueryEvent> eventManager;

    @Mock
    private ConfigDso configDso;

    private DsoCommonReferenceQueryEventTrigger trigger;

    @Before
    public void init() {
        trigger = new DsoCommonReferenceQueryEventTrigger();
        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, eventManager);
        Whitebox.setInternalState(trigger, configDso);

        PowerMockito.when(configDso.getProperty(ConfigDsoParam.DSO_INITIALIZE_PLANBOARD_DAYS_INTERVAL)).thenReturn("1");
        PowerMockito.when(configDso.getProperty(ConfigDsoParam.DSO_INITIALIZE_PLANBOARD_TIME)).thenReturn("11:00");
    }

    @Test
    public void testEventTriggerIsCorrectlyRegistered() {
        trigger.scheduleTrigger();
        Long interval = Days.ONE.toStandardDuration().getMillis();
        ArgumentCaptor<WorkItemExecution> captor = ArgumentCaptor.forClass(WorkItemExecution.class);
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.anyString(), captor.capture(),
                Matchers.any(Long.class),
                Matchers.eq(interval));

        WorkItemExecution workItemExecution = captor.getValue();
        Assert.assertNotNull("Scheduled task is empty", workItemExecution);

        workItemExecution.execute();
        Mockito.verify(eventManager, Mockito.times(1)).fire(Matchers.any(CommonReferenceQueryEvent.class));

    }
}
