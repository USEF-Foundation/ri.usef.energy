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

package energy.usef.agr.model.device.capability;

import energy.usef.agr.model.ConsumptionProductionType;

import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;

/**
 * Abstract entity representing a variation capability (increase or reduce) of a UDI device.
 */
@MappedSuperclass
public abstract class VariationCapability extends DeviceCapability {

    @Column(name = "MAX_DTUS")
    private Integer maxDtus;

    @Column(name = "POWER_STEP", precision = 18, scale = 0, nullable = false)
    private BigInteger powerStep;

    @Column(name = "CONSUMPTION_PRODUCTION_TYPE",
            length = 16)
    @Enumerated(value = EnumType.STRING)
    private ConsumptionProductionType consumptionProductionType;

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

    public ConsumptionProductionType getConsumptionProductionType() {
        return consumptionProductionType;
    }

    public void setConsumptionProductionType(ConsumptionProductionType consumptionProductionType) {
        this.consumptionProductionType = consumptionProductionType;
    }
}
