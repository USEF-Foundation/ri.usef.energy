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

package nl.energieprojecthoogdalem.forecastservice.forecast;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.ForecastPowerDataDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DefaultForecastTest
{
    private static String EAN               = "ean.800000000000000032"
                        , BATTERY_PROFILE   = "BATTERY"
                        , PV_PROFILE        = "PV"
                        , BATTERY_ENDPOINT  = "MZ29EBX000/usef/" + BATTERY_PROFILE
                        , PV_ENDPOINT       = "MZ29EBX000/usef/" + PV_PROFILE
                        ;

    private static int PTU_DURATION = 15
                     , PTU_COUNT = 96
                    ;

    private LocalDate date = new LocalDate(2016, 2, 25);

    private ConnectionPortfolioDto connection;

    @Before
    public void init() throws Exception
    {

        connection = new ConnectionPortfolioDto(EAN);
        connection.getUdis().addAll(buildUdis());
    }

    @Test
    public void testGetDefaultForecast() throws Exception
    {
        connection = DefaultForecast.getDefaultConnectionPortfolio(date, PTU_DURATION, connection );

        assertEquals(EAN, connection.getConnectionEntityAddress());

        Map<Integer, PowerContainerDto> powerPerPTU = connection.getConnectionPowerPerPTU();

        assertEquals(PTU_COUNT, powerPerPTU.size());

        for(UdiPortfolioDto udi : connection.getUdis())
            assertEquals(PTU_COUNT, udi.getUdiPowerPerDTU().size());

        for(int idx = 1; idx <= PTU_COUNT; idx++)
        {
            assertEquals(Integer.valueOf(idx), powerPerPTU.get(idx).getTimeIndex() );
            validateForecast(powerPerPTU.get(idx).getForecast());
            for(UdiPortfolioDto udi : connection.getUdis())
            {
                assertEquals(Integer.valueOf(idx), udi.getUdiPowerPerDTU().get(idx).getTimeIndex() );
                validateForecast( udi.getUdiPowerPerDTU().get(idx).getForecast());
            }
        }
    }

    private void validateForecast(ForecastPowerDataDto forecastPowerDataDto)
    {
        assertEquals(BigInteger.ZERO, forecastPowerDataDto.getUncontrolledLoad());

        assertEquals(BigInteger.ZERO, forecastPowerDataDto.getAverageConsumption());
        assertEquals(BigInteger.ZERO, forecastPowerDataDto.getAverageProduction());

        assertEquals(BigInteger.ZERO, forecastPowerDataDto.getPotentialFlexConsumption());
        assertEquals(BigInteger.ZERO, forecastPowerDataDto.getPotentialFlexProduction());

    }

    private List<UdiPortfolioDto> buildUdis()
    {
        List<UdiPortfolioDto> udis = new ArrayList<>();
        udis.add(new UdiPortfolioDto(PV_ENDPOINT, PTU_DURATION, PV_PROFILE) );
        udis.add(new UdiPortfolioDto(BATTERY_ENDPOINT, PTU_DURATION, BATTERY_PROFILE) );

        return udis;
    }
}