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

import nl.energieprojecthoogdalem.dso.limits.LimitConfiguration;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.dso.workflow.operate.DsoMonitorGridStepParameter.OUT;

/**
 * Hoogdalem implementation for the {@link DsoMonitorGrid}.
 * returns no congestion
 */
public class DsoMonitorGrid implements WorkflowStep {

    //private static final Logger LOGGER = LoggerFactory.getLogger(DsoMonitorGrid.class);

    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        //LOGGER.info("Monitor Grid returning no congestion");

        context.setValue(OUT.CONGESTION.name(), false);

        context.setValue(OUT.ACTUAL_LOAD.name(), 0L);

        context.setValue(OUT.MIN_LOAD.name(), LimitConfiguration.DEFAULT_LOWER.longValue());
        context.setValue(OUT.MAX_LOAD.name(), LimitConfiguration.DEFAULT_UPPER.longValue() );

        return context;
    }

}
