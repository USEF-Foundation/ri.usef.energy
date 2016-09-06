/*
 * This software source code is provided by the USEF Foundation. The copyright
 * and all other intellectual property rights relating to all software source
 * code provided by the USEF Foundation (and changes and modifications as well
 * as on new versions of this software source code) belong exclusively to the
 * USEF Foundation and/or its suppliers or licensors. Total or partial
 * transfer of such a right is not allowed. The user of the software source
 * code made available by USEF Foundation acknowledges these rights and will
 * refrain from any form of infringement of these rights.
 *
 * The USEF Foundation provides this software source code "as is". In no event
 * shall the USEF Foundation and/or its suppliers or licensors have any
 * liability for any incidental, special, indirect or consequential damages;
 * loss of profits, revenue or data; business interruption or cost of cover or
 * damages arising out of or in connection with the software source code or
 * accompanying documentation.
 *
 * For the full license agreement see http://www.usef.info/license.
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
