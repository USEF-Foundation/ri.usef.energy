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

package energy.usef.core.model;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity exposing the information related the settlement of a flex order for one PTU.
 */
@Entity
@Table(name = "PTU_SETTLEMENT")
public class PtuSettlement extends Document {

    @ManyToOne
    @JoinColumn(name = "FLEX_ORDER_SETTLEMENT_ID", nullable = false, foreignKey = @ForeignKey(name = "FLEX_ORDER_SETTLEMENT_FK"))
    private FlexOrderSettlement flexOrderSettlement;
    @Column(name = "PROGNOSIS_POWER", precision = 18, scale = 0, nullable = false)
    private BigInteger prognosisPower;
    @Column(name = "ACTUAL_POWER", precision = 18, scale = 0, nullable = false)
    private BigInteger actualPower;
    @Column(name = "ORDERED_FLEX_POWER", precision = 18, scale = 0, nullable = false)
    private BigInteger orderedFlexPower;
    @Column(name = "DELIVERED_FLEX_POWER", precision = 18, scale = 0, nullable = false)
    private BigInteger deliveredFlexPower;
    @Column(name = "POWER_DEFICIENCY", precision = 18, scale = 0, nullable = false)
    private BigInteger powerDeficiency;
    @Column(name = "PRICE", precision = 18, scale = 4)
    private BigDecimal price;
    @Column(name = "PENALTY", precision = 18, scale = 4)
    private BigDecimal penalty;

    public FlexOrderSettlement getFlexOrderSettlement() {
        return flexOrderSettlement;
    }

    public void setFlexOrderSettlement(FlexOrderSettlement flexOrderSettlement) {
        this.flexOrderSettlement = flexOrderSettlement;
    }

    public BigInteger getPrognosisPower() {
        return prognosisPower;
    }

    public void setPrognosisPower(BigInteger prognosisPower) {
        this.prognosisPower = prognosisPower;
    }

    public BigInteger getOrderedFlexPower() {
        return orderedFlexPower;
    }

    public void setOrderedFlexPower(BigInteger orderedFlexPower) {
        this.orderedFlexPower = orderedFlexPower;
    }

    public BigInteger getActualPower() {
        return actualPower;
    }

    public void setActualPower(BigInteger actualPower) {
        this.actualPower = actualPower;
    }

    public BigInteger getDeliveredFlexPower() {
        return deliveredFlexPower;
    }

    public void setDeliveredFlexPower(BigInteger deliveredFlexPower) {
        this.deliveredFlexPower = deliveredFlexPower;
    }

    public BigInteger getPowerDeficiency() {
        return powerDeficiency;
    }

    public void setPowerDeficiency(BigInteger powerDeficiency) {
        this.powerDeficiency = powerDeficiency;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPenalty() {
        return penalty;
    }

    public void setPenalty(BigDecimal penalty) {
        this.penalty = penalty;
    }

}
