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

import org.joda.time.Days;
import org.joda.time.LocalDate;

/**
 * Event which will trigger a process to move the PTUs of the specified day in a state <code>Day_Ahead_Closed</code>.
 */
public class DayAheadClosureEvent implements ExpirableEvent{

    private final LocalDate period;

    /**
     * Default constructor. Set the period of closure to <code>TODAY+1</code>.
     */
    public DayAheadClosureEvent() {
        this.period = DateTimeUtil.getCurrentDate().plus(Days.ONE);
    }

    /**
     * Specific constructor which can receive any {@link LocalDate} defining the period of closure. If the date is <code>null</code>
     * , period will be set to <code>TODAY+1</code>.
     * 
     * @param period {@link LocalDate}.
     */
    public DayAheadClosureEvent(LocalDate period) {
        this.period = period == null ? DateTimeUtil.getCurrentDate().plus(Days.ONE) : period;
    }

    public LocalDate getPeriod() {
        return period;
    }

    @Override
    public String toString() {
        return "DayAheadClosureEvent" + "[" +
                "period=" + period +
                "]";
    }
}
