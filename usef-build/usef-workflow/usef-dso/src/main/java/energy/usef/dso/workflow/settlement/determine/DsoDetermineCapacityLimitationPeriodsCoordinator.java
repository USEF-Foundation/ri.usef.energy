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

package energy.usef.dso.workflow.settlement.determine;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.core.model.Connection;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;
import energy.usef.dso.model.ConnectionCapacityLimitationPeriod;
import energy.usef.dso.model.ConnectionMeterEvent;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.dto.ConnectionCapacityLimitationPeriodDto;
import energy.usef.dso.workflow.transformer.DsoMeterEventTransformer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This coordinator implements the following two steps: - DSO Determine Orange Regime Reduction Periods. - DSO Determine Orange
 * Regime Outage Durations.
 */
public class DsoDetermineCapacityLimitationPeriodsCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoDetermineCapacityLimitationPeriodsCoordinator.class);

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    /**
     * This method handles the {@link DetermineOrangeEvent}.
     *
     * @param event
     */
    public void handleEvent(@Observes DetermineOrangeEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        List<ConnectionMeterEvent> meterEventsForPeriod = dsoPlanboardBusinessService
                .findConnectionMeterEventsForPeriod(event.getStartDate(), event.getEndDate());
        // create connectionMap
        Map<String, Connection> mappedConnections = meterEventsForPeriod.stream().map(ConnectionMeterEvent::getConnection)
                .distinct().collect(Collectors.toMap(Connection::getEntityAddress, Function.identity()));

        // transform to DTOs
        List<ConnectionMeterEventDto> meterEventsForPeriodDtos = DsoMeterEventTransformer.transformToDto(meterEventsForPeriod);
        // should be sorted for PBCs
        Collections.sort(meterEventsForPeriodDtos, (a, b) -> a.getEventDateTime().compareTo(b.getEventDateTime()));

        // invoke Determine Orange Regime Reduction Periods
        List<ConnectionCapacityLimitationPeriodDto> meterDataReductionEventPeriods = invokeDetermineReductionPeriods(
                meterEventsForPeriodDtos.stream().collect(Collectors.toList()));
        // transform and store results
        meterDataReductionEventPeriods.stream().map(eventPeriod -> {
            ConnectionCapacityLimitationPeriod model = DsoMeterEventTransformer.transformToModel(eventPeriod);
            model.setConnection(mappedConnections.get(eventPeriod.getEntityAddress()));
            model.setTotalOutage(false);
            return model;
        }).forEach(dsoPlanboardBusinessService::storeConnectionMeterEventPeriod);

        // invoke DSO Determine Orange Regime Outage Durations.
        List<ConnectionCapacityLimitationPeriodDto> meterDataOutageEventPeriods = invokeDetermineOutagePeriods(
                meterEventsForPeriodDtos.stream().collect(Collectors.toList()));
        // transform and store results
        meterDataOutageEventPeriods.stream().map(eventPeriod -> {
            ConnectionCapacityLimitationPeriod model = DsoMeterEventTransformer.transformToModel(eventPeriod);
            model.setConnection(mappedConnections.get(eventPeriod.getEntityAddress()));
            model.setTotalOutage(true);
            return model;
        }).forEach(dsoPlanboardBusinessService::storeConnectionMeterEventPeriod);

        // invoke Calculate/Summarize/Send Orange Regime Compensations
        invokeOrangeRegimeCompensations(meterDataOutageEventPeriods);
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void invokeOrangeRegimeCompensations(List<ConnectionCapacityLimitationPeriodDto> meterDataOutageEventPeriods) {
        WorkflowContext workflowContext = new DefaultWorkflowContext();
        workflowContext
                .setValue(DetermineOrangeRegimeCompensationsParameter.IN.CONNECTION_CAPACITY_LIMITATION_PERIOD_DTO_LIST.name(),
                        meterDataOutageEventPeriods);

        workflowStepExecuter.invoke(DsoWorkflowStep.DSO_DETERMINE_ORANGE_REGIME_COMPENSATIONS.name(), workflowContext);
    }

    @SuppressWarnings("unchecked")
    private List<ConnectionCapacityLimitationPeriodDto> invokeDetermineReductionPeriods(
            List<ConnectionMeterEventDto> meterEventsForPeriodDtos) {
        WorkflowContext workflowContext = new DefaultWorkflowContext();
        workflowContext
                .setValue(DetermineReductionPeriodsStepParameter.IN.CONNECTION_METER_EVENTS.name(), meterEventsForPeriodDtos);

        WorkflowContext resultContext = workflowStepExecuter.invoke(DsoWorkflowStep.DSO_DETERMINE_ORANGE_REDUCTION_PERIODS.name(), workflowContext);
        WorkflowUtil
                .validateContext(DsoWorkflowStep.DSO_DETERMINE_ORANGE_REDUCTION_PERIODS.name(), resultContext,
                        DetermineReductionPeriodsStepParameter.OUT.values());
        return (List<ConnectionCapacityLimitationPeriodDto>) resultContext
                .getValue(DetermineReductionPeriodsStepParameter.OUT.CONNECTION_METER_EVENT_PERIODS.name());
    }

    @SuppressWarnings("unchecked")
    private List<ConnectionCapacityLimitationPeriodDto> invokeDetermineOutagePeriods(
            List<ConnectionMeterEventDto> outageMeterEvents) {
        WorkflowContext workflowContext = new DefaultWorkflowContext();
        workflowContext.setValue(DetermineOutagePeriodsStepParameter.IN.CONNECTION_METER_EVENTS.name(), outageMeterEvents);

        WorkflowContext resultContext = workflowStepExecuter.invoke(DsoWorkflowStep.DSO_DETERMINE_ORANGE_OUTAGE_PERIODS.name(), workflowContext);
        WorkflowUtil.validateContext(DsoWorkflowStep.DSO_DETERMINE_ORANGE_OUTAGE_PERIODS.name(), resultContext,
                DetermineOutagePeriodsStepParameter.OUT.values());
        return (List<ConnectionCapacityLimitationPeriodDto>) resultContext
                .getValue(DetermineOutagePeriodsStepParameter.OUT.CONNECTION_METER_EVENT_PERIODS.name());
    }
}
