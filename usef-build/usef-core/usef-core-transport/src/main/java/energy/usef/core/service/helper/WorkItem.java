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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class used for scheduling {@link Runnable} classes.
 */
public class WorkItem implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkItem.class);

    private WorkItemExecution workItemExecution;
    private Config config;
    private String name;

    /**
     * Creates a new WorkItem object that can be scheduled using {@link SchedulerHelperService} .
     *
     * @param workItemExecution The actual {@link WorkItemExecution} to schedule.
     * @param config   The {@link Config} used to determine in the actual {@link Runnable} should be bypassed.
     * @param name     The name to used for the event.
     */
    public WorkItem(WorkItemExecution workItemExecution, Config config, String name) {
        this.workItemExecution = workItemExecution;
        this.config = config;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            boolean bypassSchedeldEvents = config.getBooleanProperty(ConfigParam.BYPASS_SCHEDULED_EVENTS);
            if (bypassSchedeldEvents) {
                LOGGER.debug("Ignored scheduled event {}", name);
            } else {
                LOGGER.debug("Started scheduled event {}", name);
                workItemExecution.execute();
                LOGGER.debug("Finished scheduled event {}", name);
            }
        } catch (Exception e) {
            // catch all to prevent shutdown of jobs
            LOGGER.error("Schedule execution failed {}", e.getMessage(), e);
        }

    }
}
