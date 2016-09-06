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

package energy.usef.agr.workflow.nonudi.dto;

import java.math.BigDecimal;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

/**
 * DTO class for the Profile of the congestion management.
 */
public class CongestionManagementProfileDto {
    @JsonProperty("time_interval")
    private String timeInterval;

    @JsonProperty("min_demand_watt")
    private BigDecimal minDemandWatt;

    @JsonProperty("max_demand_watt")
    private BigDecimal maxDemandWatt;

    public String getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(String timeInterval) {
        this.timeInterval = timeInterval;
    }

    @JsonIgnore
    public LocalDateTime getIntervalStart() {
        return new LocalDateTime(new DateTime(this.timeInterval.split("/")[0]));
    }

    @JsonIgnore
    public LocalDateTime getIntervalEnd() {
        return new LocalDateTime(new DateTime(this.timeInterval.split("/")[1]));
    }

    public BigDecimal getMinDemandWatt() {
        return minDemandWatt;
    }

    public void setMinDemandWatt(BigDecimal minDemandWatt) {
        this.minDemandWatt = minDemandWatt;
    }

    public BigDecimal getMaxDemandWatt() {
        return maxDemandWatt;
    }

    public void setMaxDemandWatt(BigDecimal maxDemandWatt) {
        this.maxDemandWatt = maxDemandWatt;
    }

    @Override
    public String toString() {
        return "CongestionManagementProfileDto" + "[" +
                "timeInterval='" + timeInterval + "'" +
                ", minDemandWatt=" + minDemandWatt +
                ", maxDemandWatt=" + maxDemandWatt +
                "]";
    }
}
