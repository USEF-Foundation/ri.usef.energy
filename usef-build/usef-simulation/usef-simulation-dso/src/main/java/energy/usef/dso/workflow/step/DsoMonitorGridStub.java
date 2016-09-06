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
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.operate.DsoMonitorGridStepParameter;
import energy.usef.pbcfeeder.dto.PbcPowerLimitsDto;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic step implementation for the {@link WorkflowStep} doing DSO grid monitoring.
 */
public class DsoMonitorGridStub implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoMonitorGridStub.class);

    @Inject
    private PbcFeederService pbcFeederUtil;

    /*
     * (non-Javadoc)
     * 
     * @see WorkflowStep#invoke(WorkflowContext)
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("Starting workflow step 'Monitor Grid'.");
        String congestionPoint = (String) context.getValue(DsoMonitorGridStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name());
        BigDecimal limitedPower = BigDecimal.valueOf(context.get(DsoMonitorGridStepParameter.IN.LIMITED_POWER.name(), Long.class));
        LocalDate period = context.get(DsoMonitorGridStepParameter.IN.PERIOD.name(), LocalDate.class);
        Integer ptuIndex = context.get(DsoMonitorGridStepParameter.IN.PTU_INDEX.name(), Integer.class);
        LOGGER.debug("Parameters given in the context: {}", context.toString());

        BigDecimal uncontrolledLoad = BigDecimal.valueOf(pbcFeederUtil.getUncontrolledLoad(congestionPoint, period, ptuIndex, 1));
        BigDecimal totalLoad = uncontrolledLoad.subtract(limitedPower);
        PbcPowerLimitsDto powerLimits = pbcFeederUtil.getCongestionPointPowerLimits(congestionPoint);

        context.setValue(DsoMonitorGridStepParameter.OUT.ACTUAL_LOAD.name(), totalLoad.longValue());
        // if production load is 'bigger' (smaller since negative sign) than lower limit, then congestion
        if (totalLoad.compareTo(powerLimits.getLowerLimit()) != 1 || totalLoad.compareTo(powerLimits.getUpperLimit()) != -1) {
            LOGGER.info("Total load [{}] is exceeding load limit [{} ; {}]: congestion is set to true!", totalLoad,
                    powerLimits.getLowerLimit(), powerLimits.getUpperLimit());
            context.setValue(DsoMonitorGridStepParameter.OUT.CONGESTION.name(), true);
        } else {
            // else no congestion (and max load is set to the consumption limit for information)
            context.setValue(DsoMonitorGridStepParameter.OUT.CONGESTION.name(), false);
        }
        context.setValue(DsoMonitorGridStepParameter.OUT.MIN_LOAD.name(), powerLimits.getLowerLimit().longValue());
        context.setValue(DsoMonitorGridStepParameter.OUT.MAX_LOAD.name(), powerLimits.getUpperLimit().longValue());

        LOGGER.info("Ending successfully workflow step 'Monitor Grid'.");
        return context;
    }

}
