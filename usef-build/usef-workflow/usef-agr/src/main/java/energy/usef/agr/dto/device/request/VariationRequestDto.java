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

package energy.usef.agr.dto.device.request;

import java.math.BigInteger;

/**
 *
 */
public class VariationRequestDto extends DeviceRequestDto {
    private String eventID;
    private BigInteger startDTU;
    private BigInteger endDTU;
    private BigInteger power;
    private ConsumptionProductionTypeDto consumptionProductionType;

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public BigInteger getStartDTU() {
        return startDTU;
    }

    public void setStartDTU(BigInteger startDTU) {
        this.startDTU = startDTU;
    }

    public BigInteger getEndDTU() {
        return endDTU;
    }

    public void setEndDTU(BigInteger endDTU) {
        this.endDTU = endDTU;
    }

    public BigInteger getPower() {
        return power;
    }

    public void setPower(BigInteger power) {
        this.power = power;
    }

    public ConsumptionProductionTypeDto getConsumptionProductionType() {
        return consumptionProductionType;
    }

    public void setConsumptionProductionType(ConsumptionProductionTypeDto consumptionProductionType) {
        this.consumptionProductionType = consumptionProductionType;
    }
}
