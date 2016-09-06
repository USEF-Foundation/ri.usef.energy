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

package energy.usef.brp.dto;

import org.joda.time.LocalDate;

/**
 * DTO for the power consumption.
 */
public class PtuPowerConsumptionDto {
    private LocalDate ptuDate;
    private Integer ptuIndex;
    private String usefIdentifier;
    private Long powerConsumption;

    /**
     * Sets USEF identifier.
     * 
     * @return the usefIdentifier
     */
    public String getUsefIdentifier() {
        return usefIdentifier;
    }

    /**
     * Sets USEF identifier.
     * 
     * @param usefIdentifier the usefIdentifier to set
     */
    public void setUsefIdentifier(String usefIdentifier) {
        this.usefIdentifier = usefIdentifier;
    }

    /**
     * Gets PTU date.
     * 
     * @return the date
     */
    public LocalDate getPtuDate() {
        return ptuDate;
    }

    /**
     * Sets PTU date.
     * 
     * @param date the date to set
     */
    public void setPtuDate(LocalDate ptuDate) {
        this.ptuDate = ptuDate;
    }

    /**
     * Gets PTU Index.
     * 
     * @return the ptuIndex
     */
    public int getPtuIndex() {
        return ptuIndex;
    }

    /**
     * Sets PTU Index.
     * 
     * @param ptuIndex the ptuIndex to set
     */
    public void setPtuIndex(Integer ptuIndex) {
        this.ptuIndex = ptuIndex;
    }

    /**
     * Gets Power Consumption.
     * 
     * @return the power consumption
     */
    public Long getPowerConsumption() {
        return powerConsumption;
    }

    /**
     * Sets Power Consumption.
     * 
     * @param powerConsumption the power consumption to set
     */
    public void setPowerConsumption(Long powerConsumption) {
        this.powerConsumption = powerConsumption;
    }

    @Override
    public String toString() {
        return "PtuPowerConsumptionDto" + "[" +
                "ptuDate=" + ptuDate +
                ", ptuIndex=" + ptuIndex +
                ", usefIdentifier='" + usefIdentifier + "'" +
                ", powerConsumption=" + powerConsumption +
                "]";
    }
}
