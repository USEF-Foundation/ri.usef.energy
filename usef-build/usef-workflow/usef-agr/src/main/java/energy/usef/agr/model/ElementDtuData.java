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

import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A extra object to represent the ElementData per DTU.
 */
@Entity
@Table(name = "ELEMENT_DTU_DATA",
        uniqueConstraints = @UniqueConstraint(columnNames = { "element_id", "dtu_index" }))
public class ElementDtuData {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ELEMENT_ID", foreignKey = @ForeignKey(name = "EDD_ELEMENT_FK"), nullable = false)
    private Element element;

    @Column(name = "DTU_INDEX", nullable = false)
    private Integer dtuIndex;

    @Column(name = "PROFILE_UNCONTROLLED_LOAD", precision = 18, scale = 0)
    private BigInteger profileUncontrolledLoad;

    @Column(name = "PROFILE_POTENTIAL_FLEX_CONSUMPTION", precision = 18, scale = 0)
    private BigInteger profilePotentialFlexConsumption;

    @Column(name = "PROFILE_POTENTIAL_FLEX_PRODUCTION", precision = 18, scale = 0)
    private BigInteger profilePotentialFlexProduction;

    @Column(name = "PROFILE_AVERAGE_CONSUMPTION", precision = 18, scale = 0)
    private BigInteger profileAverageConsumption;

    @Column(name = "PROFILE_AVERAGE_PRODUCTION", precision = 18, scale = 0)
    private BigInteger profileAverageProduction;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
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
}
