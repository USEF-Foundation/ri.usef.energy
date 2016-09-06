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

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.ForecastPowerDataDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import nl.energieprojecthoogdalem.agr.dtos.Proposition;
import nl.energieprojecthoogdalem.configurationservice.AgrConfiguration;
import nl.energieprojecthoogdalem.forecastservice.ForecastService;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import nl.energieprojecthoogdalem.forecastservice.weather.WeatherService;
import nl.energieprojecthoogdalem.util.EANUtil;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigInteger;
import static java.math.BigInteger.ZERO;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ForecastFactory.class, AgrConfiguration.class})
public class ForecastFactoryTest
{
    private ForecastFactory forecastFactory;

    private static int PTU_DURATION = 15
            , PTU_COUNT = 96
            ;

    private static double correction = 0.8D;

    private static long forecastValue = 800L
            , pvForecastValue = 2000L
            , correctionForecastValue = (long) Math.ceil(pvForecastValue * correction)
            ;

    private static final String EAN_PREFIX = "ean.80000000000000003"
            , BATTERY_PROFILE   = "BATTERY"
            , PV_PROFILE        = "PV"
            , BATTERY_ENDPOINT  = "MZ29EBX000/usef/" + BATTERY_PROFILE
            , PV_ENDPOINT       = "MZ29EBX000/usef/" + PV_PROFILE
            ;

    private static final BigInteger chargeFull = BigInteger.valueOf(1650 *60 /PTU_DURATION)
                                    ,maxCharge = BigInteger.valueOf(750);

    private static final Proposition pv = new Proposition("y","y"), noPv = new Proposition("n","y");

    private static final LocalDate date = new LocalDate(2016, 2, 25);

    private Properties prop;

    @Mock
    private ForecastService forecastService;

    @Mock
    private WeatherService weatherService;

    @Before
    public void init() throws Exception
    {
        setForecastProperties();

        PowerMockito.mockStatic(AgrConfiguration.class);
        PowerMockito.when(AgrConfiguration.getConfig(Matchers.anyString(), Matchers.any(LocalDate.class), Matchers.anyInt())).thenReturn(prop);
        PowerMockito.whenNew(WeatherService.class).withAnyArguments().thenReturn(weatherService);

        forecastFactory = new ForecastFactory();
        Whitebox.setInternalState(forecastFactory, "forecastService", forecastService);
    }

    /**
     * connection portfolios
     * 1 ZIH
     * 2 NOD
     * 3 NOD + PV
     * */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateForecastsConnected() throws Exception
    {
        Mockito.when(weatherService.getDayCorrection(Matchers.any(LocalDate.class), Matchers.anyInt())).thenReturn(buildCorrectionMap(correction), buildCorrectionMap(correction), buildCorrectionMap(correction));

        Mockito.when(forecastService.retrieveForecast(Matchers.any(LocalDate.class), Matchers.anyInt()))
                    .thenReturn(generateMap(forecastValue), generateMap(forecastValue), generateMap(forecastValue));

        Mockito.when(forecastService.retrievePVForecast(Matchers.anyString(), Matchers.any(LocalDate.class), Matchers.anyInt()))
                .thenReturn(generateMap(pvForecastValue), generateMap(pvForecastValue), generateMap(pvForecastValue));

        Mockito.when(forecastService.retrieveProposition(Matchers.eq("031"))).thenReturn(pv);
        Mockito.when(forecastService.retrieveProposition(Matchers.eq("032"))).thenReturn(noPv);
        Mockito.when(forecastService.retrieveProposition(Matchers.eq("033"))).thenReturn(pv);

        Map<Integer, ConnectionPortfolioDto> result = forecastFactory.createNdayAheadForecast(date, PTU_DURATION, buildConnections())
                                                                     .stream()
                                                                     .collect(Collectors.toMap(con -> EANUtil.toHomeInt(con.getConnectionEntityAddress()), Function.identity()));

        result.forEach((i, connection) ->
        {
            BigInteger chargeTotal;
            BigInteger batteryValue =  BigInteger.valueOf(forecastValue);
            List<UdiPortfolioDto> udis = connection.getUdis();

            assertEquals(PTU_COUNT, connection.getConnectionPowerPerPTU().size());

            for(UdiPortfolioDto udi : udis)
                assertEquals(PTU_COUNT, udi.getUdiPowerPerDTU().size());

            switch(i)
            {
                case 31:
                    chargeTotal = ZERO;
                    break;

                case 33:
                    long value = forecastValue - correctionForecastValue;
                    batteryValue = value > 0 ? BigInteger.valueOf(value) : ZERO;
                    chargeTotal = chargeFull;
                    break;

                default:
                    chargeTotal = chargeFull;
                    break;

            }

            for(int ptuIdx = 1; ptuIdx <= PTU_COUNT; ptuIdx++)
            {
                PowerContainerDto container = connection.getConnectionPowerPerPTU().get(ptuIdx);

                assertEquals(Integer.valueOf(ptuIdx), container.getTimeIndex() );
                validateConnectionForecast(container.getForecast());

                for(UdiPortfolioDto udi : connection.getUdis())
                {
                    container = udi.getUdiPowerPerDTU().get(ptuIdx);
                    assertEquals(Integer.valueOf(ptuIdx), container.getTimeIndex() );

                    switch (udi.getProfile())
                    {
                        case PV_PROFILE:
                            validatePvForecast(container.getForecast());
                            break;

                        case ElementType.BATTERY_ZIH:
                            chargeTotal = validateZIHBatteryForecast(container.getForecast(), chargeTotal);
                            break;

                        case ElementType.BATTERY_NOD:
                            chargeTotal = validateNODBatteryForecast(container.getForecast(), ptuIdx, chargeTotal, batteryValue);
                            break;
                    }
                }
            }

        });
    }

    @Test
    public void testCreateForecastsDisconnected() throws Exception
    {
        Mockito.when(weatherService.getDayCorrection(Matchers.any(LocalDate.class), Matchers.anyInt())).thenReturn(buildCorrectionMap(0D));

        Mockito.when(forecastService.retrieveForecast(Matchers.any(LocalDate.class), Matchers.anyInt()))
                .thenReturn(generateMap(0L));
        Mockito.when(forecastService.retrievePVForecast(Matchers.anyString(), Matchers.any(LocalDate.class), Matchers.anyInt()))
                .thenReturn(generateMap(0L));

        Mockito.when(forecastService.retrieveProposition(Matchers.anyString())).thenReturn(null);

        List<ConnectionPortfolioDto> result = forecastFactory.createNdayAheadForecast(date, PTU_DURATION, buildConnections());

        for (ConnectionPortfolioDto connection : result)
        {
            assertEquals(PTU_COUNT, connection.getConnectionPowerPerPTU().size());

            for (UdiPortfolioDto udi : connection.getUdis())
                assertEquals(PTU_COUNT, udi.getUdiPowerPerDTU().size());

            for (int j = 1; j <= PTU_COUNT; j++)
            {
                PowerContainerDto powerData = connection.getConnectionPowerPerPTU().get(j);
                assertEquals(Integer.valueOf(j), powerData.getTimeIndex());
                validateEmptyForecasts(powerData.getForecast());

                for (UdiPortfolioDto udi : connection.getUdis())
                {
                    powerData = udi.getUdiPowerPerDTU().get(j);
                    assertEquals(Integer.valueOf(j), powerData.getTimeIndex());
                    validateEmptyForecasts(powerData.getForecast());
                }
            }
        }
    }

    //verification
    private void validateEmptyForecasts(ForecastPowerDataDto forecastPowerDataDto)
    {
        assertEquals(ZERO, forecastPowerDataDto.getUncontrolledLoad());
        assertEquals(ZERO, forecastPowerDataDto.getAverageConsumption());
        assertEquals(ZERO, forecastPowerDataDto.getAverageProduction());
        assertEquals(ZERO, forecastPowerDataDto.getPotentialFlexConsumption());
        assertEquals(ZERO, forecastPowerDataDto.getPotentialFlexProduction());

        //System.out.println("validated empty forecast");
    }

    private void validateConnectionForecast(ForecastPowerDataDto forecast)
    {
        assertEquals(BigInteger.valueOf(forecastValue), forecast.getUncontrolledLoad());
        assertEquals(ZERO, forecast.getAverageProduction());
        assertEquals(ZERO, forecast.getAverageConsumption());
        assertEquals(ZERO, forecast.getPotentialFlexConsumption());
        assertEquals(ZERO, forecast.getPotentialFlexProduction());

        //System.out.println("validated connection forecast");
    }

    private void validatePvForecast(ForecastPowerDataDto forecast)
    {
        assertEquals(ZERO, forecast.getUncontrolledLoad());
        assertEquals(BigInteger.valueOf(correctionForecastValue), forecast.getAverageProduction());
        assertEquals(ZERO, forecast.getAverageConsumption());
        assertEquals(ZERO, forecast.getPotentialFlexConsumption());
        assertEquals(ZERO, forecast.getPotentialFlexProduction());
        //System.out.println("validated battery forecast");
    }

    private BigInteger validateNODBatteryForecast(ForecastPowerDataDto forecast, int ptuIdx, BigInteger chargeTotal, BigInteger batteryValue)
    {
        assertEquals(ZERO, forecast.getUncontrolledLoad());
        assertEquals(ZERO, forecast.getAverageConsumption());


        batteryValue = (maxCharge.compareTo(batteryValue) == 1) ? batteryValue : maxCharge;

        if(ptuIdx > 28 && ptuIdx < 93)
        {
            if(chargeTotal.signum() < 1)
            {
                assertEquals(ZERO, forecast.getAverageProduction());
                assertEquals(ZERO, forecast.getPotentialFlexConsumption());
                assertEquals(batteryValue, forecast.getPotentialFlexProduction());
            }
            else
            {
                chargeTotal = chargeTotal.subtract(batteryValue);

                if(chargeTotal.signum() < 0)
                    batteryValue = batteryValue.add(chargeTotal);

                assertEquals(batteryValue, forecast.getAverageProduction());
                assertEquals(batteryValue, forecast.getPotentialFlexConsumption());
                assertEquals(ZERO, forecast.getPotentialFlexProduction());
            }
        }
        else
        {
            assertEquals(ZERO, forecast.getAverageProduction());
            assertEquals(ZERO, forecast.getPotentialFlexConsumption());
            assertEquals(ZERO, forecast.getPotentialFlexProduction());
        }

        return chargeTotal;
    }

    private BigInteger validateZIHBatteryForecast(ForecastPowerDataDto forecast, BigInteger chargeTotal)
    {
        BigInteger batteryForecastValue = (correctionForecastValue > forecastValue) ? BigInteger.valueOf(correctionForecastValue - forecastValue) : ZERO;
        batteryForecastValue = (maxCharge.compareTo(batteryForecastValue) == 1) ? batteryForecastValue : maxCharge;

        assertEquals(ZERO, forecast.getUncontrolledLoad());
        assertEquals(ZERO, forecast.getAverageProduction());

        if(chargeTotal.compareTo(chargeFull) == -1)
        {
            chargeTotal = chargeTotal.add(batteryForecastValue);

            BigInteger delta = chargeTotal.subtract(chargeFull);
            if(delta.signum() > -1 )
                batteryForecastValue = batteryForecastValue.subtract(delta);

            assertEquals(batteryForecastValue, forecast.getAverageConsumption());
            assertEquals(ZERO, forecast.getPotentialFlexConsumption());
            assertEquals(batteryForecastValue, forecast.getPotentialFlexProduction());
        }
        else
        {
            assertEquals(ZERO, forecast.getAverageConsumption());
            assertEquals(batteryForecastValue, forecast.getPotentialFlexConsumption());
            assertEquals(ZERO, forecast.getPotentialFlexProduction());
        }

        return chargeTotal;
    }

    //input generation
    private List<ConnectionPortfolioDto> buildConnections()
    {
        List<ConnectionPortfolioDto> connections = new ArrayList<>();
        for(int idx = 1; idx <= 3; idx++)
        {
            ConnectionPortfolioDto connection = new ConnectionPortfolioDto(EAN_PREFIX + idx);
            switch(idx)
            {
                case 1:
                    connection.getUdis().add(new UdiPortfolioDto(PV_ENDPOINT, PTU_DURATION, ElementType.PV) );
                    connection.getUdis().add(new UdiPortfolioDto(BATTERY_ENDPOINT, PTU_DURATION, ElementType.BATTERY_ZIH) );
                break;

                case 2:
                    connection.getUdis().add(new UdiPortfolioDto(BATTERY_ENDPOINT, PTU_DURATION, ElementType.BATTERY_NOD) );
                break;

                case 3:
                    connection.getUdis().add(new UdiPortfolioDto(PV_ENDPOINT, PTU_DURATION, ElementType.PV) );
                    connection.getUdis().add(new UdiPortfolioDto(BATTERY_ENDPOINT, PTU_DURATION, ElementType.BATTERY_NOD) );
                break;
            }
            connections.add(connection);
        }

        return connections;
    }

    private Map<Integer, Long> generateMap(Long value)
    {
        Map<Integer, Long> l = new HashMap<>();
        for(int i = 1; i <= PTU_COUNT; l.put(i++, value));
        return l;
    }

    private Map<Integer, Double> buildCorrectionMap(double val)
    {
        Map<Integer, Double> d = new HashMap<>();
        for(int i = 1; i <= PTU_COUNT; d.put(i++, val));
        return d;
    }

    private void setForecastProperties()
    {
        prop = new Properties();
        prop.setProperty(AgrConfiguration.BATTERY_CHARGE, ""+maxCharge);
        prop.setProperty(AgrConfiguration.BATTERY_FULLYCHARGED, ""+chargeFull);
    }
}