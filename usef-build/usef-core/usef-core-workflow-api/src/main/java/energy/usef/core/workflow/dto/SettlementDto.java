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
import java.util.Map;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;

/**
 * DTO class which contains the list {@link FlexOrderSettlementDto} for an given interval of time defined by two dates.
 */
public class SettlementDto {

    private LocalDate startDate;
    private LocalDate endDate;
    private List<FlexOrderSettlementDto> flexOrderSettlementDtos;

    /**
     * Constructor with two dates specifying the settlement period.
     *
     * @param startDate {@link LocalDate} start date of the settlement period.
     * @param endDate {@link LocalDate} end date of the settlement period.
     */
    public SettlementDto(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Returns a map of {@link List} of {@link FlexOrderSettlementDto} grouped by period.
     *
     * @return {@link Map} with {@link LocalDate} period as key and {@link List} of {@link FlexOrderSettlementDto} as value.
     */
    public Map<LocalDate, List<FlexOrderSettlementDto>> flexOrderSettlementDtosPerPeriod() {
        return getFlexOrderSettlementDtos().stream().collect(Collectors.groupingBy(FlexOrderSettlementDto::getPeriod));
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<FlexOrderSettlementDto> getFlexOrderSettlementDtos() {
        if (flexOrderSettlementDtos == null) {
            flexOrderSettlementDtos = new ArrayList<>();
        }
        return flexOrderSettlementDtos;
    }

}
