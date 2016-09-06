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

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

/**
 * DTO class for the Udi Event objects.
 */
public class UdiEventDto {

    private String id;
    private LocalDate period;
    private String udiEndpoint;
    private String deviceSelector;
    private UdiEventTypeDto udiEventType;
    private Integer startDtu;
    private Integer endDtu;
    private Integer finishBeforeDtu;
    private Integer startAfterDtu;
    private List<DeviceCapabilityDto> deviceCapabilities;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public void setPeriod(LocalDate period) {
        this.period = period;
    }

    public String getUdiEndpoint() {
        return udiEndpoint;
    }

    public void setUdiEndpoint(String udiEndpoint) {
        this.udiEndpoint = udiEndpoint;
    }

    public String getDeviceSelector() {
        return deviceSelector;
    }

    public void setDeviceSelector(String deviceSelector) {
        this.deviceSelector = deviceSelector;
    }

    public UdiEventTypeDto getUdiEventType() {
        return udiEventType;
    }

    public void setUdiEventType(UdiEventTypeDto udiEventType) {
        this.udiEventType = udiEventType;
    }

    public Integer getStartDtu() {
        return startDtu;
    }

    public void setStartDtu(Integer startDtu) {
        this.startDtu = startDtu;
    }

    public Integer getEndDtu() {
        return endDtu;
    }

    public void setEndDtu(Integer endDtu) {
        this.endDtu = endDtu;
    }

    public Integer getFinishBeforeDtu() {
        return finishBeforeDtu;
    }

    public void setFinishBeforeDtu(Integer finishBeforeDtu) {
        this.finishBeforeDtu = finishBeforeDtu;
    }

    public Integer getStartAfterDtu() {
        return startAfterDtu;
    }

    public void setStartAfterDtu(Integer startAfterDtu) {
        this.startAfterDtu = startAfterDtu;
    }

    public List<DeviceCapabilityDto> getDeviceCapabilities() {
        if (deviceCapabilities == null) {
            deviceCapabilities = new ArrayList<>();
        }
        return deviceCapabilities;
    }
}
