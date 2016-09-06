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

package energy.usef.agr.dto.device.capability;

/**
 * JSON class representing an interrupt capability of a UDI device. This class extends {@link DeviceCapabilityDto}.
 */
public class InterruptCapabilityDto extends DeviceCapabilityDto {

    private Integer maxDtus;

    private InterruptCapabilityTypeDto type;

    public Integer getMaxDtus() {
        return maxDtus;
    }

    public void setMaxDtus(Integer maxDtus) {
        this.maxDtus = maxDtus;
    }

    public InterruptCapabilityTypeDto getType() {
        return type;
    }

    public void setType(InterruptCapabilityTypeDto type) {
        this.type = type;
    }

    @Override public String toString() {
        return "InterruptCapabilityDto" + "[" +
                "maxDtus=" + maxDtus +
                ", type=" + type +
                "]";
    }
}
