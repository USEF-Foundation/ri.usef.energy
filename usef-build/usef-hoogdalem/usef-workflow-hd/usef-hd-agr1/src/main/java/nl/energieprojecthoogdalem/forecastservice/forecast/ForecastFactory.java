/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.forecastservice.forecast;

import info.usef.agr.dto.*;
import info.usef.core.util.PtuUtil;
import nl.energieprojecthoogdalem.agr.dtos.ForecastType;
import nl.energieprojecthoogdalem.agr.dtos.Proposition;
import nl.energieprojecthoogdalem.configurationservice.AgrConfiguration;
import nl.energieprojecthoogdalem.forecastservice.ForecastService;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import nl.energieprojecthoogdalem.forecastservice.weather.WeatherService;
import nl.energieprojecthoogdalem.util.EANUtil;
import org.joda.time.LocalDate;

import javax.inject.Inject;
import java.math.BigInteger;
import static java.math.BigInteger.ZERO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * creates the forecast data for udis in the usef Hoogdalem implementation
 * */
public class ForecastFactory
{
    @Inject
    private ForecastService forecastService;

    private BigInteger chargeFull, maxCharge;

    /**
     * creates a forecast for each connection using the {@link ForecastService} .retrieveForecast(), retrieveProposition() and retrievePVForecast().
     * if the forecast can't be retrieved, 0 values will be used
     * @return the {@link ConnectionPortfolioDto} list with filled with forecast data
     * @param period the {@link LocalDate} of the forecast
     * @param ptuDuration the duration of one ptu
     * @param connections a list with {@link ConnectionPortfolioDto} to be filled with forecast data
     * */
    public List<ConnectionPortfolioDto> createNdayAheadForecast(LocalDate period, int ptuDuration, List<ConnectionPortfolioDto> connections)
    {
        forecastService.connect();

        connections = setForecasts(period, ptuDuration, connections);

        forecastService.disconnect();
        return connections;
    }

    /**
     * fills all the connections in the list with:
     * -the uncontrolled load in the connection forecast
     * -the pv production in the PV udi (if available)
     * -the potential flex possibilities in the BATTERY udi
     * @return the {@link ConnectionPortfolioDto} list with filled with forecast data
     * @param period the {@link LocalDate} of the forecast
     * @param ptuDuration the duration of one ptu
     * @param connections a list with {@link ConnectionPortfolioDto} to be filled with forecast data
     * */
    private List<ConnectionPortfolioDto> setForecasts(LocalDate period, int ptuDuration, List<ConnectionPortfolioDto> connections)
    {
        int ptuCount = PtuUtil.getNumberOfPtusPerDay(period, ptuDuration);
        Properties prop = AgrConfiguration.getConfig("CREATE_NDAY_AHEAD_FORECAST", period, ptuDuration);
        WeatherService weatherService = new WeatherService(prop);

        maxCharge = new BigInteger(prop.getProperty(AgrConfiguration.BATTERY_CHARGE));
        chargeFull = new BigInteger(prop.getProperty(AgrConfiguration.BATTERY_FULLYCHARGED));

        Map<ForecastType, Map<Integer, Long>> forecasts = new HashMap<>();
        Map<Integer, Double> weatherCorrectionMap = weatherService.getDayCorrection(period, ptuDuration);
        forecasts.put(ForecastType.FORECAST_MAP, forecastService.retrieveForecast(period, ptuDuration));

        connections.forEach(connection ->
        {
            BigInteger chargeTotal = ZERO;
            String homeId = EANUtil.toHomeString(connection.getConnectionEntityAddress());
            Proposition proposition = forecastService.retrieveProposition(homeId);

            if(proposition != null)
            {
                //retrieve pv independent of battery profile
                if(proposition.hasPv())
                {
                    Map<Integer, Long> pvForecast = forecastService.retrievePVForecast(homeId, period, ptuDuration);
                    for(int ptuIndex = 1; ptuIndex <= ptuCount; ptuIndex++)
                    {
                        long correction = (long) Math.floor(pvForecast.get(ptuIndex) * weatherCorrectionMap.get(ptuIndex));
                        pvForecast.put(ptuIndex, correction);
                    }

                    forecasts.put(ForecastType.PVFORECAST_MAP, pvForecast);
                }
                else
                    forecasts.put(ForecastType.PVFORECAST_MAP, defaultMap(ptuCount));

                //NOD battery profile starts charged
                if( ElementType.NOD.equals( determineBatteryProfile( connection.getUdis() )) )
                {
                    chargeTotal = chargeFull;
                }

                for(int ptuIndex = 1; ptuIndex <= ptuCount; ptuIndex++)
                {
                    //set all udi forecasts
                    for (UdiPortfolioDto udi : connection.getUdis())
                    {
                        switch (udi.getProfile())
                        {
                            case ElementType.BATTERY_NOD:
                                chargeTotal = setNODBatteryUdiForecast(ptuIndex, period, udi, forecasts, chargeTotal);
                                break;

                            case ElementType.BATTERY_ZIH:
                                chargeTotal = setZIHBatteryUdiForecast(ptuIndex, period, udi, forecasts, chargeTotal);
                                break;

                            case ElementType.PV:
                                setPvUdiForecast(ptuIndex, period, udi, forecasts.get(ForecastType.PVFORECAST_MAP));
                                break;
                        }
                    }

                    //set consumption of household
                    setConnectionForecast(ptuIndex, period, connection, forecasts.get(ForecastType.FORECAST_MAP));
                }

                forecasts.remove(ForecastType.PVFORECAST_MAP);
            }
            else
            {
                connection = DefaultForecast.getDefaultConnectionPortfolio(period, ptuDuration, connection);
            }
        });

        return connections;
    }

    /**
     * set forecast data for the ZIH BATTERY UDI
     * @param ptuIndex the number of a ptu
     * @param date the {@link LocalDate} of the forecast
     * @param udi the {@link UdiPortfolioDto} with portfolio name "BATTERY_ZIH"
     * @param forecastLists the map with pv and load forecasts
     * @param chargeTotal the value how much the battery is already charged
     * @return the amount of power that the battery has been charged
     * */
    private BigInteger setZIHBatteryUdiForecast(int ptuIndex, LocalDate date, UdiPortfolioDto udi, Map<ForecastType, Map<Integer, Long>> forecastLists, BigInteger chargeTotal)
    {
        PowerContainerDto powerContainerDto = new PowerContainerDto(date, ptuIndex);
        ForecastPowerDataDto batteryForecast = powerContainerDto.getForecast();

        long pv = forecastLists.get(ForecastType.PVFORECAST_MAP).get(ptuIndex)
                , ucl = forecastLists.get(ForecastType.FORECAST_MAP).get(ptuIndex)
                ;

        BigInteger batteryForecastValue = (pv > ucl) ? BigInteger.valueOf(pv - ucl) : ZERO;
        batteryForecastValue = (maxCharge.compareTo(batteryForecastValue) > 0) ? batteryForecastValue : maxCharge;

        batteryForecast.setUncontrolledLoad(ZERO);
        batteryForecast.setAverageProduction(ZERO);

        if(chargeTotal.compareTo(chargeFull) < 0)
        {
            chargeTotal = chargeTotal.add(batteryForecastValue);

            BigInteger delta = chargeTotal.subtract(chargeFull);
            if(delta.signum() > -1 )
                batteryForecastValue = batteryForecastValue.subtract(delta);

            batteryForecast.setAverageConsumption(batteryForecastValue);
            batteryForecast.setPotentialFlexConsumption(ZERO);
            batteryForecast.setPotentialFlexProduction(batteryForecastValue);
        }
        else
        {
            batteryForecast.setAverageConsumption(ZERO);
            batteryForecast.setPotentialFlexConsumption(batteryForecastValue);
            batteryForecast.setPotentialFlexProduction(ZERO);
        }

        udi.getUdiPowerPerDTU().put(ptuIndex, powerContainerDto);
        return chargeTotal;
    }

    /**
     * set forecast data for the NOD BATTERY UDI
     * @param ptuIndex the number of a ptu
     * @param date the {@link LocalDate} of the forecast
     * @param udi the {@link UdiPortfolioDto} with portfolio name "BATTERY_ZIH"
     * @param forecast the map with pv and load forecasts
     * @param dischargeTotal the value how much the battery is already discharged
     * @return the amount of power that the battery has been discharged
     * */
    private BigInteger setNODBatteryUdiForecast(int ptuIndex, LocalDate date, UdiPortfolioDto udi, Map<ForecastType, Map<Integer, Long>> forecast, BigInteger dischargeTotal)
    {
        Map<Integer, Long> uclForecast = forecast.get(ForecastType.FORECAST_MAP);
        Map<Integer, Long> pvForecast = forecast.get(ForecastType.PVFORECAST_MAP);

        PowerContainerDto powerContainerDto = new PowerContainerDto(date, ptuIndex);
        ForecastPowerDataDto batteryForecast = powerContainerDto.getForecast();

        batteryForecast.setUncontrolledLoad(ZERO);
        batteryForecast.setAverageConsumption(ZERO);

        //between 07:00 && 23:00 ( i > 06:45 && i < 23:15
        if(ptuIndex > 28 && ptuIndex < 93)
        {
            Long value = uclForecast.get(ptuIndex) - pvForecast.get(ptuIndex);
            BigInteger batteryForecastValue = value > 0 ? BigInteger.valueOf(value) : ZERO;
            batteryForecastValue = (maxCharge.compareTo(batteryForecastValue) > 0) ? batteryForecastValue : maxCharge;

            if(dischargeTotal.signum() != 1)
            {
                batteryForecast.setAverageProduction(ZERO);
                batteryForecast.setPotentialFlexConsumption(ZERO);
                batteryForecast.setPotentialFlexProduction(batteryForecastValue);
            }
            else
            {
                dischargeTotal = dischargeTotal.subtract(batteryForecastValue);
                if(dischargeTotal.signum() < 0)
                    batteryForecastValue = batteryForecastValue.add(dischargeTotal);

                batteryForecast.setAverageProduction(batteryForecastValue);
                batteryForecast.setPotentialFlexConsumption(batteryForecastValue);
                batteryForecast.setPotentialFlexProduction(ZERO);
            }
        }
        else
        {
            batteryForecast.setAverageProduction(ZERO);
            batteryForecast.setPotentialFlexConsumption(ZERO);
            batteryForecast.setPotentialFlexProduction(ZERO);
        }

        udi.getUdiPowerPerDTU().put(ptuIndex, powerContainerDto);
        return dischargeTotal;
    }

    /**
     * set forecast data for the PV UDI
     * @param ptuIndex the number of a ptu
     * @param date the {@link LocalDate} of the forecast
     * @param udi the {@link UdiPortfolioDto} with portfolio name "PV"
     * @param pvForecast the map with pv forecast values
     * */
    private void setPvUdiForecast(int ptuIndex, LocalDate date, UdiPortfolioDto udi, Map<Integer, Long> pvForecast)
    {
        PowerContainerDto powerContainerDto =  new PowerContainerDto(date, ptuIndex);
        ForecastPowerDataDto forecast = powerContainerDto.getForecast();

        forecast.setUncontrolledLoad(ZERO);
        forecast.setAverageConsumption(ZERO);
        forecast.setPotentialFlexConsumption(ZERO);
        forecast.setPotentialFlexProduction(ZERO);

        forecast.setAverageProduction(BigInteger.valueOf(pvForecast.get(ptuIndex)));

        udi.getUdiPowerPerDTU().put(ptuIndex, powerContainerDto);
    }

    /**
     * set the uncontrolled load  value of a connection {@link ForecastPowerDataDto} in the {@link PowerContainerDto}
     * @param ptuIndex the ptu index of the forecast value
     * @param date the {@link LocalDate} of the forecast
     * @param connection the connection to set the forecast data to
     * @param uclForecast the uncontrolled load forecast map
     * */
    private void setConnectionForecast(int ptuIndex, LocalDate date, ConnectionPortfolioDto connection, Map<Integer, Long> uclForecast)
    {
        PowerContainerDto connectionPower = new PowerContainerDto(date, ptuIndex);
        ForecastPowerDataDto connectionForecast =  connectionPower.getForecast();

        connectionForecast.setUncontrolledLoad(BigInteger.valueOf(uclForecast.get(ptuIndex)));
        connectionForecast.setAverageConsumption(ZERO);
        connectionForecast.setAverageProduction(ZERO);
        connectionForecast.setPotentialFlexProduction(ZERO);
        connectionForecast.setPotentialFlexConsumption(ZERO);

        connection.getConnectionPowerPerPTU().put(ptuIndex, connectionPower);

    }

    /**
     * checks if the udi profile contains the ElementType.BATTERY_ZIH or the ElementType.BATTERY_NOD
     * @param udis the udis for each connection
     * @return ElementType.ZIH if udi profile with ElementType.BATTERY_ZIH is found, ElementType.NOD  if udi profile with ElementType.BATTERY_NOD is found otherwise null
     * */
    private String determineBatteryProfile(List<UdiPortfolioDto> udis)
    {
        String proposition = null;

        for(UdiPortfolioDto udi : udis)
        {
            if(ElementType.BATTERY_ZIH.equals(udi.getProfile()))
            {
                proposition = ElementType.ZIH;
                break;
            }
            else if(ElementType.BATTERY_NOD.equals(udi.getProfile()))
            {
                proposition = ElementType.NOD;
                break;
            }

        }
        return proposition;
    }

    private static Map<Integer, Long> defaultMap(int ptuCount)
    {
        return IntStream.rangeClosed(1, ptuCount)
                        .boxed()
                        .collect(Collectors.toMap(Function.identity(), idx -> 0L));
    }
}
