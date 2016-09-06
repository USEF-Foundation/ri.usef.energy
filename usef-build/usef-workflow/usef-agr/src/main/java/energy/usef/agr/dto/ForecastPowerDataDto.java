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

package energy.usef.agr.dto;

import java.math.BigInteger;

/**
 * DTO to convey information related to the profile part of a PowerContainerDto.
 */
public class ForecastPowerDataDto extends PowerDataDto {

    private BigInteger allocatedFlexConsumption;
    private BigInteger allocatedFlexProduction;

    /**
     * Empty constructor.
     */
    public ForecastPowerDataDto() {
        // do nothing
    }

    public ForecastPowerDataDto(BigInteger uncontrolledLoad, BigInteger potentialFlexConsumption,
            BigInteger potentialFlexProduction, BigInteger averageConsumption, BigInteger averageProduction,
            BigInteger allocatedFlexConsumption, BigInteger allocatedFlexProduction) {
        super(uncontrolledLoad, potentialFlexConsumption, potentialFlexProduction, averageConsumption, averageProduction);
        this.allocatedFlexConsumption = allocatedFlexConsumption;
        this.allocatedFlexProduction = allocatedFlexProduction;
    }

    public BigInteger getAllocatedFlexConsumption() {
        return allocatedFlexConsumption;
    }

    public void setAllocatedFlexConsumption(BigInteger allocatedFlexConsumption) {
        this.allocatedFlexConsumption = allocatedFlexConsumption;
    }

    public BigInteger getAllocatedFlexProduction() {
        return allocatedFlexProduction;
    }

    public void setAllocatedFlexProduction(BigInteger allocatedFlexProduction) {
        this.allocatedFlexProduction = allocatedFlexProduction;
    }

    @Override
    public String toString() {
        return "PowerDataDto " + "[" +
                "uncontrolledLoad=" + getUncontrolledLoad() +
                ", potentialFlexConsumption=" + getPotentialFlexConsumption() +
                ", potentialFlexProduction=" + getPotentialFlexProduction() +
                ", averageConsumption=" + getAverageConsumption() +
                ", averageProduction=" + getAverageProduction() +
                ", allocatedFlexConsumption=" + allocatedFlexConsumption +
                ", allocatedFlexProduction=" + allocatedFlexProduction +
                "]";
    }
}
