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

package energy.usef.agr.workflow.plan.connection.forecast;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.config.Config;
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
 * Unit-test for time trigger of {@link CreateConnectionForecastEventTrigger}.
 */
@RunWith(PowerMockRunner.class)
public class CreateConnectionForecastEventTriggerTest {

    @Mock
    private SchedulerHelperService schedulerHelperService;

    @Mock
    private Event<CreateConnectionForecastEvent> eventManager;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private Config config;

    private CreateConnectionForecastEventTrigger trigger;

    @Before
    public void init() {
        trigger = new CreateConnectionForecastEventTrigger();

        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, eventManager);
        Whitebox.setInternalState(trigger, configAgr);

        PowerMockito.when(configAgr.getProperty(ConfigAgrParam.AGR_CONNECTION_FORECAST_DAYS_INTERVAL)).thenReturn("1");
        PowerMockito.when(configAgr.getProperty(ConfigAgrParam.AGR_CONNECTION_FORECAST_TIME)).thenReturn("13:00");
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
        ArgumentCaptor<CreateConnectionForecastEvent> eventCaptor = ArgumentCaptor.forClass(CreateConnectionForecastEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());
        CreateConnectionForecastEvent createConnectionForecastEvent = eventCaptor.getValue();
        Assert.assertNotNull(createConnectionForecastEvent);
        Assert.assertEquals("CreateConnectionForecastEvent[]", createConnectionForecastEvent.toString());

    }

}
