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

package energy.usef.agr.service.business;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.transaction.Transactional;

import energy.usef.core.util.DateTimeUtil;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.model.ConnectionGroupPowerContainer;
import energy.usef.agr.model.ConnectionPowerContainer;
import energy.usef.agr.model.DeviceMessage;
import energy.usef.agr.model.DeviceMessageStatus;
import energy.usef.agr.model.DeviceRequest;
import energy.usef.agr.model.ForecastPowerData;
import energy.usef.agr.model.IncreaseRequest;
import energy.usef.agr.model.InterruptRequest;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.model.PowerData;
import energy.usef.agr.model.ReduceRequest;
import energy.usef.agr.model.ReportRequest;
import energy.usef.agr.model.ShiftRequest;
import energy.usef.agr.model.Udi;
import energy.usef.agr.model.UdiEvent;
import energy.usef.agr.model.UdiPowerContainer;
import energy.usef.agr.repository.DeviceMessageRepository;
import energy.usef.agr.repository.IncreaseRequestRepository;
import energy.usef.agr.repository.InterruptRequestRepository;
import energy.usef.agr.repository.PowerContainerRepository;
import energy.usef.agr.repository.ReduceRequestRepository;
import energy.usef.agr.repository.ReportRequestRepository;
import energy.usef.agr.repository.ShiftRequestRepository;
import energy.usef.agr.repository.UdiEventRepository;
import energy.usef.agr.repository.UdiRepository;
import energy.usef.agr.transformer.ConnectionPortfolioTransformer;
import energy.usef.agr.transformer.DeviceMessageTransformer;
import energy.usef.agr.transformer.PowerContainerTransformer;
import energy.usef.agr.util.PowerContainerUtil;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.PtuUtil;

/**
 * Service class in charge of operations related to the Aggregator portfolio.
 */
@Stateless
public class AgrPortfolioBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrPortfolioBusinessService.class);

    @Inject
    private Config config;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private UdiRepository udiRepository;

    @Inject
    private UdiEventRepository udiEventRepository;

    @Inject
    private PowerContainerRepository powerContainerRepository;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private DeviceMessageRepository deviceMessageRepository;

    @Inject
    private ReduceRequestRepository reduceRequestRepository;

    @Inject
    private IncreaseRequestRepository increaseRequestRepository;

    @Inject
    private InterruptRequestRepository interruptRequestRepository;

    @Inject
    private ReportRequestRepository reportRequestRepository;

    @Inject
    private ShiftRequestRepository shiftRequestRepository;

    /**
     * Create the Power Containers with the Profile values for each connection in the portfolio.
     *
     * @param period {@link LocalDate} period.
     * @param connectionPortfolioDTOs {@link List} of {@link ConnectionPortfolioDto} with the power values.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createConnectionProfiles(final LocalDate period, List<ConnectionPortfolioDto> connectionPortfolioDTOs) {
        Map<String, Connection> connectionMap = corePlanboardBusinessService.findActiveConnections(period, Optional.empty())
                .stream().collect(Collectors.toMap(Connection::getEntityAddress, Function.identity()));

        for (ConnectionPortfolioDto connectionPortfolioDTO : connectionPortfolioDTOs) {
            final Connection connection = connectionMap.get(connectionPortfolioDTO.getConnectionEntityAddress());
            connectionPortfolioDTO.getConnectionPowerPerPTU().forEach((ptuIndex, powerContainerDto) -> {
                PowerContainer powerContainer = new ConnectionPowerContainer(connection, period, ptuIndex);
                powerContainer.setProfile(new PowerData());
                powerContainer.getProfile().setUncontrolledLoad(powerContainerDto.getProfile().getUncontrolledLoad());
                updatePowerContainerFlexibleLoad(powerContainer, powerContainerDto);
                powerContainerRepository.persist(powerContainer);
            });

        }
    }

    /**
     * Create Missing Udis.
     *
     * @param period the period for creating udis.
     * @param connectionPortfolioDTOs the connection portfolio
     */
    public void createUdis(final LocalDate period, List<ConnectionPortfolioDto> connectionPortfolioDTOs) {
        Map<String, Connection> connectionMap = corePlanboardBusinessService.findActiveConnections(period, Optional.empty())
                .stream().collect(Collectors.toMap(Connection::getEntityAddress, Function.identity()));
        List<Object> toBePersisted = new ArrayList<>();
        Map<String, Udi> activeUdis = udiRepository.findActiveUdisMappedPerEndpoint(DateTimeUtil.getCurrentDate());
        Integer initializationDuration = configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL);
        LocalDate validUntil = period.plusDays(initializationDuration);

        for (ConnectionPortfolioDto connectionPortfolioDTO : connectionPortfolioDTOs) {
            Connection connection = connectionMap.get(connectionPortfolioDTO.getConnectionEntityAddress());
            for (UdiPortfolioDto udiPortfolioDto : connectionPortfolioDTO.getUdis()) {
                if (activeUdis.containsKey(udiPortfolioDto.getEndpoint())) {
                    activeUdis.get(udiPortfolioDto.getEndpoint()).setValidUntil(validUntil);
                    continue;
                }
                toBePersisted.add(createUdi(udiPortfolioDto, connection, period, validUntil));
            }
        }
        udiRepository.persistBatch(toBePersisted);
    }

    /**
     * Update the Connection in the DB with information from the dto connection. It creates a {@link ConnectionPowerContainer} for
     * the connection with the uncontrolledLoad if available. Also it creats a {@link UdiPowerContainer} for all the Udi's.
     *
     * @param period the period for the connection forecast
     * @param connectionPortfolioDTOs the connection portfolio
     */
    public void createConnectionForecasts(final LocalDate period, List<ConnectionPortfolioDto> connectionPortfolioDTOs) {

        Map<String, Connection> connectionMap = corePlanboardBusinessService.findActiveConnections(period, Optional.empty())
                .stream().collect(Collectors.toMap(Connection::getEntityAddress, Function.identity()));

        Map<String, Map<Integer, PowerContainer>> connectionPowerContainersMap = convertToMap(
                powerContainerRepository.findConnectionPowerContainers(period, Optional.empty(), Optional.empty()));

        Map<String, Udi> activeUdis = udiRepository.findActiveUdisMappedPerEndpoint(period);

        List<Object> toBePersisted = new ArrayList<>();

        for (ConnectionPortfolioDto connectionPortfolioDTO : connectionPortfolioDTOs) {
            final Connection connection = connectionMap.get(connectionPortfolioDTO.getConnectionEntityAddress());

            // update uncontrolled load connection level (profile exists already)
            connectionPortfolioDTO.getConnectionPowerPerPTU().forEach((ptuIndex, powerContainerDto) -> {
                PowerContainer powerContainer = connectionPowerContainersMap.get(connection.getEntityAddress()).get(ptuIndex);
                if (powerContainer.getForecast() == null) {
                    powerContainer.setForecast(new ForecastPowerData());
                }
                PowerContainerTransformer.updateForecastValues(powerContainerDto.getForecast(), powerContainer.getForecast());
            });

            // create new PowerContainer for Udi level.
            for (UdiPortfolioDto udiPortfolioDto : connectionPortfolioDTO.getUdis()) {
                if (!activeUdis.containsKey(udiPortfolioDto.getEndpoint())) {
                    LOGGER.warn("Udi {} not registered in previous steps in the process.", udiPortfolioDto.getEndpoint());
                    continue;
                }

                Udi udi = activeUdis.get(udiPortfolioDto.getEndpoint());
                udiPortfolioDto.getUdiPowerPerDTU().forEach((timeIndex, pc) -> {
                    PowerContainer powerContainer = new UdiPowerContainer(udi, period, timeIndex);
                    if (powerContainer.getForecast() == null) {
                        powerContainer.setForecast(new ForecastPowerData());
                    }
                    PowerContainerTransformer.updateValues(pc.getForecast(), powerContainer.getForecast());
                    toBePersisted.add(powerContainer);
                });
            }
        }
        powerContainerRepository.persistBatch(toBePersisted);

    }

    private Map<String, Map<Integer, PowerContainer>> convertToMap(
            Map<Connection, List<PowerContainer>> connectionPowerContainers) {
        return connectionPowerContainers.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getEntityAddress(), entry -> convertToMap(entry.getValue())));
    }

    private Map<Integer, PowerContainer> convertToMap(List<PowerContainer> powerContainers) {
        return powerContainers.stream().collect(Collectors.toMap(PowerContainer::getTimeIndex, Function.identity()));
    }

    private Udi createUdi(UdiPortfolioDto udiPortfolioDto, Connection connection, LocalDate validFrom, LocalDate validUntil) {
        Udi udi = new Udi();
        udi.setValidFrom(validFrom);
        udi.setValidUntil(validUntil);
        udi.setConnection(connection);
        udi.setProfile(udiPortfolioDto.getProfile());
        udi.setEndpoint(udiPortfolioDto.getEndpoint());
        udi.setDtuSize(udiPortfolioDto.getDtuSize());
        return udi;
    }

    /**
     * Updates of creates (if needed) the PowerContainer entities on the Connection Group level for the given (and supposedly
     * entire) portfolio.
     *
     * @param connectionGroupPortfolioDtos {@link List} of {@link ConnectionGroupPortfolioDto}.
     * @param period {@link LocalDate}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateConnectionGroupPowerContainers(List<ConnectionGroupPortfolioDto> connectionGroupPortfolioDtos,
                                                     LocalDate period) {
        // get ConnectionGroup level data with lambda functions to apply on the connection portfolio.
        Function<Map.Entry<ConnectionGroup, List<PowerContainer>>, String> getUsefIdentifier = entry -> entry.getKey()
                .getUsefIdentifier();
        Function<Map.Entry<ConnectionGroup, List<PowerContainer>>, Map<Integer, PowerContainer>> getPowerContainerMap = entry ->
                entry.getValue().stream().collect(Collectors.toMap(PowerContainer::getTimeIndex, Function.identity()));

        // lambda function to set the observed value in a PowerContainer
        BiConsumer<PowerContainer, BigInteger> setObservedValue = (powerContainer, power) -> {
            if (powerContainer.getObserved() == null) {
                powerContainer.setObserved(new PowerData());
            }
            if (power == null) {
                return;
            }
            if (power.compareTo(BigInteger.ZERO) < 0) {
                powerContainer.getObserved().setAverageProduction(power.abs());
                powerContainer.getObserved().setAverageConsumption(BigInteger.ZERO);
            } else {
                powerContainer.getObserved().setAverageConsumption(power);
                powerContainer.getObserved().setAverageProduction(BigInteger.ZERO);
            }
        };
        // existing connection group power containers
        Map<String, Map<Integer, PowerContainer>> connectionGroupPowerContainers = powerContainerRepository
                .findConnectionGroupPowerContainers(period, Optional.<ConnectionGroup>empty())
                .entrySet().stream()
                .collect(Collectors.toMap(getUsefIdentifier::apply, getPowerContainerMap::apply));
        // valid connection groups in the portfolio/planboard
        Map<String, ConnectionGroup> connectionGroupPerUsefIdentifier = corePlanboardBusinessService
                .findActiveConnectionGroupStates(period, null).stream()
                .map(ConnectionGroupState::getConnectionGroup)
                .distinct()
                .collect(Collectors.toMap(ConnectionGroup::getUsefIdentifier, Function.identity()));

        for (ConnectionGroupPortfolioDto connectionGroupPortfolioDto : connectionGroupPortfolioDtos) {
            String usefIdentifier = connectionGroupPortfolioDto.getUsefIdentifier();
            if (!connectionGroupPerUsefIdentifier.containsKey(usefIdentifier)) {
                continue;
            }
            // update PowerContainer if it exists already, or create it otherwise.
            connectionGroupPortfolioDto.getConnectionGroupPowerPerPTU().forEach((index, powerContainerDto) -> {
                PowerContainer powerContainer;
                if (connectionGroupPowerContainers.containsKey(usefIdentifier) &&
                        connectionGroupPowerContainers.get(usefIdentifier).containsKey(index)) {
                    powerContainer = connectionGroupPowerContainers.get(usefIdentifier).get(index);
                    setObservedValue.accept(powerContainer, powerContainerDto.getObserved().calculatePower());
                    powerContainerRepository.update(powerContainer);
                } else {
                    powerContainer = new ConnectionGroupPowerContainer(connectionGroupPerUsefIdentifier.get(usefIdentifier), period,
                            index);
                    setObservedValue.accept(powerContainer, powerContainerDto.getObserved().calculatePower());
                    powerContainerRepository.persist(powerContainer);
                }
            });
        }
    }

    /**
     * Updates the ConnectionPortfolio.
     *
     * @param period {@link LocalDate}.
     * @param updatedConnectionPortfolioDTO the updated connection portfolio.
     */
    public void updateConnectionPortfolio(LocalDate period, List<ConnectionPortfolioDto> updatedConnectionPortfolioDTO) {

        //get Connection level data
        Map<String, List<PowerContainer>> connectionPowerContainers = powerContainerRepository
                .findConnectionPowerContainers(period, Optional.empty(), Optional.empty()).entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getEntityAddress(), Map.Entry::getValue));

        Map<String, List<Udi>> udisPerConnectionEntityAddress = udiRepository.findActiveUdisPerConnection(period).entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getEntityAddress(), Map.Entry::getValue));

        Map<String, List<PowerContainer>> powerContainersPerUdiEndpoint = powerContainerRepository
                .findUdiPowerContainers(period, Optional.empty(), Optional.empty()).entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getEndpoint(), Map.Entry::getValue));

        for (ConnectionPortfolioDto connectionPortfolioDTO : updatedConnectionPortfolioDTO) {
            String connectionEntityAddress = connectionPortfolioDTO.getConnectionEntityAddress();
            //connectionLevel == Uncontrolled Load
            if (connectionPowerContainers.containsKey(connectionEntityAddress)) {
                updatePowerContainer(connectionPortfolioDTO.getConnectionPowerPerPTU(),
                        connectionPowerContainers.get(connectionEntityAddress));
                updateFlexibleLoad(connectionPortfolioDTO.getConnectionPowerPerPTU(),
                        connectionPowerContainers.get(connectionEntityAddress));
            }
            if (udisPerConnectionEntityAddress == null || !udisPerConnectionEntityAddress.containsKey(connectionEntityAddress)) {
                continue;
            }

            for (UdiPortfolioDto udiPortfolioDto : connectionPortfolioDTO.getUdis()) {
                if (powerContainersPerUdiEndpoint.containsKey(udiPortfolioDto.getEndpoint())) {
                    updateProductionConsumption(udiPortfolioDto.getUdiPowerPerDTU(),
                            powerContainersPerUdiEndpoint.get(udiPortfolioDto.getEndpoint()));
                }
            }
        }
    }

    private void updatePowerContainer(Map<Integer, PowerContainerDto> connectionPowerPerPTU,
                                      List<PowerContainer> powerContainers) {
        powerContainers.stream()
                .filter(powerContainer -> connectionPowerPerPTU.containsKey(powerContainer.getTimeIndex()))
                .forEach(powerContainer -> {
                    PowerContainerDto powerContainerDto = connectionPowerPerPTU.get(powerContainer.getTimeIndex());
                    if (powerContainer.getForecast() == null) {
                        powerContainer.setForecast(new ForecastPowerData());
                    }
                    PowerContainerTransformer.updateForecastValues(powerContainerDto.getForecast(), powerContainer.getForecast());
                    if (powerContainer.getObserved() == null) {
                        powerContainer.setObserved(new PowerData());
                    }
                    powerContainer.getObserved().setUncontrolledLoad(powerContainerDto.getObserved().getUncontrolledLoad());
                    if (powerContainerDto.getProfile() != null) {
                        powerContainer.getProfile().setUncontrolledLoad(powerContainerDto.getProfile().getUncontrolledLoad());
                    }
                });
    }

    private void updateFlexibleLoad(Map<Integer, PowerContainerDto> connectionPowerPerPTU, List<PowerContainer> powerContainers) {
        powerContainers.stream()
                .filter(powerContainer -> connectionPowerPerPTU.containsKey(powerContainer.getTimeIndex()))
                .forEach(powerContainer -> {
                    PowerContainerDto powerContainerDto = connectionPowerPerPTU.get(powerContainer.getTimeIndex());
                    updatePowerContainerFlexibleLoad(powerContainer, powerContainerDto);
                });
    }

    private void updatePowerContainerFlexibleLoad(PowerContainer powerContainer, PowerContainerDto powerContainerDto) {
        if (powerContainer.getProfile() == null || powerContainerDto.getProfile() == null) {
            return;
        }
        powerContainer.getProfile()
                .setAverageConsumption(powerContainerDto.getProfile().getAverageConsumption());
        powerContainer.getProfile()
                .setPotentialFlexConsumption(powerContainerDto.getProfile().getPotentialFlexConsumption());
        powerContainer.getProfile()
                .setAverageProduction(powerContainerDto.getProfile().getAverageProduction());
        powerContainer.getProfile()
                .setPotentialFlexProduction(powerContainerDto.getProfile().getPotentialFlexProduction());
    }

    private void updateProductionConsumption(Map<Integer, PowerContainerDto> connectionPowerPerPTU,
                                             List<? extends PowerContainer> powerContainers) {
        powerContainers.stream()
                .filter(powerContainer -> connectionPowerPerPTU.containsKey(powerContainer.getTimeIndex()))
                .forEach(powerContainer -> {
                    PowerContainerDto powerContainerDto = connectionPowerPerPTU.get(powerContainer.getTimeIndex());

                    // update the forecast, observed and actual values
                    forecastValues(powerContainer, powerContainerDto);
                    observedValues(powerContainer, powerContainerDto);
                });
    }

    private void observedValues(PowerContainer powerContainer, PowerContainerDto powerContainerDto) {
        if (powerContainerDto.getObserved() != null) {
            if (powerContainer.getObserved() == null) {
                powerContainer.setObserved(new PowerData());
            }
            PowerContainerTransformer.updateValues(powerContainerDto.getObserved(), powerContainer.getObserved());
        }
    }

    private void forecastValues(PowerContainer powerContainer, PowerContainerDto powerContainerDto) {
        if (powerContainerDto.getForecast() != null) {
            if (powerContainer.getForecast() == null) {
                powerContainer.setForecast(new ForecastPowerData());
            }
            PowerContainerTransformer.updateForecastValues(powerContainerDto.getForecast(), powerContainer.getForecast());
        }
    }

    /**
     * Finds the correctly summed {@link PowerContainer}'s per PTU and {@link ConnectionGroup}.
     *
     * @param period The {@link LocalDate} period.
     * @param optionalConnectionGroup The {@link Optional} of type {@link ConnectionGroup}.
     * @return the active portfolio for specified period.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Map<ConnectionGroup, Map<Integer, PowerContainer>> findActivePortfolioForConnectionGroupLevel(LocalDate period,
                                                                                                         Optional<ConnectionGroup> optionalConnectionGroup) {

        boolean aggregatorInABox = configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR);
        Integer ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);
        Integer ptusInDay = PtuUtil.getNumberOfPtusPerDay(period, ptuDuration);

        LOGGER.debug("Find active Portfolio for period: {} where aggregatorInABox is: {}", period, aggregatorInABox);

        //get portfolio
        Map<ConnectionGroup, List<Connection>> activeConnectionGroupsWithConnections = buildConnectionGroupWithConnectionsMap(
                period, optionalConnectionGroup);

        //get ConnectionGroup level data
        Map<ConnectionGroup, List<PowerContainer>> connectionGroupPowerContainers = powerContainerRepository
                .findConnectionGroupPowerContainers(period, optionalConnectionGroup);

        //get Connection level data
        Map<Connection, List<PowerContainer>> connectionPowerContainers = powerContainerRepository.findConnectionPowerContainers(
                period, Optional.empty(), optionalConnectionGroup);

        Map<Connection, Map<Integer, PowerContainer>> udiPowerContainers = new HashMap<>();
        if (!aggregatorInABox) {
            udiPowerContainers = findAndSumUdisPerPtuPerConnection(period, ptuDuration, ptusInDay);
        }

        //get and Sum UDI level data

        //sum all
        Map<ConnectionGroup, Map<Integer, PowerContainer>> summedPowerPerPtuPerConnectionGroup =
                new HashMap<>(connectionGroupPowerContainers.size());
        for (Map.Entry<ConnectionGroup, List<Connection>> connectionGroupConnectionsEntry : activeConnectionGroupsWithConnections
                .entrySet()) {
            ConnectionGroup connectionGroup = connectionGroupConnectionsEntry.getKey();

            //start with ConnectionGroup level data.
            List<PowerContainer> powerContainers = connectionGroupPowerContainers.getOrDefault(connectionGroup, new ArrayList<>());

            //collect all relevant PowerContainers
            for (Connection connection : connectionGroupConnectionsEntry.getValue()) {
                powerContainers.addAll(connectionPowerContainers.getOrDefault(connection, new ArrayList<>()));
                if (!aggregatorInABox) {
                    powerContainers.addAll(udiPowerContainers.getOrDefault(connection, new HashMap<>()).values());
                }
            }
            summedPowerPerPtuPerConnectionGroup.put(connectionGroup, sumPowerContainersPerPtu(powerContainers));
        }

        return summedPowerPerPtuPerConnectionGroup;
    }

    /**
     * Finds the  connection ONLY Portfolio and maps it to the DTO Model. Usable only for UDI Aggregator.
     *
     * @param startDate The starting {@link LocalDate}.
     * @param endDate The end date inclusive {@link LocalDate}.
     * @return The portfolio per requested {@link LocalDate}.
     */
    public Map<LocalDate, List<ConnectionPortfolioDto>> findConnectionPortfolioDto(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<ConnectionPortfolioDto>> portfolioPerDate = new HashMap<>();
        for (LocalDate portfolioDate = startDate; !portfolioDate.isAfter(endDate); portfolioDate = portfolioDate.plusDays(1)) {
            portfolioPerDate.put(portfolioDate, findConnectionPortfolioDto(portfolioDate));
        }
        return portfolioPerDate;
    }

    /**
     * Finds the portfolio and maps it to the DTO Model.
     *
     * @param period The period.
     * @return The portfolio for the requested period and connections.
     */
    public List<ConnectionPortfolioDto> findConnectionPortfolioDto(LocalDate period) {
        return findConnectionPortfolioDto(period, Optional.empty());
    }

    /**
     * Finds the portfolio and maps it to the DTO Model. Only a subset is returned if connectionEntityList is specified.
     *
     * @param period The period.
     * @param connectionEntityList A {@link List} of connection entity addresses.
     * @return The portfolio for the requested period and connections.
     */
    public List<ConnectionPortfolioDto> findConnectionPortfolioDto(LocalDate period, Optional<List<String>> connectionEntityList) {

        List<Connection> activeConnections = corePlanboardBusinessService.findActiveConnections(period, connectionEntityList);

        //get Connection level data
        Map<Connection, List<PowerContainer>> connectionPowerContainers = powerContainerRepository
                .findConnectionPowerContainers(period, connectionEntityList, Optional.empty());

        Map<Connection, List<Udi>> udisPerConnection = udiRepository.findActiveUdisPerConnection(period, connectionEntityList);

        Map<Udi, List<PowerContainer>> udiPowerContainers = powerContainerRepository.findUdiPowerContainers(period, connectionEntityList, Optional.empty());

        return ConnectionPortfolioTransformer
                .transformToDTO(activeConnections, connectionPowerContainers, udisPerConnection, udiPowerContainers);
    }

    private Map<ConnectionGroup, List<Connection>> buildConnectionGroupWithConnectionsMap(LocalDate period,
                                                                                          Optional<ConnectionGroup> optionalConnectionGroup) {
        Map<ConnectionGroup, List<Connection>> activeConnectionGroupsWithConnections;
        if (optionalConnectionGroup.isPresent()) {
            activeConnectionGroupsWithConnections = new HashMap<>();
            List<Connection> connections = corePlanboardBusinessService
                    .findConnectionsForConnectionGroup(optionalConnectionGroup.get().getUsefIdentifier(), period);
            activeConnectionGroupsWithConnections.put(optionalConnectionGroup.get(), connections);
        } else {
            activeConnectionGroupsWithConnections = corePlanboardBusinessService.findActiveConnectionGroupsWithConnections(period);
        }
        return activeConnectionGroupsWithConnections;
    }

    private Map<Integer, PowerContainer> sumPowerContainersPerPtu(List<PowerContainer> powerContainers) {
        Map<Integer, PowerContainer> result = new HashMap<>();
        powerContainers.forEach(powerContainer -> {
            if (result.containsKey(powerContainer.getTimeIndex())) {
                PowerContainer summedPC = PowerContainerUtil.sum(result.get(powerContainer.getTimeIndex()), powerContainer);
                summedPC.setTimeIndex(powerContainer.getTimeIndex());
                result.put(powerContainer.getTimeIndex(), summedPC);
            } else {
                result.put(powerContainer.getTimeIndex(), powerContainer);
            }
        });
        return result;
    }

    private Map<Connection, Map<Integer, PowerContainer>> findAndSumUdisPerPtuPerConnection(LocalDate period, Integer ptuDuration,
                                                                                            Integer ptusInDay) {
        Map<Connection, Map<Integer, PowerContainer>> summedUdisPerConnectionPerPtu = new HashMap<>();

        Map<Connection, List<Udi>> udisPerConnection = udiRepository.findActiveUdisPerConnection(period);

        Map<Udi, List<PowerContainer>> udiPowerContainers = powerContainerRepository.findUdiPowerContainers(period,
                Optional.empty(), Optional.empty());

        udisPerConnection.forEach((connection, udis) -> summedUdisPerConnectionPerPtu
                .put(connection, PowerContainerUtil.sumUdisPerPtu(udis, udiPowerContainers, ptuDuration, ptusInDay)));

        return summedUdisPerConnectionPerPtu;
    }

    /**
     * Finds all the {@link UdiEvent} entities for the given period.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link List} of {@link UdiEvent}.
     */
    public List<UdiEvent> findUdiEventsForPeriod(LocalDate period) {
        return udiEventRepository.findUdiEventsForPeriod(period);
    }

    /**
     * Retrieves a Map of all the Udis for a specified period.
     *
     * @param period the {@link LocalDate} of the period.
     * @return a Map where the Udi endpoint is mapped to the Udi.
     */
    public Map<String, Udi> findActiveUdisMappedPerEndpoint(LocalDate period) {
        return udiRepository.findActiveUdisMappedPerEndpoint(period);
    }

    /**
     * Finds the connection group portfolio for specific period and optional connection group.
     *
     * @param period the period
     * @param optionalConnectionGroup optional connection group ({@link ConnectionGroup})
     * @return A {@link List} of {@link ConnectionGroupPortfolioDto} objects
     */
    public List<ConnectionGroupPortfolioDto> findConnectionGroupPortfolioDto(LocalDate period,
                                                                             Optional<ConnectionGroup> optionalConnectionGroup) {

        Map<ConnectionGroup, List<PowerContainer>> connectionGroupPowerContainers = powerContainerRepository
                .findConnectionGroupPowerContainers(period, optionalConnectionGroup);

        return ConnectionPortfolioTransformer.transformToDTO(connectionGroupPowerContainers);

    }

    /**
     * Stores a list of device messages.
     *
     * @param deviceMessageDtos List of {@link DeviceMessageDto}.
     * @param udis Map with udi endpoint -> udi.
     */
    public void storeDeviceMessages(List<DeviceMessageDto> deviceMessageDtos, Map<String, Udi> udis) {
        List<DeviceMessage> deviceMessages = DeviceMessageTransformer.transform(deviceMessageDtos, udis);

        deviceMessages.forEach(deviceMessage -> {
            deviceMessageRepository.persist(deviceMessage);
            deviceMessage.getDeviceRequests().forEach(deviceRequest -> {
                deviceRequest.setDeviceMessage(deviceMessage);
                storeDeviceRequest(deviceRequest);
            });
        });
    }

    private void storeDeviceRequest(DeviceRequest deviceRequest) {
        if (deviceRequest instanceof ReduceRequest) {
            reduceRequestRepository.persist((ReduceRequest) deviceRequest);
        } else if (deviceRequest instanceof IncreaseRequest) {
            increaseRequestRepository.persist((IncreaseRequest) deviceRequest);
        } else if (deviceRequest instanceof InterruptRequest) {
            interruptRequestRepository.persist((InterruptRequest) deviceRequest);
        } else if (deviceRequest instanceof ReportRequest) {
            reportRequestRepository.persist((ReportRequest) deviceRequest);
        } else if (deviceRequest instanceof ShiftRequest) {
            shiftRequestRepository.persist((ShiftRequest) deviceRequest);
        }
    }

    /**
     * This method finds the {@link DeviceMessage} entities with values matching the given parameters.
     *
     * @param connectionEntityAddress {@link String} optional entity address of a connection.
     * @param deviceMessageStatus {@link DeviceMessageStatus} optional status of a device message.
     * @return a {@link List} of {@link DeviceMessage} entities.
     */
    public List<DeviceMessage> findDeviceMessages(String connectionEntityAddress, DeviceMessageStatus deviceMessageStatus) {
        return deviceMessageRepository.findDeviceMessages(connectionEntityAddress, deviceMessageStatus);
    }

    /**
     * Update the device message status in the database.
     *
     * @param deviceMessage {@link DeviceMessage} a device message entity.
     * @param status {@link DeviceMessageStatus} a device message status.
     */
    public void updateDeviceMessageStatus(DeviceMessage deviceMessage, DeviceMessageStatus status) {
        deviceMessage.setDeviceMessageStatus(status);
    }


}