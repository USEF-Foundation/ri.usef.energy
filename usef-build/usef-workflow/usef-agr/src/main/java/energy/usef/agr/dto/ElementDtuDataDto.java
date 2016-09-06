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

import energy.usef.agr.model.ElementDtuData;

/**
 * An ElementDtuDataDto contains all Element related to {@link ElementDtuData}.
 */
public class ElementDtuDataDto {
    private Long id;
    private Integer dtuIndex;
    private BigInteger profileUncontrolledLoad;
    private BigInteger profilePotentialFlexConsumption;
    private BigInteger profilePotentialFlexProduction;
    private BigInteger profileAverageConsumption;
    private BigInteger profileAverageProduction;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDtuIndex() {
        return dtuIndex;
    }

    public void setDtuIndex(Integer dtuIndex) {
        this.dtuIndex = dtuIndex;
    }

    public BigInteger getProfilePotentialFlexConsumption() {
        return profilePotentialFlexConsumption;
    }

    public void setProfilePotentialFlexConsumption(BigInteger profilePotentialFlexConsumption) {
        this.profilePotentialFlexConsumption = profilePotentialFlexConsumption;
    }

    public BigInteger getProfilePotentialFlexProduction() {
        return profilePotentialFlexProduction;
    }

    public void setProfilePotentialFlexProduction(BigInteger profilePotentialFlexProduction) {
        this.profilePotentialFlexProduction = profilePotentialFlexProduction;
    }

    public BigInteger getProfileAverageConsumption() {
        return profileAverageConsumption;
    }

    public void setProfileAverageConsumption(BigInteger profileAverageConsumption) {
        this.profileAverageConsumption = profileAverageConsumption;
    }

    public BigInteger getProfileAverageProduction() {
        return profileAverageProduction;
    }

    public void setProfileAverageProduction(BigInteger profileAverageProduction) {
        this.profileAverageProduction = profileAverageProduction;
    }

    public BigInteger getProfileUncontrolledLoad() {
        return profileUncontrolledLoad;
    }

    public void setProfileUncontrolledLoad(BigInteger profileUncontrolledLoad) {
        this.profileUncontrolledLoad = profileUncontrolledLoad;
    }

    @Override
    public String toString() {
        return "ElementDtuDataDto" + "[" +
                "id=" + id +
                ", dtuIndex=" + dtuIndex +
                ", profileUncontrolledLoad=" + profileUncontrolledLoad +
                ", profilePotentialFlexConsumption=" + profilePotentialFlexConsumption +
                ", profilePotentialFlexProduction=" + profilePotentialFlexProduction +
                ", profileAverageConsumption=" + profileAverageConsumption +
                ", profileAverageProduction=" + profileAverageProduction +
                "]";
    }

}
