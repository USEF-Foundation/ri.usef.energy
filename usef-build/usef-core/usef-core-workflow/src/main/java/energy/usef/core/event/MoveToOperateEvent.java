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
 * Event which will trigger a process to move the PTUs on a specified day with a specified PTU index in a state <code>Operate</code>
 * and move the previous ones in a state <code>Pending_Settlement</code>.
 */
public class MoveToOperateEvent {

    private final LocalDate period;
    private final Integer ptuIndex;

    /**
     * Specific constructor which requires the date and index of the PTUs that move to <code>Operate</code>.
     * 
     * Date and index of the PTUs that will move to <code>Pending_Settlement</code> will be derived.
     * 
     * @param period {@link LocalDate} date of the PTUs that move to <code>Operate</code>. Can be <code>null</code> (evaluated to
     *            <code>TODAY</code>).
     * @param ptuIndex {@link Integer} index of the PTUs that move to <code>Operate</code>. Cannot be null.
     */
    public MoveToOperateEvent(LocalDate period, Integer ptuIndex) {
        if (ptuIndex == null) {
            throw new IllegalArgumentException("PTU index cannot be null.");
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
        return "MoveToOperateEvent" + "[" +
                "period=" + period +
                ", ptuIndex=" + ptuIndex +
                "]";
    }
}
