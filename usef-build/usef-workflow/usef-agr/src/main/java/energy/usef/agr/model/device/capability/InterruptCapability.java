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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * Entity class representing an interrupt capability of a UDI device. This class extends {@link DeviceCapability}.
 */
@Entity
@Table(name = "INTERRUPT_CAPABILITY")
public class InterruptCapability extends DeviceCapability {

    @Column(name = "MAX_DTUS")
    private Integer maxDtus;

    @Column(name = "TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private InterruptCapabilityType type;

    public Integer getMaxDtus() {
        return maxDtus;
    }

    public void setMaxDtus(Integer maxDtus) {
        this.maxDtus = maxDtus;
    }

    public InterruptCapabilityType getType() {
        return type;
    }

    public void setType(InterruptCapabilityType type) {
        this.type = type;
    }
}
