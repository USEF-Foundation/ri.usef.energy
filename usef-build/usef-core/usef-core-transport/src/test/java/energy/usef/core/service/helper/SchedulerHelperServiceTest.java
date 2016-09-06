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
import energy.usef.core.util.DateTimeUtil;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.Days;
import org.joda.time.Seconds;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class for units tests related to the {@link SchedulerHelperService} class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DateTimeUtil.class })
public class SchedulerHelperServiceTest {

    private SchedulerHelperService schedulerHelperService;

    private static final long TIME_FACTOR = 2;

    @Mock
    private Config config;

    @Mock
    private ScheduledExecutorService scheduledExecutorService;

    @Before
    public void init() {
        schedulerHelperService = new SchedulerHelperService();
        Whitebox.setInternalState(schedulerHelperService, "config", config);
        Whitebox.setInternalState(schedulerHelperService, "scheduler", scheduledExecutorService);
        PowerMockito.mockStatic(DateTimeUtil.class);
        PowerMockito.when(DateTimeUtil.getTimeFactor()).thenReturn(TIME_FACTOR);
    }

    /**
     * Tests whether the {@link SchedulerHelperService} actually registers a scheduled task.
     */
    @Test
    public void testRegisterScheduledCall() {
        long delay = Seconds.ONE.toStandardDuration().getMillis();
        long period = Days.ONE.toStandardDuration().getMillis();
        schedulerHelperService.registerScheduledCall("test", System::currentTimeMillis, delay, period);
        Mockito.verify(scheduledExecutorService, Mockito.times(1)).scheduleAtFixedRate(Matchers.any(Runnable.class),
                Matchers.eq(delay / TIME_FACTOR),
                Matchers.eq(period / TIME_FACTOR),
                Matchers.eq(TimeUnit.MILLISECONDS));
    }
}
