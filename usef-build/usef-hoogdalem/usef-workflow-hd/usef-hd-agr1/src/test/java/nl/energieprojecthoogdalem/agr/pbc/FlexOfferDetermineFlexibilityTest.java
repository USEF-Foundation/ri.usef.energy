/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.agr.pbc;

import static java.math.BigInteger.ZERO;
import static java.util.stream.Collectors.toList;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.ForecastPowerDataDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.workflow.validate.flexoffer.FlexOfferDetermineFlexibilityStepParameter.OUT;
import info.usef.agr.workflow.validate.flexoffer.FlexOfferDetermineFlexibilityStepParameter.IN;
import info.usef.core.config.AbstractConfig;
import info.usef.core.util.DateTimeUtil;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.DispositionTypeDto;
import info.usef.core.workflow.dto.FlexOfferDto;
import info.usef.core.workflow.dto.FlexRequestDto;
import info.usef.core.workflow.dto.PtuFlexOfferDto;
import info.usef.core.workflow.dto.PtuFlexRequestDto;
import info.usef.core.workflow.dto.USEFRoleDto;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import nl.energieprojecthoogdalem.agr.devicemessages.ReservedDevice;
import nl.energieprojecthoogdalem.configurationservice.AgrConfiguration;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import nl.energieprojecthoogdalem.util.EANUtil;
import nl.energieprojecthoogdalem.util.ReserveDeviceUtil;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.LocalDate;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class in charge of the unit tests related to the {@link FlexOfferDetermineFlexibility} PBC class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({FlexOfferDetermineFlexibility.class, AgrConfiguration.class})
@SuppressWarnings("unchecked")
public class FlexOfferDetermineFlexibilityTest
{
    private FlexOfferDetermineFlexibility determineFlexibility;

    private static final int PTU_DURATION = 15
                            , PTU_COUNT = 96
                            , CONNECTION_COUNT = 32
                            , REQUEST_COUNT = 1
                            , OFFER_COUNT = 2
                            , ZIH_CONNECTION_COUNT = 24
                        ;

    private static final long REQUEST_SEQUENCE_PREFIX = 20L;

    private BigInteger chargeFull = BigInteger.valueOf(1650 *60 /PTU_DURATION);

    private final Random random = new Random();

    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678"
                        , FLEX_INPUT = "FLEX"
                        , FLEX_RESULT = "RESULT"
                        , PV_ENDPOINT = "MZ29EBX000/usef/" + ElementType.PV
                        , BATTERY_ENDPOINT_PREFIX = "MZ29EBX00"
                        , BATTERY_ENDPOINT_SUFFIX  = "/usef/" + ElementType.BATTERY
                        , RESERVED_MESSAGES_FILE = AbstractConfig.getConfigurationFolder() + "reserved_messages.json"
                        ;

    private static final LocalDate PERIOD = DateTimeUtil.parseDate("2015-06-17");

    private Map<Integer, BigInteger> pvMap
                                    , ZIHBatteryMap
                                    , NODBatteryMap
                                    ,flexMap
                                    , resultMap
            ;

    private Properties prop;

    @Before
    public void init() throws Exception
    {
        setConfigProperties();
        PowerMockito.mockStatic(AgrConfiguration.class);
        PowerMockito.when(AgrConfiguration.getConfig(Matchers.anyString(), Matchers.any(LocalDate.class), Matchers.anyInt())).thenReturn(prop);

        determineFlexibility = new FlexOfferDetermineFlexibility();
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<Integer, BigInteger> > mapref = new TypeReference<HashMap<Integer, BigInteger>>(){};

        JsonNode root = objectMapper.readTree( new File(AbstractConfig.getConfigurationFolder() + "dummy_data/offer.json" ) );

        pvMap = objectMapper.convertValue(root.get(ElementType.PV) ,mapref);
        ZIHBatteryMap = objectMapper.convertValue(root.get(ElementType.BATTERY_ZIH) ,mapref);
        NODBatteryMap = objectMapper.convertValue(root.get(ElementType.BATTERY_NOD) ,mapref);
        flexMap = objectMapper.convertValue(root.get(FLEX_INPUT) ,mapref);
        resultMap = objectMapper.convertValue(root.get(FLEX_RESULT) ,mapref);

    }

    @Test
    public void testInvoke() throws Exception
    {
        deleteFile();
        WorkflowContext result = determineFlexibility.invoke(buildContext());

        // assertions
        assertNotNull(result);

        List<FlexOfferDto> flexOfferDtos = result.get(OUT.FLEX_OFFER_DTO_LIST.name(), List.class);
        assertNotNull(flexOfferDtos);
        assertEquals(REQUEST_COUNT, flexOfferDtos.size());

        long idx = 1;
        for(FlexOfferDto flexOfferDto : flexOfferDtos)
        {
            Long requestSequence = flexOfferDto.getFlexRequestSequenceNumber();
            assertNotNull(requestSequence);
            assertEquals(Long.valueOf(REQUEST_SEQUENCE_PREFIX + idx) , requestSequence);

            for(PtuFlexOfferDto ptuFlexOfferDto : flexOfferDto.getPtus())
            {
                //System.out.println(",\""+ptuFlexOfferDto.getPtuIndex()+"\":\t"+ptuFlexOfferDto.getPower());
                assertEquals(resultMap.get( ptuFlexOfferDto.getPtuIndex().intValue() ), ptuFlexOfferDto.getPower());
            }

            Map<String, ReservedDevice> reservedResultMap = ReserveDeviceUtil.readReservation(PERIOD.toString(ReserveDeviceUtil.PERIOD_STRING_FORMAT));
            assertEquals(24, reservedResultMap.size());

            for(int zih = 1; zih <= 19; zih++)
            {
                ReservedDevice resultDevice = reservedResultMap.get(BATTERY_ENDPOINT_PREFIX + zih + BATTERY_ENDPOINT_SUFFIX);
                assertNotNull(resultDevice);

                assertEquals(PERIOD, resultDevice.getPeriod());
                assertTrue(resultDevice.getStartIndex() == 44 || resultDevice.getStartIndex() == 53);
            }

            for(int nod = 26; nod <= 28; nod++)
            {
                ReservedDevice resultDevice = reservedResultMap.get(BATTERY_ENDPOINT_PREFIX + nod + BATTERY_ENDPOINT_SUFFIX);
                assertNotNull(resultDevice);

                assertEquals(PERIOD, resultDevice.getPeriod());
                assertEquals(75, resultDevice.getStartIndex());
            }

            idx++;
        }

        deleteFile();

    }

    private void deleteFile()
    {
        File reserved = new File(RESERVED_MESSAGES_FILE);

        if(reserved.delete())
            System.out.println("file deleted");
        else
            System.out.println("file not deleted");
    }

    private WorkflowContext buildContext()
    {
        WorkflowContext workflowContext = new DefaultWorkflowContext();

        workflowContext.setValue(IN.PERIOD.name(), PERIOD);
        workflowContext.setValue(IN.PTU_DURATION.name(), PTU_DURATION);

        workflowContext.setValue(IN.FLEX_OFFER_DTO_LIST.name(), buildFlexOffers());
        workflowContext.setValue(IN.FLEX_REQUEST_DTO_LIST.name(), buildFlexRequests());

        workflowContext.setValue(IN.CONNECTION_PORTFOLIO_DTO.name(), buildConnectionPortfolio());
        workflowContext.setValue(IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(), buildConnectionsPerConnectionGroupMap());

        return workflowContext;
    }

    private List<FlexRequestDto> buildFlexRequests()
    {
        return IntStream.rangeClosed(1, REQUEST_COUNT).mapToObj(i ->
        {
            FlexRequestDto flexRequestDto = new FlexRequestDto();
            IntStream.rangeClosed(1, PTU_COUNT).mapToObj(index ->
            {
                PtuFlexRequestDto ptuFlexRequestDto = new PtuFlexRequestDto();
                ptuFlexRequestDto.setPtuIndex(BigInteger.valueOf(index));
                BigInteger flexValue = flexMap.get(index);

                if( BigInteger.ZERO.equals(flexValue) )
                {
                    ptuFlexRequestDto.setDisposition(DispositionTypeDto.AVAILABLE);
                    ptuFlexRequestDto.setPower(flexValue);
                }
                else
                {
                    ptuFlexRequestDto.setDisposition(DispositionTypeDto.REQUESTED);
                    ptuFlexRequestDto.setPower(flexValue);
                }
                //System.out.println("idx " + index + " value " + ptuFlexRequestDto.getPower() + " type " + ptuFlexRequestDto.getDisposition() );
                return ptuFlexRequestDto;

            }).forEach(ptu -> flexRequestDto.getPtus().add(ptu));
            flexRequestDto.setSequenceNumber(REQUEST_SEQUENCE_PREFIX + (long)i);
            flexRequestDto.setPrognosisSequenceNumber(10L + (long)i);
            flexRequestDto.setParticipantDomain("dso" + i + ".usef-example.com");
            flexRequestDto.setConnectionGroupEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
            flexRequestDto.setPeriod(PERIOD);
            flexRequestDto.setParticipantRole(USEFRoleDto.DSO);
            return flexRequestDto;
        }).collect(toList());
    }

    private Map<String, List<String>> buildConnectionsPerConnectionGroupMap()
    {
        Map<String, List<String>> connectionsPerConnectionGroup = new HashMap<>();
        connectionsPerConnectionGroup.put(CONGESTION_POINT_ENTITY_ADDRESS, new ArrayList<>());
        connectionsPerConnectionGroup.get(CONGESTION_POINT_ENTITY_ADDRESS).addAll(IntStream.rangeClosed(1, CONNECTION_COUNT)
                .mapToObj(i -> "ean.00000000000" + i)
                .collect(toList()));
        return connectionsPerConnectionGroup;
    }
    
    private List<FlexOfferDto> buildFlexOffers()
    {
        return IntStream.rangeClosed(1, OFFER_COUNT).mapToObj(i -> {
            FlexOfferDto flexOfferDto = new FlexOfferDto();
            IntStream.rangeClosed(1, PTU_COUNT).mapToObj(index -> {
                PtuFlexOfferDto ptuFlexOfferDto = new PtuFlexOfferDto();
                ptuFlexOfferDto.setPtuIndex(BigInteger.valueOf(index));
                ptuFlexOfferDto.setPower(BigInteger.valueOf(-250));
                ptuFlexOfferDto.setPrice(BigDecimal.TEN);
                return ptuFlexOfferDto;
            }).forEach(ptu -> flexOfferDto.getPtus().add(ptu));
            flexOfferDto.setSequenceNumber(500L + (long)i);
            flexOfferDto.setFlexRequestSequenceNumber(Math.abs(random.nextLong()));
            flexOfferDto.setParticipantDomain("dso" + i + ".usef-example.com");
            flexOfferDto.setConnectionGroupEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
            flexOfferDto.setPeriod(PERIOD);
            return flexOfferDto;
        }).collect(toList());
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolio()
    {
        return IntStream.rangeClosed(1, CONNECTION_COUNT)
                .mapToObj(i -> 
                {
                    ConnectionPortfolioDto connection = new ConnectionPortfolioDto(EANUtil.toEAN(i));
                    if(i <= ZIH_CONNECTION_COUNT )
                    {
                        connection.getUdis().add( createZIHBatteryUDI(ElementType.BATTERY_ZIH, i) );
                        connection.getUdis().add( createPVUDI() );
                    }
                    else
                        connection.getUdis().add(createNODBatteryUDI(ElementType.BATTERY_NOD, i));

                    return connection;
                })
                .collect(Collectors.toList());
    }

    private UdiPortfolioDto createPVUDI()
    {
        UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto(PV_ENDPOINT, PTU_DURATION, ElementType.PV);

        IntStream.rangeClosed(1, PTU_COUNT)
                .forEach(index ->
                {
                    PowerContainerDto  pvPower = new PowerContainerDto(PERIOD, index);
                    ForecastPowerDataDto pvForecast = new ForecastPowerDataDto();

                    pvForecast.setUncontrolledLoad(BigInteger.ZERO);
                    pvForecast.setAverageConsumption(BigInteger.ZERO);
                    pvForecast.setAverageProduction( pvMap.get(index));
                    pvForecast.setPotentialFlexConsumption(BigInteger.ZERO);
                    pvForecast.setPotentialFlexProduction(BigInteger.ZERO);

                    pvPower.setForecast(pvForecast);
                    udiPortfolioDto.getUdiPowerPerDTU().put(index, pvPower );
                });
        return udiPortfolioDto;
    }

    private UdiPortfolioDto createNODBatteryUDI(String profile, int idx)
    {
        UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto(BATTERY_ENDPOINT_PREFIX + idx + BATTERY_ENDPOINT_SUFFIX, PTU_DURATION, profile);

        BigInteger dischargeTotal = chargeFull;

        for(int index = 1; index <= PTU_COUNT; index++)
        {
            PowerContainerDto nodPower = new PowerContainerDto(PERIOD, index);
            ForecastPowerDataDto nodForecast = nodPower.getForecast();

            if(index > 28 && index < 94)
            {
                BigInteger batteryForecastValue = NODBatteryMap.get(index);

                if(dischargeTotal.signum() != 1)
                {
                    nodForecast.setAverageProduction(ZERO);
                    nodForecast.setPotentialFlexConsumption(ZERO);
                    nodForecast.setPotentialFlexProduction(batteryForecastValue);
                }
                else
                {
                    dischargeTotal = dischargeTotal.subtract(batteryForecastValue);
                    if(dischargeTotal.signum() < 0)
                        batteryForecastValue = batteryForecastValue.add(dischargeTotal);

                    nodForecast.setAverageProduction(batteryForecastValue);
                    nodForecast.setPotentialFlexConsumption(batteryForecastValue);
                    nodForecast.setPotentialFlexProduction(ZERO);
                }
            }
            else
            {
                nodForecast.setAverageProduction(ZERO);
                nodForecast.setPotentialFlexConsumption(ZERO);
                nodForecast.setPotentialFlexProduction(ZERO);
            }

            udiPortfolioDto.getUdiPowerPerDTU().put(index, nodPower);
        }
        return udiPortfolioDto;
    }

    private UdiPortfolioDto createZIHBatteryUDI(String profile, int idx)
    {
        UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto(BATTERY_ENDPOINT_PREFIX + idx + BATTERY_ENDPOINT_SUFFIX, PTU_DURATION, profile);

        BigInteger chargeTotal = ZERO;

        for(int index = 1; index <= PTU_COUNT; index++)
        {
            PowerContainerDto batteryPower = new PowerContainerDto(PERIOD, index);
            ForecastPowerDataDto batteryForecast = new ForecastPowerDataDto();

            BigInteger batteryValue = ZIHBatteryMap.get(index);

            batteryForecast.setUncontrolledLoad(BigInteger.ZERO);
            batteryForecast.setAverageProduction(BigInteger.ZERO);

            if(chargeTotal.compareTo(chargeFull) == -1)
            {
                chargeTotal = chargeTotal.add(batteryValue);

                BigInteger delta = chargeTotal.subtract(chargeFull);
                if(delta.signum() > -1 )
                    batteryValue = batteryValue.subtract(delta);

                batteryForecast.setAverageConsumption(batteryValue);
                batteryForecast.setPotentialFlexConsumption(ZERO);
                batteryForecast.setPotentialFlexProduction(batteryValue);
            }
            else
            {
                batteryForecast.setAverageConsumption(ZERO);
                batteryForecast.setPotentialFlexConsumption(batteryValue);
                batteryForecast.setPotentialFlexProduction(ZERO);
            }

            batteryPower.setForecast(batteryForecast);

            udiPortfolioDto.getUdiPowerPerDTU().put(index, batteryPower);
        }

        return udiPortfolioDto;
    }

    private void setConfigProperties()
    {
        prop = new Properties();
        prop.setProperty(AgrConfiguration.BATTERY_CHARGE, ""+750);
        prop.setProperty(AgrConfiguration.BATTERY_CHARGE_DURATION, ""+8);
    }

}
