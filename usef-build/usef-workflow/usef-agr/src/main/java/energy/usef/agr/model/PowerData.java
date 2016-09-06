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

package energy.usef.agr.model;

import java.beans.Transient;
import java.math.BigInteger;

import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

/**
 * Helper object which contains all the fields for one set of PowerData.
 */
@Embeddable
@MappedSuperclass
public class PowerData {

    private BigInteger uncontrolledLoad;
    private BigInteger averageConsumption;
    private BigInteger averageProduction;
    private BigInteger potentialFlexConsumption;
    private BigInteger potentialFlexProduction;

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

    @Override
    public String toString() {
        return "PowerData" + "[" +
                "uncontrolledLoad=" + uncontrolledLoad +
                ", averageConsumption=" + averageConsumption +
                ", averageProduction=" + averageProduction +
                ", potentialFlexConsumption=" + potentialFlexConsumption +
                ", potentialFlexProduction=" + potentialFlexProduction +
                "]";
    }
}
