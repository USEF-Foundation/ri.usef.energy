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

package energy.usef.pbcfeeder.dto;

import java.math.BigDecimal;

/**
 * DTO object to transfer the power limits for the PBC.
 */
public class PbcPowerLimitsDto {

    private final BigDecimal lowerLimit;
    private final BigDecimal upperLimit;

    /**
     * Constructs a new {@link PbcPowerLimitsDto} wrapper for power limits for a congestion point.
     * @param lowerLimit {@link BigDecimal} lower limit (i.e. production limit).
     * @param upperLimit {@link BigDecimal} upper limit (i.e. consumption limit).
     */
    public PbcPowerLimitsDto(BigDecimal lowerLimit, BigDecimal upperLimit) {
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
    }

    public BigDecimal getLowerLimit() {
        return lowerLimit;
    }

    public BigDecimal getUpperLimit() {
        return upperLimit;
    }
}
