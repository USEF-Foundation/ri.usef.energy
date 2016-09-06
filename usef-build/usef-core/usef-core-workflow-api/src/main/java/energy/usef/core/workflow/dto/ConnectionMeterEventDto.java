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

package energy.usef.core.workflow.dto;

import java.math.BigInteger;

import org.joda.time.LocalDateTime;

/**
 * DTO object for the ConnectionMeterEvent xml entity.
 */
public class ConnectionMeterEventDto {

    private String entityAddress;
    private MeterEventTypeDto eventType;
    private BigInteger eventData;
    private LocalDateTime eventDateTime;

    public String getEntityAddress() {
        return entityAddress;
    }

    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    public MeterEventTypeDto getEventType() {
        return eventType;
    }

    public void setEventType(MeterEventTypeDto eventType) {
        this.eventType = eventType;
    }

    public BigInteger getEventData() {
        return eventData;
    }

    public void setEventData(BigInteger eventData) {
        this.eventData = eventData;
    }

    public LocalDateTime getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(LocalDateTime eventDateTime) {
        this.eventDateTime = eventDateTime;
    }
}
