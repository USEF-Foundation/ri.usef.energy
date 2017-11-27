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
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.core.util.PtuUtil;
import org.joda.time.LocalDate;

import java.math.BigInteger;
/**
 * default value class for USEF forecasts per connection
 * */
public final class DefaultForecast
{

    /**
     * returns an {@link ConnectionPortfolioDto} for a connection
     * with the connection and udi(s) forecasts set to 0
     * @param date the date of the forecast
     * @param ptuDuration the duration of one ptu
     * @param connection the connection to add zero 'ed forecasts for
     * @return a {@link ConnectionPortfolioDto} with forecasts containing 0 values
     * */
    public static ConnectionPortfolioDto getDefaultConnectionPortfolio(LocalDate date, int ptuDuration, ConnectionPortfolioDto connection)
    {
        int ptuCount = PtuUtil.getNumberOfPtusPerDay(date, ptuDuration);

        for (int i = 1; i <= ptuCount; i++)
        {
            PowerContainerDto powerContainerDto = buildPowerContainerDto(date, i);

            connection.getConnectionPowerPerPTU().put(i, powerContainerDto);
            for (UdiPortfolioDto udi : connection.getUdis())
                udi.getUdiPowerPerDTU().put(i, powerContainerDto);
        }

        return connection;
    }

    /**
     * returns an {@link PowerContainerDto} with everything set to 0
     * @param date the date of the forecast
     * @param idx the ptu index of the forecast
     * @return a {@link PowerContainerDto} containing 0 values
     * */
    private static PowerContainerDto buildPowerContainerDto(LocalDate date, int idx)
    {
        PowerContainerDto powerContainerDto = new PowerContainerDto(date, idx);
        powerContainerDto.getForecast().setUncontrolledLoad(BigInteger.ZERO);

        powerContainerDto.getForecast().setAverageConsumption(BigInteger.ZERO);
        powerContainerDto.getForecast().setAverageProduction(BigInteger.ZERO);

        powerContainerDto.getForecast().setPotentialFlexConsumption(BigInteger.ZERO);
        powerContainerDto.getForecast().setPotentialFlexProduction(BigInteger.ZERO);

        return powerContainerDto;
    }
}