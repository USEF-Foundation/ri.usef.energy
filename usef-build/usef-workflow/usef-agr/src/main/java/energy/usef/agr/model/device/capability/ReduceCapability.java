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

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Entity class representing a reduce capability of a UDI device. This class extends {@link VariationCapability}.
 */
@Entity
@Table(name = "REDUCE_CAPABILITY")
public class ReduceCapability extends VariationCapability {

    @Column(name = "MIN_POWER", precision = 18, scale = 0, nullable = false)
    private BigInteger minPower;

    @Column(name = "DURATION_MULTIPLIER", precision = 18, scale = 2)
    private BigDecimal durationMultiplier;

    public BigInteger getMinPower() {
        return minPower;
    }

    public void setMinPower(BigInteger minPower) {
        this.minPower = minPower;
    }

    public BigDecimal getDurationMultiplier() {
        return durationMultiplier;
    }

    public void setDurationMultiplier(BigDecimal durationMultiplier) {
        this.durationMultiplier = durationMultiplier;
    }
}
