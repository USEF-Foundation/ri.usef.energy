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
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.operate.DsoLimitConnectionsStepParameter;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertTrue;

/**
 * Test class in charge of the unit tests related to the {@link DsoLimitConnectionsStub} class.
 */
@RunWith(PowerMockRunner.class)
public class DsoLimitConnectionsStubTest {

    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private static final LocalDate PTU_DATE = new LocalDate(2014, 11, 28);
    private DsoLimitConnectionsStub dsoLimitConnections;

    @Mock private PbcFeederService pbcFeederUtil;

    @Before
    public void init() throws Exception {
        dsoLimitConnections = new DsoLimitConnectionsStub();
        Whitebox.setInternalState(dsoLimitConnections, pbcFeederUtil);
        // stubbing of the pbc feeder util
        Mockito.when(pbcFeederUtil.getUncontrolledLoad(Matchers.any(String.class), Matchers.any(LocalDate.class),
                Matchers.any(Integer.class), Matchers.any(Integer.class))).thenReturn(5000);
    }

    @Test
    public void testInvoke() {
        WorkflowContext context = buildWorkflowContext();
        dsoLimitConnections.invoke(context);
        Long powerDeficiency = (long) context.getValue(DsoLimitConnectionsStepParameter.OUT.POWER_DECREASE.name());
        assertTrue(powerDeficiency >= 2500); // at least 50% of uncontrolled load
        assertTrue(powerDeficiency <= 3750); // no more than 75% of uncontrolled load
    }

    private WorkflowContext buildWorkflowContext() {
        DefaultWorkflowContext context = new DefaultWorkflowContext();
        context.setValue(DsoLimitConnectionsStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(),
                CONGESTION_POINT_ENTITY_ADDRESS);
        context.setValue(DsoLimitConnectionsStepParameter.IN.PERIOD.name(), PTU_DATE);
        context.setValue(DsoLimitConnectionsStepParameter.IN.PTU_INDEX.name(), 1);
        return context;
    }
}
