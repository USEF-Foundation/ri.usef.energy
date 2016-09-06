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

package energy.usef.agr.dto.device.capability;

import energy.usef.agr.dto.device.request.ConsumptionProductionTypeDto;

import java.math.BigInteger;

/**
 * Abstract entity representing a variation capability (increase or reduce) of a UDI device.
 */
public abstract class VariationCapabilityDto extends DeviceCapabilityDto {

    private Integer maxDtus;

    private BigInteger powerStep;

    private ConsumptionProductionTypeDto consumptionProductionType;

    public Integer getMaxDtus() {
        return maxDtus;
    }

    public void setMaxDtus(Integer maxDtus) {
        this.maxDtus = maxDtus;
    }

    public BigInteger getPowerStep() {
        return powerStep;
    }

    public void setPowerStep(BigInteger powerStep) {
        this.powerStep = powerStep;
    }

    public ConsumptionProductionTypeDto getConsumptionProductionType() {
        return consumptionProductionType;
    }

    public void setConsumptionProductionType(ConsumptionProductionTypeDto consumptionProductionType) {
        this.consumptionProductionType = consumptionProductionType;
    }
}
