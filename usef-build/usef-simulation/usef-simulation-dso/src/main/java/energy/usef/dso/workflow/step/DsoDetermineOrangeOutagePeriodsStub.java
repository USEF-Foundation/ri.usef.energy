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
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.dto.MeterEventTypeDto;
import energy.usef.dso.workflow.dto.ConnectionCapacityLimitationPeriodDto;
import energy.usef.dso.workflow.settlement.determine.DetermineOutagePeriodsStepParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DsoDetermineOrangeReductionPeriodsStub.
 */
public class DsoDetermineOrangeOutagePeriodsStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoDetermineOrangeOutagePeriodsStub.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        @SuppressWarnings("unchecked")
        List<ConnectionMeterEventDto> meterEventsForPeriodDtos = (List<ConnectionMeterEventDto>) context.getValue(
                DetermineOutagePeriodsStepParameter.IN.CONNECTION_METER_EVENTS.name());

        LOGGER.debug("Starting workflow step 'Determine Orange Outage Periods', events: {}.", meterEventsForPeriodDtos.size());
        // should be sorted already, just making sure
        Collections.sort(meterEventsForPeriodDtos, (a, b) -> a.getEventDateTime().compareTo(b.getEventDateTime()));

        List<ConnectionCapacityLimitationPeriodDto> results = meterEventsForPeriodDtos.stream()
                .collect(Collectors.groupingBy(ConnectionMeterEventDto::getEntityAddress))
                .values().stream()
                .map(this::buildPeriods)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        context.setValue(DetermineOutagePeriodsStepParameter.OUT.CONNECTION_METER_EVENT_PERIODS.name(), results);
        LOGGER.debug("Ending successfully workflow step 'Determine Orange Outage Periods, periods: {}.'.", results.size());
        return context;
    }

    private List<ConnectionCapacityLimitationPeriodDto> buildPeriods(List<ConnectionMeterEventDto> connectionMeterEventDtos) {
        List<ConnectionCapacityLimitationPeriodDto> results = new ArrayList<>();
        ConnectionCapacityLimitationPeriodDto connectionMeterEventPeriodDto = null;
        for (ConnectionMeterEventDto connectionMeterEventDto : connectionMeterEventDtos) {
            if (MeterEventTypeDto.CAPACITY_MANAGEMENT.equals(connectionMeterEventDto.getEventType())) {
                continue;
            }
            // if no period active, create
            if (connectionMeterEventPeriodDto == null) {
                connectionMeterEventPeriodDto = new ConnectionCapacityLimitationPeriodDto();
                connectionMeterEventPeriodDto.setEntityAddress(connectionMeterEventDto.getEntityAddress());
            }
            // if starting event, than set start time and set max end time
            if (MeterEventTypeDto.CONNECTION_INTERRUPTION.equals(connectionMeterEventDto.getEventType())) {
                connectionMeterEventPeriodDto.setStartDateTime(connectionMeterEventDto.getEventDateTime());
                LocalDateTime endOfDay = connectionMeterEventDto.getEventDateTime()
                        .withMillisOfDay(connectionMeterEventDto.getEventDateTime().millisOfDay().getMaximumValue());
                connectionMeterEventPeriodDto.setEndDateTime(endOfDay);
                results.add(connectionMeterEventPeriodDto);
            }
            // if ending event check if startTime is set, otherwise set beginning of day. And set endDateTime.
            if (MeterEventTypeDto.CONNECTION_RESUMPTION.equals(connectionMeterEventDto.getEventType())) {
                if (connectionMeterEventPeriodDto.getStartDateTime() == null) {
                    LocalDateTime startOfDay = connectionMeterEventDto.getEventDateTime()
                            .withMillisOfDay(connectionMeterEventDto.getEventDateTime().millisOfDay().getMinimumValue());
                    connectionMeterEventPeriodDto.setStartDateTime(startOfDay);
                }
                connectionMeterEventPeriodDto.setEndDateTime(connectionMeterEventDto.getEventDateTime());
                if (!results.contains(connectionMeterEventPeriodDto)) {
                    results.add(connectionMeterEventPeriodDto);
                }
                connectionMeterEventPeriodDto = null;
            }
        }
        return results;
    }
}
