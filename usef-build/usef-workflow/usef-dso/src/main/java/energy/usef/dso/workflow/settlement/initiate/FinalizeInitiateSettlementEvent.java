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

package energy.usef.dso.workflow.settlement.initiate;

import energy.usef.core.data.xml.bean.message.MeterData;
import energy.usef.core.data.xml.bean.message.MeterDataSet;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

/**
 * Event implementation for the finalization of the Initiate Settlement workflow.
 */
public class FinalizeInitiateSettlementEvent {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private List<MeterDataSet> meterDataSets;

    /**
     * Default constructor.
     *
     * @param startDate
     * @param endDate
     * @param meterDataSets
     */
    public FinalizeInitiateSettlementEvent(LocalDate startDate, LocalDate endDate, List<MeterDataSet> meterDataSets) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.meterDataSets = meterDataSets;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Get List of {@link MeterData}.
     *
     * @return the meterdataList
     */
    public List<MeterDataSet> getMeterDataPerCongestionPoint() {
        if (meterDataSets == null) {
            meterDataSets = new ArrayList<>();
        }
        return meterDataSets;
    }

    @Override
    public String toString() {
        return "FinalizeInitiateSettlementEvent" + "[" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", #meterDataSets=" + getMeterDataPerCongestionPoint().size() +
                "]";
    }
}
