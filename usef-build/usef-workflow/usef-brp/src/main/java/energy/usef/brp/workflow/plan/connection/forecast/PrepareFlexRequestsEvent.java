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

package energy.usef.brp.workflow.plan.connection.forecast;

import energy.usef.core.event.ExpirableEvent;
import org.joda.time.LocalDate;

/**
 * Event class to trigger the preparation of the workflow in charge of creating actual flex requests.
 */
public class PrepareFlexRequestsEvent implements ExpirableEvent {

    private final LocalDate period;

    /**
     * Constructor with the date.
     *
     * @param period {@link org.joda.time.LocalDate} period for which one needs to prepare.
     */
    public PrepareFlexRequestsEvent(LocalDate period) {
        this.period = period;
    }

    public LocalDate getPeriod() {
        return period;
    }
}
