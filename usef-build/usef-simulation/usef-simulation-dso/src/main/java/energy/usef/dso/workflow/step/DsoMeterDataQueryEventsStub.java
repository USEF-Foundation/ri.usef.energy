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
import energy.usef.dso.workflow.settlement.collect.MeterDataQueryEventsParameter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow step implementation for the Workflow 'DSO collect orange regime data'. This step retrieves connection meter event data
 * if it was not provided by MDC. This implementation expects to find the following parameters as input:
 * <ul>
 * <li>CONNECTION_LIST: the connection entity address list ({@link List<String>})</li>
 * <li>PERIOD: day for which the calculation is invoked ({@link LocalDate})</li>
 * </ul>
 * This implementation must return the following parameters as input:
 * <ul>
 * <li>CONNECTION_METER_EVENT_DTO_LIST: List of ConnectionMeterEventDto items({@link ConnectionMeterEventDto})</li>
 * </ul>
 * This stub randomly generates the required data.
 */
public class DsoMeterDataQueryEventsStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoMeterDataQueryEventsStub.class);
    private static final Random RANDOM = new Random();
    private static final int MINUTES_PER_HOUR = 60;
    private static final int POWER_LIMIT = 500;

    private static final int EVENT_TYPE_CASES_AMOUNT = 5;
    private static final int CAPACITY_MANAGEMENT_ONLY = 0;
    private static final int CONNECTION_INTERRUPTION_AND_RESUMPTION = 1;
    private static final int CAPACITY_MANAGEMENT_WITH_RANDOM = 2;
    private static final int CONNECTION_INTERRUPTION_ONLY = 3;
    private static final int CONNECTION_RESUMPTION_ONLY = 4;

    /**
     * {@inheritDoc}
     */
    public WorkflowContext invoke(WorkflowContext context) {
        @SuppressWarnings("unchecked")
        List<String> connectionList = (List<String>) context.getValue(MeterDataQueryEventsParameter.IN.CONNECTION_LIST.name());
        LocalDate period = (LocalDate) context.getValue(MeterDataQueryEventsParameter.IN.PERIOD.name());
        LOGGER.info("PBC invoked with the connection list size {}, for the day {}", connectionList.size(), period);

        List<ConnectionMeterEventDto> connectionMeterEventDtoList = new ArrayList<>();
        for (String entityAddress : connectionList) {
            connectionMeterEventDtoList.addAll(generateConnectionMeterEventDto(entityAddress, period));
        }

        context.setValue(MeterDataQueryEventsParameter.OUT.CONNECTION_METER_EVENT_DTO_LIST.name(),
                connectionMeterEventDtoList);
        return context;
    }

    private List<ConnectionMeterEventDto> generateConnectionMeterEventDto(String entityAddress, LocalDate day) {
        List<ConnectionMeterEventDto> result = new ArrayList<>();

        ConnectionMeterEventDto connectionMeterEventDto1 = new ConnectionMeterEventDto();
        connectionMeterEventDto1.setEntityAddress(entityAddress);
        LocalDateTime eventDateTime1 = new LocalDateTime(day.toDateMidnight().toDate())
                .plusMinutes(RANDOM.nextInt(MINUTES_PER_HOUR));
        connectionMeterEventDto1.setEventDateTime(eventDateTime1);
        result.add(connectionMeterEventDto1);

        ConnectionMeterEventDto connectionMeterEventDto2 = new ConnectionMeterEventDto();
        connectionMeterEventDto2.setEntityAddress(entityAddress);
        LocalDateTime eventDateTime2 = new LocalDateTime(day.toDateMidnight().toDate())
                .plusMinutes(MINUTES_PER_HOUR + 1 + RANDOM.nextInt(MINUTES_PER_HOUR));
        connectionMeterEventDto2.setEventDateTime(eventDateTime2);

        int randomInt = RANDOM.nextInt(EVENT_TYPE_CASES_AMOUNT);
        if (randomInt == CAPACITY_MANAGEMENT_ONLY) {
            connectionMeterEventDto1.setEventType(MeterEventTypeDto.CAPACITY_MANAGEMENT);
            BigInteger eventData = computeRandomPower();
            connectionMeterEventDto1.setEventData(eventData);

            connectionMeterEventDto2.setEventType(MeterEventTypeDto.CAPACITY_MANAGEMENT);
            result.add(connectionMeterEventDto2);
        } else if (randomInt == CONNECTION_INTERRUPTION_AND_RESUMPTION) {
            connectionMeterEventDto1.setEventType(MeterEventTypeDto.CONNECTION_INTERRUPTION);

            connectionMeterEventDto2.setEventType(MeterEventTypeDto.CONNECTION_RESUMPTION);
            result.add(connectionMeterEventDto2);
        } else if (randomInt == CAPACITY_MANAGEMENT_WITH_RANDOM) {
            connectionMeterEventDto1.setEventType(MeterEventTypeDto.CAPACITY_MANAGEMENT);
            if (RANDOM.nextBoolean()) {
                BigInteger eventData = computeRandomPower();
                connectionMeterEventDto1.setEventData(eventData);
            }
        } else if (randomInt == CONNECTION_INTERRUPTION_ONLY) {
            connectionMeterEventDto1.setEventType(MeterEventTypeDto.CONNECTION_INTERRUPTION);
        } else if (randomInt == CONNECTION_RESUMPTION_ONLY) {
            connectionMeterEventDto1.setEventType(MeterEventTypeDto.CONNECTION_RESUMPTION);
        }

        return result;
    }

    private BigInteger computeRandomPower() {
        return BigInteger.valueOf(RANDOM.nextInt(POWER_LIMIT + 1) * randomSignum());
    }

    private int randomSignum() {
        return RANDOM.nextBoolean() ? -1 : 1;
    }

}
