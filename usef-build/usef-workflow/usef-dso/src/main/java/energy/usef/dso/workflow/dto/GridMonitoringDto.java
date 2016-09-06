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

package energy.usef.dso.workflow.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

/**
 * DTO class to carry information related to the grid monitoring.
 */
public class GridMonitoringDto {

    private String congestionPointEntityAddress;
    private LocalDate period;
    private List<PtuGridMonitoringDto> ptuGridMonitoringDtos;
    private Map<String, Integer> connectionCountPerAggregator;

    public GridMonitoringDto() {
        // default constructor.
    }

    /**
     * Constructor with congestion point address and period.
     *
     * @param congestionPointEntityAddress {@link String} congestion point entity address.
     * @param period {@link LocalDate} period.
     */
    public GridMonitoringDto(String congestionPointEntityAddress, LocalDate period) {
        this.congestionPointEntityAddress = congestionPointEntityAddress;
        this.period = period;
    }

    public String getCongestionPointEntityAddress() {
        return congestionPointEntityAddress;
    }

    public void setCongestionPointEntityAddress(String congestionPointEntityAddress) {
        this.congestionPointEntityAddress = congestionPointEntityAddress;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public void setPeriod(LocalDate period) {
        this.period = period;
    }

    public List<PtuGridMonitoringDto> getPtuGridMonitoringDtos() {
        if (ptuGridMonitoringDtos == null) {
            ptuGridMonitoringDtos = new ArrayList<>();
        }
        return ptuGridMonitoringDtos;
    }

    public Map<String, Integer> getConnectionCountPerAggregator() {
        if (connectionCountPerAggregator == null) {
            connectionCountPerAggregator = new HashMap<>();
        }
        return connectionCountPerAggregator;
    }
}
