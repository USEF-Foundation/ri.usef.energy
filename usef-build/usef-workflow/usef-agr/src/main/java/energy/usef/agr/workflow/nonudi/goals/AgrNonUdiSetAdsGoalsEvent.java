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

package energy.usef.agr.workflow.nonudi.goals;

import energy.usef.core.event.ExpirableEvent;
import org.joda.time.LocalDate;

/**
 * Event to trigger the set ADS goals workflow.
 */
public class AgrNonUdiSetAdsGoalsEvent implements ExpirableEvent {

    private LocalDate period;
    private String usefIdentifier;

    public AgrNonUdiSetAdsGoalsEvent(LocalDate period, String usefIdentifier) {
        this.period = period;
        this.usefIdentifier = usefIdentifier;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public String getUsefIdentifier() {
        return usefIdentifier;
    }

    @Override
    public String toString() {
        return "AgrNonUdiSetAdsGoalsEvent" + "[" +
                "period=" + period +
                ", usefIdentifier=" + usefIdentifier +
                "]";
    }
}
