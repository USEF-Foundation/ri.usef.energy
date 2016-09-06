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

package energy.usef.dso.workflow.transformer;

import java.util.List;
import java.util.stream.Collectors;

import energy.usef.core.data.xml.bean.message.MeterEventType;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.dto.MeterEventTypeDto;
import energy.usef.dso.model.ConnectionCapacityLimitationPeriod;
import energy.usef.dso.model.ConnectionMeterEvent;
import energy.usef.dso.model.EventType;
import energy.usef.dso.workflow.dto.ConnectionCapacityLimitationPeriodDto;

/**
 * Transformer class for transforming between different MeterEvent types.
 */
public class DsoMeterEventTransformer {

    /*
     * Hide implicit public constructor.
     */
    private  DsoMeterEventTransformer () {
    }

    /**
     * Transforms List of DB model's to list of ConnectionMeterEventDto's.
     *
     * @param meterEventsForPeriod A {@link List} of {@link ConnectionMeterEvent} objects.
     * @return A {@link List} of {@link ConnectionMeterEventDto} objects.
     */
    public static List<ConnectionMeterEventDto> transformToDto(List<ConnectionMeterEvent> meterEventsForPeriod) {
        return meterEventsForPeriod.stream().map(DsoMeterEventTransformer::transformToDto).collect(Collectors.toList());

    }

    /**
     * Transforms DB model to ConnectionMeterEventDto.
     *
     * @param connectionMeterEvent the {@link ConnectionMeterEvent}.
     * @return the connectionMeterEvent transformed to a {@link ConnectionMeterEventDto}.
     */
    public static ConnectionMeterEventDto transformToDto(ConnectionMeterEvent connectionMeterEvent) {
        if (connectionMeterEvent == null) {
            return null;
        }
        ConnectionMeterEventDto result = new ConnectionMeterEventDto();
        result.setEntityAddress(connectionMeterEvent.getConnection().getEntityAddress());
        result.setEventData(connectionMeterEvent.getCapacity());
        result.setEventType(transformToDto(connectionMeterEvent.getEventType()));
        result.setEventDateTime(connectionMeterEvent.getDateTime());
        return result;
    }

    /**
     * Transforms EventType to MeterEventTypeDto.
     *
     * @param eventType {@link EventType}
     * @return the eventType transformed to a {@link MeterEventTypeDto}.
     */
    public static MeterEventTypeDto transformToDto(EventType eventType) {
        if (eventType == null) {
            return null;
        }
        switch (eventType) {
        case CapacityManagement:
            return MeterEventTypeDto.CAPACITY_MANAGEMENT;
        case ConnectionInterruption:
            return MeterEventTypeDto.CONNECTION_INTERRUPTION;
        case ConnectionResumption:
            return MeterEventTypeDto.CONNECTION_RESUMPTION;
        default:
            return null;
        }
    }

    /**
     * Transforms ConnectionCapacityLimitationPeriodDto to DB model ConnectionCapacityLimitationPeriod.
     *
     * @param connectionMeterEventPeriodDto {@link ConnectionCapacityLimitationPeriodDto}.
     * @return the connectionMeterEventPeriodDto transformed to a {@link ConnectionCapacityLimitationPeriod}.
     */
    public static ConnectionCapacityLimitationPeriod transformToModel(
            ConnectionCapacityLimitationPeriodDto connectionMeterEventPeriodDto) {
        if (connectionMeterEventPeriodDto == null) {
            return null;
        }
        ConnectionCapacityLimitationPeriod result = new ConnectionCapacityLimitationPeriod();
        result.setCapacityReduction(connectionMeterEventPeriodDto.getCapacityReduction());
        result.setStartDateTime(connectionMeterEventPeriodDto.getStartDateTime());
        result.setEndDateTime(connectionMeterEventPeriodDto.getEndDateTime());
        return result;
    }

    /**
     * Transforms MeterEventTypeDto to DB model EventType.
     *
     * @param eventType {@link MeterEventTypeDto}
     * @return the DB model of the given eventType
     */
    public static EventType transformToModel(MeterEventTypeDto eventType) {
        if (eventType == null) {
            return null;
        }
        switch (eventType) {
        case CAPACITY_MANAGEMENT:
            return EventType.CapacityManagement;
        case CONNECTION_INTERRUPTION:
            return EventType.ConnectionInterruption;
        case CONNECTION_RESUMPTION:
            return EventType.ConnectionResumption;
        default:
            return null;
        }
    }

    /**
     * Transforms XML MeterEventType to DB model EventType.
     *
     * @param meterEventType {@link MeterEventType}
     * @return the model {@link EventType}
     */
    public static EventType transformToModel(MeterEventType meterEventType) {
        if (meterEventType == MeterEventType.CAPACITY_MANAGEMENT) {
            return EventType.CapacityManagement;
        } else if (meterEventType == MeterEventType.CONNECTION_INTERRUPTION) {
            return EventType.ConnectionInterruption;
        } else if (meterEventType == MeterEventType.CONNECTION_RESUMPTION) {
            return EventType.ConnectionResumption;
        }
        return null;
    }
}
