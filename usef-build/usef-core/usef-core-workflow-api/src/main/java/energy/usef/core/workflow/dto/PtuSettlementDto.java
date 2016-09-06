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

package energy.usef.core.workflow.dto;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * DTO class containing the information of the settlement of a Flex Order for 1 PTU.
 */
public class PtuSettlementDto {

    private BigInteger ptuIndex;
    private BigInteger prognosisPower;
    private BigInteger orderedFlexPower;
    private BigInteger actualPower;
    private BigInteger deliveredFlexPower;
    private BigInteger powerDeficiency;
    private BigDecimal price;
    private BigDecimal penalty;
    private BigDecimal netSettlement;

    public BigInteger getPtuIndex() {
        return ptuIndex;
    }

    public void setPtuIndex(BigInteger ptuIndex) {
        this.ptuIndex = ptuIndex;
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

    public BigDecimal getNetSettlement() {
        return netSettlement;
    }

    public void setNetSettlement(BigDecimal netSettlement) {
        this.netSettlement = netSettlement;
    }
}
