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

import energy.usef.core.service.helper.SchedulerHelperService;
import energy.usef.core.service.helper.WorkItemExecution;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;

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
public class DsoCreateConnectionForecastEventTriggerTest {

    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    private Event<CreateConnectionForecastEvent> createConnectionForecastEventManager;
    @Mock
    private ConfigDso configDso;

    private DsoCreateConnectionForecastEventTrigger trigger;

    @Before
    public void setUp() throws Exception {
        trigger = new DsoCreateConnectionForecastEventTrigger();
        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, configDso);
        Whitebox.setInternalState(trigger, createConnectionForecastEventManager);
    }

    @Test
    public void testRegisterTrigger() throws Exception {
        // stubbing of the Config
        Mockito.when(configDso.getProperty(ConfigDsoParam.DSO_CONNECTION_FORECAST_TIME)).thenReturn("11:00");
        Mockito.when(configDso.getProperty(ConfigDsoParam.DSO_CONNECTION_FORECAST_DAYS_INTERVAL)).thenReturn("1");

        trigger.registerTrigger();

        // captor
        ArgumentCaptor<WorkItemExecution> runnableCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);

        // verification
        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(
                Matchers.eq("DSOCreateConnectionForecastEvent"),
                runnableCaptor.capture(),
                Matchers.anyLong(),
                Matchers.anyLong());

        runnableCaptor.getValue().execute();

        Mockito.verify(createConnectionForecastEventManager, Mockito.times(1))
                .fire(Matchers.any(CreateConnectionForecastEvent.class));
    }
}
