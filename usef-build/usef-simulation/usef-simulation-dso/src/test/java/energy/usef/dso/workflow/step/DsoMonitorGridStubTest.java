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
import energy.usef.dso.workflow.operate.DsoMonitorGridStepParameter;
import energy.usef.pbcfeeder.dto.PbcPowerLimitsDto;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.math.BigDecimal;

/**
 * Test class in charge of the unit tests related to the {@link DsoMonitorGridStub}.
 */
@RunWith(PowerMockRunner.class)
public class DsoMonitorGridStubTest {

    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private static final LocalDate PTU_DATE = new LocalDate(2014, 11, 28);
    private DsoMonitorGridStub dsoMonitorGrid;
    @Mock private PbcFeederService pbcFeederUtil;

    @Before
    public void setUp() throws Exception {
        dsoMonitorGrid = new DsoMonitorGridStub();
        Whitebox.setInternalState(dsoMonitorGrid, pbcFeederUtil);
    }

    @Test
    public void testInvokeOutputsCongestion() {
        // stubbing
        PowerMockito.when(pbcFeederUtil.getUncontrolledLoad(Matchers.any(String.class),
                Matchers.any(LocalDate.class), Matchers.any(Integer.class), Matchers.any(Integer.class))).thenReturn(500);
        PowerMockito.when(pbcFeederUtil.getCongestionPointPowerLimits(Matchers.any(String.class))).thenReturn(new PbcPowerLimitsDto(
                BigDecimal.valueOf(-400L), BigDecimal.valueOf(400L)));
        WorkflowContext context = buildWorkflowContext();
        // invoke the PBC
        WorkflowContext outputContext = dsoMonitorGrid.invoke(context);
        BigDecimal actualLoad = BigDecimal.valueOf(outputContext.get(DsoMonitorGridStepParameter.OUT.ACTUAL_LOAD.name(), Long.class));
        BigDecimal maxLoad = BigDecimal.valueOf(outputContext.get(DsoMonitorGridStepParameter.OUT.MAX_LOAD.name(), Long.class));
        boolean congestion = outputContext.get(DsoMonitorGridStepParameter.OUT.CONGESTION.name(), Boolean.class);

        // assertions
        Assert.assertTrue(congestion);
        Assert.assertEquals(BigDecimal.valueOf(400L), maxLoad);
        Assert.assertEquals(BigDecimal.valueOf(500L), actualLoad);
    }

    @Test
    public void testInvokeOutputsNoCongestion() {
        // stubbing
        PowerMockito.when(pbcFeederUtil.getUncontrolledLoad(Matchers.any(String.class),
                Matchers.any(LocalDate.class), Matchers.any(Integer.class), Matchers.any(Integer.class))).thenReturn(500);
        PowerMockito.when(pbcFeederUtil.getCongestionPointPowerLimits(Matchers.any(String.class))).thenReturn(new PbcPowerLimitsDto(
                BigDecimal.valueOf(-400L), BigDecimal.valueOf(400L)));
        WorkflowContext context = buildWorkflowContext();
        context.setValue(DsoMonitorGridStepParameter.IN.LIMITED_POWER.name(), 200L);
        // invoke the PBC
        WorkflowContext outputContext = dsoMonitorGrid.invoke(context);
        BigDecimal actualLoad = BigDecimal.valueOf(outputContext.get(DsoMonitorGridStepParameter.OUT.ACTUAL_LOAD.name(), Long.class));
        boolean congestion = outputContext.get(DsoMonitorGridStepParameter.OUT.CONGESTION.name(), Boolean.class);

        // assertions
        Assert.assertFalse(congestion);
        Assert.assertEquals(400L, context.getValue(DsoMonitorGridStepParameter.OUT.MAX_LOAD.name()));
        Assert.assertEquals(BigDecimal.valueOf(300L), actualLoad);
    }

    private WorkflowContext buildWorkflowContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(DsoMonitorGridStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), CONGESTION_POINT_ENTITY_ADDRESS);
        context.setValue(DsoMonitorGridStepParameter.IN.NUM_CONNECTIONS.name(), 2L);
        context.setValue(DsoMonitorGridStepParameter.IN.PERIOD.name(), PTU_DATE);
        context.setValue(DsoMonitorGridStepParameter.IN.LIMITED_POWER.name(), 0L);
        context.setValue(DsoMonitorGridStepParameter.IN.PTU_INDEX.name(), 46);
        return context;
    }
}
