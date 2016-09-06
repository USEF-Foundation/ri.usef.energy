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

package energy.usef.dso.model;

import energy.usef.core.model.Document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Entity class {@link PtuGridMonitor}: This class is a representation of a PTU-GridMonitor for the DSO role.
 */
@Entity
@Table(name = "PTU_GRID_MONITOR")
public class PtuGridMonitor extends Document {
    @Column(name = "ACTUAL_POWER", nullable = true)
    private Long actualPower;

    @Column(name = "LIMITED_POWER", nullable = true)
    private Long limitedPower;

    public Long getActualPower() {
        return actualPower;
    }

    public void setActualPower(Long actualPower) {
        this.actualPower = actualPower;
    }

    public Long getLimitedPower() {
        return limitedPower;
    }

    public void setLimitedPower(Long limitedPower) {
        this.limitedPower = limitedPower;
    }

    @Override
    public String toString() {
        return "PtuGridMonitor" + "[" +
                "actualPower=" + actualPower +
                ", limitedPower=" + limitedPower +
                "]";
    }
}
