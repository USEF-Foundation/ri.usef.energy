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

package energy.usef.core.workflow.step;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;

/**
 * This class loads the workflow step classes when the application is deployed, based on the contents of a file
 * "pbc-catalog.properties". The coordinator invokes a workflow step by calling the
 * {@link WorkflowStepExecuter#invoke(String, WorkflowContext)} method to find and execute the {@link WorkflowStep}.
 */
public class WorkflowStepExecuter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowStepExecuter.class);

    @Inject
    private WorkflowStepLoader workflowStepLoader;

    @Inject
    @Any
    private Instance<WorkflowStep> myBeans;


    /**
     * Method to find and invoke a workflowStep. An illegalArgumentException is thrown when the workflow step does not exists in the
     * map of workflow steps.
     *
     * @param workflowStepName the name of the workflow step.
     * @param inContext The context used to execute the step with.
     * @return
     */
    public WorkflowContext invoke(String workflowStepName, WorkflowContext inContext) {
        try {
            LOGGER.info("Starting WorkflowStep {}", workflowStepName);

            if (StringUtils.isEmpty(workflowStepName)) {
                throw new IllegalArgumentException("WorkflowStepName can not be empty.");
            }
            Class<WorkflowStep> clazz = workflowStepLoader.getWorkflowStep(workflowStepName);
            if (clazz == null) {
                throw new IllegalArgumentException("WorkflowStep: " + workflowStepName + " is not configured correctly, class " + workflowStepName + " .");
            }
            LOGGER.debug("Executing PBC: {} with input: {}", clazz, inContext);
            WorkflowContext resultContext = myBeans.select(clazz).get().invoke(inContext);
            LOGGER.debug("Executed PBC: {} with output: {}", clazz, resultContext);
            return resultContext;
        } finally {
            LOGGER.info("Finished WorkflowStep {}", workflowStepName);
        }
    }
}
