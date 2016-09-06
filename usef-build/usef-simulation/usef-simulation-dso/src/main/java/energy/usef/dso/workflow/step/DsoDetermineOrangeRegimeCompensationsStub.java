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
import energy.usef.dso.workflow.dto.ConnectionCapacityLimitationPeriodDto;
import energy.usef.dso.workflow.settlement.determine.DetermineOrangeRegimeCompensationsParameter.IN;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This workflow step will handle the DSO Calculate/Summarize/Send Orange Regime Compensations.
 */
public class DsoDetermineOrangeRegimeCompensationsStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoDetermineOrangeRegimeCompensationsStub.class);

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("Starting workflow step 'DSO Calculate/Summarize/Send Orange Regime Compensations'.");

        List<ConnectionCapacityLimitationPeriodDto> connectionCapacityLimitationPeriodDto =
                (List<ConnectionCapacityLimitationPeriodDto>) context.getValue(IN.CONNECTION_CAPACITY_LIMITATION_PERIOD_DTO_LIST
                        .name());
        LOGGER.debug("Using {} ConnectionCapacityLimitationPeriodDto's.", connectionCapacityLimitationPeriodDto.size());

        // For each connection involved
        //
        // - For each day in the Next Interval since Last Interval Settled
        // - - For each capacity limitation applied
        // - - - Calculate capacity limitation compensation (price per kW reduction per hour [€]) * (capacity reduction [kW]) *
        // (duration [hour])
        //
        // - - - Calculate outage compensation (price per hour [€]) * (duration [hour])
        // - - End
        // - End
        //
        // - Summarize compensations
        //
        // - Send specification of compensation to Prosumer
        //
        // - Schedule for payment
        // End

        return context;
    }
}
