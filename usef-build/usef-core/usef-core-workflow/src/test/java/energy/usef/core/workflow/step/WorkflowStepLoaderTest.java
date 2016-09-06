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

import energy.usef.core.workflow.WorkflowStep;

import java.util.concurrent.ExecutorService;

import javax.enterprise.inject.Instance;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
public class WorkflowStepLoaderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowStepLoaderTest.class);

    private WorkflowStepLoader workflowStepLoader;

    @Mock
    Instance<TestWorkflowStep> instance;

    @Mock
    private Instance<WorkflowStep> myBeans;

    @Mock
    private ExecutorService watcherExecutor;

    @Before
    public void init() {
        workflowStepLoader = new WorkflowStepLoader();
        Whitebox.setInternalState(workflowStepLoader, myBeans);
        Whitebox.setInternalState(workflowStepLoader, watcherExecutor);
    }

    @Test
    public void testGetWorkflowStep() {

        Mockito.when(myBeans.select(TestWorkflowStep.class)).thenReturn(instance);
        Mockito.when(instance.get()).thenReturn(new TestWorkflowStep());

        workflowStepLoader.init();

        Assert.assertNotNull(workflowStepLoader.getWorkflowStep("TestWorkflowStep"));
    }

    @Test
    public void testFailedFind() {
        workflowStepLoader.init();
        Assert.assertNull(workflowStepLoader.getWorkflowStep("NONSENSE"));
    }

}
