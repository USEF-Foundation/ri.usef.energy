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

package nl.energieprojecthoogdalem.agr.pbc;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastStepParameter.OUT;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem implementation returning no changes in forecast
 * <p>
 * The PBC receives the following parameters as input to take the decision:
 * <ul>
 * <li>CONNECTION_PORTFOLIO : a Map of List {@link ConnectionPortfolioDto} per period.</li>
 * <li>PERIOD: the period for which changes in forecast are being identified.</li>
 * <li>PTU_DURATION : the size of a PTU in minutes.</li>
 * </ul>
 * <p>
 * The PBC must return one flag indicating whether Re-optimize portfolio workflow should be triggered. The output parameter:
 * <ul>
 * <li>FORECAST_CHANGED : boolean value indicating that the Re-optimize portfolio workflow should be triggered</li>
 * </ul>
 *
 */
public class IdentifyChangeInForecast implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifyChangeInForecast.class);

    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.info("IdentifyChangeInForecast returning no changes");

        context.setValue(OUT.FORECAST_CHANGED.name(), false);
        context.setValue(OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name(), new ArrayList<>());

        return context;
    }

}
