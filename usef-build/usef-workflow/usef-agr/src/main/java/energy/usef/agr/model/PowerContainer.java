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

package energy.usef.agr.model;

import java.util.Date;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;

/**
 * This object represents all power data within usef.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn( name = "TYPE")
@Table(name = "POWER_CONTAINER")
public class PowerContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PERIOD")
    @Temporal(TemporalType.DATE)
    private Date period;

    @Column(name = "TIME_INDEX")
    private Integer timeIndex;

    @AttributeOverrides({
            @AttributeOverride(name = "uncontrolledLoad", column = @Column(name = "PROFILE_UNCONTROLLED_LOAD", precision = 18, scale = 0)),
            @AttributeOverride(name = "averageConsumption", column = @Column(name = "PROFILE_AVERAGE_CONSUMPTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "averageProduction", column = @Column(name = "PROFILE_AVERAGE_PRODUCTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "potentialFlexConsumption", column = @Column(name = "PROFILE_POTENTIAL_FLEX_CONSUMPTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "potentialFlexProduction", column = @Column(name = "PROFILE_POTENTIAL_FLEX_PRODUCTION", precision = 18, scale = 0))
    })
    private PowerData profile;

    /**
     * This should be filled with metered observed data by the party.
     */
    @AttributeOverrides({
            @AttributeOverride(name = "uncontrolledLoad", column = @Column(name = "FORECAST_UNCONTROLLED_LOAD", precision = 18, scale = 0)),
            @AttributeOverride(name = "averageConsumption", column = @Column(name = "FORECAST_AVERAGE_CONSUMPTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "averageProduction", column = @Column(name = "FORECAST_AVERAGE_PRODUCTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "potentialFlexConsumption", column = @Column(name = "FORECAST_POTENTIAL_FLEX_CONSUMPTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "potentialFlexProduction", column = @Column(name = "FORECAST_POTENTIAL_FLEX_PRODUCTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "allocatedFlexConsumption", column = @Column(name = "FORECAST_ALLOCATED_FLEX_CONSUMPTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "allocatedFlexProduction", column = @Column(name = "FORECAST_ALLOCATED_FLEX_PRODUCTION", precision = 18, scale = 0))
    })
    private ForecastPowerData forecast;

    @AttributeOverrides({
            @AttributeOverride(name = "uncontrolledLoad", column = @Column(name = "OBSERVED_UNCONTROLLED_LOAD", precision = 18, scale = 0)),
            @AttributeOverride(name = "averageConsumption", column = @Column(name = "OBSERVED_AVERAGE_CONSUMPTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "averageProduction", column = @Column(name = "OBSERVED_AVERAGE_PRODUCTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "potentialFlexConsumption", column = @Column(name = "OBSERVED_POTENTIAL_FLEX_CONSUMPTION", precision = 18, scale = 0)),
            @AttributeOverride(name = "potentialFlexProduction", column = @Column(name = "OBSERVED_POTENTIAL_FLEX_PRODUCTION", precision = 18, scale = 0))
    })
    private PowerData observed;

    /**
     * Default constructor.
     */
    public PowerContainer() {
        profile = new PowerData();
        forecast = new ForecastPowerData();
        observed = new PowerData();
    }

    /**
     * Initial Constructor.
     *
     * @param period
     * @param timeIndex
     */
    public PowerContainer(LocalDate period, Integer timeIndex) {
        this();
        setPeriod(period);
        this.timeIndex = timeIndex;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getPeriod() {
        if (period == null) {
            return null;
        }
        return new LocalDate(period);
    }

    public void setPeriod(LocalDate period) {
        if (period == null) {
            this.period = null;
        } else {
            this.period = period.toDateMidnight().toDate();
        }
    }

    public Integer getTimeIndex() {
        return timeIndex;
    }

    public void setTimeIndex(Integer timeIndex) {
        this.timeIndex = timeIndex;
    }

    public PowerData getProfile() {
        return profile;
    }

    public void setProfile(PowerData profile) {
        this.profile = profile;
    }

    public ForecastPowerData getForecast() {
        return forecast;
    }

    public void setForecast(ForecastPowerData forecast) {
        this.forecast = forecast;
    }

    public PowerData getObserved() {
        return observed;
    }

    public void setObserved(PowerData observed) {
        this.observed = observed;
    }

    @Override
    public String toString() {
        return "PowerContainer" + "[" +
                "id=" + id +
                ", period=" + period +
                ", timeIndex=" + timeIndex +
                "]";
    }
}
