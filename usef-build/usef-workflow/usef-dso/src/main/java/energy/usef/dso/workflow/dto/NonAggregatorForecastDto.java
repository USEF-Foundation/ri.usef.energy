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
import java.util.List;

import org.joda.time.LocalDate;

/**
 * DTO for the NonAggregatorForecast Input Data.
 */
public class NonAggregatorForecastDto {
    private String entityAddress;
    private LocalDate ptuDate;

    private List<PtuNonAggregatorForecastDto> ptus;

    public String getEntityAddress() {
        return entityAddress;
    }

    public void setEntityAddress(String entityAddress) {
        this.entityAddress = entityAddress;
    }

    public LocalDate getPtuDate() {
        return ptuDate;
    }

    public void setPtuDate(LocalDate ptuDate) {
        this.ptuDate = ptuDate;
    }

    public List<PtuNonAggregatorForecastDto> getPtus() {
        if (ptus == null) {
            ptus = new ArrayList<>();
        }
        return ptus;
    }

    public void setPtus(List<PtuNonAggregatorForecastDto> ptus) {
        this.ptus = ptus;
    }
}
