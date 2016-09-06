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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper service class to register scheduled call to methods.
 */
@Singleton
public class SchedulerHelperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerHelperService.class);

    @Inject
    private Config config;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(50);

    /**
     * Register a new scheduled task.
     *
     * @param name
     * @param runnable {@link Runnable} wrapping a call to a method.
     * @param initialDelay {@link Long} delay before the first execution of the scheduled call (in Milliseconds)
     * @param timePeriod {@link Long} interval between two executions of the scheduled call (in Milliseconds)
     */
    public void registerScheduledCall(final String name, final WorkItemExecution runnable, long initialDelay, long timePeriod) {
        long timeFactor = DateTimeUtil.getTimeFactor();
        LOGGER.info("Registering a new scheduled call: [{}].", name);
        LOGGER.info(" # Initial delay: {}, # Interval duration: {}", initialDelay, timePeriod);

        long realInitialDelay = initialDelay / timeFactor;
        long realTimePeriod = timePeriod / timeFactor;

        LOGGER.info(" # Accelerated delay {}, # Accelerated interval : {} (factor: {})", realInitialDelay, realTimePeriod,
                timeFactor);
        scheduler
                .scheduleAtFixedRate(new WorkItem(runnable, config, name), realInitialDelay, realTimePeriod, TimeUnit.MILLISECONDS);
        LOGGER.info(".. Registration successful!");
    }

    /**
     * Cleanup the memory before destroying the bean.
     */
    @PreDestroy
    public void cleanUp() {
        LOGGER.warn(" ### Bean will be destroyed and scheduler will be down!");
        scheduler.shutdown();
    }
}
