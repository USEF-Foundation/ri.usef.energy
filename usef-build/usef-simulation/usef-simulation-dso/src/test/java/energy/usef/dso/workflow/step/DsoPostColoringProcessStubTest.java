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

import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.dso.workflow.coloring.PostColoringProcessParameter;

import java.util.ArrayList;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class DsoPostColoringProcessStubTest {
    private DsoPostColoringProcessStub dsoPostColoringProcessStub;

    @Before
    public void init() throws Exception {
        dsoPostColoringProcessStub = new DsoPostColoringProcessStub();
    }

    @Test
    public void testInvoke() {
        WorkflowContext context = buildWorkflowContext();
        WorkflowContext outContext = dsoPostColoringProcessStub.invoke(context);

        Assert.assertNotNull(outContext);
    }

    private WorkflowContext buildWorkflowContext() {
        DefaultWorkflowContext context = new DefaultWorkflowContext();

        context.setValue(PostColoringProcessParameter.IN.PERIOD.name(), new LocalDate());
        context.setValue(PostColoringProcessParameter.IN.PTU_CONTAINER_DTO_LIST.name(), new ArrayList<>());
        return context;
    }

}
