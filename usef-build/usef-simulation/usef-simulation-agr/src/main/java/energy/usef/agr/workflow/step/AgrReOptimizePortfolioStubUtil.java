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

import static energy.usef.core.util.StreamUtil.flatMapping;
import static java.math.BigInteger.ZERO;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.util.PowerContainerDtoUtil;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class with logic which can be reused by any PBC in charge of the 'Re-Optimize Portfolio' workflow.
 */
public class AgrReOptimizePortfolioStubUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrReOptimizePortfolioStubUtil.class);
    public static final Map<Integer, BigInteger> EMPTY_MAP = new HashMap<>();

    private AgrReOptimizePortfolioStubUtil() {
        // hide implicit constructor
    }

    /**
     * Creates a map structure linking the connection group entity addresses to their list of {@link ConnectionPortfolioDto} items.
     *
     * @param connectionPortfolio          {@link List} of {@link ConnectionPortfolioDto} the connection portfolio.
     * @param connectionGroupToConnections {@link Map} with {@link String} USEF identifier of the connection group as key and {@link List} of {@link String}
     *                                     connection entity addresses of the connection group as value.
     * @return a {@link Map} of {@link String} USEF identifier of the connection group as key and {@link List} of
     * {@link ConnectionPortfolioDto} partial connection portfolio as value.
     */
    public static Map<String, List<ConnectionPortfolioDto>> mapConnectionPortfolioPerConnectionGroup(
            List<ConnectionPortfolioDto> connectionPortfolio, Map<String, List<String>> connectionGroupToConnections) {
        Map<String, ConnectionPortfolioDto> connectionDtosByAddress = connectionPortfolio.stream()
                .collect(toMap(ConnectionPortfolioDto::getConnectionEntityAddress, identity()));
        return connectionGroupToConnections.entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(connectionDtosByAddress::get).collect(Collectors.toList())));
    }

    /**
     * Groups and sum the power of a list of prognoses per connection group entity address and ptu index.
     *
     * @param aPlans     {@link List} of {@link PrognosisDto} A-Plans.
     * @param dPrognosis {@link List} of {@link PrognosisDto} D-Prognoses.
     * @return a {@link Map} like <code>connection_group_identifier > PTU_index > sum_of_the_power</code>.
     */
    public static Map<String, Map<Integer, BigInteger>> mapPrognosisPowerPerPtuPerConnectionGroup(List<PrognosisDto> aPlans,
                                                                                                  List<PrognosisDto> dPrognosis) {
        // No distinction between a-plans and d-prognosis needed, so join both lists into one
        List<PrognosisDto> prognosisDtos = new ArrayList<>(dPrognosis);
        prognosisDtos.addAll(aPlans);

        return prognosisDtos.stream()
                .collect(groupingBy(PrognosisDto::getConnectionGroupEntityAddress,
                        flatMapping(prognosisDto -> prognosisDto.getPtus().stream(),
                                groupingBy(ptuPrognosisDto -> ptuPrognosisDto.getPtuIndex().intValue(),
                                        reducing(ZERO, PtuPrognosisDto::getPower, BigInteger::add)))));
    }

    /**
     * Groups and sum the power of a list of flex orders per connection group entity address and ptu index.
     *
     * @param flexOrders {@link List} of {@link FlexOrderDto} A-Plans.
     * @return a {@link Map} like <code>connection_group_identifier > PTU_index > sum_of_the_power</code>.
     */
    public static Map<String, Map<Integer, BigInteger>> sumOrderedPowerPerPtuPerConnectionGroup(List<FlexOrderDto> flexOrders) {
        return flexOrders.stream()
                .collect(groupingBy(FlexOrderDto::getConnectionGroupEntityAddress,
                        flatMapping(flexOrderDto -> flexOrderDto.getPtus().stream(),
                                groupingBy(ptuFlexOrderDto -> ptuFlexOrderDto.getPtuIndex().intValue(),
                                        reducing(ZERO, PtuFlexOrderDto::getPower, BigInteger::add)))));
    }

    /**
     * Sums the forecast power values from the connection portfolio per connection group identifier and PTU index.
     *
     * @param connectionPortfolioPerConnectionGroup {@link Map} of {@link String} connection group identifier as key and {@link List} of {@link ConnectionPortfolioDto} as
     *                                              value; the connection portfolio already grouped per connection group identifier.
     * @param period                                {@link LocalDate} the period for which the method is called (needed to know the amount of PTUs for that
     *                                              period).
     * @param ptuDuration                           {@link Integer} the duration of a PTU in minutes (needed to know the amount of PTUs for the period and
     *                                              to know the amount of DTUs per PTU).
     * @return a {@link Map} like <code>connection_group_identifier > PTU_index > sum_of_the_power </code>.
     */
    public static Map<String, Map<Integer, BigInteger>> sumForecastPowerPerPtuPerConnectionGroup(
            Map<String, List<ConnectionPortfolioDto>> connectionPortfolioPerConnectionGroup, LocalDate period,
            Integer ptuDuration) {
        Map<String, Map<Integer, BigInteger>> result = new HashMap<>();
        connectionPortfolioPerConnectionGroup.forEach((connectionGroupId, connectionPortfolioList) -> {
            result.put(connectionGroupId, new HashMap<>());
            addConnectionForecast(result, connectionGroupId, connectionPortfolioList);
            addUdiForecast(period, ptuDuration, result, connectionGroupId, connectionPortfolioList);
        });
        return result;
    }

    private static void addUdiForecast(LocalDate period, Integer ptuDuration, Map<String, Map<Integer, BigInteger>> result,
                                       String connectionGroupId, List<ConnectionPortfolioDto> connectionPortfolioList) {
        connectionPortfolioList.stream()
                .flatMap(connectionPortfolioDTO -> connectionPortfolioDTO.getUdis().stream())
                .map(udi -> PowerContainerDtoUtil.average(udi, period, ptuDuration))
                .forEach(powerContainerPerPtu -> powerContainerPerPtu.forEach((ptuIndex, powerContainerDto) -> {
                    if (powerContainerDto.getForecast() != null) {
                        BigInteger forecast = result.get(connectionGroupId).getOrDefault(ptuIndex, ZERO);
                        forecast = forecast.add(powerContainerDto.getForecast().calculatePower());
                        result.get(connectionGroupId).put(ptuIndex, forecast);
                    }
                }));
    }

    private static void addConnectionForecast(Map<String, Map<Integer, BigInteger>> result, String connectionGroupId,
                                              List<ConnectionPortfolioDto> connectionPortfolioList) {
        connectionPortfolioList.stream()
                .map(ConnectionPortfolioDto::getConnectionPowerPerPTU)
                .forEach(connectionPowerPerPtuMap -> connectionPowerPerPtuMap.forEach((ptuIndex, powerContainerDto) -> {
                    if (powerContainerDto.getForecast() != null) {
                        BigInteger forecast = result.get(connectionGroupId).getOrDefault(ptuIndex, ZERO);
                        forecast = forecast.add(powerContainerDto.getForecast().calculatePower());
                        result.get(connectionGroupId).put(ptuIndex, forecast);
                    }
                }));
    }

    // Map targeted power (prognosis + ordered - forecast) per ptu per connection group

    /**
     * Computes the target power for the portfolio re-optimization per connection group and PTU index using the prognoses power,
     * ordered flex power and forecasted power.
     *
     * @param prognosisPowerPerPtuPerConnectionGroup {@link Map} with the prognoses power aggregated as
     *                                               <code>connection_group_identifier > ptu_index > sum_of_the_power</code>.
     * @param orderedPowerPerPtuPerConnectionGroup   {@link Map} with the ordered power aggregated as
     *                                               <code>connection_group_identifier > ptu_index > sum_of_the_power</code>.
     * @param forecastPowerPerPtuPerConnectionGroup  {@link Map} with the forecasted power aggregated as
     *                                               <code>connection_group_identifier > ptu_index > sum_of_the_power</code>.
     * @return the target power (prognosis + order - forecast) in a {@link Map} aggregated as <code>connection_group_identifier >
     * ptu_index > sum_of_the_power</code>.
     */
    public static Map<String, Map<Integer, BigInteger>> fetchTargetPowerPerPtuPerConnectionGroup(
            Map<String, Map<Integer, BigInteger>> prognosisPowerPerPtuPerConnectionGroup,
            Map<String, Map<Integer, BigInteger>> orderedPowerPerPtuPerConnectionGroup,
            Map<String, Map<Integer, BigInteger>> forecastPowerPerPtuPerConnectionGroup) {
        Map<String, Map<Integer, BigInteger>> result = new HashMap<>();
        forecastPowerPerPtuPerConnectionGroup.forEach((connectionGroupId, forecastPerPtu) -> {
            result.put(connectionGroupId, new HashMap<>());
            final boolean prognosisReceived = prognosisPowerPerPtuPerConnectionGroup.containsKey(connectionGroupId);
            forecastPerPtu.forEach((ptuIndex, forecast) -> {
                BigInteger targetedPower = null;
                if (!prognosisReceived) {
                    targetedPower = ZERO;
                } else {
                    targetedPower = forecast.negate()
                            .add(prognosisPowerPerPtuPerConnectionGroup.getOrDefault(connectionGroupId, EMPTY_MAP)
                                    .getOrDefault(ptuIndex, ZERO))
                            .add(orderedPowerPerPtuPerConnectionGroup.getOrDefault(connectionGroupId, EMPTY_MAP)
                                    .getOrDefault(ptuIndex, ZERO));
                }
                result.get(connectionGroupId).put(ptuIndex, targetedPower);
            });
        });
        return result;
    }

    /**
     * Aggregates the potential flexibility of consumption of the connection portfolio per connection group and PTU index.
     *
     * @param connectionsPerConnectionGroup {@link Map} of {@link String} connection group identifier as key and {@link List} of {@link ConnectionPortfolioDto} as
     *                                      value; the connection portfolio already grouped per connection group identifier.
     * @param period                        {@link LocalDate} the period for which the method is called (needed to know the amount of PTUs for that
     *                                      period).
     * @param ptuDuration                   {@link Integer} the duration of a PTU in minutes (needed to know the amount of PTUs for the period and
     *                                      to know the amount of DTUs per PTU).
     * @return the potential flex of the portfolio aggregated in a {@link Map} as <code>connection_group_identifier > ptu_index >
     * sum_of_the_potential_flex</code>.
     */
    public static Map<String, Map<Integer, BigInteger>> sumPotentialFlexConsumptionPerPtuPerConnectionGroup(
            Map<String, List<ConnectionPortfolioDto>> connectionsPerConnectionGroup, LocalDate period, Integer ptuDuration) {
        Function<List<ConnectionPortfolioDto>, Map<Integer, BigInteger>> sumPotentialFlexConsumptionPerPtu =
                connectionPortfolioDTOs -> connectionPortfolioDTOs.stream()
                        .flatMap(connectionPortfolioDTO -> connectionPortfolioDTO.getConnectionPowerPerPTU().values().stream())
                        .collect(groupingBy(PowerContainerDto::getTimeIndex, reducing(ZERO, powerContainerDto -> {
                            BigInteger allocatedFlexConsumption = value(
                                    powerContainerDto.getForecast().getAllocatedFlexConsumption());
                            BigInteger potentialFlexConsumption = value(
                                    powerContainerDto.getForecast().getPotentialFlexConsumption());
                            return potentialFlexConsumption.subtract(allocatedFlexConsumption);
                        }, BigInteger::add)));

        // sum potential flex consumption from connection level
        Map<String, Map<Integer, BigInteger>> connectionsPotentialFlex = connectionsPerConnectionGroup.entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, connectionsForConnectionGroup -> sumPotentialFlexConsumptionPerPtu.apply(
                        connectionsForConnectionGroup.getValue())));
        // add potential flex consumption from UDI level
        connectionsPerConnectionGroup.forEach((connectionGroupId, connectionDtos) -> connectionDtos.stream()
                .flatMap(connectionPortfolioDTO -> connectionPortfolioDTO.getUdis().stream())
                .map(udi -> PowerContainerDtoUtil.average(udi, period, ptuDuration))
                .forEach(powerContainerDtoPerPtu -> powerContainerDtoPerPtu.forEach((ptuIndex, powerContainerDto) -> {
                    BigInteger potentialFlex = connectionsPotentialFlex.get(connectionGroupId).getOrDefault(ptuIndex, ZERO);
                    BigInteger powercontainerFlex = value(powerContainerDto.getForecast().getPotentialFlexConsumption()).
                            subtract(value(powerContainerDto.getForecast().getAllocatedFlexConsumption()));
                    connectionsPotentialFlex.get(connectionGroupId).put(ptuIndex, potentialFlex.add(powercontainerFlex));
                })));
        return connectionsPotentialFlex;
    }

    /**
     * Aggregates the potential flexibility of production of the connection portfolio per connection group and PTU index.
     *
     * @param connectionsPerConnectionGroup {@link Map} of {@link String} connection group identifier as key and {@link List} of {@link ConnectionPortfolioDto} as
     *                                      value; the connection portfolio already grouped per connection group identifier.
     * @param period                        {@link LocalDate} the period for which the method is called (needed to know the amount of PTUs for that
     *                                      period).
     * @param ptuDuration                   {@link Integer} the duration of a PTU in minutes (needed to know the amount of PTUs for the period and
     *                                      to know the amount of DTUs per PTU).
     * @return the potential flex of the portfolio aggregated in a {@link Map} as <code>connection_group_identifier > ptu_index >
     * sum_of_the_potential_flex</code>.
     */
    public static Map<String, Map<Integer, BigInteger>> sumPotentialFlexProductionPerPtuPerConnectionGroup(
            Map<String, List<ConnectionPortfolioDto>> connectionsPerConnectionGroup, LocalDate period, Integer ptuDuration) {
        Function<List<ConnectionPortfolioDto>, Map<Integer, BigInteger>> sumPotentialFlexProductionPerPtu = connectionPortfolioDTOs -> connectionPortfolioDTOs
                .stream().flatMap(connectionPortfolioDTO -> connectionPortfolioDTO.getConnectionPowerPerPTU().values().stream())
                .collect(groupingBy(PowerContainerDto::getTimeIndex, reducing(ZERO, powerContainerDto -> {
                    BigInteger allocatedFlexProduction = value(powerContainerDto.getForecast().getAllocatedFlexProduction());
                    BigInteger potentialFlexProduction = value(powerContainerDto.getForecast().getPotentialFlexProduction());
                    return potentialFlexProduction.subtract(allocatedFlexProduction);
                }, BigInteger::add)));

        // sum potential flex production from connection level
        Map<String, Map<Integer, BigInteger>> connectionsPotentialFlex = connectionsPerConnectionGroup.entrySet().stream().collect(
                toMap(Map.Entry::getKey, connectionsForConnectionGroup -> sumPotentialFlexProductionPerPtu
                        .apply(connectionsForConnectionGroup.getValue())));
        // add potential flex production from UDI level
        connectionsPerConnectionGroup.forEach((connectionGroupId, connectionDtos) -> connectionDtos.stream()
                .flatMap(connectionPortfolioDTO -> connectionPortfolioDTO.getUdis().stream())
                .map(udi -> PowerContainerDtoUtil.average(udi, period, ptuDuration))
                .forEach(powerContainerDtoPerPtu -> powerContainerDtoPerPtu.forEach((ptuIndex, powerContainerDto) -> {
                    BigInteger potentialFlex = connectionsPotentialFlex.get(connectionGroupId).getOrDefault(ptuIndex, ZERO);
                    BigInteger powercontainerFlex = value(powerContainerDto.getForecast().getPotentialFlexProduction()).
                            subtract(value(powerContainerDto.getForecast().getAllocatedFlexProduction()));
                    connectionsPotentialFlex.get(connectionGroupId).put(ptuIndex, potentialFlex.add(powercontainerFlex));
                })));
        return connectionsPotentialFlex;
    }

    // returns the value or zero is the value is null
    private static BigInteger value(BigInteger theValue) {
        if (theValue == null) {
            return ZERO;
        } else {
            return theValue;
        }
    }

    /**
     * Computes the flex factor for the portfolio re-optimization, using the target power and the potential flex available.
     *
     * @param targetPowerPerPtuPerConnectionGroup                    {@link Map} with the target power aggregated as
     *                                                               <code>connection_group_identifier > ptu_index > sum_of_the_power</code>.
     * @param summedPotentialFlexConsumptionPerPtuPerConnectionGroup {@link Map} with the potential flex consumption aggregated as
     *                                                               <code>connection_group_identifier > ptu_index > sum_of_potential_flex</code>.
     * @param summedPotentialFlexProductionPerPtuPerConnectionGroup  {@link Map} with the potential flex production aggregated as
     *                                                               <code>connection_group_identifier > ptu_index > sum_of_potential_flex</code>.
     * @return the flex factor (target_power / potential_flex, bounded to [0,1]) in a {@link Map} as
     * <code>connection_group_identifier > ptu_index > flex_factor</code>.
     */
    public static Map<String, Map<Integer, BigDecimal>> fetchFlexFactorPerPtuPerConnectionGroup(
            Map<String, Map<Integer, BigInteger>> targetPowerPerPtuPerConnectionGroup,
            Map<String, Map<Integer, BigInteger>> summedPotentialFlexConsumptionPerPtuPerConnectionGroup,
            Map<String, Map<Integer, BigInteger>> summedPotentialFlexProductionPerPtuPerConnectionGroup) {
        Map<String, Map<Integer, BigDecimal>> flexFactorPerPtuPerConnectionGroup = new HashMap<>();

        targetPowerPerPtuPerConnectionGroup.forEach((connectionGroupId, targetPowerPerPtu) -> {
            flexFactorPerPtuPerConnectionGroup.put(connectionGroupId, new HashMap<>());
            targetPowerPerPtu.forEach((ptuIndex, targetPower) -> {
                BigInteger potentialFlex = determinePotentialFlex(summedPotentialFlexConsumptionPerPtuPerConnectionGroup, summedPotentialFlexProductionPerPtuPerConnectionGroup, connectionGroupId, ptuIndex, targetPower);

                if (potentialFlex.equals(ZERO)) {
                    // there is no flex available, so do not flex (factor = ZERO)
                    flexFactorPerPtuPerConnectionGroup.get(connectionGroupId).put(ptuIndex, BigDecimal.ZERO);
                } else {
                    BigDecimal factor = calculateFactor(ptuIndex, potentialFlex, targetPower);

                    flexFactorPerPtuPerConnectionGroup.get(connectionGroupId).put(ptuIndex, factor);
                }
            });
        });

        return flexFactorPerPtuPerConnectionGroup;
    }

    private static BigInteger determinePotentialFlex(Map<String, Map<Integer, BigInteger>> summedPotentialFlexConsumptionPerPtuPerConnectionGroup, Map<String, Map<Integer, BigInteger>> summedPotentialFlexProductionPerPtuPerConnectionGroup, String connectionGroupId, Integer ptuIndex, BigInteger targetPower) {
        BigInteger potentialFlex = ZERO;
        // since we only support reduction of consumption and reduction of production we solve negative targetPowers by
        // reducing the consumption and positive targetPowers by reducing the production.
        if (targetPower.compareTo(BigInteger.ZERO) < 0) {
            potentialFlex = summedPotentialFlexConsumptionPerPtuPerConnectionGroup
                    .getOrDefault(connectionGroupId, new HashMap<>()).getOrDefault(ptuIndex, ZERO);
        } else if (targetPower.compareTo(BigInteger.ZERO) > 0) {
            potentialFlex = summedPotentialFlexProductionPerPtuPerConnectionGroup
                    .getOrDefault(connectionGroupId, new HashMap<>()).getOrDefault(ptuIndex, ZERO);
        }
        return potentialFlex;
    }

    private static BigDecimal calculateFactor(Integer ptuIndex, BigInteger potentialFlex, BigInteger targetPower) {

        // since we only support reduction of consumption or production, the potential flex should always be negative
        // return ZERO if we only have positive potential flex (increase is not supported)
        if (potentialFlex.compareTo(BigInteger.ZERO) >= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal summedPotentialFlex = new BigDecimal(potentialFlex);
        BigDecimal factor = new BigDecimal(targetPower).divide(summedPotentialFlex, 5, BigDecimal.ROUND_CEILING);
        // never exceed factor of 1 to avoid using more flex than available later in the process
        if (factor.compareTo(BigDecimal.ONE) > 0) {
            LOGGER.warn("Potential flex factor for ptu {} is capped to 1, this will cause the target power to be out of reach",
                    ptuIndex);
            factor = BigDecimal.ONE;
        } else if (factor.compareTo(BigDecimal.ONE.negate()) < 0) {
            LOGGER.warn("Potential flex factor for ptu {} is capped to -1, this will cause the target power to be out of reach",
                    ptuIndex);
            factor = BigDecimal.ONE.negate();
        }
        return factor;
    }
}
