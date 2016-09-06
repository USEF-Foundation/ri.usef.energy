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

package energy.usef.agr.workflow.plan.updateforecast;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_CREATE_N_DAY_AHEAD_FORECAST;
import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_NON_UDI_CREATE_N_DAY_AHEAD_FORECAST;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.IN;
import energy.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.OUT;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.List;
import java.util.Optional;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This contains the business logic for updating existing Connection Portfolio.
 */
@Singleton
public class AgrUpdateConnectionForecastCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrUpdateConnectionForecastCoordinator.class);

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private Config config;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    /**
     * {@inheritDoc}
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void updateConnectionForecast(@Observes UpdateConnectionForecastEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        // 1. Collect unique gridpoints for the set of connections
        Optional<List<String>> connections = event.getConnections();

        // 2. Call step with list from context
        int interval = configAgr.getIntegerProperty(ConfigAgrParam.AGR_CONNECTION_FORECAST_DAYS_INTERVAL);

        for (int dayShift = 1; dayShift <= interval; dayShift++) {
            LocalDate forecastDay = DateTimeUtil.getCurrentDate().plusDays(dayShift);

            List<ConnectionPortfolioDto> connectionPortfolioDTOs = agrPortfolioBusinessService
                    .findConnectionPortfolioDto(forecastDay,connections);

            LOGGER.info("Updating ConnectionForecasts for date {}", forecastDay);

            List<ConnectionPortfolioDto> connectionPortfolioResults = invokeCreateNDayAheadForecastPbc(connectionPortfolioDTOs, forecastDay);

            // Saving portfolio data
            LOGGER.debug("Saving the forecast request results");
            agrPortfolioBusinessService.updateConnectionPortfolio(forecastDay, connectionPortfolioResults);
        }

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    @SuppressWarnings("unchecked") private List<ConnectionPortfolioDto> invokeCreateNDayAheadForecastPbc(List<ConnectionPortfolioDto> connectionPortfolioDTOs, LocalDate forecastDay) {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.CONNECTION_PORTFOLIO.name(), connectionPortfolioDTOs);
        context.setValue(IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        context.setValue(IN.PTU_DATE.name(), forecastDay);

        WorkflowContext outContext;
        if (configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            outContext = workflowStepExecuter.invoke(AGR_NON_UDI_CREATE_N_DAY_AHEAD_FORECAST.name(), context);
        } else {
            outContext = workflowStepExecuter.invoke(AGR_CREATE_N_DAY_AHEAD_FORECAST.name(), context);
        }
        WorkflowUtil.validateContext(AGR_CREATE_N_DAY_AHEAD_FORECAST.name(), outContext, OUT.values());

        // Executing step
        return outContext.get(OUT.CONNECTION_PORTFOLIO.name(), List.class);
    }
}
