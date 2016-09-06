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

package energy.usef.dso.workflow.validate.gridsafetyanalysis;

import energy.usef.core.event.ExpirableEvent;
import org.joda.time.LocalDate;

import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;

/**
 * Event implementation for starting the Grid Safety Analysis workflow.
 */
public class StoreGridSafetyAnalysisEvent implements ExpirableEvent {

    private String congestionPointEntityAddress;
    private LocalDate period;
    private GridSafetyAnalysisDto gridSafetyAnalysisDto;

    /**
     * Constructor with analysis day.
     *
     * @param congestionPointEntityAddress The entityAddress of the congestionPoint on which a GridSafetyAnalysis should be
     *                                     performed.
     * @param period                  {@link LocalDate} day for which the analysis is done.
     */
    public StoreGridSafetyAnalysisEvent(String congestionPointEntityAddress, LocalDate period,
            GridSafetyAnalysisDto gridSafetyAnalysisDto) {
        this.congestionPointEntityAddress = congestionPointEntityAddress;
        this.period = period;
        this.gridSafetyAnalysisDto = gridSafetyAnalysisDto;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public String getCongestionPointEntityAddress() {
        return congestionPointEntityAddress;
    }

    public GridSafetyAnalysisDto getGridSafetyAnalysisDto() {
        return gridSafetyAnalysisDto;
    }

    @Override
    public String toString() {
        return "GridSafetyAnalysisEvent" + "[" +
                "congestionPointEntityAddress='" + congestionPointEntityAddress + "'" +
                ", analysisDay=" + period +
                "]";
    }
}
