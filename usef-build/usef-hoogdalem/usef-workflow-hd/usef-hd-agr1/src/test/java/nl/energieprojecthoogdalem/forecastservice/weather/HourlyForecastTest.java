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
import org.joda.time.LocalDateTime;
import org.junit.Test;

import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.*;

public class HourlyForecastTest
{
    private HourlyForecast hourlyForecast;

    private static final double SKY = 50D
                                , IRRADIATION = 0.5D
                                , RESULT = ((1D - IRRADIATION) *(100D -SKY)) /100D +IRRADIATION
            ;
    private static final int PTU_DURATION = 15;

    private static final Map<String, String> DATE = new HashMap<>();
    private static final LocalDateTime datetime = new LocalDateTime(2016, 3, 10, 12, 0, 0 );

    @Test
    public void testHourlyForecast() throws Exception
    {
        DATE.put("year", ""+ datetime.getYear());
        DATE.put("mon", "" + datetime.getMonthOfYear());
        DATE.put("mday", "" + datetime.getDayOfMonth());
        DATE.put("hour", "" + datetime.getHourOfDay());

        hourlyForecast = new HourlyForecast(SKY, DATE);

        assertEquals(RESULT, hourlyForecast.getSkyCorrectionFactor(IRRADIATION), 0.02D);
        assertEquals(PtuUtil.getPtuIndex(datetime, PTU_DURATION), hourlyForecast.getPtuIndex(PTU_DURATION));
        assertTrue(datetime.equals(hourlyForecast.getDateTime()));
    }

}