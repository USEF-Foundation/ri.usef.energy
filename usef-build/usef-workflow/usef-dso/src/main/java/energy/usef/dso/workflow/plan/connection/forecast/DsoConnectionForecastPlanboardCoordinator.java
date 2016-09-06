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

package energy.usef.dso.workflow.plan.connection.forecast;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.RequestMoveToValidateEvent;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;

/**
 * DSO Non Aggreagator Connection Forecast workflow, Plan board sub-flow workflow coordinator.
 */
@Singleton
public class DsoConnectionForecastPlanboardCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoConnectionForecastPlanboardCoordinator.class);

    private static final int MINUTES_PER_DAY = 24 * 60;

    @Inject
    private Config config;

    @Inject
    private ConfigDso configDso;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private Event<RequestMoveToValidateEvent> moveToValidateEventManager;

    /**
     * {@inheritDoc}
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void handleEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) CreateConnectionForecastEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        int interval = configDso.getIntegerProperty(ConfigDsoParam.DSO_CONNECTION_FORECAST_DAYS_INTERVAL);
        Map<ConnectionGroup, List<Connection>> connectionGroups;
        LocalDate forecastDay;
        for (int dayShift = 1; dayShift <= interval; dayShift++) {
            forecastDay = DateTimeUtil.getCurrentDate().plusDays(dayShift);
            LOGGER.debug("Creating connection forecasts for period [{}]", forecastDay);
            connectionGroups = corePlanboardBusinessService.findActiveConnectionGroupsWithConnections(forecastDay);
            for (Map.Entry<ConnectionGroup, List<Connection>> entry : connectionGroups.entrySet()) {
                LOGGER.trace("Handling connection forecast for ConnectionGroup [{}].", entry.getKey().getUsefIdentifier());
                processForecastPeriod(forecastDay, entry);
            }

            // Move to validate phase
            moveToValidateEventManager.fire(new RequestMoveToValidateEvent(forecastDay));

            // Next day
            forecastDay = forecastDay.plusDays(1);
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    @SuppressWarnings("unchecked")
    private void processForecastPeriod(LocalDate forecastDay, Map.Entry<ConnectionGroup, List<Connection>> connectionGroup) {
        if (!(connectionGroup.getKey() instanceof CongestionPointConnectionGroup)) {
            return;
        }

        String entityAddress = connectionGroup.getKey().getUsefIdentifier();

        LOGGER.info("Fetched all connections (# {}) for connectionGroup {}", connectionGroup.getValue().size(),
                connectionGroup.getKey().getUsefIdentifier());

        List<AggregatorOnConnectionGroupState> aggregatorsByEntityAddress = dsoPlanboardBusinessService
                .findAggregatorOnConnectionGroupStateByCongestionPointAddress(entityAddress, forecastDay);

        long connectionCount = connectionGroup.getValue().size();

        List<Long> power;
        List<Long> maxload;

        WorkflowContext context = new DefaultWorkflowContext();
        if (checkIfNonAgrConnectionsExist(context, entityAddress, forecastDay, aggregatorsByEntityAddress,
                connectionCount)) {
            LOGGER.info("Requesting the forecast request for the day {}, congestion point: {}, connection count: {}",
                    forecastDay, entityAddress, connectionCount);

            // DsoNonAggregatorForecastStub step invocation, getting back non-aggregator connection forecast (consumed per PTU,
            // max total load)
            context = workflowStepExecuter.invoke(DsoWorkflowStep.DSO_CREATE_NON_AGGREGATOR_FORECAST.name(), context);

            // Validating out context
            WorkflowUtil.validateContext(DsoWorkflowStep.DSO_CREATE_NON_AGGREGATOR_FORECAST.name(), context,
                    DsoCreateNonAggregatorForecastParameter.OUT.values());

            // Store a congestion point (max total load + load per PTU) and aggregator (domain + connection count)
            LOGGER.info("Saving the forecast request results");

            power = (List<Long>) context.get(DsoCreateNonAggregatorForecastParameter.OUT.POWER.name(), List.class);
            maxload = (List<Long>) context.get(DsoCreateNonAggregatorForecastParameter.OUT.MAXLOAD.name(), List.class);
        } else {
            // Since all connections have an aggregator, the non-aggregator forecast must
            // be empty. There is no need to invoke the step.
            LOGGER.info("All connections on congestion point: {} have an aggregator, total connection count: {}",
                    entityAddress,
                    connectionCount);
            int nrOfPtus = MINUTES_PER_DAY / config.getIntegerProperty(ConfigParam.PTU_DURATION);
            power = new ArrayList<>(nrOfPtus);
            maxload = new ArrayList<>(nrOfPtus);
            for (int j = 0; j < nrOfPtus; ++j) {
                power.add(0L);
                maxload.add(0L);
            }
        }
        dsoPlanboardBusinessService.saveNonAggregatorConnectionForecast((CongestionPointConnectionGroup) connectionGroup.getKey(),
                forecastDay, power, maxload);
    }

    private boolean checkIfNonAgrConnectionsExist(WorkflowContext context, String entityAddress,
            LocalDate day, List<AggregatorOnConnectionGroupState> aggregatorsByEntityAddress,
            long numConnections) {

        List<String> aggregatorDomainList = new ArrayList<>();
        List<Long> aggregatorConnectionCountList = new ArrayList<>();

        long totalConnectionCount = 0;
        for (AggregatorOnConnectionGroupState aggregator : aggregatorsByEntityAddress) {
            totalConnectionCount += aggregator.getConnectionCount().longValue();

            aggregatorDomainList.add(aggregator.getAggregator().getDomain());
            aggregatorConnectionCountList.add(aggregator.getConnectionCount().longValue());
        }

        if (numConnections > totalConnectionCount) {
            // # of connections not on an aggregator
            aggregatorConnectionCountList.add(numConnections - totalConnectionCount);
        }

        LOGGER.info("There are {} total connections and {} non-aggregator connections for congestion point: {} .",
                totalConnectionCount, numConnections - totalConnectionCount,
                entityAddress);

        // set the input parameters for the PBC
        context.setValue(DsoCreateNonAggregatorForecastParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), entityAddress);
        context.setValue(DsoCreateNonAggregatorForecastParameter.IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        context.setValue(DsoCreateNonAggregatorForecastParameter.IN.PTU_DATE.name(), day);
        context.setValue(DsoCreateNonAggregatorForecastParameter.IN.AGR_DOMAIN_LIST.name(), aggregatorDomainList);
        context.setValue(DsoCreateNonAggregatorForecastParameter.IN.AGR_CONNECTION_COUNT_LIST.name(), aggregatorConnectionCountList);

        return totalConnectionCount < numConnections;
    }
}
