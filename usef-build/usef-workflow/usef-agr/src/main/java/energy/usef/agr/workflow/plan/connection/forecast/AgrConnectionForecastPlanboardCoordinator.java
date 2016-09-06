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

package energy.usef.agr.workflow.plan.connection.forecast;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_CREATE_N_DAY_AHEAD_FORECAST;
import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_NON_UDI_CREATE_N_DAY_AHEAD_FORECAST;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioEvent;
import energy.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.IN;
import energy.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.OUT;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AGR Create N-Day-Ahead Forecasts workflow, Plan board sub-flow workflow coordinator.
 */
@Singleton
@Transactional(Transactional.TxType.REQUIRES_NEW)
public class AgrConnectionForecastPlanboardCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrConnectionForecastPlanboardCoordinator.class);

    @Inject
    private Config config;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private Event<ReOptimizePortfolioEvent> reOptimizePortfolioEventManager;

    /**
     * {@inheritDoc}
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void handleEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) CreateConnectionForecastEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        int interval = configAgr.getIntegerProperty(ConfigAgrParam.AGR_CONNECTION_FORECAST_DAYS_INTERVAL);

        // For each day in the N-day period
        for (int dayShift = 1; dayShift <= interval; dayShift++) {
            LocalDate forecastDay = DateTimeUtil.getCurrentDate().plusDays(dayShift);

            List<ConnectionPortfolioDto> connectionPortfolioDTOs = agrPortfolioBusinessService
                    .findConnectionPortfolioDto(forecastDay);
            List<ConnectionPortfolioDto> resultConnectionPortfolioDTOs = invokeCreateNDayAheadForecastPbc(forecastDay,
                    connectionPortfolioDTOs);
            LOGGER.debug("Saving the forecast request results");
            agrPortfolioBusinessService.createConnectionForecasts(forecastDay, resultConnectionPortfolioDTOs);
            // Re-optimize Connection Portfolio
            reOptimizePortfolioEventManager.fire(new ReOptimizePortfolioEvent(forecastDay));
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }


    @SuppressWarnings("unchecked") private List<ConnectionPortfolioDto> invokeCreateNDayAheadForecastPbc(LocalDate forecastDay,
            List<ConnectionPortfolioDto> resultConnectionPortfolioDTOs) {
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(IN.CONNECTION_PORTFOLIO.name(), resultConnectionPortfolioDTOs);
        inContext.setValue(IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        inContext.setValue(IN.PTU_DATE.name(), forecastDay);

        // AGRCreateNDayAheadForecast step invocation, getting back a corresponding ConnectionForecastDto list
        LOGGER.debug("Requesting the N-Day Ahead Forecast for the day {}", forecastDay);

        WorkflowContext outContext;
        if (configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            outContext = workflowStepExecuter.invoke(AGR_NON_UDI_CREATE_N_DAY_AHEAD_FORECAST.name(), inContext);
        } else {
            outContext = workflowStepExecuter.invoke(AGR_CREATE_N_DAY_AHEAD_FORECAST.name(), inContext);
        }
        // Validating out context
        WorkflowUtil.validateContext(AGR_CREATE_N_DAY_AHEAD_FORECAST.name(), outContext, OUT.values());

        return outContext.get(OUT.CONNECTION_PORTFOLIO.name(), List.class);
    }

}
