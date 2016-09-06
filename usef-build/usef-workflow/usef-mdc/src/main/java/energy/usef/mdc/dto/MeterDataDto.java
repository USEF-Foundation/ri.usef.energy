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

package energy.usef.mdc.dto;

import energy.usef.core.workflow.dto.ConnectionMeterEventDto;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

/**
 * DTO class for the MeterData xml entity.
 */
public class MeterDataDto {

    private List<ConnectionMeterDataDto> connectionMeterDataDtos;
    private List<ConnectionMeterEventDto> connectionMeterEventDtos;
    private LocalDate period;

    public List<ConnectionMeterDataDto> getConnectionMeterDataDtos() {
        if (connectionMeterDataDtos == null) {
            connectionMeterDataDtos = new ArrayList<>();
        }
        return connectionMeterDataDtos;
    }

    public List<ConnectionMeterEventDto> getConnectionMeterEventDtos() {
        if (connectionMeterEventDtos == null) {
            connectionMeterEventDtos = new ArrayList<>();
        }
        return connectionMeterEventDtos;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public void setPeriod(LocalDate period) {
        this.period = period;
    }
}
