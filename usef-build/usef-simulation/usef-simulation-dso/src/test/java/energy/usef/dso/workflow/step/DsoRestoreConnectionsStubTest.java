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
import energy.usef.dso.workflow.operate.RestoreConnectionsStepParameter;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests to test the DsoRestoreConnectionsStub.
 */
public class DsoRestoreConnectionsStubTest {
    private DsoRestoreConnectionsStub dsoRestoreConnections;
    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private static final LocalDate PTU_DATE = new LocalDate(2014, 11, 28);
    private static final int PTU_INDEX = 1;

    @Before
    public void init() throws Exception {
        dsoRestoreConnections = new DsoRestoreConnectionsStub();
    }

    @Test
    public void testInvoke() {
        WorkflowContext context = dsoRestoreConnections.invoke(buildWorkflowContext());
        Assert.assertNotNull(context);
    }

    private DefaultWorkflowContext buildWorkflowContext() {
        DefaultWorkflowContext context = new DefaultWorkflowContext();
        context.setValue(RestoreConnectionsStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), CONGESTION_POINT_ENTITY_ADDRESS);
        context.setValue(RestoreConnectionsStepParameter.IN.PERIOD.name(), PTU_DATE);
        context.setValue(RestoreConnectionsStepParameter.IN.PTU_INDEX.name(), PTU_INDEX);
        return context;
    }
}
