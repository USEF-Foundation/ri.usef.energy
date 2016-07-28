/*
 * Copyright 2015 USEF Foundation
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

import energy.usef.core.util.DateTimeUtil;
import org.joda.time.LocalDate;

/**
 * Event implementation for starting the Grid Safety Analysis workflow.
 */
public class GridSafetyAnalysisEvent {

    private String congestionPointEntityAddress;
    private LocalDate analysisDay;

    /**
     * Constructor with analysis day.
     *
     * @param congestionPointEntityAddress The entityAddress of the congestionPoint on which a GridSafetyAnalysis should be
     *            performed.
     * @param analysisDay {@link LocalDate} day for which the analysis is done.
     */
    public GridSafetyAnalysisEvent(String congestionPointEntityAddress, LocalDate analysisDay) {
        this.congestionPointEntityAddress = congestionPointEntityAddress;
        this.analysisDay = analysisDay;
    }

    public LocalDate getAnalysisDay() {
        return analysisDay;
    }

    public String getCongestionPointEntityAddress() {
        return congestionPointEntityAddress;
    }

    public boolean isExpired() {
        return (this.analysisDay.isBefore(DateTimeUtil.getCurrentDate()));
    }
    @Override
    public String toString() {
        return "GridSafetyAnalysisEvent" + "[" +
                "congestionPointEntityAddress='" + congestionPointEntityAddress + "'" +
                ", analysisDay=" + analysisDay +
                "]";
    }
}
