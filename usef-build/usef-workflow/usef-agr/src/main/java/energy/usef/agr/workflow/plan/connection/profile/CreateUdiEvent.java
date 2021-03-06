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

package energy.usef.agr.workflow.plan.connection.profile;

import energy.usef.core.event.ExpirableEvent;
import org.joda.time.LocalDate;

/**
 * Event to trigger the workflow populating the Udis for the connection portfolio.
 */
public class CreateUdiEvent implements ExpirableEvent {

    private final LocalDate period;

    /**
     * Constructor with the date of initialization of the planboard, which will be used as reference to begin the creation of UDIs
     *
     * @param period {@link LocalDate} period.
     */
    public CreateUdiEvent(LocalDate period) {
        this.period = period;
    }

    public LocalDate getPeriod() {
        return period;
    }

    @Override
    public String toString() {
        return "CreateUdiEvent" + "[" +
                "period=" + period +
                "]";
    }
}
