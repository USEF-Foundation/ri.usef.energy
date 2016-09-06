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

package energy.usef.brp.workflow.settlement.send;

/**
 * Event class which can be used to trigger the sending of Settlement messages.
 */
public class SendSettlementMessageEvent {

    private final Integer year;
    private final Integer month;

    /**
     * Specific constructor, taking the year and month of the year number in parameter.
     * 
     * @param year {@link Integer} non-null year.
     * @param month {@link Integer} non-null month of the year number (e.g. 2 for February, 10 for October).
     */
    public SendSettlementMessageEvent(int year, int month) {
        this.month = month;
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getYear() {
        return year;
    }

    @Override
    public String toString() {
        return "SendSettlementMessageEvent" + "[" +
                "year=" + year +
                ", month=" + month +
                "]";
    }
}
