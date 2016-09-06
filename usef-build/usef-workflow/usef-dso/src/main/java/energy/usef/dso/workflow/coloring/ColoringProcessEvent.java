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

package energy.usef.dso.workflow.coloring;

import energy.usef.core.event.ExpirableEvent;
import org.joda.time.LocalDate;

/**
 * Event implementation for starting the Coloring Process workflow to determine if PTU(s) become orange.
 */
public class ColoringProcessEvent implements ExpirableEvent {

    private LocalDate period;
    private String congestionPoint;

    /**
     * Constructor for ColoringProcessEvent for given date and congestionPoint.
     *
     * @param period
     * @param congestionPoint
     */
    public ColoringProcessEvent(LocalDate period, String congestionPoint) {
        this.period = period;
        this.congestionPoint = congestionPoint;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public void setPeriod(LocalDate period) {
        this.period = period;
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
                "date=" + period +
                ", congestionPoint='" + congestionPoint + "'" +
                "]";
    }
}
