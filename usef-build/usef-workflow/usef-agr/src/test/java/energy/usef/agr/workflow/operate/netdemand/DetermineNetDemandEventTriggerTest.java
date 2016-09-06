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

package energy.usef.agr.workflow.operate.netdemand;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.service.helper.SchedulerHelperService;

import javax.enterprise.event.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit test for the {@link DetermineNetDemandEventTrigger} class.
 */
@RunWith(PowerMockRunner.class)
public class DetermineNetDemandEventTriggerTest {

    private final static int AGR_DETERMINE_NETDEMANDS_INITIAL_DELAY_IN_SECONDS = 60;
    private final static int AGR_DETERMINE_NETDEMANDS_INTERVAL_IN_SECONDS = 120;

    @Mock
    private SchedulerHelperService schedulerHelperService;

    @Mock
    private Event<DetermineNetDemandEvent> eventManager;

    @Mock
    private ConfigAgr configAgr;

    private DetermineNetDemandEventTrigger trigger;

    @Before
    public void init() {
        trigger = new DetermineNetDemandEventTrigger();

        Whitebox.setInternalState(trigger, schedulerHelperService);
        Whitebox.setInternalState(trigger, eventManager);
        Whitebox.setInternalState(trigger, configAgr);

        PowerMockito.when(configAgr.getProperty(ConfigAgrParam.AGR_DETERMINE_NETDEMANDS_INITIAL_DELAY_IN_SECONDS))
                .thenReturn(String.valueOf(AGR_DETERMINE_NETDEMANDS_INITIAL_DELAY_IN_SECONDS));
        PowerMockito.when(configAgr.getProperty(ConfigAgrParam.AGR_DETERMINE_NETDEMANDS_INTERVAL_IN_SECONDS))
                .thenReturn(String.valueOf(AGR_DETERMINE_NETDEMANDS_INTERVAL_IN_SECONDS));
    }

    @Test
    public void testEventTriggerForNonUdiAggregator() {
        PowerMockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(true);

        trigger.registerTrigger();

        Mockito.verifyZeroInteractions(schedulerHelperService);
    }

    @Test
    public void testEventTriggerForUdiAggregator() {
        PowerMockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(false);

        trigger.registerTrigger();

        Mockito.verify(schedulerHelperService, Mockito.times(1)).registerScheduledCall(Matchers.anyString(), Matchers.any(),
                Matchers.eq((long) AGR_DETERMINE_NETDEMANDS_INITIAL_DELAY_IN_SECONDS * 1000),
                Matchers.eq((long) AGR_DETERMINE_NETDEMANDS_INTERVAL_IN_SECONDS * 1000));
    }
}
