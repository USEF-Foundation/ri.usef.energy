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

package energy.usef.core.service.helper;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class for units tests related to the {@link WorkItem} class.
 */
@RunWith(PowerMockRunner.class)
public class WorkItemTest {

    @Mock
    private Config config;

    @Mock
    private WorkItemExecution runnable;

    private WorkItem workItem;

    @Before
    public void init() {
        workItem = new WorkItem(runnable, config, "AWorkItem");
    }

    /**
     * Tests whether the {@link WorkItemTest} event is actually executed.
     */
    @Test
    public void testRunScheduledEventsNotBypassed() {
        PowerMockito.when(config.getBooleanProperty(ConfigParam.BYPASS_SCHEDULED_EVENTS)).thenReturn(false);
        workItem.run();
        Mockito.verify(runnable, Mockito.times(1)).execute();
    }

    /**
     * Tests whether the {@link WorkItemTest} event is actually bypassed.
     */
    @Test
    public void testRunScheduledEventsBypassed() {
        PowerMockito.when(config.getBooleanProperty(ConfigParam.BYPASS_SCHEDULED_EVENTS)).thenReturn(true);
        workItem.run();
        Mockito.verify(runnable, Mockito.times(0)).execute();
    }
}
