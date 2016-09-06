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

package energy.usef.agr.dto;

import java.math.BigInteger;

import org.joda.time.LocalDate;

/**
 * This DTO object represents all power data within usef.
 */
public class PowerContainerDto {

    private LocalDate period;

    private Integer timeIndex;

    private PowerDataDto profile;

    private ForecastPowerDataDto forecast;

    private PowerDataDto observed;

    /**
     * Initial Constructor.
     *
     * @param period
     * @param timeIndex
     */
    public PowerContainerDto(LocalDate period, Integer timeIndex) {
        this.period = period;
        this.timeIndex = timeIndex;
    }

    public LocalDate getPeriod() {
        if (period == null) {
            return null;
        }
        return new LocalDate(period);
    }

    public void setPeriod(LocalDate period) {
        this.period = period;
    }

    public Integer getTimeIndex() {
        return timeIndex;
    }

    public void setTimeIndex(Integer timeIndex) {
        this.timeIndex = timeIndex;
    }

    public PowerDataDto getProfile() {
        if (profile == null) {
            profile = new PowerDataDto();
        }
        return profile;
    }

    public void setProfile(PowerDataDto profile) {
        this.profile = profile;
    }

    public ForecastPowerDataDto getForecast() {
        if (forecast == null) {
            forecast = new ForecastPowerDataDto();
        }
        return forecast;
    }

    public void setForecast(ForecastPowerDataDto forecast) {
        this.forecast = forecast;
    }

    public PowerDataDto getObserved() {
        if (observed == null) {
            observed = new PowerDataDto();
        }
        return observed;
    }

    public void setObserved(PowerDataDto observed) {
        this.observed = observed;
    }

    /**
     * Returns the most accurate uncontrolled load in the following priority:
     * 1. Observed
     * 2. Forecast
     * 3. Profile
     * 4. 0W
     *
     * @return (@link BigInteger) containing the most accurate uncontrolled load.
     */
    public BigInteger getMostAccurateUncontrolledLoad() {
        if (getObserved().getUncontrolledLoad() != null) {
            return getObserved().getUncontrolledLoad();
        } else if (getForecast().getUncontrolledLoad() != null) {
            return getForecast().getUncontrolledLoad();
        } else  {
            return BigInteger.ZERO;
        }
    }

    /**
     * Returns the most accurate average consumption in the following priority:
     * 1. Observed
     * 2. Forecast
     * 3. Profile
     * 4. 0W
     *
     * @return (@link BigInteger) containing the most accurate average consumption.
     */
    public BigInteger getMostAccurateAverageConsumption() {
        if (getObserved().getAverageConsumption() != null) {
            return getObserved().getAverageConsumption();
        } else if (getForecast().getAverageConsumption() != null) {
            return getForecast().getAverageConsumption();
        } else {
            return BigInteger.ZERO;
        }
    }

    /**
     * Returns the most accurate average production in the following priority:
     * 1. Observed
     * 2. Forecast
     * 3. Profile
     * 4. 0W
     *
     * @return (@link BigInteger) containing the most accurate average production.
     */
    public BigInteger getMostAccurateAverageProduction() {
        if (getObserved().getAverageProduction() != null) {
            return getObserved().getAverageProduction();
        } else if (getForecast().getAverageProduction() != null) {
            return getForecast().getAverageProduction();
        } else {
            return BigInteger.ZERO;
        }
    }

    @Override
    public String toString() {
        return "PowerContainerDto" + "[" +
                "period=" + period +
                ", timeIndex=" + timeIndex +
                ", profile=" + profile +
                ", forecast=" + forecast +
                ", observed=" + observed +
                "]";
    }
}
