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

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.ElementDto;
import info.usef.agr.dto.ElementDtuDataDto;
import info.usef.agr.dto.ElementTypeDto;
import nl.energieprojecthoogdalem.agr.dtos.Proposition;
import nl.energieprojecthoogdalem.configurationservice.AgrConfiguration;
import nl.energieprojecthoogdalem.forecastservice.ForecastService;
import nl.energieprojecthoogdalem.forecastservice.weather.WeatherService;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigInteger;
import static java.math.BigInteger.ZERO;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ElementsFactory.class, AgrConfiguration.class})
public class ElementsFactoryTest
{
    private ElementsFactory elementsFactory;

    private static final int PTU_COUNT = 96
            , PTU_DURATION = 15;

    private static final double correction = 0.8D;

    private static final Long forecast = 800L
                        , pvForecast = 2000L
                        , correctedForecast = (long) Math.ceil(pvForecast * correction)
            ;

    private static final String EAN_PREFIX = "ean.800000000000000";

    private static final BigInteger chargeFull = BigInteger.valueOf(1650 *60 /PTU_DURATION)
                                    ,maxCharge = BigInteger.valueOf(750);

    private static final LocalDate PERIOD = new LocalDate(2016, 1, 28);

    private Properties prop;

    private Proposition noPV, proposition;

    @Mock
    private ForecastService forecastService;

    @Mock
    private WeatherService weatherService;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception
    {
        setForecastProperties();

        PowerMockito.mockStatic(AgrConfiguration.class);
        PowerMockito.when(AgrConfiguration.getConfig(Matchers.anyString(), Matchers.any(LocalDate.class), Matchers.anyInt())).thenReturn(prop);
        PowerMockito.whenNew(WeatherService.class).withAnyArguments().thenReturn(weatherService);

        proposition =new Proposition("y","y");
        noPV =new Proposition("n","y");

        Mockito.when(weatherService.getDayCorrection(Matchers.any(LocalDate.class), Matchers.anyInt())).thenReturn(buildCorrectionMap(correction));

        Mockito.doNothing().when(forecastService).disconnect();
        Mockito.when(forecastService.retrieveForecast(Matchers.any(), Matchers.anyInt())).thenReturn(generateMap(forecast));
        Mockito.when(forecastService.retrievePVForecast(Matchers.anyString(),Matchers.any(), Matchers.anyInt())).thenReturn(generateMap(pvForecast), generateMap(pvForecast));

        elementsFactory = new ElementsFactory();
        Whitebox.setInternalState(elementsFactory, "forecastService", forecastService);
    }

    @Test
    public void testCreateElementsDisconnected() throws Exception
    {
        Mockito.when(forecastService.connect()).thenReturn(false);
        Mockito.when(forecastService.retrieveProposition(Matchers.anyString())).thenReturn(null);

        List<ElementDto> processed = elementsFactory.createElements(buildConnections(), PERIOD, PTU_DURATION, PTU_COUNT);

        assertEquals(3, processed.size());

        assertEmptyElement(processed.get(0), EAN_PREFIX + "03" + 1, ElementTypeDto.SYNTHETIC_DATA, ElementType.HOME, PTU_DURATION, ElementType.HOME_ID );
        assertEmptyElement(processed.get(1), EAN_PREFIX + "03" + 2, ElementTypeDto.SYNTHETIC_DATA, ElementType.HOME, PTU_DURATION, ElementType.HOME_ID );
        assertEmptyElement(processed.get(2), EAN_PREFIX + "03" + 3, ElementTypeDto.SYNTHETIC_DATA, ElementType.HOME, PTU_DURATION, ElementType.HOME_ID );

    }

    @Test
    public void testCreateElements() throws Exception
    {
        Mockito.when(forecastService.connect()).thenReturn(true);
        Mockito.when(forecastService.retrieveProposition(Matchers.anyString())).thenReturn(noPV, retrievePVPropositions(2));

        List<ElementDto> processed = elementsFactory.createElements(buildConnections(), PERIOD, PTU_DURATION, PTU_COUNT);

        assertEquals(3 *2, processed.size());

        assertElement(processed.get(0), EAN_PREFIX +"03" +1, ElementTypeDto.SYNTHETIC_DATA, noPV, ElementType.HOME, PTU_DURATION, ElementType.HOME_ID);
        assertElement(processed.get(1), EAN_PREFIX +"03" +1, ElementTypeDto.MANAGED_DEVICE, noPV, ElementType.BATTERY_NOD, PTU_DURATION, ElementType.BATTERY_NOD_ID);

        assertElement(processed.get(2), EAN_PREFIX +"03" +2, ElementTypeDto.SYNTHETIC_DATA, proposition, ElementType.HOME, PTU_DURATION, ElementType.HOME_ID);
        assertElement(processed.get(3), EAN_PREFIX +"03" +2, ElementTypeDto.MANAGED_DEVICE, proposition, ElementType.BATTERY_ZIH, PTU_DURATION, ElementType.BATTERY_ZIH_ID);

        assertElement(processed.get(4), EAN_PREFIX +"03" +3, ElementTypeDto.SYNTHETIC_DATA, proposition, ElementType.HOME, PTU_DURATION, ElementType.HOME_ID);
        assertElement(processed.get(5), EAN_PREFIX +"03" +3, ElementTypeDto.MANAGED_DEVICE, proposition, ElementType.BATTERY_ZIH, PTU_DURATION, ElementType.BATTERY_ZIH_ID);
    }

    //assert real elements
    private void assertElement(ElementDto elementDto, String addr, ElementTypeDto type, Proposition proposition, String profile, Integer dtuDuration, String id)
    {
        System.out.println("addr " + addr + " type " + type + " profile " + profile + " dtu duration " + dtuDuration + " id " + id + " Dtu size " + elementDto.getElementDtuData().size());
        assertEquals(addr, elementDto.getConnectionEntityAddress());
        assertEquals(type, elementDto.getElementType());
        assertEquals(profile, elementDto.getProfile());
        assertEquals(dtuDuration, elementDto.getDtuDuration());
        assertEquals(addr + '.' + id, elementDto.getId());

        assertEquals(PTU_COUNT, elementDto.getElementDtuData().size());
        assertDtuData(elementDto.getElementDtuData(), proposition, profile);
    }

    private void assertDtuData(List<ElementDtuDataDto> elementDataList, Proposition proposition, String profile)
    {
        BigInteger chargeTotal = (ElementType.BATTERY_ZIH.equals(profile)) ? ZERO : chargeFull;

        //NOTE LIST USES 0 < - > 95, INPUT MAP USES REAL 1 <-> 96
        for(int i = 0; i < PTU_COUNT; i++)
        {
            ElementDtuDataDto data = elementDataList.get(i);
            switch (profile)
            {
                case ElementType.HOME:
                    assertEquals(ZERO, data.getProfileAverageConsumption());
                    assertEquals(ZERO, data.getProfilePotentialFlexConsumption());
                    assertEquals(ZERO, data.getProfilePotentialFlexProduction());
                    assertEquals(BigInteger.valueOf(forecast), data.getProfileUncontrolledLoad());
                    if (proposition.hasPv())
                        assertEquals(BigInteger.valueOf( correctedForecast ), data.getProfileAverageProduction());

                    else
                        assertEquals(ZERO, data.getProfileAverageProduction());

                    break;


                case ElementType.BATTERY_NOD:
                    assertEquals(ZERO, data.getProfileUncontrolledLoad());
                    assertEquals(ZERO, data.getProfileAverageConsumption());

                    BigInteger batteryValue = BigInteger.valueOf(forecast);
                    batteryValue = (maxCharge.compareTo(batteryValue) == 1) ? batteryValue : maxCharge;

                    //NOTE LIST USES 0 < - > 95, INPUT MAP USES REAL 1 <-> 96
                    if(i > 27 && i < 92)
                    {
                        if(chargeTotal.signum() < 1)
                        {
                            assertEquals(ZERO, data.getProfileAverageProduction());
                            assertEquals(ZERO, data.getProfilePotentialFlexConsumption());
                            assertEquals(batteryValue, data.getProfilePotentialFlexProduction());
                        }
                        else
                        {
                            chargeTotal = chargeTotal.subtract(batteryValue);

                            if(chargeTotal.signum() < 0)
                                batteryValue = batteryValue.add(chargeTotal);

                            assertEquals(batteryValue, data.getProfileAverageProduction());
                            assertEquals(batteryValue, data.getProfilePotentialFlexConsumption());
                            assertEquals(ZERO, data.getProfilePotentialFlexProduction());


                        }
                    }
                    else
                    {
                        assertEquals(ZERO, data.getProfileAverageProduction());
                        assertEquals(ZERO, data.getProfilePotentialFlexConsumption());
                        assertEquals(ZERO, data.getProfilePotentialFlexProduction());
                    }

                    break;

                case ElementType.BATTERY_ZIH:
                    assertEquals(ZERO, data.getProfileUncontrolledLoad());
                    assertEquals(ZERO, data.getProfileAverageProduction());

                    BigInteger batteryForecast = (correctedForecast > forecast) ? BigInteger.valueOf(correctedForecast - forecast) : ZERO;
                    batteryForecast = (maxCharge.compareTo(batteryForecast) == 1) ? batteryForecast : maxCharge;

                    if(chargeTotal.compareTo(chargeFull) == -1)
                    {
                        chargeTotal = chargeTotal.add(batteryForecast);

                        BigInteger delta = chargeTotal.subtract(chargeFull);
                        if(delta.signum() > -1)
                            batteryForecast = batteryForecast.subtract(delta);

                        assertEquals(batteryForecast, data.getProfileAverageConsumption());
                        assertEquals(batteryForecast, data.getProfilePotentialFlexProduction());
                        assertEquals(ZERO, data.getProfilePotentialFlexConsumption());
                    }
                    else
                    {
                        assertEquals(ZERO, data.getProfileAverageConsumption());
                        assertEquals(ZERO, data.getProfilePotentialFlexProduction());
                        assertEquals(batteryForecast, data.getProfilePotentialFlexConsumption());
                    }


                    break;
            }
        }
        System.out.println("validated DTU contents");
    }

    //assert 'empty' elements
    private void assertEmptyElement(ElementDto elementDto, String addr, ElementTypeDto type, String profile, Integer dtuDuration, String id)
    {
        System.out.println("addr " + addr + " type " + type + " profile " + profile + " dtu duration " + dtuDuration + " id " + id + " Dtu size " + elementDto.getElementDtuData().size());
        assertEquals(addr, elementDto.getConnectionEntityAddress());
        assertEquals(type, elementDto.getElementType());
        assertEquals(profile, elementDto.getProfile());
        assertEquals(dtuDuration, elementDto.getDtuDuration());
        assertEquals(addr + '.' + id, elementDto.getId());

        assertEquals(PTU_COUNT, elementDto.getElementDtuData().size() );
        assertEmptyDtuData(elementDto.getElementDtuData(), profile);

    }

    private void assertEmptyDtuData(List<ElementDtuDataDto> elementDtuDataDtos, String profile)
    {
        //NOTE LIST USES 0 < - > 95, INPUT MAP USES REAL 1 <-> 96
        for(int i = 0; i < PTU_COUNT; i++)
        {
            ElementDtuDataDto data = elementDtuDataDtos.get(i);
            switch (profile)
            {
                default:
                    assertEquals(ZERO, data.getProfileUncontrolledLoad());
                    assertEquals(ZERO, data.getProfileAverageConsumption());
                    assertEquals(ZERO, data.getProfileAverageProduction());
                    assertEquals(ZERO, data.getProfilePotentialFlexConsumption());
                    assertEquals(ZERO, data.getProfilePotentialFlexProduction());
                    break;
            }
        }
        System.out.println("validated empty DTU contents");
    }

    //input generation
    private Proposition[] retrievePVPropositions(int len)
    {
        Proposition[] propositions = new Proposition[len];
        for(int i = 0; i < len; propositions[i++] = proposition);
        return propositions;
    }

    private List<ConnectionPortfolioDto> buildConnections()
    {
        List<ConnectionPortfolioDto> connections = new ArrayList<>();
        for(int idx = 1; idx <= 3; idx++)
        {
            ConnectionPortfolioDto connection = new ConnectionPortfolioDto(EAN_PREFIX +"03" +idx);
            connections.add(connection);
        }

        return connections;
    }

    private Map<Integer, Long> generateMap(Long value)
    {
        Map<Integer, Long> map = new HashMap<>();
        for(int i = 1; i<=96; map.put(i++, value));
        return map;
    }

    private Map<Integer, Double> buildCorrectionMap(double val)
    {
        Map<Integer, Double> m = new HashMap<>();
        for(int i = 1; i <= PTU_COUNT; m.put(i++, val));
        return m;
    }

    private void setForecastProperties()
    {
        prop = new Properties();
        prop.setProperty(AgrConfiguration.BATTERY_CHARGE, ""+maxCharge);
        prop.setProperty(AgrConfiguration.BATTERY_FULLYCHARGED, ""+chargeFull);
    }

}