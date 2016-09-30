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

package energy.usef.dso.workflow.altstep;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.operate.DsoMonitorGridStepParameter;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic step implementation for the {@link WorkflowStep} doing DSO grid monitoring. This stub result in no congestion.
 */
public class DsoMonitorGridStubNoCongestion implements WorkflowStep {
    // Adjusted to never have Congestion

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoMonitorGridStubNoCongestion.class);

    @Inject private PbcFeederService pbcFeederService;

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

        BigDecimal uncontrolledLoad = BigDecimal
                .valueOf(pbcFeederService.getUncontrolledLoad(congestionPoint, period, ptuIndex, 1));
        BigDecimal totalLoad = uncontrolledLoad.subtract(limitedPower);

        BigDecimal moreLoad = totalLoad.add(new BigDecimal(-1));
        BigDecimal lessLoad = totalLoad.add(new BigDecimal(1));
        List<BigDecimal> powerLimits = new ArrayList<>();
        powerLimits.add(moreLoad);
        powerLimits.add(lessLoad);
        LOGGER.debug("Total load: [{}], powerlimits: [{}],[{}]", totalLoad, powerLimits.get(0), powerLimits.get(1));
        context.setValue(DsoMonitorGridStepParameter.OUT.ACTUAL_LOAD.name(), totalLoad.longValue());
        context.setValue(DsoMonitorGridStepParameter.OUT.MIN_LOAD.name(), powerLimits.get(0).longValue());
        context.setValue(DsoMonitorGridStepParameter.OUT.MAX_LOAD.name(), powerLimits.get(1).longValue());
        context.setValue(DsoMonitorGridStepParameter.OUT.CONGESTION.name(), false);
        LOGGER.info("No Congestion enforced by alternative stub");
        LOGGER.info("Ending successfully workflow step 'Monitor Grid'.");
        return context;
    }
}
