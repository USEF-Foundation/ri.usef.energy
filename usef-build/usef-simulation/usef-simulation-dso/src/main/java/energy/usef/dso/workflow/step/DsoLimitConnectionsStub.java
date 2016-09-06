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
import energy.usef.dso.workflow.operate.DsoLimitConnectionsStepParameter;

import java.util.Random;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DsoLimitConnectionsStub.
 */
public class DsoLimitConnectionsStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoLimitConnectionsStub.class);

    private static final int LOWER_PERCENTAGE = 50;
    private static final int UPPER_PERCENTAGE = 75;
    private static final double HUDRED_PERCENT = 100d;
    private static final int LIMITATION_DURATION_IN_PTUS = 1;

    @Inject private PbcFeederService pbcFeederService;

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.debug("Starting workflow step 'Limit Connections'.");
        String congestionPointEntityAddress = (String) context.getValue(
                DsoLimitConnectionsStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name());
        LocalDate period = (LocalDate) context.getValue(DsoLimitConnectionsStepParameter.IN.PERIOD.name());
        Integer ptuIndex = (Integer) context.getValue(DsoLimitConnectionsStepParameter.IN.PTU_INDEX.name());
        LOGGER.debug("Parameters in the context : congestion point=[{}], period=[{}], ptu index=[{}]", congestionPointEntityAddress,
                period, ptuIndex);

        context.setValue(DsoLimitConnectionsStepParameter.OUT.POWER_DECREASE.name(),
                Math.round(determineMaximumLoad(congestionPointEntityAddress, period, ptuIndex)));
        LOGGER.debug("Ending successfully workflow step 'Limit Connections'.");

        return context;
    }

    private double determineMaximumLoad(String congestionPointEntityAddress, LocalDate period, Integer ptuIndex) {
        double uncontrolledLoad = pbcFeederService.getUncontrolledLoad(congestionPointEntityAddress, period, ptuIndex,
                LIMITATION_DURATION_IN_PTUS);
        double upperdBound = (UPPER_PERCENTAGE / HUDRED_PERCENT) * uncontrolledLoad;
        double lowerBound = (LOWER_PERCENTAGE / HUDRED_PERCENT) * uncontrolledLoad;
        return lowerBound + ((upperdBound - lowerBound) * new Random().nextDouble());
    }
}
