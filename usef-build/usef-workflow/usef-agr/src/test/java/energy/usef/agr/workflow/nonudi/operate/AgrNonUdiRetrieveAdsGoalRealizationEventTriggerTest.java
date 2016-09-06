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

package energy.usef.agr.workflow.nonudi.operate;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.workflow.AgrWorkflowStep;
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

/**
 * Test class in charge of the unit tests related to the {@link AgrNonUdiRetrieveAdsGoalRealizationEventTrigger} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrNonUdiRetrieveAdsGoalRealizationEventTriggerTest {

    private AgrNonUdiRetrieveAdsGoalRealizationEventTrigger trigger;
    @Mock
    private ConfigAgr configAgr;
    @Mock
    private SchedulerHelperService schedulerHelperService;
    @Mock
    private Event<AgrNonUdiRetrieveAdsGoalRealizationEvent> agrNonUdiRetrieveAdsGoalRealizationEventManager;

    @Before
    public void setUp() throws Exception {
        trigger = new AgrNonUdiRetrieveAdsGoalRealizationEventTrigger();
        Whitebox.setInternalState(trigger, configAgr);
        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, agrNonUdiRetrieveAdsGoalRealizationEventManager);
        Mockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION_INTERVAL_IN_MINUTES))
                .thenReturn(15);
        Mockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(Boolean.TRUE);
    }

    @Test
    public void testRegisterTrigger() {
        trigger.registerTrigger();
        ArgumentCaptor<WorkItemExecution> workItemArgumentCaptor = ArgumentCaptor.forClass(WorkItemExecution.class);
        Mockito.verify(configAgr, Mockito.times(1))
                .getIntegerProperty(ConfigAgrParam.AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION_INTERVAL_IN_MINUTES);
        Mockito.verify(schedulerHelperService, Mockito.times(1))
                .registerScheduledCall(Matchers.eq(AgrWorkflowStep.AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION.name()),
                        workItemArgumentCaptor.capture(), Matchers.any(Long.class), Matchers.any(Long.class));
        WorkItemExecution workItemExecution = workItemArgumentCaptor.getValue();
        Assert.assertNotNull(workItemExecution);
        workItemExecution.execute();
        Mockito.verify(agrNonUdiRetrieveAdsGoalRealizationEventManager, Mockito.times(1))
                .fire(Matchers.any(AgrNonUdiRetrieveAdsGoalRealizationEvent.class));
    }
}
