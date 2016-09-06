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

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.dso.workflow.operate.RestoreConnectionsStepParameter;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow step implementation of the Restore Limited Connections step in the DSO operate phase. This PBC expects to find the
 * following parameters as input:
 * <ul>
 * <li>CONGESTION_POINT_ENTITY_ADDRESS: Entity address of the congestion point ({@link String})</li>
 * <li>PERIOD: PTU day ({@link LocalDate})</li>
 * <li>PTU_INDEX: PTU Index {@link Integer}</li>
 * </ul>
 * No output is expected. The PBC implementation does not implement any functionality and just logs the input parameters.
 */
public class DsoRestoreConnectionsStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoRestoreConnectionsStub.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.debug("Starting workflow step 'Restore Connections'.");
        restoreConnections(context);
        LOGGER.debug("Ending successfully workflow step 'Restore Connections'.");
        return context;
    }

    private void restoreConnections(WorkflowContext context) {
        String entityAddress = (String) context.getValue(RestoreConnectionsStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name());
        LocalDate ptuDate = (LocalDate) context.getValue(RestoreConnectionsStepParameter.IN.PERIOD.name());
        int ptuIndex = (int) context.getValue(RestoreConnectionsStepParameter.IN.PTU_INDEX.name());

        LOGGER.debug("Restoring limited connections for Entity Address: {}, PTU Date: {}, PTU Index: {}", entityAddress,
                ptuDate, ptuIndex);
    }

}
