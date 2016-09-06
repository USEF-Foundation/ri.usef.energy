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

import energy.usef.agr.model.PowerData;

import java.beans.Transient;
import java.math.BigInteger;

/**
 * DTO to convey information related to the profile part of a PowerContainerDto.
 */
public class PowerDataDto {

    private BigInteger uncontrolledLoad;
    private BigInteger potentialFlexConsumption;
    private BigInteger potentialFlexProduction;
    private BigInteger averageConsumption;
    private BigInteger averageProduction;

    /**
     * Empty constructor.
     */
    public PowerDataDto() {
        // do nothing
    }

    /**
     * Constructor with all the possible fields.
     *
     * @param uncontrolledLoad {@link BigInteger}.
     * @param potentialFlexConsumption {@link BigInteger}.
     * @param potentialFlexProduction {@link BigInteger}.
     * @param averageConsumption {@link BigInteger}.
     * @param averageProduction {@link BigInteger}.
     */
    public PowerDataDto(BigInteger uncontrolledLoad, BigInteger potentialFlexConsumption,
            BigInteger potentialFlexProduction, BigInteger averageConsumption, BigInteger averageProduction) {
        this.uncontrolledLoad = uncontrolledLoad;
        this.potentialFlexConsumption = potentialFlexConsumption;
        this.potentialFlexProduction = potentialFlexProduction;
        this.averageConsumption = averageConsumption;
        this.averageProduction = averageProduction;
    }

    /**
     * Constructor with a PowerData element.
     *
     * @param powerData {@link PowerData}.
     */
    public PowerDataDto(PowerData powerData) {
        this.uncontrolledLoad = powerData.getUncontrolledLoad();
        this.potentialFlexConsumption = powerData.getPotentialFlexConsumption();
        this.potentialFlexProduction = powerData.getPotentialFlexProduction();
        this.averageConsumption = powerData.getAverageConsumption();
        this.averageProduction = powerData.getAverageProduction();
    }

    /**
     * This method sums the uncontrolledLoad, averageConsumption and averageProduction.
     *
     * @return The sum, if no values are present it will return BigInteger.ZERO.
     */
    @Transient
    public BigInteger calculatePower() {
        BigInteger sum = BigInteger.ZERO;
        if (uncontrolledLoad != null) {
            sum = sum.add(uncontrolledLoad);
        }
        //the assumption here is that this will be a negative number so {@link BigInteger#add} should be used.
        if (averageConsumption != null) {
            sum = sum.add(averageConsumption);
        }
        if (averageProduction != null) {
            sum = sum.subtract(averageProduction);
        }
        return sum;
    }

    public BigInteger getUncontrolledLoad() {
        return uncontrolledLoad;
    }

    public void setUncontrolledLoad(BigInteger uncontrolledLoad) {
        this.uncontrolledLoad = uncontrolledLoad;
    }

    public BigInteger getPotentialFlexConsumption() {
        return potentialFlexConsumption;
    }

    public void setPotentialFlexConsumption(BigInteger potentialFlexConsumption) {
        this.potentialFlexConsumption = potentialFlexConsumption;
    }

    public BigInteger getPotentialFlexProduction() {
        return potentialFlexProduction;
    }

    public void setPotentialFlexProduction(BigInteger potentialFlexProduction) {
        this.potentialFlexProduction = potentialFlexProduction;
    }

    public BigInteger getAverageConsumption() {
        return averageConsumption;
    }

    public void setAverageConsumption(BigInteger averageConsumption) {
        this.averageConsumption = averageConsumption;
    }

    public BigInteger getAverageProduction() {
        return averageProduction;
    }

    public void setAverageProduction(BigInteger averageProduction) {
        this.averageProduction = averageProduction;
    }

    @Override
    public String toString() {
        return "PowerDataDto " + "[" +
                "uncontrolledLoad=" + uncontrolledLoad +
                ", potentialFlexConsumption=" + potentialFlexConsumption +
                ", potentialFlexProduction=" + potentialFlexProduction +
                ", averageConsumption=" + averageConsumption +
                ", averageProduction=" + averageProduction +
                "]";
    }
}
