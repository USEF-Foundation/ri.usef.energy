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

package energy.usef.brp.workflow.step;

import energy.usef.brp.workflow.BrpWorkflowStep;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.step.WorkflowStepLoader;

import java.util.concurrent.ExecutorService;

import javax.enterprise.inject.Instance;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class PBCCatalogTest {
    private WorkflowStepLoader workflowStepLoader;

    @Mock
    private Instance<WorkflowStep> instance;

    @Mock
    private Instance<WorkflowStep> myBeans;

    @Mock
    private ExecutorService watcherExecutor;

    @Before
    public void init() {
        workflowStepLoader = new WorkflowStepLoader();
        Whitebox.setInternalState(workflowStepLoader, myBeans);
        Whitebox.setInternalState(workflowStepLoader, watcherExecutor);

        ArgumentCaptor<Class> classCaptor = ArgumentCaptor.forClass(Class.class);
        Mockito.when(myBeans.select(classCaptor.capture())).thenReturn(instance);
        Mockito.when(instance.get()).then(methodCall -> classCaptor.getValue().newInstance());

        workflowStepLoader.init();
    }

    @Test
    public void testPBCCatalog() {
        for (BrpWorkflowStep brpWorkflowStep : BrpWorkflowStep.values()) {
            Assert.assertNotNull(workflowStepLoader.getWorkflowStep(brpWorkflowStep.name()));
        }
    }
}
