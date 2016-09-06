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

package energy.usef.dso.model;

import energy.usef.core.model.Document;
import energy.usef.core.util.DateTimeUtil;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity class {@link NonAggregatorForecast}: This class is an implementation of {@link Document} class for a
 * NonAggregatorForecast.
 *
 */
@Entity
@Table(name = "NON_AGGREGATOR_FORECAST")
public class NonAggregatorForecast extends Document {

    @Column(name = "POWER", nullable = false)
    private Long power;

    @Column(name = "MAX_LOAD", nullable = false)
    private Long maxLoad;

    @Column(name = "CREATION_DATE", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate = DateTimeUtil.getCurrentDateTime().toDateTime().toDate();

    /**
     * Returns the power.
     * 
     * @return the power in W.
     */
    public Long getPower() {
        return power;
    }

    /**
     * Sets the power.
     * 
     * @param power the power in W.
     */
    public void setPower(Long power) {
        this.power = power;
    }

    public Long getMaxLoad() {
        return maxLoad;
    }

    public void setMaxLoad(Long maxLoad) {
        this.maxLoad = maxLoad;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public String toString() {
        return "NonAggregatorForecast" + "[" +
                "power=" + power +
                ", maxLoad=" + maxLoad +
                ", creationDate=" + creationDate +
                "]";
    }
}
