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

package nl.energieprojecthoogdalem.dso.pbc;

import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.dso.workflow.operate.DsoMonitorGridStepParameter.IN;
import info.usef.dso.workflow.operate.DsoMonitorGridStepParameter.OUT;
import nl.energieprojecthoogdalem.dso.limits.LimitConfiguration;
import org.joda.time.LocalDate;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link DsoMonitorGrid}.
 */
public class DsoMonitorGridTest
{
    private DsoMonitorGrid dsoMonitorGrid;

    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private static final LocalDate PTU_DATE = new LocalDate(2014, 11, 28);

    @Test
    public void testInvokeOutputsCongestion()
    {
        dsoMonitorGrid = new DsoMonitorGrid();

        // invoke the PBC
        WorkflowContext result = dsoMonitorGrid.invoke(buildWorkflowContext());

        assertEquals(0L, (long) result.get(OUT.ACTUAL_LOAD.name(), long.class) );
        assertEquals(LimitConfiguration.DEFAULT_UPPER.longValue(), (long) result.get(OUT.MAX_LOAD.name(), long.class));
        assertEquals(LimitConfiguration.DEFAULT_LOWER.longValue(), (long) result.get(OUT.MIN_LOAD.name(), long.class));
        assertFalse(result.get(OUT.CONGESTION.name(), Boolean.class));
    }

    private WorkflowContext buildWorkflowContext()
    {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), CONGESTION_POINT_ENTITY_ADDRESS);
        context.setValue(IN.NUM_CONNECTIONS.name(), 2L);
        context.setValue(IN.PERIOD.name(), PTU_DATE);
        context.setValue(IN.LIMITED_POWER.name(), 0L);
        context.setValue(IN.PTU_INDEX.name(), 46);
        return context;
    }
}
