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

package energy.usef.dso.workflow.step;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.dso.workflow.coloring.PostColoringProcessParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This workflow step will be invoked after the Coloring Process is executed. This PBC will possibility to add extra (automatic)
 * process (e.g. raise an alarm, notify team member, ...) to be executed after the coloring process is finished.
 */
public class DsoPostColoringProcessStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoPostColoringProcessStub.class);

    /**
     * {@inheritDoc}
     */
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("Started post process for the coloring process for date {}.",
                context.getValue(PostColoringProcessParameter.IN.PERIOD.name()));

        return context;
    }

}
