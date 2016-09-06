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

package energy.usef.dso.workflow.dto;

import java.math.BigInteger;

import org.joda.time.LocalDateTime;

/**
 * ConnectionCapacityLimitationPeriodDto used to communicate EventPeriods with PBC's.
 */
public class ConnectionCapacityLimitationPeriodDto {

    private String entityAddress;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private Boolean totalOutage;

    private BigInteger capacityReduction;

    public String getEntityAddress() {
        return entityAddress;
    }

    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public BigInteger getCapacityReduction() {
        return capacityReduction;
    }

    public void setCapacityReduction(BigInteger capacityReduction) {
        this.capacityReduction = capacityReduction;
    }

    public Boolean isTotalOutage() {
        return totalOutage;
    }

    public void setTotalOutage(Boolean totalOutage) {
        this.totalOutage = totalOutage;
    }

    @Override
    public String toString() {
        return "ConnectionCapacityLimitationPeriodDto{" +
                "entityAddress='" + entityAddress + '\'' +
                ", startDateTime=" + startDateTime +
                ", endDateTime=" + endDateTime +
                ", totalOutage=" + totalOutage +
                ", capacityReduction=" + capacityReduction +
                '}';
    }
}
