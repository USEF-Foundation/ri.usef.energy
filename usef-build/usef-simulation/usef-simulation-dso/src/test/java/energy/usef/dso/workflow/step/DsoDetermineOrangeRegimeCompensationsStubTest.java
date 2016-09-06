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
import energy.usef.dso.workflow.dto.ConnectionCapacityLimitationPeriodDto;
import energy.usef.dso.workflow.settlement.determine.DetermineOrangeRegimeCompensationsParameter.IN;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for the {@link DsoDetermineOrangeRegimeCompensationsStub} class.
 */
@RunWith(PowerMockRunner.class)
public class DsoDetermineOrangeRegimeCompensationsStubTest {

    private DsoDetermineOrangeRegimeCompensationsStub stub;

    @Before
    public void init() {
        stub = new DsoDetermineOrangeRegimeCompensationsStub();
    }

    @Test
    public void testInvoke() throws Exception {
        WorkflowContext context = stub.invoke(buildContext());

        Assert.assertNotNull(context);
    }

    private WorkflowContext buildContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.CONNECTION_CAPACITY_LIMITATION_PERIOD_DTO_LIST.name(),
                new ArrayList<ConnectionCapacityLimitationPeriodDto>());

        return context;
    }
}
