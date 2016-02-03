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

package energy.usef.dso.workflow.coloring;

import org.joda.time.LocalDate;

/**
 * Event implementation for starting the Coloring Process workflow to determine if PTU(s) become orange.
 */
public class ColoringProcessEvent {

    private LocalDate date;
    private String congestionPoint;

    /**
     * Constructor for ColoringProcessEvent for given date and congestionPoint.
     *
     * @param date
     * @param congestionPoint
     */
    public ColoringProcessEvent(LocalDate date, String congestionPoint) {
        this.date = date;
        this.congestionPoint = congestionPoint;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCongestionPoint() {
        return congestionPoint;
    }

    public void setCongestionPoint(String congestionPoint) {
        this.congestionPoint = congestionPoint;
    }

    @Override
    public String toString() {
        return "ColoringProcessEvent" + "[" +
                "date=" + date +
                ", congestionPoint='" + congestionPoint + "'" +
                "]";
    }
}
