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

package energy.usef.agr.workflow.plan.create.aplan;

import org.joda.time.LocalDate;

import energy.usef.core.event.ExpirableEvent;
import energy.usef.core.util.DateTimeUtil;

/**
 * Event class that is used to trigger the workflow of creating and sending A-Plan.
 */
public class CreateAPlanEvent implements ExpirableEvent {

    private final LocalDate period;
    private final String usefIdentifier;

    /**
     * Constructor with the given parameters.
     *
     * @param period         {@link LocalDate} day for the a-plan.
     * @param usefIdentifier {@link String} optional usef identifier.
     */
    public CreateAPlanEvent(LocalDate period, String usefIdentifier) {
        this.period = period;
        this.usefIdentifier = usefIdentifier;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public String getUsefIdentifier() {
        return usefIdentifier;
    }

    public boolean isExpired() {
        return period.isBefore(DateTimeUtil.getCurrentDate());
    }

    @Override
    public String toString() {
        return "CreateAPlanEvent" + "[" +
                "period=" + period +
                ", usefIdentifier='" + usefIdentifier + "'" +
                "]";
    }
}
