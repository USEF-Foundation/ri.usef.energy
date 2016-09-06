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

package energy.usef.core.workflow.transformer;

import energy.usef.core.data.xml.bean.message.ConnectionMeterEvent;
import energy.usef.core.data.xml.bean.message.MeterEventType;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.dto.MeterEventTypeDto;

/**
 * Transformer class for entities related to the MeterEvent xml entity.
 */
public class MeterEventTransformer {

    private MeterEventTransformer() {
        //private constructor
    }

    /**
     * Transforms a ConnectionMeterEventDto to its correspondent XML object.
     *
     * @param connectionMeterEventDto {@link ConnectionMeterEventDto} dto.
     * @return a {@link ConnectionMeterEvent} xml object as specified by the xsd specification.
     */
    public static ConnectionMeterEvent transformToXml(ConnectionMeterEventDto connectionMeterEventDto) {
        if (connectionMeterEventDto == null) {
            return null;
        }
        ConnectionMeterEvent connectionMeterEvent = new ConnectionMeterEvent();
        connectionMeterEvent.setEntityAddress(connectionMeterEventDto.getEntityAddress());
        connectionMeterEvent.setEventData(connectionMeterEventDto.getEventData());
        connectionMeterEvent.setEventDateTime(connectionMeterEventDto.getEventDateTime());
        connectionMeterEvent.setEventType(trasformToXml(connectionMeterEventDto.getEventType()));
        return connectionMeterEvent;
    }

    /**
     * Transforms a MeterEventTypeDto to its correspondent XML object.
     *
     * @param meterEventTypeDto {@link MeterEventTypeDto} dto.
     * @return a {@link MeterEventType} xml object as specified by the xsd specification.
     */
    public static MeterEventType trasformToXml(MeterEventTypeDto meterEventTypeDto) {
        if (meterEventTypeDto == null) {
            return null;
        }
        switch (meterEventTypeDto) {
        case CAPACITY_MANAGEMENT:
            return MeterEventType.CAPACITY_MANAGEMENT;
        case CONNECTION_INTERRUPTION:
            return MeterEventType.CONNECTION_INTERRUPTION;
        case CONNECTION_RESUMPTION:
            return MeterEventType.CONNECTION_RESUMPTION;
        default:
            return null;
        }
    }

}
