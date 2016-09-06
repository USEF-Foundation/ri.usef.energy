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

package energy.usef.agr.workflow.operate.recreate.prognoses;

import energy.usef.core.event.ExpirableEvent;
import org.joda.time.LocalDate;

import energy.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyEvent;

/**
 * Event class which will call a PBC that will decided to re-create new A-Plans and/or new D-Prognoses or not.
 */
public class ReCreatePrognosesEvent implements ExpirableEvent {

    private final LocalDate period;

    /**
     * Constructor which will receive the {@link ControlActiveDemandSupplyEvent} to fire
     * at the end of the workflow.
     *
     * @param period {@link org.joda.time.LocalDate} period for which one wants to trigger the workflow.
     */
    public ReCreatePrognosesEvent(LocalDate period) {
        this.period = period;
    }

    public LocalDate getPeriod() {
        return period;
    }

    @Override
    public String toString() {
        return "ReCreatePrognosesEvent" + "[" +
                "period=" + period +
                "]";
    }
}
