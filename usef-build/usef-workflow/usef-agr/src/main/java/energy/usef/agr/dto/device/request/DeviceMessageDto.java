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

package energy.usef.agr.dto.device.request;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object class for the DeviceMessage entities.
 */
public class DeviceMessageDto {

    private String endpoint;

    private List<ShiftRequestDto> shiftRequestDtos;
    private List<ReduceRequestDto> reduceRequestDtos;
    private List<IncreaseRequestDto> increaseRequestDtos;
    private List<InterruptRequestDto> interruptRequestDtos;
    private List<ReportRequestDto> reportRequestDtos;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public List<ShiftRequestDto> getShiftRequestDtos() {
        if (shiftRequestDtos == null) {
            shiftRequestDtos = new ArrayList<>();
        }
        return shiftRequestDtos;
    }

    public List<ReduceRequestDto> getReduceRequestDtos() {
        if (reduceRequestDtos == null) {
            reduceRequestDtos = new ArrayList<>();
        }
        return reduceRequestDtos;
    }

    public List<IncreaseRequestDto> getIncreaseRequestDtos() {
        if (increaseRequestDtos == null) {
            increaseRequestDtos = new ArrayList<>();
        }
        return increaseRequestDtos;
    }

    public List<InterruptRequestDto> getInterruptRequestDtos() {
        if (interruptRequestDtos == null) {
            interruptRequestDtos = new ArrayList<>();
        }
        return interruptRequestDtos;
    }

    public List<ReportRequestDto> getReportRequestDtos() {
        if (reportRequestDtos == null) {
            reportRequestDtos = new ArrayList<>();
        }
        return reportRequestDtos;
    }
}
