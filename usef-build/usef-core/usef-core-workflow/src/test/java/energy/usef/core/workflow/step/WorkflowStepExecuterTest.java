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

import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowStep;

import javax.enterprise.inject.Instance;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
public class WorkflowStepExecuterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowStepExecuterTest.class);

    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    Instance<TestWorkflowStep> instance;

    @Mock
    private Instance<WorkflowStep> myBeans;
    @Mock
    private WorkflowStepLoader workflowStepLoader;

    @Before
    public void init() {
        workflowStepExecuter = new WorkflowStepExecuter();
        Whitebox.setInternalState(workflowStepExecuter, myBeans);
        Whitebox.setInternalState(workflowStepExecuter, workflowStepLoader);
    }

    @Test
    public void testStepLoader() {
        PowerMockito.doReturn(TestWorkflowStep.class).when(workflowStepLoader).getWorkflowStep("TestWorkflowStep");
        Mockito.when(myBeans.select(TestWorkflowStep.class)).thenReturn(instance);
        Mockito.when(instance.get()).thenReturn(new TestWorkflowStep());
        Assert.assertNotNull(workflowStepExecuter.invoke("TestWorkflowStep", new DefaultWorkflowContext()));
    }

    @Test
    public void testFailedFind() {
        try {
            workflowStepExecuter.invoke("NONSENSE", null);
            Assert.fail("Expecting a runtimeException");
        } catch (RuntimeException e) {
            LOGGER.info(e.getMessage());
        }
    }

}
