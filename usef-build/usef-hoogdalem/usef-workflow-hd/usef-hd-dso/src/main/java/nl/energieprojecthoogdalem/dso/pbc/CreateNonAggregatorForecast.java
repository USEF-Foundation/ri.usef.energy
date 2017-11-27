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

import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.dso.workflow.plan.connection.forecast.DsoCreateNonAggregatorForecastParameter.OUT;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem version of create non-Aggregator Forecast, no forecast required
 * */
public class CreateNonAggregatorForecast implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateNonAggregatorForecast.class);

    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.info("create non-Aggregator Forecast executed, no forecast required, returning empty lists");

        context.setValue(OUT.POWER.name(), new ArrayList<>());
        context.setValue(OUT.MAXLOAD.name(), new ArrayList<>());

        return context;
    }
}
