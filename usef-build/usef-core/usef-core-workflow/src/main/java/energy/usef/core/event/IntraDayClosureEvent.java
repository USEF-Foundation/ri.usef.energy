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

package energy.usef.core.event;

import energy.usef.core.util.DateTimeUtil;

import org.joda.time.LocalDate;

/**
 * Event which will trigger a process to move the PTUs on a specified day with a specified PTU index in a state
 * <code>Intra_Day_Closed</code>.
 */
public class IntraDayClosureEvent {

    private final LocalDate period;
    private final Integer ptuIndex;

    /**
     * Specific constructor which receives a date and a PTU index.
     * 
     * @param period {@link LocalDate} day of the PTU that will be <code>Intra_Day_Closed</code>. If <code>null</code>, the date
     *            will be <code>TODAY</code>.
     * @param ptuIndex {@link Integer} PTU index of the PTU which will be <code>Intra_Day_Closed</code>. This value cannot be
     *            <code>null</code>.
     */
    public IntraDayClosureEvent(LocalDate period, Integer ptuIndex) {
        if (ptuIndex == null) {
            throw new IllegalArgumentException("PTU index cannot be null");
        }
        this.period = period == null ? DateTimeUtil.getCurrentDate() : period;
        this.ptuIndex = ptuIndex;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public Integer getPtuIndex() {
        return ptuIndex;
    }

    @Override
    public String toString() {
        return "IntraDayClosureEvent" + "[" +
                "period=" + period +
                ", ptuIndex=" + ptuIndex +
                "]";
    }
}
