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
import static java.util.stream.Collectors.*;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.device.request.ConsumptionProductionTypeDto;
import energy.usef.agr.pbcfeederimpl.PbcFeederService;
import energy.usef.agr.util.PowerContainerDtoUtil;
import energy.usef.agr.workflow.validate.flexoffer.FlexOfferDetermineFlexibilityStepParameter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PowerUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexRequestDto;
import energy.usef.core.workflow.dto.USEFRoleDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.inject.Inject;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.workflow.dto.PrognosisDto;

/**
 * Implementation of a workflow step "AGRFlexOfferDetermineFlexibility".
 * <p>
 * The PBC receives the following parameters as input: <ul> <li>PERIOD: period {@link org.joda.time.LocalDate} for which flex
 * requests are processed.</li> <li>PTU_DURATION: duration of a PTU in minutes.</li> <li>LATEST_D_PROGNOSES_DTO_LIST: {@link List}
 * of {@link PrognosisDto}.</li> <li>LATEST_A_PLANS_DTO_LIST : the list of the latest A-Plans {@link
 * PrognosisDto} of the 'A-Plan'type.</li> <li>FLEX_OFFER_DTO_LIST : the list of already placed flex
 * offers {@link FlexOfferDto} for the period.</li> <li>FLEX_REQUEST_DTO_LIST : the list of flex requests {@link FlexRequestDto} to
 * process.</li> <li>CONNECTION_PORTFOLIO_DTO : the current connection portfolio, a list of {@link ConnectionPortfolioDto}.</li>
 * <li>CONNECTION_GROUPS_TO_CONNECTIONS_MAP : a map providing the relationship between a connection group and the connections
 * attached to it.</li> </ul>
 * <p>
 * The PBC returns the following parameters as output: <ul> <li>FLEX_OFFER_DTO_LIST : Flex offer DTO list {@link List} of {@link
 * FlexOfferDto}.</li> </ul>
 * <p>
 * This step generates random flexibility on ptu's where flexibility is requested.
 */
public class AgrFlexOfferDetermineFlexibilityStub implements WorkflowStep {

    public static final int PRECISION_OF_PRICE = 4;
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrFlexOfferDetermineFlexibilityStub.class);
    private static final Random RANDOM = new Random();
    private static final int FLEX_OFFER_EXPIRATION_DAYS = 1;

    @Inject
    private PbcFeederService pbcFeederService;

    private static BigDecimal randomPercentage(int lowerBound, int upperBound) {
        return BigDecimal.valueOf((RANDOM.nextInt(upperBound - lowerBound) + lowerBound) / 100D);
    }

    private static boolean isFlexRequestForCongestionPoint(FlexRequestDto flexRequestDto) {
        return flexRequestDto.getParticipantRole() == USEFRoleDto.DSO;
    }

    private static ConsumptionProductionTypeDto determineConsumptionProductionType(PtuFlexRequestDto ptu) {
        if (ptu == null) {
            return null;
        }
        if (ptu.getPower().signum() == -1) {
            return ConsumptionProductionTypeDto.PRODUCTION;
        } else {
            return ConsumptionProductionTypeDto.CONSUMPTION;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see WorkflowStep#invoke(WorkflowContext)
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.debug("Received context parameters: {}", context);

        LocalDate period = context.get(FlexOfferDetermineFlexibilityStepParameter.IN.PERIOD.name(), LocalDate.class);
        Integer ptuDuration = context.get(FlexOfferDetermineFlexibilityStepParameter.IN.PTU_DURATION.name(), Integer.class);
        List<FlexRequestDto> inputFlexRequests = context.get(FlexOfferDetermineFlexibilityStepParameter.IN.FLEX_REQUEST_DTO_LIST.name(), List.class);
        List<FlexOfferDto> existingFlexOffers = context.get(FlexOfferDetermineFlexibilityStepParameter.IN.FLEX_OFFER_DTO_LIST.name(), List.class);
        // build maps of:  ConnectionGroup > List<ConnectionDto>
        Map<String, List<ConnectionPortfolioDto>> connectionPortfolioDtoMap = buildConnectionDtoPerConnectionGroup(context);

        // retrieve map of flexible potential per congestion point
        Map<String, Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>>> potentialFlexPerConnectionGroupPerPtu =
                fetchPotentialFlex(
                        connectionPortfolioDtoMap, period, ptuDuration);
        Map<String, Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>>> offeredFlexPerConnectionGroupPerPtu =
                fetchAlreadyOfferedFlex(
                        existingFlexOffers);

        LocalDateTime now = DateTimeUtil.getCurrentDateTime();

        // retrieve APX prices from the pbc feeder
        final int ptusPerDay = Days.ONE.toStandardMinutes().getMinutes() / context.get(
                FlexOfferDetermineFlexibilityStepParameter.IN.PTU_DURATION.name(), Integer.class);
        Map<Integer, BigDecimal> apxPrices = pbcFeederService.retrieveApxPrices(period, 1, ptusPerDay);
        List<FlexOfferDto> outputFlexOffers = new ArrayList<>();
        for (FlexRequestDto flexRequestDto : inputFlexRequests) {
            boolean congestionPointContext = isFlexRequestForCongestionPoint(flexRequestDto);
            String connectionGroup = flexRequestDto.getConnectionGroupEntityAddress();
            // possibleFlex = potentialFlex - offered_flex
            Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>> possibleFlexPerPtu = fetchPossibleFlex(
                    potentialFlexPerConnectionGroupPerPtu.get(connectionGroup),
                    offeredFlexPerConnectionGroupPerPtu.getOrDefault(connectionGroup, new HashMap<>()));
            // create new flex offer
            FlexOfferDto flexOfferDto = new FlexOfferDto();
            flexOfferDto.setFlexRequestSequenceNumber(flexRequestDto.getSequenceNumber());
            flexOfferDto.setPeriod(flexRequestDto.getPeriod());
            flexOfferDto.setConnectionGroupEntityAddress(flexRequestDto.getConnectionGroupEntityAddress());
            flexOfferDto.setParticipantDomain(flexRequestDto.getParticipantDomain());
            flexOfferDto.setExpirationDateTime(now.plusDays(FLEX_OFFER_EXPIRATION_DAYS).withTime(0, 0, 0, 0));

            // for each ptu of the flex request
            for (PtuFlexRequestDto ptu : flexRequestDto.getPtus()) {
                BigInteger maxPotentialFlex;
                BigDecimal price;
                if (ptu.getDisposition() == DispositionTypeDto.AVAILABLE) {
                    maxPotentialFlex = ZERO;
                    price = BigDecimal.ZERO;
                } else {
                    maxPotentialFlex = determineMaxPotentialFlex(possibleFlexPerPtu, ptu);
                    price = determinePrice(possibleFlexPerPtu, apxPrices, ptu, congestionPointContext, ptuDuration);
                }

                PtuFlexOfferDto ptuFlexOfferDto = new PtuFlexOfferDto();
                ptuFlexOfferDto.setPower(maxPotentialFlex);
                ptuFlexOfferDto.setPrice(price.setScale(PRECISION_OF_PRICE, RoundingMode.HALF_UP));
                ptuFlexOfferDto.setPtuIndex(ptu.getPtuIndex());
                LOGGER.trace("{} added to {}.", ptuFlexOfferDto, flexOfferDto);
                flexOfferDto.getPtus().add(ptuFlexOfferDto);
            }
            outputFlexOffers.add(flexOfferDto);
        }
        context.setValue(FlexOfferDetermineFlexibilityStepParameter.OUT.FLEX_OFFER_DTO_LIST.name(), outputFlexOffers);
        return context;
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<ConnectionPortfolioDto>> buildConnectionDtoPerConnectionGroup(WorkflowContext context) {
        Map<String, List<String>> congestionPointsToConnections = (Map<String, List<String>>) context.get(
                FlexOfferDetermineFlexibilityStepParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(), List.class);
        Map<String, ConnectionPortfolioDto> connectionPortfolioPerAddress = (Map<String, ConnectionPortfolioDto>) context.get(
                FlexOfferDetermineFlexibilityStepParameter.IN.CONNECTION_PORTFOLIO_DTO.name(), List.class)
                .stream()
                .collect(toMap(ConnectionPortfolioDto::getConnectionEntityAddress, identity()));
        Map<String, List<ConnectionPortfolioDto>> connectionPortfolioPerConnectionGroup = new HashMap<>();
        congestionPointsToConnections.forEach(
                (connectionGroup, connectionList) -> connectionPortfolioPerConnectionGroup.put(connectionGroup,
                        connectionList.stream().map(connectionPortfolioPerAddress::get).collect(toList())));
        return connectionPortfolioPerConnectionGroup;
    }

    /*
     * Collects the potential flex per connection group per PTU, with a distinction between potential flex in consumption and
     * potential flex in production.
     */
    private Map<String, Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>>> fetchPotentialFlex(
            Map<String, List<ConnectionPortfolioDto>> connectionPortfolioDtoMap, LocalDate period, Integer ptuDuration) {
        return connectionPortfolioDtoMap.entrySet()
                .stream()
                .collect(toMap(connectionListForConnectionGroup -> connectionListForConnectionGroup.getKey(),
                        connectionListForConnectionGroup -> fetchPotentialFlexForConnectionList(
                                connectionListForConnectionGroup.getValue(), period, ptuDuration)));
    }

    /*
     * Fetches the potential flex for a list of connections (ConnectionPortfolioDTO), one a given day and with a given PTU duration.
     * This will average the potential flex of each udi per PTU, then sum it as potential flex in consumption or production,
     * depending on its sign.
     */
    private Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>> fetchPotentialFlexForConnectionList(
            List<ConnectionPortfolioDto> connectionPortfolioDtos, LocalDate period, Integer ptuDuration) {
        Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>> potentialFlexPerPtu = new HashMap<>();
        // fetch potential flex at connection level (mainly for Non-UDI)
        connectionPortfolioDtos.stream().map(ConnectionPortfolioDto::getConnectionPowerPerPTU)
                .forEach(powerContainerDtoPerPtu -> powerContainerDtoPerPtu.forEach(
                        (ptuIndex, powerContainer) -> buildPotentialFlexMapForPtu(potentialFlexPerPtu, ptuIndex, powerContainer)));
        // fetch potential flex at UDI level
        connectionPortfolioDtos.stream()
                .flatMap(connection -> connection.getUdis().stream())
                .map(udi -> PowerContainerDtoUtil.average(udi, period, ptuDuration))
                .forEach(powerContainerPerPtu -> powerContainerPerPtu.forEach(
                        (ptuIndex, powerContainer) -> buildPotentialFlexMapForPtu(potentialFlexPerPtu, ptuIndex, powerContainer)));
        return potentialFlexPerPtu;
    }

    /*
     * Populate a potential flex map for the given PTU with the given Power Container. This method will give the potential flex
     * in production and the potential flex in consumption for the PTU.
     */
    private void buildPotentialFlexMapForPtu(Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>> potentialFlexPerPtu,
            Integer ptuIndex, PowerContainerDto powerContainer) {
        Map<ConsumptionProductionTypeDto, BigInteger> powerMap = potentialFlexPerPtu.getOrDefault(ptuIndex,
                new HashMap<>());
        if (!potentialFlexPerPtu.containsKey(ptuIndex)) {
            potentialFlexPerPtu.put(ptuIndex, powerMap);
        }
        BigInteger potentialFlexConsumption = powerContainer.getForecast().getPotentialFlexConsumption();
        if (potentialFlexConsumption == null) {
            powerMap.put(ConsumptionProductionTypeDto.CONSUMPTION, ZERO);
        } else if (potentialFlexConsumption.signum() >= 0) {
            powerMap.put(
                    ConsumptionProductionTypeDto.CONSUMPTION, powerMap.getOrDefault(ConsumptionProductionTypeDto.CONSUMPTION, ZERO).add(potentialFlexConsumption));
        } else {
            powerMap.put(
                    ConsumptionProductionTypeDto.PRODUCTION, powerMap.getOrDefault(ConsumptionProductionTypeDto.PRODUCTION, ZERO).add(potentialFlexConsumption.abs()));
        }
        BigInteger potentialFlexProduction = powerContainer.getForecast().getPotentialFlexProduction();
        if (potentialFlexProduction == null) {
            powerMap.put(ConsumptionProductionTypeDto.PRODUCTION, ZERO);
        } else if (potentialFlexProduction.signum() >= 0) {
            powerMap.put(
                    ConsumptionProductionTypeDto.PRODUCTION, powerMap.getOrDefault(ConsumptionProductionTypeDto.PRODUCTION, ZERO).add(potentialFlexProduction));
        } else {
            powerMap.put(
                    ConsumptionProductionTypeDto.CONSUMPTION, powerMap.getOrDefault(ConsumptionProductionTypeDto.CONSUMPTION, ZERO).add(potentialFlexProduction.abs()));
        }
    }

    /*
     * Fetches the flex power already offered, which is the sum of power in the offers per connection group per PTU.
     * The use of a flatMapping collector with the stream allows to create a stream of all the PTUs of all the flex offers, and
     * then do the sum of all the powers (doing the distinction between CONSUMPTION and PRODUCTION).
     */
    private Map<String, Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>>> fetchAlreadyOfferedFlex(
            List<FlexOfferDto> existingFlexOffers) {
        // create a function to convert a PtuFlexOffer object to a powerMap like Map[CONSUMPTION: power, PRODUCTION: abs(power)]
        Function<PtuFlexOfferDto, Map<ConsumptionProductionTypeDto, BigInteger>> ptuFlexOfferToPowerMapFunction = ptu -> {
            if (ptu == null || ptu.getPower() == null) {
                return new HashMap<>();
            }
            Map<ConsumptionProductionTypeDto, BigInteger> result = new HashMap<>();
            if (ptu.getPower().signum() == -1) {
                result.put(ConsumptionProductionTypeDto.CONSUMPTION, ZERO);
                result.put(ConsumptionProductionTypeDto.PRODUCTION, ptu.getPower().abs());
            } else {
                result.put(ConsumptionProductionTypeDto.PRODUCTION, ZERO);
                result.put(ConsumptionProductionTypeDto.CONSUMPTION, ptu.getPower());
            }
            return result;
        };
        /*
         * create a function to sum the power maps:
         * map[CONSUMPTION] = map1[CONSUMPTION] + map2[CONSUMPTION]
         * map[PRODUCTION] = map1[PRODUCTION] + map2[PRODUCTION]
         */
        BiFunction<Map<ConsumptionProductionTypeDto, BigInteger>, Map<ConsumptionProductionTypeDto, BigInteger>,
                Map<ConsumptionProductionTypeDto, BigInteger>> mapAdder = (map1, map2) -> {
            Map<ConsumptionProductionTypeDto, BigInteger> result = new HashMap<>();
            result.put(ConsumptionProductionTypeDto.CONSUMPTION, map1.getOrDefault(ConsumptionProductionTypeDto.CONSUMPTION, ZERO).add(map2.getOrDefault(
                    ConsumptionProductionTypeDto.CONSUMPTION, ZERO)));
            result.put(ConsumptionProductionTypeDto.PRODUCTION, map1.getOrDefault(ConsumptionProductionTypeDto.PRODUCTION, ZERO).add(map2.getOrDefault(
                    ConsumptionProductionTypeDto.PRODUCTION, ZERO)));
            return result;
        };
        return existingFlexOffers.stream()
                .collect(groupingBy(FlexOfferDto::getConnectionGroupEntityAddress,
                        flatMapping(flexOfferDto -> flexOfferDto.getPtus().stream(),
                                groupingBy(ptuFlexOfferDto -> ptuFlexOfferDto.getPtuIndex().intValue(),
                                        reducing(new HashMap<>(), ptuFlexOfferToPowerMapFunction::apply, mapAdder::apply)))));
    }

    /*
     * Fetches the possible flex per PTU (with a distinction between CONSUMPTION and PRODUCTION), which is a substraction of the
     * potential flex (known to be positive) by the already offered flex (positive or negative).
     */
    private Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>> fetchPossibleFlex(
            Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>> potentialFlexPerPtu,
            Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>> offeredFlexPerPtu) {
        Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>> possibleFlexPerPtu = new HashMap<>();
        // this operation may produce negative possible flex. Negative possible flex has to be considered as 0 available flex (it
        // means that too much flex has been offered already)
        for (Integer ptuIndex : potentialFlexPerPtu.keySet()) {
            Map<ConsumptionProductionTypeDto, BigInteger> possibleFlex = new HashMap<>();
            possibleFlexPerPtu.put(ptuIndex, possibleFlex);
            possibleFlex.put(ConsumptionProductionTypeDto.CONSUMPTION, potentialFlexPerPtu.getOrDefault(ptuIndex, new HashMap<>())
                    .getOrDefault(ConsumptionProductionTypeDto.CONSUMPTION, ZERO)
                    .subtract(offeredFlexPerPtu.getOrDefault(ptuIndex, new HashMap<>()).getOrDefault(
                            ConsumptionProductionTypeDto.CONSUMPTION, ZERO)));
            possibleFlex.put(ConsumptionProductionTypeDto.PRODUCTION, potentialFlexPerPtu.getOrDefault(ptuIndex, new HashMap<>())
                    .getOrDefault(ConsumptionProductionTypeDto.PRODUCTION, ZERO)
                    .subtract(offeredFlexPerPtu.getOrDefault(ptuIndex, new HashMap<>()).getOrDefault(
                            ConsumptionProductionTypeDto.PRODUCTION, ZERO)));
        }
        return possibleFlexPerPtu;
    }

    private BigInteger determineMaxPotentialFlex(Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>> possibleFlexPerPtu,
            PtuFlexRequestDto ptu) {
        ConsumptionProductionTypeDto type = determineConsumptionProductionType(ptu);
        Integer ptuIndex = ptu.getPtuIndex().intValue();
        BigInteger maxPotentialFlex;
        BigInteger possibleFlex = possibleFlexPerPtu.get(ptuIndex).get(type);
        if (possibleFlex.signum() < 1) {
            // possible flex is negative or 0, which means no more flex can be exchanged.
            return ZERO;
        }
        if (possibleFlex.compareTo(ptu.getPower().abs()) == -1) {
            // maximum flex is the potentialFlex since potentialFlex < RequestedFlex
            maxPotentialFlex = possibleFlex;
            LOGGER.trace("Maximum flexibility is the potential flexibility [{}] (requested flex: {})", maxPotentialFlex,
                    ptu.getPower());
        } else {
            // maximum flex is the requested flex since potentialFlex >= RequestedFlex
            maxPotentialFlex = ptu.getPower();
            LOGGER.trace("Maximum flexibility is the requested flexibility [{}] (potential flex: {})", maxPotentialFlex,
                    possibleFlex);
        }
        if (type == ConsumptionProductionTypeDto.PRODUCTION) {
            maxPotentialFlex = maxPotentialFlex.abs().negate();
        }
        return maxPotentialFlex;
    }

    private BigDecimal determinePrice(Map<Integer, Map<ConsumptionProductionTypeDto, BigInteger>> possibleFlexPerPtu,
            Map<Integer, BigDecimal> apxPrices, PtuFlexRequestDto ptu, boolean congestionPointContext, Integer ptuDuration) {
        ConsumptionProductionTypeDto type = determineConsumptionProductionType(ptu);
        Integer ptuIndex = ptu.getPtuIndex().intValue();
        BigDecimal price;
        BigDecimal ptuApxPrice = PowerUtil.megaWattHourPriceToWattPricePerPtu(apxPrices.get(ptuIndex), ptuDuration);
        boolean maximumFlexIsPotentialFlex = possibleFlexPerPtu.get(ptuIndex).get(type).compareTo(ptu.getPower().abs()) == -1;
        if (congestionPointContext) {
            // CongestionPoint case
            if (ptuApxPrice.compareTo(BigDecimal.ZERO) == 1) {
                // Positive APX Price
                price = maximumFlexIsPotentialFlex ?
                        ptuApxPrice.multiply(randomPercentage(100, 120)) :
                        ptuApxPrice.multiply(randomPercentage(80, 100));
            } else {
                // Negative/Zero APX Price
                price = maximumFlexIsPotentialFlex ?
                        ptuApxPrice.multiply(randomPercentage(80, 100)) :
                        ptuApxPrice.multiply(randomPercentage(100, 120));
            }
        } else {
            // BRP case
            if (ptuApxPrice.compareTo(BigDecimal.ZERO) == 1) {
                // Positive APX Price
                price = maximumFlexIsPotentialFlex ?
                        ptuApxPrice.multiply(randomPercentage(70, 90)) :
                        ptuApxPrice.multiply(randomPercentage(50, 70));
            } else {
                // Negative/Zero APX Price
                price = maximumFlexIsPotentialFlex ?
                        ptuApxPrice.multiply(randomPercentage(50, 70)) :
                        ptuApxPrice.multiply(randomPercentage(70, 90));
            }
        }
        return price;
    }
}
