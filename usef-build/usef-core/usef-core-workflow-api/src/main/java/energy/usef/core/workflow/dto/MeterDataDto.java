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

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

/**
 * DTO class to carry information about smart meter data per period.
 */
public class MeterDataDto {

    private LocalDate period;
    private List<ConnectionMeterDataDto> connectionMeterDataDtos;

    public LocalDate getPeriod() {
        return period;
    }

    public void setPeriod(LocalDate period) {
        this.period = period;
    }

    public List<ConnectionMeterDataDto> getConnectionMeterDataDtos() {
        if (connectionMeterDataDtos == null) {
            connectionMeterDataDtos = new ArrayList<>();
        }
        return connectionMeterDataDtos;
    }

}
