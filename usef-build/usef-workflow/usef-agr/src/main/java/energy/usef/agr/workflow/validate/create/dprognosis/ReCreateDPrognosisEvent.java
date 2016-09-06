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

package energy.usef.agr.workflow.validate.create.dprognosis;

import energy.usef.core.event.ExpirableEvent;
import energy.usef.core.util.DateTimeUtil;

import org.joda.time.LocalDate;

/**
 * Event class in charge of triggering the workflow 'Re-Create D-Prognosis' for the aggregator only.
 */
public class ReCreateDPrognosisEvent implements ExpirableEvent {

    private final LocalDate period;

    /**
     * Default constructor. If the given period is <code>null</code>, it will be set to the current date.
     *
     * @param period {@link org.joda.time.LocalDate}
     */
    public ReCreateDPrognosisEvent(LocalDate period) {
        this.period = period == null ? DateTimeUtil.getCurrentDate() : period;
    }

    public LocalDate getPeriod() {
        return period;
    }

    @Override
    public String toString() {
        return "ReCreateDPrognosisEvent" + "[" +
                "period=" + period +
                "]";
    }
}
