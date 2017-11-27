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

package nl.energieprojecthoogdalem.forecastservice.weather;

import info.usef.core.util.PtuUtil;

import java.util.Map;

import org.joda.time.LocalDateTime;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * POJO of JSON:
 * {
 *      "FCTTIME":
 *      {
 *      "year": "2016"
 *      ,"mon": "3"
 *      ,"mday": "11"
 *      ,"hour": "11"
 *      ,... IGNORED
 *     }
 *   ,"sky": "5"
 *   ,... IGNORED
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HourlyForecast
{
    private final double sky;
    private final LocalDateTime dateTime;

    @JsonCreator
    public HourlyForecast
    (
        @JsonProperty("sky") double sky
        ,@JsonProperty("FCTTIME") Map<String, String> date
    )
    {
        int year = Integer.parseInt(date.get("year"))
            ,monthOfYear = Integer.parseInt(date.get("mon"))
            ,dayOfMonth = Integer.parseInt(date.get("mday"))
            ,hourOfDay = Integer.parseInt(date.get("hour"))
            ;

        dateTime = new LocalDateTime(year, monthOfYear, dayOfMonth, hourOfDay, 0, 0, 0);
        this.sky = sky;
    }

    public double getSkyCorrectionFactor(double irradiation)
    {
        return ((1D -irradiation) *(100D -sky)) /100D +irradiation;
    }

    public LocalDateTime getDateTime()
    {
        return dateTime;
    }

    public int getPtuIndex(int ptuDuration)
    {
        return PtuUtil.getPtuIndex(dateTime, ptuDuration);
    }

}
