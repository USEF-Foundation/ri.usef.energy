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
import energy.usef.dso.workflow.operate.PrepareStepwiseLimitingStepParameter;

import java.util.Date;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class DsoPrepareStepwiseLimitingStubTest {
    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private static final Date PTU_DATE = new LocalDate(2014, 11, 28).toDateMidnight().toDate();
    private DsoPrepareStepwiseLimitingStub dsoStub;

    @Before
    public void init() throws Exception {
        dsoStub = new DsoPrepareStepwiseLimitingStub();
    }

    @Test
    public void testInvoke() {
        WorkflowContext context = buildWorkflowContext();
        dsoStub.invoke(context);
    }

    private WorkflowContext buildWorkflowContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(PrepareStepwiseLimitingStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(),
                CONGESTION_POINT_ENTITY_ADDRESS);
        context.setValue(PrepareStepwiseLimitingStepParameter.IN.PERIOD.name(), PTU_DATE);
        return context;
    }
}
