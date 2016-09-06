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

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.ForecastPowerDataDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.dto.device.capability.ShiftCapabilityDto;
import info.usef.agr.dto.device.capability.UdiEventDto;
import info.usef.agr.dto.device.capability.UdiEventTypeDto;
import info.usef.agr.dto.device.request.DeviceMessageDto;
import info.usef.agr.dto.device.request.ShiftRequestDto;
import info.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter.OUT;
import info.usef.core.config.AbstractConfig;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter.IN;
import info.usef.core.workflow.dto.AcknowledgementStatusDto;
import info.usef.core.workflow.dto.FlexOrderDto;
import info.usef.core.workflow.dto.PtuFlexOrderDto;
import nl.energieprojecthoogdalem.agr.devicemessages.ReservedDevice;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import nl.energieprojecthoogdalem.configurationservice.AgrConfiguration;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import nl.energieprojecthoogdalem.util.ReserveDeviceUtil;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.LocalDate;
import org.junit.After;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static java.math.BigInteger.ZERO;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AgrConfiguration.class, ReOptimizePortfolio.class})
public class ReOptimizePortfolioTest
{
    private final static int PTU_COUNT = 96
            , PTU_DURATION = 15
            ,CONNECTIONS_COUNT = 32
            ,ZIH_CONNECTION_COUNT = 24
            ,SHIFT_REQUESTS = 8
            ,SHIFT_IDX = 60
            ,CHARGE_DURATION = 8
            ;

    private final static long FLEX_OFFER_SEQUENCE = 1L;

    private BigInteger CHARGE_FULL = BigInteger.valueOf(1650 *60 /PTU_DURATION)
            ,MAX_CHARGE = BigInteger.valueOf(750);

    private static final String RESERVED_MESSAGES_FILE = AbstractConfig.getConfigurationFolder() + "reserved_messages.json"
            , PV_ENDPOINT = "MZ29EBX000/usef/" + ElementType.PV
            , BATTERY_ENDPOINT_PREFIX = "MZ29EBX00"
            , BATTERY_ENDPOINT_SUFFIX  = "/usef/" + ElementType.BATTERY
            , UCL = "UCL"
            ;

    private static final LocalDate PERIOD = new LocalDate(2016, 4, 1);

    private Properties prop;

    private Map<Integer, BigInteger>  ZIHBatteryMap
            , NODBatteryMap
            , UCLMap
            ;

    private ReOptimizePortfolio reOptimizePortfolio;

    @Before
    public void init() throws Exception
    {
        setConfigProperties();
        deleteFile();
        createReservedFile();

        PowerMockito.mockStatic(AgrConfiguration.class);
        PowerMockito.when(AgrConfiguration.getConfig(Matchers.anyString(), Matchers.any(LocalDate.class), Matchers.anyInt())).thenReturn(prop);

        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<Integer, BigInteger> > mapRef = new TypeReference<HashMap<Integer, BigInteger>>(){};

        JsonNode root = objectMapper.readTree( new File(AbstractConfig.getConfigurationFolder() + "dummy_data/order.json" ) );

        ZIHBatteryMap = objectMapper.convertValue(root.get(ElementType.BATTERY_ZIH), mapRef);
        NODBatteryMap = objectMapper.convertValue(root.get(ElementType.BATTERY_NOD), mapRef);
        UCLMap = objectMapper.convertValue(root.get(UCL), mapRef);

        reOptimizePortfolio = new ReOptimizePortfolio();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvoke() throws Exception
    {
        Map<String, ReservedDevice> reservedDevices = ReserveDeviceUtil.readReservation(PERIOD.toString(ReserveDeviceUtil.PERIOD_STRING_FORMAT));

        WorkflowContext result = reOptimizePortfolio.invoke(buildContext());
        assertTrue(ReserveDeviceUtil.readReservation(PERIOD.toString(ReserveDeviceUtil.PERIOD_STRING_FORMAT)).isEmpty());

        List<ConnectionPortfolioDto> connections = result.get(OUT.CONNECTION_PORTFOLIO_OUT.name(), List.class);
        List<DeviceMessageDto> deviceMessages = result.get(OUT.DEVICE_MESSAGES_OUT.name(), List.class);

        assertNotNull(connections);
        assertNotNull(deviceMessages);

        assertEquals(SHIFT_REQUESTS, deviceMessages.size());

        Map<String, DeviceMessageDto> deviceMessagePerEndpoint = deviceMessages.stream()
                .collect(Collectors.toMap(DeviceMessageDto::getEndpoint, Function.identity()));

        reservedDevices.forEach((endpoint, reservedDevice) ->
        {
            DeviceMessageDto deviceMessage = deviceMessagePerEndpoint.get(endpoint);
            assertNotNull(deviceMessage);
            assertEquals(1, deviceMessage.getShiftRequestDtos().size());

            ShiftRequestDto shiftRequest = deviceMessage.getShiftRequestDtos().get(0);
            assertEquals(PERIOD, shiftRequest.getDate());
            assertEquals(BigInteger.valueOf(SHIFT_IDX), shiftRequest.getStartDTU());

        });

        Map<String,UdiPortfolioDto> shiftedBatteries = connections.stream()
                                                                  .flatMap(connection -> connection.getUdis()
                                                                  .stream()
                                                                  .filter(udi -> reservedDevices.keySet().contains(udi.getEndpoint())))
                                                                  .collect(Collectors.toMap(UdiPortfolioDto::getEndpoint,Function.identity()));

        assertEquals(SHIFT_REQUESTS, shiftedBatteries.size());
        IntStream.rangeClosed(1, SHIFT_REQUESTS).forEach(batteryIdx ->
        {
            if(batteryIdx <= SHIFT_REQUESTS/2)
            {
                UdiPortfolioDto battery = shiftedBatteries.get(BATTERY_ENDPOINT_PREFIX + batteryIdx + BATTERY_ENDPOINT_SUFFIX);
                BigInteger chargeTotal = ZERO;
                for(int ptuIndex = 1; ptuIndex <= PTU_COUNT; ptuIndex++)
                {
                    BigInteger batteryValue = ZIHBatteryMap.get(ptuIndex);
                    chargeTotal = chargeTotal.add(batteryValue);

                    if(chargeTotal.compareTo(CHARGE_FULL) > 0)
                        batteryValue = MAX_CHARGE.compareTo(batteryValue) > 0 ? batteryValue : MAX_CHARGE;

                    else
                        batteryValue = ZERO;

                    ForecastPowerDataDto forecast = battery.getUdiPowerPerDTU().get(ptuIndex).getForecast();

                    assertEquals(ZERO, forecast.getAverageProduction());
                    assertEquals(ZERO, forecast.getPotentialFlexProduction());
                    assertEquals(ZERO, forecast.getPotentialFlexConsumption());
                    assertEquals(ZERO, forecast.getAllocatedFlexProduction());

                    if( ptuIndex >= SHIFT_IDX && ptuIndex <= SHIFT_IDX + CHARGE_DURATION )
                    {
                        assertEquals(batteryValue, forecast.getAverageConsumption());
                        assertEquals(batteryValue, forecast.getAllocatedFlexConsumption());
                    }
                    else
                    {
                        assertEquals(ZERO, forecast.getAverageConsumption());
                        assertEquals(ZERO, forecast.getAllocatedFlexConsumption());
                    }

                }

            }
            else
            {
                UdiPortfolioDto battery = shiftedBatteries.get(BATTERY_ENDPOINT_PREFIX + (ZIH_CONNECTION_COUNT - SHIFT_REQUESTS/2 +batteryIdx) + BATTERY_ENDPOINT_SUFFIX);
                for(int ptuIndex = 1; ptuIndex <= PTU_COUNT; ptuIndex++)
                {
                    ForecastPowerDataDto forecast = battery.getUdiPowerPerDTU().get(ptuIndex).getForecast();

                    assertEquals(ZERO, forecast.getAverageConsumption());
                    assertEquals(ZERO, forecast.getPotentialFlexProduction());
                    assertEquals(ZERO, forecast.getPotentialFlexConsumption());
                    assertEquals(ZERO, forecast. getAllocatedFlexConsumption());

                    if( ptuIndex >= SHIFT_IDX && ptuIndex <= SHIFT_IDX + CHARGE_DURATION )
                    {
                        BigInteger batteryValue = UCLMap.get(ptuIndex);
                        batteryValue = MAX_CHARGE.compareTo(batteryValue) > 0 ? batteryValue : MAX_CHARGE;
                        assertEquals(batteryValue, forecast.getAverageProduction());
                        assertEquals(batteryValue, forecast.getAllocatedFlexProduction());
                    }
                    else
                    {
                        assertEquals(ZERO, forecast.getAverageProduction());
                        assertEquals(ZERO, forecast.getAllocatedFlexProduction());
                    }

                }
            }

        });

    }

    @After
    public void cleanup() throws Exception
    {
        deleteFile();
    }

    private WorkflowContext buildContext()
    {
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(IN.PTU_DURATION.name(), PTU_DURATION);
        context.setValue(IN.PTU_DATE.name(), PERIOD.minusDays(1));
        context.setValue(IN.CURRENT_PTU_INDEX.name(), 1);
        context.setValue(IN.CONNECTION_PORTFOLIO_IN.name(), buildPortfolio());
        context.setValue(IN.UDI_EVENTS.name(), buildUdiEvents());
        context.setValue(IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(), buildMap());
        context.setValue(IN.RECEIVED_FLEXORDER_LIST.name(), buildOrder());
        context.setValue(IN.LATEST_A_PLAN_DTO_LIST.name(), new ArrayList<>());
        context.setValue(IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), new ArrayList<>());

        return context;
    }

    private List<UdiEventDto> buildUdiEvents()
    {
        return  IntStream.rangeClosed(1, CONNECTIONS_COUNT)
                .mapToObj(idx ->
                {
                    UdiEventDto udiEvent = new UdiEventDto();
                    udiEvent.setDeviceSelector(BATTERY_ENDPOINT_PREFIX + idx + BATTERY_ENDPOINT_SUFFIX);
                    udiEvent.setUdiEndpoint(BATTERY_ENDPOINT_PREFIX + idx + BATTERY_ENDPOINT_SUFFIX);
                    udiEvent.setPeriod(PERIOD);
                    udiEvent.setId(""+idx);
                    udiEvent.getDeviceCapabilities().add(new ShiftCapabilityDto());
                    udiEvent.setUdiEventType(UdiEventTypeDto.CONSUMPTION);
                    udiEvent.setStartAfterDtu(1);
                    udiEvent.setStartDtu(1);
                    udiEvent.setEndDtu(PTU_COUNT);
                    return udiEvent;

                })
                .collect(Collectors.toList());
    }

    private List<FlexOrderDto> buildOrder()
    {
        List<FlexOrderDto> flexOrderDtos = new ArrayList<>();

        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setFlexOfferSequenceNumber(FLEX_OFFER_SEQUENCE);
        flexOrderDto.setAcknowledgementStatus(AcknowledgementStatusDto.ACCEPTED);
        flexOrderDto.setConnectionGroupEntityAddress("EAN.CG.1");
        flexOrderDto.setParticipantDomain("dso.usef-example.com");
        flexOrderDto.setPeriod(PERIOD);
        flexOrderDto.getPtus().addAll(buildPtus());

        flexOrderDtos.add(flexOrderDto);

        return flexOrderDtos;
    }

    private List<PtuFlexOrderDto> buildPtus()
    {
        return IntStream.rangeClosed(1, PTU_COUNT).mapToObj(ptuIndex ->
        {
            PtuFlexOrderDto ptuFlexOrderDto = new PtuFlexOrderDto();
            ptuFlexOrderDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
            ptuFlexOrderDto.setPower(ZERO);
            return ptuFlexOrderDto;
        }).collect(Collectors.toList());
    }

    private Map<String, List<String>> buildMap() {
        Map<String, List<String>> connectionGroupsToConnectionMap = new HashMap<>();
        connectionGroupsToConnectionMap.put("EAN.CG.1", Arrays.asList("EAN.1", "EAN.2", "EAN.3", "EAN.4", "EAN.5"));
        return connectionGroupsToConnectionMap;
    }

    private List<ConnectionPortfolioDto> buildPortfolio()
    {

        return IntStream.rangeClosed(1, CONNECTIONS_COUNT)
                .mapToObj(idx->
                {
                    ConnectionPortfolioDto connection = new ConnectionPortfolioDto("EAN." + idx);

                    connection.getConnectionPowerPerPTU().putAll(buildUclForecast());

                    if( idx <= ZIH_CONNECTION_COUNT)
                    {
                        connection.getUdis().add(new UdiPortfolioDto(PV_ENDPOINT, PTU_COUNT, ElementType.PV));
                        connection.getUdis().add(createZIHBatteryUDI(idx));
                    }
                    else
                        connection.getUdis().add(createNODBatteryUDI(idx));


                    return connection;
                })
                .collect(Collectors.toList());
    }

    private Map<Integer, PowerContainerDto> buildUclForecast()
    {
        return IntStream.rangeClosed(1, PTU_COUNT).mapToObj(ptuIndex ->
        {
            ForecastPowerDataDto forecast = new ForecastPowerDataDto();
            forecast.setUncontrolledLoad(UCLMap.get(ptuIndex));

            PowerContainerDto power = new PowerContainerDto(PERIOD, ptuIndex);
            power.setForecast(forecast);

            return power;
        }).collect(Collectors.toMap(PowerContainerDto::getTimeIndex, Function.identity()));
    }

    private void createReservedFile()
    {
        Map<String, ReservedDevice> shiftRequests = IntStream.rangeClosed(1, SHIFT_REQUESTS)
                .boxed()
                .collect(Collectors.toMap(idx ->
                        {
                            if(idx <= SHIFT_REQUESTS /2)
                                return BATTERY_ENDPOINT_PREFIX + idx + BATTERY_ENDPOINT_SUFFIX;
                            else
                                return BATTERY_ENDPOINT_PREFIX + (ZIH_CONNECTION_COUNT - SHIFT_REQUESTS/2 +idx) + BATTERY_ENDPOINT_SUFFIX;
                        }
                        , idx -> new ReservedDevice(SHIFT_IDX, PERIOD.toString(ReserveDeviceUtil.PERIOD_STRING_FORMAT)) ));

        ReserveDeviceUtil.writeReservation(PERIOD.toString(ReserveDeviceUtil.PERIOD_STRING_FORMAT), shiftRequests);

    }

    private void deleteFile()
    {
        File reserved = new File(RESERVED_MESSAGES_FILE);

        if(reserved.delete())
            System.out.println("file deleted");
        else
            System.out.println("file not deleted");
    }

    private UdiPortfolioDto createNODBatteryUDI( int idx)
    {
        UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto(BATTERY_ENDPOINT_PREFIX + idx + BATTERY_ENDPOINT_SUFFIX, PTU_DURATION, ElementType.BATTERY_NOD);

        BigInteger dischargeTotal = CHARGE_FULL;

        for(int index = 1; index <= PTU_COUNT; index++)
        {
            PowerContainerDto nodPower = new PowerContainerDto(PERIOD, index);
            ForecastPowerDataDto nodForecast = nodPower.getForecast();

            if(index > 28 && index < 94)
            {
                BigInteger batteryForecastValue = NODBatteryMap.get(index);

                if(dischargeTotal.signum() <= 0)
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

    private UdiPortfolioDto createZIHBatteryUDI(int idx)
    {
        UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto(BATTERY_ENDPOINT_PREFIX + idx + BATTERY_ENDPOINT_SUFFIX, PTU_DURATION, ElementType.BATTERY_ZIH);

        BigInteger chargeTotal = ZERO;

        for(int index = 1; index <= PTU_COUNT; index++)
        {
            PowerContainerDto batteryPower = new PowerContainerDto(PERIOD, index);
            ForecastPowerDataDto batteryForecast = new ForecastPowerDataDto();

            BigInteger batteryValue = ZIHBatteryMap.get(index);

            batteryForecast.setUncontrolledLoad(BigInteger.ZERO);
            batteryForecast.setAverageProduction(BigInteger.ZERO);

            if(chargeTotal.compareTo(CHARGE_FULL) < 0)
            {
                chargeTotal = chargeTotal.add(batteryValue);

                BigInteger delta = chargeTotal.subtract(CHARGE_FULL);
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
        prop.setProperty(AgrConfiguration.BATTERY_CHARGE, ""+MAX_CHARGE);
        prop.setProperty(AgrConfiguration.BATTERY_FULLYCHARGED, ""+CHARGE_FULL);
        prop.setProperty(AgrConfiguration.BATTERY_CHARGE_DURATION, ""+CHARGE_DURATION);
    }

}