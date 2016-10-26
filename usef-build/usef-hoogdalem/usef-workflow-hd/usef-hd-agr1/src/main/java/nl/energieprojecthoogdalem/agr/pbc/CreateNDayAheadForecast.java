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
import info.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter;
import info.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.IN;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import nl.energieprojecthoogdalem.forecastservice.forecast.ForecastFactory;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem Implementation of workflow step of the AGR Create N-Day-Ahead Forecasts.
 * <p>
 * The PBC receives the following parameters as input:
 * <ul>
 * <li>PTU_DATE : PTU day {@link org.joda.time.LocalDate}.</li>
 * <li>PTU_DURATION : PTU duration as int.</li>
 * <li>CONNECTIONS : the list of connections {@link info.usef.agr.dto.ConnectionPortfolioDto}.</li>
 * <li>CONNECTION_TO_CONGESTION_POINT_MAP : connection entity address to corresponding congestion point entity address map
 * {@link Map<String, String>}.</li>
 * <li>CONNECTION_TO_BRP_MAP : connection entity address to corresponding BRP domain map {@link Map<String, String>}.</li>
 * </ul>
 * <p>
 * The PBC returns the following parameters as output:
 * <ul>
 * <li>CONNECTION_PORTFOLIO : connection portfolio - a list of connections {@link info.usef.agr.dto.ConnectionPortfolioDto}
 * containing the connection portfolio data.</li>
 * </ul>
 */
public class CreateNDayAheadForecast implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateNDayAheadForecast.class);

    @Inject
    private ForecastFactory forecastFactory;

    /**
     * creates forecasts through the {@link ForecastFactory} createNdayAheadForecast()
     */
    @SuppressWarnings("unchecked") @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.info("AGRCreateNDayAheadForecast step is invoked");

        LocalDate forecastDay = (LocalDate) context.getValue(IN.PTU_DATE.name());
        int ptuDuration = (int) context.getValue(IN.PTU_DURATION.name());

        List<ConnectionPortfolioDto> connections = (List<ConnectionPortfolioDto>) context.getValue(IN.CONNECTION_PORTFOLIO.name());

        connections = forecastFactory.createNdayAheadForecast(forecastDay, ptuDuration, connections);

        return buildResultContext(connections);
    }

    private WorkflowContext buildResultContext(List<ConnectionPortfolioDto> connections) {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(ConnectionForecastStepParameter.OUT.CONNECTION_PORTFOLIO.name(), connections);
        return context;
    }
}
