/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nl.energieprojecthoogdalem.forecastservice.element;

import nl.energieprojecthoogdalem.agr.dtos.ForecastType;
import nl.energieprojecthoogdalem.agr.dtos.Proposition;
import nl.energieprojecthoogdalem.configurationservice.AgrConfiguration;
import nl.energieprojecthoogdalem.forecastservice.ForecastService;
import nl.energieprojecthoogdalem.forecastservice.weather.WeatherService;
import nl.energieprojecthoogdalem.util.EANUtil;
import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.ElementDto;
import info.usef.agr.dto.ElementDtuDataDto;
import info.usef.agr.dto.ElementTypeDto;
import org.joda.time.LocalDate;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.*;

import static java.math.BigInteger.ZERO;

/**
 * creates elements for the usef Hoogdalem implementation
 * */
public class ElementsFactory
{
    private BigInteger chargeFull
                        ,maxCharge
            ;

    @Inject
    private ForecastService forecastService;

    /**
     * Creates and fills elements for a list of {@link ConnectionPortfolioDto}s, data will be retrieved from a database: {@link ForecastService}
     * 2 Element(s) are created: 1 HOME(SYNTHETIC_DATA) and depending on proposition 1 BATTERY(MANAGED_DEVICE).
     *
     * @param connections EANs received from CRO
     * @param period date of ptus
     * @param ptuDuration duration of one ptu (15)
     * @param ptuCount total ptus in a day (96)
     * @return returns a list of elements created per CreateConnection
     */
    public List<ElementDto> createElements(List<ConnectionPortfolioDto> connections, LocalDate period
            ,Integer ptuDuration ,Integer ptuCount )
    {
        List<ElementDto> elements = new ArrayList<>();

        Properties prop = AgrConfiguration.getConfig("CREATE_ELEMENTS", period, ptuDuration);
        WeatherService weatherService = new WeatherService(prop);

        chargeFull = new BigInteger( prop.getProperty(AgrConfiguration.BATTERY_FULLYCHARGED));
        maxCharge = new BigInteger(prop.getProperty(AgrConfiguration.BATTERY_CHARGE));

        Map<ForecastType, Map<Integer, Long>> forecasts = new HashMap<>();
        Map<Integer, Double> weatherCorrectionMap = weatherService.getDayCorrection(period, ptuDuration);

        forecastService.connect();

        forecasts.put(ForecastType.FORECAST_MAP, forecastService.retrieveForecast(period, ptuDuration));

        connections.forEach(connection ->
        {
            String ean = connection.getConnectionEntityAddress()
                    ,home = EANUtil.toHomeString(ean)
                    ,batteryElementType = ElementType.BATTERY_NOD
                    ,batteryElementId = ElementType.BATTERY_NOD_ID
                    ;
            Proposition proposition = forecastService.retrieveProposition(home);

            if(proposition != null)
            {
                //USING PV TO ADD BATTERYS TO ONLY ZIH LATER USE if( proposition.equals("ZIH") )
                if(proposition.hasPv() )
                {
                    batteryElementType = ElementType.BATTERY_ZIH;
                    batteryElementId = ElementType.BATTERY_ZIH_ID;
                    forecasts.put(ForecastType.PVFORECAST_MAP, forecastService.retrievePVForecast(home, period, ptuDuration));
                }
                //GET PVFORECAST FIRST BEFORE ADDING HOME! (SYNTHETIC_DATA)
                ElementDto batteryElement = null
                        , homeElement = createElement(ean, ElementTypeDto.SYNTHETIC_DATA, ElementType.HOME, ptuDuration, ElementType.HOME_ID);

                elements.add(homeElement);

                if(proposition.hasBattery())
                {
                    batteryElement = createElement(ean, ElementTypeDto.MANAGED_DEVICE, batteryElementType, ptuDuration, batteryElementId);
                    elements.add(batteryElement);
                }

                BigInteger chargeTotal = (ElementType.BATTERY_ZIH.equals(batteryElementType)) ? ZERO : chargeFull;

                for(int idx = 1; idx <= ptuCount; idx++)
                {
                    if(proposition.hasPv())
                        forecasts.get(ForecastType.PVFORECAST_MAP).put(idx, (long) Math.ceil( forecasts.get(ForecastType.PVFORECAST_MAP).get(idx) * weatherCorrectionMap.get(idx) ));

                    fillHomeDtuData(homeElement, proposition, idx, forecasts );

                    if(batteryElement != null)
                    {
                        switch (batteryElementType)
                        {
                            case ElementType.BATTERY_ZIH:
                                chargeTotal = fillZIHBatteryData(batteryElement, idx, forecasts, chargeTotal);
                                break;

                            case ElementType.BATTERY_NOD:
                                chargeTotal = fillNODBatteryData(batteryElement, idx, forecasts.get(ForecastType.FORECAST_MAP), chargeTotal);
                                break;
                        }
                    }

                }

            }
            else
                elements.add(DefaultElement.getHomeElement(ean));


        });

        forecastService.disconnect();

        return  elements;
    }


    /**
     * creates an element, for ZIH algorithm
     *
     * @param addr EAN string from connection
     * @param type the elementtype defined in {@link ElementTypeDto}
     * @param profile the name of the profile
     * @param ptuDuration thhe duration of one ptu (15)
     * @param id the name of the identifier matching the profile name
     * @return ElementDto a HOME or BATTERY element
     * */
    private ElementDto createElement(String addr, ElementTypeDto type, String profile, Integer ptuDuration, String id)
    {
        ElementDto elementDto = new ElementDto();

        elementDto.setElementType(type);
        elementDto.setConnectionEntityAddress(addr);
        elementDto.setProfile(profile);
        elementDto.setDtuDuration(ptuDuration);

        elementDto.setId( addr + '.' + id );

        return elementDto;
    }

    /**
     * sets the values in the {@link ElementDtuDataDto} using the ZIH algorithm for the home element
     *
     * @param elementDto the {@link ElementDto} for adding all the {@link ElementDtuDataDto}s
     * @param proposition the proposition for the given ean
     * @param idx the ptu index number between 1 and 96
     * @param forecasts the uncontrolled load forecast and depending on proposition, the pv production forecast
     * */
    private void fillHomeDtuData(ElementDto elementDto, Proposition proposition, int idx, Map<ForecastType, Map<Integer, Long>> forecasts)
    {
        ElementDtuDataDto elementDtuDataDto = new ElementDtuDataDto();
        elementDtuDataDto.setDtuIndex(idx);

        elementDtuDataDto.setProfileAverageConsumption(ZERO);
        elementDtuDataDto.setProfilePotentialFlexConsumption(ZERO);
        elementDtuDataDto.setProfilePotentialFlexProduction(BigInteger.ZERO);

        // forecast
        elementDtuDataDto.setProfileUncontrolledLoad(BigInteger.valueOf(forecasts.get(ForecastType.FORECAST_MAP).get(idx)) );

        //pv forecast
        if(proposition.hasPv())
            elementDtuDataDto.setProfileAverageProduction(BigInteger.valueOf(forecasts.get(ForecastType.PVFORECAST_MAP).get(idx)));

        else
            elementDtuDataDto.setProfileAverageProduction(ZERO);

        elementDto.getElementDtuData().add(elementDtuDataDto);
    }

    /**
     * sets the values in the {@link ElementDtuDataDto} using the ZIH algorithm for the battery element
     *
     * @param ZIHBattery the elementDto for adding the elementDtuDataDto
     * @param idx the ptu index number between 1 and 96
     * @param forecasts the uncontrolled load forecast and the pv production forecast
     * @param chargeTotal the current amount of the charged battery
     * @return the summed amount of the battery forecast value and the chargeTotal value
     * */
    private BigInteger fillZIHBatteryData(ElementDto ZIHBattery, int idx, Map<ForecastType, Map<Integer, Long>> forecasts, BigInteger chargeTotal)
    {
        ElementDtuDataDto elementDtuDataDto = new ElementDtuDataDto();

        elementDtuDataDto.setDtuIndex(idx);
        elementDtuDataDto.setProfileUncontrolledLoad(ZERO);
        elementDtuDataDto.setProfileAverageProduction(ZERO);

        long pv = forecasts.get(ForecastType.PVFORECAST_MAP).get(idx)
                , ucl = forecasts.get(ForecastType.FORECAST_MAP).get(idx)
                    ;
        BigInteger batteryForecast = ( pv > ucl ) ? BigInteger.valueOf( pv - ucl ) : ZERO;
        batteryForecast = (maxCharge.compareTo(batteryForecast) == 1) ? batteryForecast : maxCharge;

        //chargeTotal < chargeFull
        if(chargeTotal.compareTo(chargeFull) == -1)
        {
            chargeTotal = chargeTotal.add(batteryForecast);

            BigInteger delta = chargeTotal.subtract(chargeFull);
            if(delta.signum() > -1 )
                batteryForecast = batteryForecast.subtract(delta);

            elementDtuDataDto.setProfileAverageConsumption(batteryForecast);
            elementDtuDataDto.setProfilePotentialFlexProduction(batteryForecast);
            elementDtuDataDto.setProfilePotentialFlexConsumption(ZERO);
        }
        //chargeTotal >= chargeFull
        else
        {
            elementDtuDataDto.setProfileAverageConsumption(ZERO);
            elementDtuDataDto.setProfilePotentialFlexProduction(ZERO);
            elementDtuDataDto.setProfilePotentialFlexConsumption(batteryForecast);
        }

        ZIHBattery.getElementDtuData().add(elementDtuDataDto);
        return chargeTotal;
    }

    /**
     * sets the values in the {@link ElementDtuDataDto} using the NOD algorithm for the battery element
     *
     * @param NODBattery the elementDto for adding the elementDtuDataDto
     * @param idx the ptu index number between 1 and 96
     * @param forecast the uncontrolled load forecast
     * @param dischargeTotal the current amount of the charged battery
     * @return the subtracted amount of the dischargeTotal value and the battery forecast value
     * */
    private BigInteger fillNODBatteryData(ElementDto NODBattery, int idx, Map<Integer, Long> forecast, BigInteger dischargeTotal)
    {
        ElementDtuDataDto elementData = new ElementDtuDataDto();

        elementData.setDtuIndex(idx);
        elementData.setProfileUncontrolledLoad(ZERO);
        elementData.setProfileAverageConsumption(ZERO);

        //between 07:00 && 23:00 ( i > 06:45 && i < 23:15
        if(idx > 28 && idx < 93)
        {
            BigInteger batteryValue = BigInteger.valueOf(forecast.get(idx));
            batteryValue = (maxCharge.compareTo(batteryValue) == 1) ? batteryValue : maxCharge;

            if(dischargeTotal.signum() < 1)
            {
                elementData.setProfileAverageProduction(ZERO);
                elementData.setProfilePotentialFlexConsumption(ZERO);
                elementData.setProfilePotentialFlexProduction(batteryValue);
            }
            else
            {
                dischargeTotal = dischargeTotal.subtract(batteryValue);

                if(dischargeTotal.signum() < 0)
                    batteryValue = batteryValue.add(dischargeTotal);

                elementData.setProfileAverageProduction(batteryValue);
                elementData.setProfilePotentialFlexConsumption(batteryValue);
                elementData.setProfilePotentialFlexProduction(ZERO);
            }
        }
        else
        {
            elementData.setProfileAverageProduction(ZERO);
            elementData.setProfilePotentialFlexConsumption(ZERO);
            elementData.setProfilePotentialFlexProduction(ZERO);
        }

        NODBattery.getElementDtuData().add(elementData);
        return dischargeTotal;
    }
}
