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
package nl.energieprojecthoogdalem.forecastservice;

import nl.energieprojecthoogdalem.agr.dtos.Proposition;
import nl.energieprojecthoogdalem.forecastservice.weather.WeatherService;
import nl.energieprojecthoogdalem.util.EANUtil;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.sql.Date;
import java.util.Map;
import java.util.Properties;

/*real database test to check results*/

public class RealDatabaseConnectionTest {

    //BROKEN TEST
    private Properties prop;
    private WeatherService weatherService;
    private ForecastService forecastService;
    private boolean isConnected;

    private static int PTU_DURATION = 15;
    private static LocalDate PERIOD = new LocalDate().plusDays(1);

    @Before
    public void setup()
    {
        weatherService = new WeatherService(prop);
        forecastService = new ForecastService();
        Whitebox.setInternalState(forecastService, "weatherService", weatherService);
        isConnected = forecastService.connect();
    }

    @After
    public void teardown()
    {
        if(isConnected)
        {
            forecastService.disconnect();
            isConnected = false;
        }
    }

    @Test
    public void retrieveProposition() throws Exception {
        if(isConnected)
        {
            Proposition result = forecastService.retrieveProposition("031");
            System.out.println("hasBattery: " + result.hasBattery());
            System.out.println("hasPV: " + result.hasPv());
            forecastService.disconnect();
        }
        else
            System.err.println("unable to connect");
    }

    @Test
    public void retrieveForecast() throws Exception {

        if (isConnected)
        {
            Map<Integer, Long> result = forecastService.retrieveForecast(PERIOD, PTU_DURATION);
            readMap(result);
        }
    }

    @Test
    public void retrievePVForecast() throws Exception
    {
        String ean = EANUtil.EAN_PREFIX + "031";

        if(isConnected)
        {
            Map<Integer ,Long> result = forecastService.retrievePVForecast(EANUtil.toHomeString(ean) ,PERIOD, PTU_DURATION);
            readMap(result);
        }

    }

    private void readMap(Map<Integer, Long> map )
    {
        System.out.println("size: " + map.size());

        int found = 0;
        for (int idx = 1; idx <= 96; idx++)
        {
            if(found == 5)
                break;

            Long val = map.get(idx);
            val =  (val != null) ? val : 0L;
            if(val != 0L)
            {
                System.out.println( "ptuidx " + idx + " value " + map.get(idx));
                found++;
            }
        }

    }
}