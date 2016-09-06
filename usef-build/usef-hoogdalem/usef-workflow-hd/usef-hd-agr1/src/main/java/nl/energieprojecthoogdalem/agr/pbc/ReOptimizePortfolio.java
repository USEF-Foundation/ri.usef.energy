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
import info.usef.agr.dto.ConnectionGroupPortfolioDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.ForecastPowerDataDto;
import info.usef.agr.dto.device.capability.UdiEventDto;
import info.usef.agr.dto.device.request.DeviceMessageDto;
import info.usef.agr.dto.device.request.ShiftRequestDto;
import info.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter.IN;
import info.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter.OUT;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.FlexOrderDto;
import info.usef.core.workflow.dto.PrognosisDto;
import nl.energieprojecthoogdalem.agr.devicemessages.ReservedDevice;
import nl.energieprojecthoogdalem.configurationservice.AgrConfiguration;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import nl.energieprojecthoogdalem.util.ReserveDeviceUtil;
import org.joda.time.LocalDate;


import static java.math.BigInteger.ZERO;

/**
 * The PBC receives the following parameters as input : <ul> <li>PTU_DURATION : PTU duration.</li> <li>CURRENT_PTU_INDEX : Current
 * PTU index.</li> <li>PTU_DATE : Period of re-optimization.</li> <li>CONNECTION_PORTFOLIO_IN : List of connection group portfolios
 * {@link ConnectionPortfolioDto}.</li> <li>CONNECTION_GROUPS_TO_CONNECTIONS_MAP : map giving the relationship between each
 * connection group and its connections.</li> <li>RECEIVED_FLEXORDER_LIST : aggregate info and collection of {@link
 * FlexOrderDto}</li> <li>LATEST_A_PLAN_DTO_LIST : contains list of most recent {@link PrognosisDto} (A-plans)</li>
 * <li>LATEST_D_PROGNOSIS_DTO_LIST : contains list of most recent {@link PrognosisDto} (D-Prognoses)</li>
 * <li>RELEVANT_PROGNOSIS_LIST : contains list of prognosis relevant to FlexOrder.</li> </ul>
 * <p>
 * The PBC must output the modified connection portfolio and device messages: <ul> <li>CONNECTION_PORTFOLIO_OUT : re-optimized
 * connection portfolio {@link ConnectionGroupPortfolioDto}.</li> <li>DEVICE_MESSAGES_OUT: A list of {@link DeviceMessageDto}
 * objects containing the device messages.</li> </ul>
 */
public class ReOptimizePortfolio implements WorkflowStep {
    private int CHARGE_DURATION;
    private BigInteger MAX_CHARGE;

    /**
     * retrieves the flex order, udi events, connection portfolio
     * reads the reserved device json file to create device messages for
     * filters all udis and uncontrolled load for the udis using the endpoint
     * shifts and creates device messages for the selected udis
     * deletes key of reserved device file
     * returns list of {@link DeviceMessageDto}
     *
     * @param context incoming workflow context
     * @return WorkflowContext containing a list of deviceMessage
     */
    @Override
    @SuppressWarnings("unchecked")
    public WorkflowContext invoke(WorkflowContext context)
    {
        //input
        List<FlexOrderDto> flexOrders = context.get(IN.RECEIVED_FLEXORDER_LIST.name(), List.class);
        List<UdiEventDto> udiEvents = context.get(IN.UDI_EVENTS.name(), List.class);
        List<ConnectionPortfolioDto> allConnections = context.get(IN.CONNECTION_PORTFOLIO_IN.name(), List.class);

        //output
        List<DeviceMessageDto> deviceMessages = new ArrayList<>();

        for (FlexOrderDto flexOrder : flexOrders)
        {
            LocalDate period = flexOrder.getPeriod();
            Properties prop = AgrConfiguration.getConfig("REOPTIMIZE_PORTFOLIO", period, context.get(IN.PTU_DURATION.name(), Integer.class));

            MAX_CHARGE = new BigInteger(prop.getProperty(AgrConfiguration.BATTERY_CHARGE));
            CHARGE_DURATION = Integer.parseInt(prop.getProperty(AgrConfiguration.BATTERY_CHARGE_DURATION));

            String key = period.toString(ReserveDeviceUtil.PERIOD_STRING_FORMAT);
            List<UdiPortfolioDto> udis = new ArrayList<>();
            Map<String, Map<Integer, BigInteger>> powerPerEndpointMap = new HashMap<>();
            Map<String, ReservedDevice> reservedDeviceMap = ReserveDeviceUtil.readReservation(key);

            filterUdis(udis, powerPerEndpointMap, allConnections, reservedDeviceMap.keySet());

            udis.forEach(udi ->
            {
                String endpoint = udi.getEndpoint();
                ReservedDevice device = reservedDeviceMap.get(endpoint);
                UdiEventDto udiEvent = findEventForUdi(udiEvents, endpoint);

                switch (udi.getProfile()) {
                    case ElementType.BATTERY_ZIH:
                        shiftZIH(udi, powerPerEndpointMap.get(endpoint), device.getStartIndex());
                        createShiftMessage(deviceMessages, udiEvent, device);
                        break;

                    case ElementType.BATTERY_NOD:
                        shiftNOD(udi, powerPerEndpointMap.get(endpoint), device.getStartIndex());
                        createShiftMessage(deviceMessages, udiEvent, device);
                        break;

                }
            });

            ReserveDeviceUtil.deleteReservation(key);
        }

        context.setValue(OUT.CONNECTION_PORTFOLIO_OUT.name(), allConnections);
        context.setValue(OUT.DEVICE_MESSAGES_OUT.name(), deviceMessages);

        return context;
    }

    /**
     * shifts the ZIH udi forecast
     * fills the forecast values of allocated / avg consumption between the shifted ptu index and 8 following ptus,
     * other values becomes 0
     *
     * @param batteryUdi             the battery {@link UdiPortfolioDto} to shift
     * @param powerPerPtu            the PV - UCL values related to the battery {@link UdiPortfolioDto}
     * @param shiftIdx               the index to shift the battery {@link UdiPortfolioDto} to
     */
    private void shiftZIH(UdiPortfolioDto batteryUdi, Map<Integer, BigInteger> powerPerPtu, int shiftIdx) {
        Map<Integer, PowerContainerDto> powerContainers = batteryUdi.getUdiPowerPerDTU();

        for (int ptuIdx = 1; ptuIdx <= powerContainers.size(); ptuIdx++) {
            ForecastPowerDataDto forecast = powerContainers.get(ptuIdx).getForecast();

            forecast.setPotentialFlexProduction(ZERO);
            forecast.setPotentialFlexConsumption(ZERO);
            forecast.setAverageProduction(ZERO);
            forecast.setAllocatedFlexProduction(ZERO);

            if (ptuIdx >= shiftIdx && ptuIdx <= shiftIdx + CHARGE_DURATION) {
                BigInteger ucl = powerPerPtu.get(ptuIdx);
                BigInteger batteryValue = (MAX_CHARGE.compareTo(ucl) > 0) ? ucl : MAX_CHARGE;

                forecast.setAverageConsumption(batteryValue);
                forecast.setAllocatedFlexConsumption(batteryValue);
            } else {
                forecast.setAverageConsumption(ZERO);
                forecast.setAllocatedFlexConsumption(ZERO);
            }
        }
    }

    /**
     * shifts the NOD udi forecast
     * fills the forecast values of allocated / avg production between the shifted ptu index and 8 following ptus,
     * other values becomes 0
     *
     * @param batteryUdi             the battery {@link UdiPortfolioDto} to shift
     * @param uncontrolledLoadPerPtu the uncontrolled load values related to the battery {@link UdiPortfolioDto}
     * @param shiftIdx               the index to shift the battery {@link UdiPortfolioDto} to
     */
    private void shiftNOD(UdiPortfolioDto batteryUdi, Map<Integer, BigInteger> uncontrolledLoadPerPtu, int shiftIdx) {
        Map<Integer, PowerContainerDto> powerContainers = batteryUdi.getUdiPowerPerDTU();

        for (int ptuIdx = 1; ptuIdx <= powerContainers.size(); ptuIdx++) {
            ForecastPowerDataDto forecast = powerContainers.get(ptuIdx).getForecast();

            forecast.setUncontrolledLoad(ZERO);
            forecast.setPotentialFlexProduction(ZERO);
            forecast.setPotentialFlexConsumption(ZERO);
            forecast.setAverageConsumption(ZERO);
            forecast.setAllocatedFlexConsumption(ZERO);

            if (ptuIdx >= shiftIdx && ptuIdx <= shiftIdx + CHARGE_DURATION) {
                BigInteger ucl = uncontrolledLoadPerPtu.get(ptuIdx);
                BigInteger batteryValue = (MAX_CHARGE.compareTo(ucl) > 0) ? ucl : MAX_CHARGE;

                forecast.setAverageProduction(batteryValue);
                forecast.setAllocatedFlexProduction(batteryValue);
            } else {
                forecast.setAverageProduction(ZERO);
                forecast.setAllocatedFlexProduction(ZERO);
            }
        }
    }

    /**
     * adds a shift message to the list of {@link DeviceMessageDto},
     * uses the id, endpoint of the {@link UdiEventDto},
     * uses the shift index, period of the {@link ReservedDevice}
     *
     * @param deviceMessages the list of {@link DeviceMessageDto} to fill
     * @param udiEvent       the {@link UdiEventDto} to read the id, endpoint from
     * @param device         the {@link ReservedDevice} to read the period, shift index from
     */
    private void createShiftMessage(List<DeviceMessageDto> deviceMessages, UdiEventDto udiEvent, ReservedDevice device) {
        ShiftRequestDto shiftRequest = new ShiftRequestDto();
        shiftRequest.setId(UUID.randomUUID().toString());
        shiftRequest.setEventID(udiEvent.getId());
        shiftRequest.setStartDTU(BigInteger.valueOf(device.getStartIndex()));
        shiftRequest.setDate(device.getPeriod());

        DeviceMessageDto message = new DeviceMessageDto();
        message.setEndpoint(udiEvent.getUdiEndpoint());
        message.getShiftRequestDtos().add(shiftRequest);

        deviceMessages.add(message);
    }

    /**
     * find the specific {@link UdiEventDto} for the {@link UdiPortfolioDto} in the udi event list
     *
     * @param udiEvents the list of {@link UdiEventDto} to search in
     * @param endpoint  the endpoint of the {@link UdiPortfolioDto} we need to find the {@link UdiEventDto} for
     * @return the {@link UdiEventDto} found in the list
     */
    private UdiEventDto findEventForUdi(List<UdiEventDto> udiEvents, String endpoint) {
        return udiEvents.stream()
                .filter(udiEvent -> udiEvent.getUdiEndpoint().equals(endpoint))
                .findFirst()
                .get();
    }

    /**
     * filter all the udis containing the endpoint specified in the endpoints set and puts the udi in the udis list,
     * retrieves the uncontrolled load of the connection containing the filtered udi and puts the forecast in the uncontrolledLoadPerEndpoint map
     *
     * @param udis                           a list that will contain with {@link UdiPortfolioDto}'s we want to shift
     * @param powerPerEndpointMap            a map with endpoints as key and powerPerPtu forecast data as value (UCL for NOD, PV - UCL for ZIH)
     * @param connections                    a list containing {@link ConnectionPortfolioDto}'s with udis to be filtered
     * @param endpoints                      a set of endpoint strings that we want to shift
     */
    private void filterUdis(List<UdiPortfolioDto> udis, Map<String, Map<Integer, BigInteger>> powerPerEndpointMap, List<ConnectionPortfolioDto> connections, Set<String> endpoints) {
        connections.stream().forEach(connection ->
        {
            UdiPortfolioDto battery = connection.getUdis()
                    .stream()
                    .filter(udi -> endpoints.contains(udi.getEndpoint()))
                    .findAny()
                    .orElse(null);

            if (battery != null)
            {
                Map<Integer, BigInteger> powerPerPtu = defaultPowerMap(battery.getDtuSize());

                switch (battery.getProfile())
                {
                    case ElementType.BATTERY_ZIH:
                        powerPerPtu = battery.getUdiPowerPerDTU()
                                             .values()
                                             .stream()
                                             .collect(Collectors.toMap(PowerContainerDto::getTimeIndex, power -> power.getForecast().getPotentialFlexConsumption() ));
                        break;

                    case ElementType.BATTERY_NOD:
                        powerPerPtu = connection.getConnectionPowerPerPTU()
                                                .values()
                                                .stream()
                                                .collect(Collectors.toMap(PowerContainerDto::getTimeIndex, power -> power.getForecast().getUncontrolledLoad()));
                        break;
                }


                udis.add(battery);
                powerPerEndpointMap.put(battery.getEndpoint(), powerPerPtu);
            }
        });
    }

    /**
     * returns a default map with 0 values from 1 to the ptu count number
     * @param size the amount of values to generate 0 values for
     * @return a map with 1 - ptuCount as key and 0 as BigInteger value
     * */
    private Map<Integer, BigInteger> defaultPowerMap(int size)
    {
        return IntStream.rangeClosed(1, size)
                .boxed()
                .collect(Collectors.toMap(Function.identity(),idx -> ZERO));
    }
}
