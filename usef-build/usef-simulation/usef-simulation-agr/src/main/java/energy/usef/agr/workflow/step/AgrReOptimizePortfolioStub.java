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

package energy.usef.agr.workflow.step;

import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.dto.device.capability.DeviceCapabilityDto;
import energy.usef.agr.dto.device.capability.ReduceCapabilityDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.dto.device.request.ConsumptionProductionTypeDto;
import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.dto.device.request.ReduceRequestDto;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PrognosisDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a workflow step to simulation the behavior of an Aggregator Re-optimize portfolio for udi aggregators.
 * This implementation only reduce the forecast consumption or production of the udis. It will not do anything with increase of
 * consumption or production.
 * <p>
 * The calculation done in this PBC is:
 * <li>1. Calculate the target power per ptu with target = prognosis + ordered - summed forecast consumption of all udis </li>
 * <li>2. Calculate the factor per ptu with factor = target / summed potential flex consumption for all udis </li>
 * <li>3. Calculate the new forecast consumption/production per udi per ptu/dtu with forecast = forecast + potential flex *
 * factor</li>
 * <p>
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
 * <p>
 * Note: The device messages created in this PBC do NOT match with the changed forecasts in the re-optimized connection portfolio!
 */
public class AgrReOptimizePortfolioStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrReOptimizePortfolioStub.class);

    // For generating the device messages an extra percentage is added to the calculated target (5%)
    private static final BigDecimal DEVICE_MESSAGE_TARGET_FACTOR = new BigDecimal(1.05);

    /**
     * Invoke step to re-optimize the portfolio (adjust the forecast's of the udi's)
     *
     * @param context incoming workflow context
     * @return WorkflowContext containing a new list of deviceMessage
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {

        // Getting input parameters
        int ptuDuration = (int) context.getValue(ReOptimizePortfolioStepParameter.IN.PTU_DURATION.name());
        int currentPtuIndex = (int) context.getValue(ReOptimizePortfolioStepParameter.IN.CURRENT_PTU_INDEX.name());
        LocalDate ptuDate = (LocalDate) context.getValue(ReOptimizePortfolioStepParameter.IN.PTU_DATE.name());
        List<ConnectionPortfolioDto> connectionPortfolio = context.get(ReOptimizePortfolioStepParameter.IN.CONNECTION_PORTFOLIO_IN.name(), List.class);
        List<UdiEventDto> udiEvents = context.get(ReOptimizePortfolioStepParameter.IN.UDI_EVENTS.name(), List.class);
        Map<String, List<String>> connectionGroupsToConnectionMap = context.get(ReOptimizePortfolioStepParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(),
                HashMap.class);
        List<FlexOrderDto> flexOrders = context.get(ReOptimizePortfolioStepParameter.IN.RECEIVED_FLEXORDER_LIST.name(), List.class);
        List<PrognosisDto> dPrognosis = context.get(ReOptimizePortfolioStepParameter.IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), List.class);
        List<PrognosisDto> aPlans = context.get(ReOptimizePortfolioStepParameter.IN.LATEST_A_PLAN_DTO_LIST.name(), List.class);

        LOGGER.info("Aggregator Re-optimize portfolio Stub started with {} connections in the portfolio.",
                connectionPortfolio.size());

        // validate the input
        if (!validateInput(context)) {
            return returnDefaultContext(context);
        }

        // Do some mapping, summing and calculation for quick access later in the process
        Map<String, List<ConnectionPortfolioDto>> connectionPortfolioPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .mapConnectionPortfolioPerConnectionGroup(
                        connectionPortfolio, connectionGroupsToConnectionMap);
        Map<String, Map<Integer, BigInteger>> prognosisPowerPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .mapPrognosisPowerPerPtuPerConnectionGroup(
                        aPlans, dPrognosis);
        Map<String, Map<Integer, BigInteger>> orderedPowerPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .sumOrderedPowerPerPtuPerConnectionGroup(
                        flexOrders);
        Map<String, Map<Integer, BigInteger>> forecastPowerPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .sumForecastPowerPerPtuPerConnectionGroup(
                        connectionPortfolioPerConnectionGroup, ptuDate, ptuDuration);
        Map<String, Map<Integer, BigInteger>> targetPowerPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .fetchTargetPowerPerPtuPerConnectionGroup(
                        prognosisPowerPerPtuPerConnectionGroup, orderedPowerPerPtuPerConnectionGroup,
                        forecastPowerPerPtuPerConnectionGroup);
        Map<String, Map<Integer, BigInteger>> sumPotentialFlexConsumptionPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .sumPotentialFlexConsumptionPerPtuPerConnectionGroup(
                        connectionPortfolioPerConnectionGroup, ptuDate, ptuDuration);
        Map<String, Map<Integer, BigInteger>> sumPotentialFlexProductionPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .sumPotentialFlexProductionPerPtuPerConnectionGroup(
                        connectionPortfolioPerConnectionGroup, ptuDate, ptuDuration);
        Map<String, Map<Integer, BigDecimal>> flexFactorPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .fetchFlexFactorPerPtuPerConnectionGroup(
                        targetPowerPerPtuPerConnectionGroup, sumPotentialFlexConsumptionPerPtuPerConnectionGroup,
                        sumPotentialFlexProductionPerPtuPerConnectionGroup);
        Map<String, List<UdiEventDto>> udiEventsPerUdi = mapUdiEvents(udiEvents);

        logDebugReport(connectionPortfolioPerConnectionGroup, prognosisPowerPerPtuPerConnectionGroup, orderedPowerPerPtuPerConnectionGroup, forecastPowerPerPtuPerConnectionGroup, targetPowerPerPtuPerConnectionGroup, sumPotentialFlexConsumptionPerPtuPerConnectionGroup, sumPotentialFlexProductionPerPtuPerConnectionGroup, flexFactorPerPtuPerConnectionGroup);


        // Calculate the new portfolio consumption  / production forecast (consumption/production + potentialFlex * factor)
        calculatePortfolioForecast(connectionPortfolioPerConnectionGroup, flexFactorPerPtuPerConnectionGroup, ptuDate,
                currentPtuIndex, ptuDuration);

        // Create reduce requests to match the new target
        List<DeviceMessageDto> deviceMessages = createDeviceMessages(connectionPortfolioPerConnectionGroup, udiEventsPerUdi,
                targetPowerPerPtuPerConnectionGroup, ptuDate, ptuDuration, currentPtuIndex);

        context.setValue(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name(), connectionPortfolio);
        context.setValue(ReOptimizePortfolioStepParameter.OUT.DEVICE_MESSAGES_OUT.name(), deviceMessages);

        return context;
    }

    private void logDebugReport(Map<String, List<ConnectionPortfolioDto>> connectionPortfolioPerConnectionGroup, Map<String, Map<Integer, BigInteger>> prognosisPowerPerPtuPerConnectionGroup, Map<String, Map<Integer, BigInteger>> orderedPowerPerPtuPerConnectionGroup, Map<String, Map<Integer, BigInteger>> forecastPowerPerPtuPerConnectionGroup, Map<String, Map<Integer, BigInteger>> targetPowerPerPtuPerConnectionGroup, Map<String, Map<Integer, BigInteger>> sumPotentialFlexConsumptionPerPtuPerConnectionGroup, Map<String, Map<Integer, BigInteger>> sumPotentialFlexProductionPerPtuPerConnectionGroup, Map<String, Map<Integer, BigDecimal>> flexFactorPerPtuPerConnectionGroup) {
        // do some logging, useful for debugging or understanding what's happening in unit test
        if (LOGGER.isDebugEnabled()) {
            connectionPortfolioPerConnectionGroup.keySet().forEach(connectionGroupId -> {
                LOGGER.debug("=== ConnectionGroup: {} ===", connectionGroupId);
                prognosisPowerPerPtuPerConnectionGroup.getOrDefault(connectionGroupId, new HashMap<>())
                        .forEach((ptuIndex, prognosis) -> {
                            BigInteger target = targetPowerPerPtuPerConnectionGroup.getOrDefault(connectionGroupId, new HashMap<>())
                                    .getOrDefault(ptuIndex, BigInteger.ZERO);

                            LOGGER.debug("--- PTU Index: {} ---", ptuIndex);
                            LOGGER.debug("Prognosis: {}", prognosis);
                            LOGGER.debug("Ordered Power: {}",
                                    orderedPowerPerPtuPerConnectionGroup.getOrDefault(connectionGroupId, new HashMap<>())
                                            .getOrDefault(ptuIndex, BigInteger.ZERO));
                            LOGGER.debug("Forecast: {}",
                                    forecastPowerPerPtuPerConnectionGroup.getOrDefault(connectionGroupId, new HashMap<>())
                                            .getOrDefault(ptuIndex, BigInteger.ZERO));
                            LOGGER.debug("Target: {} (prognosis + ordered power - forecast)", target);
                            LOGGER.debug("Potential Flex Consumption: {} {}",
                                    sumPotentialFlexConsumptionPerPtuPerConnectionGroup.getOrDefault(connectionGroupId, new HashMap<>())
                                            .getOrDefault(ptuIndex, BigInteger.ZERO), (target.compareTo(BigInteger.ZERO) > 0) ? "" : "*");
                            LOGGER.debug("Potential Flex Production: {} {}",
                                    sumPotentialFlexProductionPerPtuPerConnectionGroup.getOrDefault(connectionGroupId, new HashMap<>())
                                            .getOrDefault(ptuIndex, BigInteger.ZERO), (target.compareTo(BigInteger.ZERO) > 0) ? "*" : "");
                            LOGGER.debug("Flex Factor: {}",
                                    flexFactorPerPtuPerConnectionGroup.getOrDefault(connectionGroupId, new HashMap<>())
                                            .getOrDefault(ptuIndex, BigDecimal.ZERO));
                        });
            });
        }
    }

    private List<DeviceMessageDto> createDeviceMessages(
            Map<String, List<ConnectionPortfolioDto>> connectionPortfolioPerConnectionGroup,
            Map<String, List<UdiEventDto>> udiEventsPerUdi,
            Map<String, Map<Integer, BigInteger>> targetPowerPerPtuPerConnectionGroup, LocalDate ptuDate, int ptuDuration,
            int currentPtuIndex) {
        List<DeviceMessageDto> deviceMessageDtoList = new ArrayList<>();
        boolean isAfterToday = ptuDate.isAfter(DateTimeUtil.getCurrentDate());
        connectionPortfolioPerConnectionGroup.forEach(
                (connectionGroupId, connectionPortfolioList) -> targetPowerPerPtuPerConnectionGroup.get(connectionGroupId)
                        .entrySet().stream()
                        .filter(entry -> isAfterToday || entry.getKey() >= currentPtuIndex)
                        .forEach(entry -> {
                            int ptuIndex = entry.getKey();
                            BigInteger targetPower = entry.getValue();
                            List<UdiPortfolioDto> udis = connectionPortfolioList.stream().map(ConnectionPortfolioDto::getUdis)
                                    .flatMap(Collection::stream).collect(Collectors.toList());

                            distributeTargetPower(udiEventsPerUdi, ptuDate, ptuDuration, deviceMessageDtoList, ptuIndex,
                                    targetPower, udis);
                        }));

        return deviceMessageDtoList;
    }

    private enum Direction {
        PLUS, MIN
    }

    private void distributeTargetPower(Map<String, List<UdiEventDto>> udiEventsPerUdi, LocalDate ptuDate, int ptuDuration,
                                       List<DeviceMessageDto> deviceMessageDtoList, int ptuIndex, BigInteger targetPower, List<UdiPortfolioDto> udis) {
        if (targetPower.equals(BigInteger.ZERO)) {
            // nothing to do
            return;
        }

        BigInteger targetPowerWithFactor = new BigDecimal(targetPower).multiply(DEVICE_MESSAGE_TARGET_FACTOR)
                .toBigInteger();

        Direction direction = (targetPowerWithFactor.compareTo(BigInteger.ZERO) < 0) ? Direction.MIN : Direction.PLUS;
        BigInteger toBeDistributed = targetPowerWithFactor.abs();

        for (UdiPortfolioDto udi : udis) {

            List<UdiEventDto> udiEvents = udiEventsPerUdi.get(udi.getEndpoint());

            // only continue if there are udi events for this udi
            if (udiEvents == null || udiEvents.isEmpty()) {
                continue;
            }

            if (direction == Direction.MIN) {
                // to be distributed < 0, reduce consumption
                toBeDistributed = createReduceRequestsForUdi(udi, udiEvents, deviceMessageDtoList, toBeDistributed, ptuDate,
                        ptuIndex, ptuDuration, ConsumptionProductionTypeDto.CONSUMPTION);
            } else if (direction == Direction.PLUS) {
                // to be distributed > 0, reduce production
                toBeDistributed = createReduceRequestsForUdi(udi, udiEvents, deviceMessageDtoList, toBeDistributed, ptuDate,
                        ptuIndex, ptuDuration, ConsumptionProductionTypeDto.PRODUCTION);
            }

            // stop if all to be distributed power has been distributed
            if (toBeDistributed.compareTo(BigInteger.ZERO) <= 0) {
                return;
            }
        }

        // log the remaining power if there is power remaining after distribution
        if (toBeDistributed.compareTo(BigInteger.ZERO) > 0) {
            LOGGER.warn("There is still some power ({}) left after trying to distribute the target power {} "
                            + "amongst all udi's with ReduceCapabilities for ptu index {}.", toBeDistributed, targetPowerWithFactor,
                    ptuIndex);
        }
    }

    private BigInteger createReduceRequestsForUdi(UdiPortfolioDto udi, List<UdiEventDto> udiEvents,
                                                  List<DeviceMessageDto> deviceMessageDtoList, BigInteger initialPowerToBeDistributed, LocalDate ptuDate, int ptuIndex,
                                                  int ptuDuration, ConsumptionProductionTypeDto consumptionProductionTypeDto) {

        BigInteger toBeDistributed = initialPowerToBeDistributed;

        int dtusPerPtu = ptuDuration / udi.getDtuSize();
        int startDtu = 1 + ((ptuIndex - 1) * dtusPerPtu);

        BigInteger allocatedFlex;
        if (consumptionProductionTypeDto == ConsumptionProductionTypeDto.CONSUMPTION) {
            allocatedFlex = udi.getUdiPowerPerDTU().get(startDtu).getForecast().getAllocatedFlexConsumption();
        } else {
            allocatedFlex = udi.getUdiPowerPerDTU().get(startDtu).getForecast().getAllocatedFlexProduction();
        }
        if (allocatedFlex == null) {
            allocatedFlex = BigInteger.ZERO;
        }

        DeviceMessageDto deviceMessageDto = new DeviceMessageDto();
        deviceMessageDto.setEndpoint(udi.getEndpoint());

        // Loop through all the udi events for this udi that are valid for all dtus within current ptu index
        List<UdiEventDto> filteredUdiEvents = udiEvents.stream()
                .filter(udiEventDto -> udiEventDto.getStartDtu().compareTo(startDtu) <= 0)
                .filter(udiEventDto -> udiEventDto.getEndDtu().compareTo(startDtu + dtusPerPtu) >= 0)
                .collect(Collectors.toList());

        for (UdiEventDto udiEventDto : filteredUdiEvents) {

            // get the first reduce (consumption / production) capability
            Optional<DeviceCapabilityDto> deviceCapability = udiEventDto.getDeviceCapabilities()
                    .stream()
                    .filter(deviceCapabilityDto -> deviceCapabilityDto instanceof ReduceCapabilityDto)
                    .filter(deviceCapabilityDto -> ((ReduceCapabilityDto) deviceCapabilityDto).getConsumptionProductionType()
                            == consumptionProductionTypeDto)
                    .findFirst();

            if (deviceCapability.isPresent()) {
                ReduceCapabilityDto capability = (ReduceCapabilityDto) deviceCapability.get();

                BigInteger reducePower = calculateReducePower(toBeDistributed, allocatedFlex, capability);

                if (reducePower.compareTo(BigInteger.ZERO) < 0) {
                    // create one device message for all dtus within current ptu
                    deviceMessageDto.getReduceRequestDtos()
                            .add(createReduceRequest(ptuDate, dtusPerPtu, startDtu, udiEventDto, reducePower,
                                    consumptionProductionTypeDto));
                    deviceMessageDtoList.add(deviceMessageDto);

                    if (consumptionProductionTypeDto == ConsumptionProductionTypeDto.CONSUMPTION) {
                        updateAllocatedFlexConsumption(udi, dtusPerPtu, startDtu, allocatedFlex, reducePower);
                    } else {
                        updateAllocatedFlexProduction(udi, dtusPerPtu, startDtu, allocatedFlex, reducePower);
                    }
                }

                // keep track of all the amount of power that already is reduced (reducePower is always negative, toBeDistributed
                // is always positive) and stop if all to be distributed power has been distributed
                toBeDistributed = toBeDistributed.add(reducePower);
                if (toBeDistributed.compareTo(BigInteger.ZERO) <= 0) {
                    break;
                }
            }
        }

        return toBeDistributed;
    }

    private BigInteger calculateReducePower(BigInteger toBeDistributed, BigInteger allocatedFlex, ReduceCapabilityDto capability) {
        // calculate the max power that can be reduced for this capability
        BigInteger maxReduce = capability.getMinPower().subtract(allocatedFlex);

        // use the smallest value for further calculation (maxReduce should always be a negative value, toBeDistributed is always a
        // positive value)
        maxReduce = (toBeDistributed.compareTo(maxReduce.abs()) < 0) ? toBeDistributed.negate() : maxReduce;

        // how many times can we reduce the power using the power step size
        BigInteger stepCounter = new BigDecimal(maxReduce).divide(new BigDecimal(capability.getPowerStep()),
                RoundingMode.UP).toBigInteger();

        if (stepCounter.compareTo(BigInteger.ZERO) <= 0) {
            return BigInteger.ZERO;
        }

        // calculate the power we are going to reduce for this particular capability
        return capability.getPowerStep().multiply(stepCounter);
    }

    private void updateAllocatedFlexConsumption(UdiPortfolioDto udi, int dtusPerPtu, int startDtu, BigInteger allocatedFlex,
                                                BigInteger reducePower) {
        // store the allocated flex for this udi and all dtus affected
        for (int dtuIndex = startDtu; dtuIndex < startDtu + dtusPerPtu; dtuIndex++) {
            udi.getUdiPowerPerDTU()
                    .get(dtuIndex)
                    .getForecast()
                    .setAllocatedFlexConsumption(allocatedFlex.add(reducePower));
        }
    }

    private void updateAllocatedFlexProduction(UdiPortfolioDto udi, int dtusPerPtu, int startDtu, BigInteger allocatedFlex,
                                               BigInteger reducePower) {
        // store the allocated flex for this udi and all dtus affected
        for (int dtuIndex = startDtu; dtuIndex < startDtu + dtusPerPtu; dtuIndex++) {
            udi.getUdiPowerPerDTU().get(dtuIndex).getForecast().setAllocatedFlexProduction(allocatedFlex.add(reducePower));
        }
    }

    private ReduceRequestDto createReduceRequest(LocalDate ptuDate, int dtusPerPtu, int startDtu, UdiEventDto udiEventDto,
                                                 BigInteger reducePower, ConsumptionProductionTypeDto consumptionProductionTypeDto) {
        ReduceRequestDto reduceRequestDto = new ReduceRequestDto();
        reduceRequestDto.setId(UUID.randomUUID().toString());
        reduceRequestDto.setEventID(udiEventDto.getId());
        reduceRequestDto.setDate(ptuDate);
        reduceRequestDto.setStartDTU(BigInteger.valueOf(startDtu));
        reduceRequestDto.setEndDTU(BigInteger.valueOf(startDtu + dtusPerPtu - 1));
        reduceRequestDto.setPower(reducePower);
        reduceRequestDto.setConsumptionProductionType(consumptionProductionTypeDto);
        return reduceRequestDto;
    }

    // Calculate the new portfolio forecasts (forecast = forecast + summedPotentialFlex * factor)
    private void calculatePortfolioForecast(Map<String, List<ConnectionPortfolioDto>> connectionPortfolioPerConnectionGroup,
                                            Map<String, Map<Integer, BigDecimal>> potentialFlexFactorPerPtuPerConnectionGroup, LocalDate ptuDate,
                                            int currentPtuIndex, int ptuDuration) {
        final LocalDate currentDate = DateTimeUtil.getCurrentDate();
        connectionPortfolioPerConnectionGroup.forEach(
                (connectionGroupId, connectionPortfolioList) -> potentialFlexFactorPerPtuPerConnectionGroup.get(connectionGroupId)
                        .forEach((ptuIndex, factor) -> {
                            // only for future period / ptu indices
                            if ((ptuDate.compareTo(currentDate) > 0 || ptuIndex >= currentPtuIndex)
                                    && factor.compareTo(BigDecimal.ZERO) != 0) {
                                connectionPortfolioList.forEach(
                                        connectionPortfolioDTO -> divideFlexOverUdis(connectionPortfolioDTO, factor, ptuIndex,
                                                ptuDuration));
                            }
                        }));
    }

    private void divideFlexOverUdis(ConnectionPortfolioDto connectionPortfolioDTO, BigDecimal factor, int ptuIndex,
                                    int ptuDuration) {

        if (factor.compareTo(BigDecimal.ZERO) == 0) {
            // nothing to do
            return;
        }

        for (UdiPortfolioDto udi : connectionPortfolioDTO.getUdis()) {
            int dtusPerPtu = ptuDuration / udi.getDtuSize();
            int startDtu = 1 + ((ptuIndex - 1) * dtusPerPtu);

            for (int dtuIndex = startDtu; dtuIndex < startDtu + dtusPerPtu; dtuIndex++) {
                PowerContainerDto powerContainer = udi.getUdiPowerPerDTU().get(dtuIndex);

                if (factor.compareTo(BigDecimal.ZERO) > 0) {
                    // factor > 0, reduce consumption (increase of production is not (yet) supported by this stub)
                    // new consumption power = forecast consumption + flex consumption * factor
                    BigDecimal flexConsumption = new BigDecimal(powerContainer.getForecast().getPotentialFlexConsumption());
                    BigInteger factoredFlex = flexConsumption.multiply(factor).toBigInteger();

                    if (factoredFlex.compareTo(BigInteger.ZERO) > 0) {
                        continue;
                    }
                    BigInteger newForecast = powerContainer.getForecast().getAverageConsumption().add(factoredFlex);

                    // consumption cannot be less than 0 (to prevent garbage data if potential flex is more than forecast)
                    if (newForecast.compareTo(BigInteger.ZERO) < 0) {
                        newForecast = BigInteger.ZERO;
                    }

                    powerContainer.getForecast().setAverageConsumption(newForecast);
                } else {
                    // factor < 0, reduce production (increase of consumption is not (yet) supported by this stub)
                    // new production power = forecast production + flex production * factor
                    BigDecimal flexProduction = new BigDecimal(powerContainer.getForecast().getPotentialFlexProduction());
                    BigInteger factoredFlex = flexProduction.multiply(factor).toBigInteger();

                    if (factoredFlex.compareTo(BigInteger.ZERO) < 0) {
                        continue;
                    }
                    BigInteger newForecast = powerContainer.getForecast().getAverageProduction().subtract(factoredFlex);

                    // production cannot be less than 0 (to prevent garbage data if potential flex is more than forecast)
                    if (newForecast.compareTo(BigInteger.ZERO) < 0) {
                        newForecast = BigInteger.ZERO;
                    }

                    powerContainer.getForecast().setAverageProduction(newForecast);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean validateInput(WorkflowContext context) {
        boolean validInput = true;

        LocalDate ptuDate = context.get(ReOptimizePortfolioStepParameter.IN.PTU_DATE.name(), LocalDate.class);
        List<ConnectionPortfolioDto> connectionPortfolio = context.get(ReOptimizePortfolioStepParameter.IN.CONNECTION_PORTFOLIO_IN.name(), List.class);
        List<FlexOrderDto> flexOrders = context.get(ReOptimizePortfolioStepParameter.IN.RECEIVED_FLEXORDER_LIST.name(), List.class);
        List<PrognosisDto> dPrognosis = context.get(ReOptimizePortfolioStepParameter.IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), List.class);
        List<PrognosisDto> aPlans = context.get(ReOptimizePortfolioStepParameter.IN.LATEST_A_PLAN_DTO_LIST.name(), List.class);

        if (connectionPortfolio.isEmpty()) {
            validInput = false;
            LOGGER.info("No connections in portfolio, unable to re-optimize portfolio.");
        }

        LocalDate currentDate = DateTimeUtil.getCurrentDate();
        if (ptuDate.isBefore(currentDate)) {
            validInput = false;
            LOGGER.error("Unable to re-optimize portfolio for periods in the past: {}", ptuDate);
        }

        if ((dPrognosis == null || dPrognosis.isEmpty()) && (aPlans == null || aPlans.isEmpty())) {
            validInput = false;
            LOGGER.error("D-Prognosis and/or A-Plans expected, but non were found in the input.");
        }
        return validInput;
    }

    private Map<String, List<UdiEventDto>> mapUdiEvents(List<UdiEventDto> udiEvents) {
        return udiEvents.stream().collect(Collectors.groupingBy(UdiEventDto::getUdiEndpoint));
    }

    private WorkflowContext returnDefaultContext(WorkflowContext context) {
        context.setValue(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name(), new ArrayList<ConnectionPortfolioDto>());
        context.setValue(ReOptimizePortfolioStepParameter.OUT.DEVICE_MESSAGES_OUT.name(), new ArrayList<DeviceMessageDto>());
        LOGGER.info("Ended AgrReOptimizePortfolioStub workflow with empty return values");
        return context;
    }

}
