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
import energy.usef.dso.workflow.operate.PrepareStepwiseLimitingStepParameter.IN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DsoPrepareStepwiseLimitingStub.
 */
public class DsoPrepareStepwiseLimitingStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoPrepareStepwiseLimitingStub.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.debug("Starting workflow step 'Prepare Stepwise Limiting'.");
        LOGGER.debug("Parameters given in the context:\n # congestion point: {}\n",
                context.getValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name()));

        // Get list of PTU containers, and do something with it.
        context.getValue(IN.PTU_CONTAINERS.name());
        LOGGER.debug("Ending successfully workflow step 'Prepare Stepwise Limiting'.");

        return context;
    }
}
