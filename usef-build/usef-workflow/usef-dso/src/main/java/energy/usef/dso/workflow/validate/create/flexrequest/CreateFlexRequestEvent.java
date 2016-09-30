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

package energy.usef.dso.workflow.validate.create.flexrequest;

import java.util.Arrays;

import org.joda.time.LocalDate;

import energy.usef.core.event.ExpirableEvent;

/**
 * Event class used to trigger the 'Create Flew Request' DSO workflow.
 */
public class CreateFlexRequestEvent implements ExpirableEvent {

    private final String congestionPointEntityAddress;
    private final LocalDate period;
    private final Integer[] ptuIndexes;

    /**
     * Specific constructor for the {@link CreateFlexRequestEvent}. Initiliazes the workflow name as well.
     *
     * @param congestionPointEntityAddress {@link String} Entity Address of the related congestion point.
     * @param period                       {@link LocalDate} period for which flex request is created.
     * @param ptuIndexes                   Array of {@link Integer} with the indexes of the PTUs involved in the flex request creation.
     */
    public CreateFlexRequestEvent(String congestionPointEntityAddress, LocalDate period, Integer[] ptuIndexes) {
        this.congestionPointEntityAddress = congestionPointEntityAddress;
        this.period = period;
        this.ptuIndexes = Arrays.copyOf(ptuIndexes, ptuIndexes.length);
    }

    public String getCongestionPointEntityAddress() {
        return congestionPointEntityAddress;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public Integer[] getPtuIndexes() {
        return ptuIndexes;
    }

    @Override
    public String toString() {
        return "CreateFlexRequestEvent" + "[" +
                "congestionPointEntityAddress='" + congestionPointEntityAddress + "'" +
                ", ptuDate=" + period +
                "]";
    }
}
