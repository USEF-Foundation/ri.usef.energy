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

import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ForecastPowerDataDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.PowerDataDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.dto.device.request.IncreaseRequestDto;
import energy.usef.agr.dto.device.request.InterruptRequestDto;
import energy.usef.agr.dto.device.request.ReduceRequestDto;
import energy.usef.agr.dto.device.request.ReportRequestDto;
import energy.usef.agr.dto.device.request.ShiftRequestDto;
import energy.usef.agr.model.DeviceMessage;
import energy.usef.agr.model.DeviceMessageStatus;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.model.PowerData;
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
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.dto.USEFRoleDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * AgrPortfolioBusinessService Test.
 */
@RunWith(PowerMockRunner.class)
public class AgrPortfolioBusinessServiceTest {

    private static final String USEF_IDENTIFIER = "USEF_IDENTIFIER";
    private static final int SET_TIME_INDEX = 9;

    @Mock
    private ConfigAgr configAgr;
    @Mock
    private Config config;
    @Mock
    private PowerContainerRepository powerContainerRepository;

    @Mock
    private UdiRepository udiRepository;
    @Mock
    private UdiEventRepository udiEventRepository;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private DeviceMessageRepository deviceMessageRepository;
    @Mock
    private ReduceRequestRepository reduceRequestRepository;
    @Mock
    private IncreaseRequestRepository increaseRequestRepository;
    @Mock
    private InterruptRequestRepository interruptRequestRepository;
    @Mock
    private ReportRequestRepository reportRequestRepository;
    @Mock
    private ShiftRequestRepository shiftRequestRepository;

    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Before
    public void init() {
        agrPortfolioBusinessService = new AgrPortfolioBusinessService();
        Whitebox.setInternalState(agrPortfolioBusinessService, config);
        Whitebox.setInternalState(agrPortfolioBusinessService, configAgr);
        Whitebox.setInternalState(agrPortfolioBusinessService, powerContainerRepository);
        Whitebox.setInternalState(agrPortfolioBusinessService, corePlanboardBusinessService);
        Whitebox.setInternalState(agrPortfolioBusinessService, deviceMessageRepository);
        Whitebox.setInternalState(agrPortfolioBusinessService, udiRepository);
        Whitebox.setInternalState(agrPortfolioBusinessService, udiEventRepository);
        Whitebox.setInternalState(agrPortfolioBusinessService, "reduceRequestRepository", reduceRequestRepository);
        Whitebox.setInternalState(agrPortfolioBusinessService, "increaseRequestRepository", increaseRequestRepository);
        Whitebox.setInternalState(agrPortfolioBusinessService, "interruptRequestRepository", interruptRequestRepository);
        Whitebox.setInternalState(agrPortfolioBusinessService, "reportRequestRepository", reportRequestRepository);
        Whitebox.setInternalState(agrPortfolioBusinessService, "shiftRequestRepository", shiftRequestRepository);

        Mockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(false);
        Mockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
    }

    @Test
    public void testFindActivePortfolioForConnectionGroupLevel() throws Exception {
        LocalDate period = new LocalDate("2015-01-01");

        Map<ConnectionGroup, List<Connection>> connectionGroupListMap = buildConnectionGroupWithConnectionsMap();
        Mockito.when(corePlanboardBusinessService.findActiveConnectionGroupsWithConnections(period))
                .thenReturn(connectionGroupListMap);

        Map<Udi, List<PowerContainer>> udiPowerContainers = new HashMap<>();

        Map<Connection, List<Udi>> udiPerConnection = buildUdiPowerContainerMap(
                connectionGroupListMap.values().stream().findFirst().get().get(0), udiPowerContainers);

        Mockito.when(udiRepository.findActiveUdisPerConnection(period)).thenReturn(udiPerConnection);
        Mockito.when(powerContainerRepository.findUdiPowerContainers(period, Optional.empty(), Optional.empty()))
                .thenReturn(udiPowerContainers);

        //test
        Map<ConnectionGroup, Map<Integer, PowerContainer>> results = agrPortfolioBusinessService
                .findActivePortfolioForConnectionGroupLevel(period, Optional.empty());

        Assert.assertNotNull(results);
        assertEquals(1, results.size());
        Map.Entry<ConnectionGroup, Map<Integer, PowerContainer>> connectionGroupMapEntry = results.entrySet().stream().findFirst()
                .get();

        assertEquals(USEF_IDENTIFIER, connectionGroupMapEntry.getKey().getUsefIdentifier());
        assertEquals(96, connectionGroupMapEntry.getValue().size());

    }

    @Test
    public void testFindDeviceMessages() {
        // stubbing
        when(deviceMessageRepository
                .findDeviceMessages(any(String.class), any(DeviceMessageStatus.class)))
                .thenReturn(new ArrayList<>());
        // invocation
        List<DeviceMessage> deviceMessages = agrPortfolioBusinessService
                .findDeviceMessages("ean.000000000000000001", DeviceMessageStatus.NEW);
        // assertions
        Assert.assertNotNull(deviceMessages);
        Mockito.verify(deviceMessageRepository, Mockito.times(1))
                .findDeviceMessages(eq("ean.000000000000000001"), eq(DeviceMessageStatus.NEW));
    }

    @Test
    public void testUpdateDeviceMessageStatus() {
        // creation of variables
        DeviceMessageStatus futureStatus = DeviceMessageStatus.IN_PROCESS;
        DeviceMessage deviceMessage = new DeviceMessage();
        deviceMessage.setDeviceMessageStatus(DeviceMessageStatus.NEW);
        // invocation
        agrPortfolioBusinessService.updateDeviceMessageStatus(deviceMessage, futureStatus);
        Assert.assertEquals(futureStatus, deviceMessage.getDeviceMessageStatus());
    }

    @Test
    public void testUpdateConnectionPortfolio() throws Exception {
        when(powerContainerRepository
                .findConnectionPowerContainers(any(LocalDate.class), eq(Optional.empty()), eq(Optional.empty())))
                .thenReturn(buildConnectionPowerContainers(new Connection("ean.0000000001")));
        when(powerContainerRepository.findUdiPowerContainers(any(LocalDate.class), eq(Optional.empty()), eq(Optional.empty())))
                .thenReturn(new HashMap<>());

        agrPortfolioBusinessService.updateConnectionPortfolio(DateTimeUtil.getCurrentDate(), buildConnectionPortfolioDTOList());

        // TODO: add some verifications / assertions to make sure the method is working.
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateUdis() {

        final LocalDate period = new LocalDate(2015, 8, 24);
        final ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto("ean.0000000001");
        List<ConnectionPortfolioDto> connectionPortfolioDTOs = Collections.singletonList(connectionPortfolioDTO);
        UdiPortfolioDto udi = new UdiPortfolioDto("udi://udi", 5, "ADS_1");
        connectionPortfolioDTO.getUdis().add(udi);

        Mockito.when(corePlanboardBusinessService.findActiveConnections(period, Optional.empty())).thenReturn(new ArrayList<>());
        Mockito.when(udiRepository.findActiveUdisMappedPerEndpoint(period)).thenReturn(new HashMap<>());

        agrPortfolioBusinessService.createUdis(period, connectionPortfolioDTOs);

        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(udiRepository, Mockito.times(1)).persistBatch(listArgumentCaptor.capture());

        List persistedList = listArgumentCaptor.getValue();
        assertEquals(1, persistedList.size());
        assertEquals("udi://udi", ((Udi) persistedList.get(0)).getEndpoint());
    }

    @Test
    public void testCreateConnectionProfiles() {
        // variables
        final LocalDate period = new LocalDate(2015, 8, 24);
        final ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto("ean.0000000001");
        connectionPortfolioDTO.getConnectionPowerPerPTU().putAll(IntStream.rangeClosed(1, 12).mapToObj(index -> {
            PowerContainerDto powerContainerDto = new PowerContainerDto(period, index);
            powerContainerDto.setProfile(new PowerDataDto(TEN, TEN, ZERO, TEN, ZERO));
            return powerContainerDto;
        }).collect(Collectors.toMap(PowerContainerDto::getTimeIndex, Function.identity())));
        List<ConnectionPortfolioDto> connectionPortfolioDTOs = Collections.singletonList(connectionPortfolioDTO);
        // invocation
        agrPortfolioBusinessService.createConnectionProfiles(period, connectionPortfolioDTOs);
        // verification
        ArgumentCaptor<PowerContainer> powerContainerArgumentCaptor = ArgumentCaptor.forClass(PowerContainer.class);
        Mockito.verify(powerContainerRepository, Mockito.times(12)).persist(powerContainerArgumentCaptor.capture());
        powerContainerArgumentCaptor.getAllValues().stream().forEach(powerContainer -> {
            assertEquals(TEN, powerContainer.getProfile().getUncontrolledLoad());
            assertEquals(TEN, powerContainer.getProfile().getAverageConsumption());
            assertEquals(TEN, powerContainer.getProfile().getPotentialFlexConsumption());
            assertEquals(ZERO, powerContainer.getProfile().getPotentialFlexProduction());
            assertEquals(ZERO, powerContainer.getProfile().getAverageProduction());
        });
    }

    @Test
    public void testCreateConnectionForecasts() {
        // variables
        final LocalDate period = new LocalDate(2015, 8, 24);
        String connectionEntityAddress = "ean.0000000001";
        String endpoint = "udi://endpoint1";
        List<ConnectionPortfolioDto> connectionPortfolioDto = buildConnectionForecastList(period, connectionEntityAddress,
                endpoint);
        stubRepositoriesForConnectionForecast(period, connectionEntityAddress, endpoint);
        // invocation
        agrPortfolioBusinessService.createConnectionForecasts(period, connectionPortfolioDto);
        // verification
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).findActiveConnections(eq(period), eq(Optional.empty()));
        Mockito.verify(udiRepository, Mockito.times(1)).findActiveUdisMappedPerEndpoint(eq(period));
        Mockito.verify(powerContainerRepository, Mockito.times(1)).persistBatch(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFindConnectionPortfolio() {
        final LocalDate period = new LocalDate();
        final String connectionEntityAddress = "ean.111111111111";
        Mockito.when(corePlanboardBusinessService.findActiveConnections(eq(period), eq(Optional.empty())))
                .thenReturn(Arrays.asList(new Connection(connectionEntityAddress)));
        Mockito.when(powerContainerRepository.findConnectionPowerContainers(eq(period), any(Optional.class), any(Optional.class)))
                .then(call -> Collections.singletonMap(new Connection(connectionEntityAddress),
                        IntStream.rangeClosed(1, 12).mapToObj(index -> buildPowerContainer(period, index))
                                .collect(Collectors.toList())));
        List<ConnectionPortfolioDto> connectionPortfolioDto = agrPortfolioBusinessService.findConnectionPortfolioDto(period);
        Assert.assertEquals(1, connectionPortfolioDto.size());
        Assert.assertEquals(connectionEntityAddress, connectionPortfolioDto.get(0).getConnectionEntityAddress());
        Assert.assertEquals(12, connectionPortfolioDto.get(0).getConnectionPowerPerPTU().size());
        connectionPortfolioDto.get(0).getConnectionPowerPerPTU().forEach((index, powerContainer) -> Assert
                .assertEquals(index.intValue(), powerContainer.getObserved().calculatePower().intValue()));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateConnectionGroupPowerContainers() {
        // variables and additional mocking
        final LocalDate today = DateTimeUtil.getCurrentDate();
        Mockito.when(powerContainerRepository.findConnectionGroupPowerContainers(any(LocalDate.class),
                any(Optional.class))).thenReturn(new HashMap<>());
        Mockito.when(corePlanboardBusinessService
                .findActiveConnectionGroupStates(any(LocalDate.class), Matchers.isNull(Class.class))).then(call -> {
            BrpConnectionGroup brpConnectionGroup = new BrpConnectionGroup("brp.usef-example.com");
            CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup("ean.111111111111");
            ConnectionGroupState connectionGroupState1 = new ConnectionGroupState();
            ConnectionGroupState connectionGroupState2 = new ConnectionGroupState();
            connectionGroupState1.setConnectionGroup(brpConnectionGroup);
            connectionGroupState2.setConnectionGroup(congestionPoint);
            return Arrays.asList(connectionGroupState1, connectionGroupState2);
        });
        // invocation
        agrPortfolioBusinessService.updateConnectionGroupPowerContainers(buildConnectionGroupPortfolioDtos(today), today);
        // verifications and assertions
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .findActiveConnectionGroupStates(eq(today), Matchers.isNull(Class.class));
        Mockito.verify(powerContainerRepository, Mockito.times(1))
                .findConnectionGroupPowerContainers(eq(today), eq(Optional.empty()));
        Mockito.verify(powerContainerRepository, Mockito.times(2)).persist(any(PowerContainer.class));

    }

    @Test
    public void testFindUdiEventsForPeriod() {
        // variables and mocking
        final LocalDate period = new LocalDate(2015, 10, 5);
        Mockito.when(udiEventRepository.findUdiEventsForPeriod(eq(period)))
                .thenReturn(Collections.singletonList(new UdiEvent()));
        // actual invocation
        List<UdiEvent> udiEvents = agrPortfolioBusinessService.findUdiEventsForPeriod(period);
        // assertions and verifications
        Assert.assertNotNull(udiEvents);
        assertEquals(1, udiEvents.size());
        Mockito.verify(udiEventRepository, Mockito.times(1)).findUdiEventsForPeriod(eq(period));
    }

    @Test
    public void testStoreDeviceMessages() {
        Map<String, Udi> udis = new HashMap<>();
        Udi udi = new Udi();
        udi.setEndpoint("endpoint");
        udi.setDtuSize(15);
        udis.put("endpoint", udi);

        List<DeviceMessageDto> deviceMessages = new ArrayList<>();
        DeviceMessageDto deviceMessage = new DeviceMessageDto();
        addDeviceRequestsToDeviceMessage(deviceMessage);
        deviceMessage.setEndpoint("endpoint");
        deviceMessages.add(deviceMessage);

        agrPortfolioBusinessService.storeDeviceMessages(deviceMessages, udis);

        Mockito.verify(deviceMessageRepository, Mockito.times(1)).persist(any(DeviceMessage.class));
    }

    private void addDeviceRequestsToDeviceMessage(DeviceMessageDto deviceMessage) {
        ShiftRequestDto shiftRequestDto = new ShiftRequestDto();
        shiftRequestDto.setStartDTU(BigInteger.ONE);
        ReduceRequestDto reduceRequestDto = new ReduceRequestDto();
        reduceRequestDto.setStartDTU(BigInteger.ONE);
        reduceRequestDto.setEndDTU(BigInteger.valueOf(2));
        IncreaseRequestDto increaseRequestDto = new IncreaseRequestDto();
        increaseRequestDto.setStartDTU(BigInteger.ONE);
        increaseRequestDto.setEndDTU(BigInteger.valueOf(2));
        InterruptRequestDto interruptRequestDto = new InterruptRequestDto();
        interruptRequestDto.setDtus("1,2");
        ReportRequestDto reportRequestDto = new ReportRequestDto();
        reportRequestDto.setDtus("1,2");
        deviceMessage.getShiftRequestDtos().add(shiftRequestDto);
        deviceMessage.getInterruptRequestDtos().add(interruptRequestDto);
        deviceMessage.getReduceRequestDtos().add(reduceRequestDto);
        deviceMessage.getIncreaseRequestDtos().add(increaseRequestDto);
        deviceMessage.getReportRequestDtos().add(reportRequestDto);
    }

    @Test
    public void testFindConnectionPortfolioDtoWithConnectionList() {
        Connection connection = new Connection();
        connection.setEntityAddress("ean.1");

        Map<Udi, List<PowerContainer>> udiPowerContainers = new HashMap<>();

        Mockito.when(corePlanboardBusinessService.findActiveConnections(any(LocalDate.class), any(Optional.class)))
                .thenReturn(Arrays.asList(connection));
        Mockito.when(powerContainerRepository
                .findConnectionPowerContainers(any(LocalDate.class), any(Optional.class), any(Optional.class)))
                .thenReturn(buildConnectionPowerContainers(connection));
        Mockito.when(udiRepository.findActiveUdisPerConnection(any(LocalDate.class), any(Optional.class)))
                .thenReturn(buildUdiPowerContainerMap(connection, udiPowerContainers));
        Mockito.when(
                powerContainerRepository.findUdiPowerContainers(any(LocalDate.class), any(Optional.class), any(Optional.class)))
                .thenReturn(udiPowerContainers);

        LocalDate period = new LocalDate();
        List<String> connectionList = Arrays.asList(connection.getEntityAddress());

        List<ConnectionPortfolioDto> connectionPortfolioDto = agrPortfolioBusinessService
                .findConnectionPortfolioDto(period, Optional.of(connectionList));

        Assert.assertNotNull(connectionPortfolioDto);
        Assert.assertEquals(1, connectionPortfolioDto.size());
        Assert.assertEquals("ean.1", connectionPortfolioDto.get(0).getConnectionEntityAddress());
    }

    /**
     * Creates a Connection Group Portfolio DTO with:
     * <ul>
     * <li>BRP Connection Group with 1000 W Observed power for PTU 1.</li>
     * <li>DSO Connection Group with -2000 W Observed power for PTU 1.</li>
     * </ul>
     *
     * @param period the {@link LocalDate} period.
     * @return {@link List} of {@link ConnectionGroupPortfolioDto} objects.
     */
    private List<ConnectionGroupPortfolioDto> buildConnectionGroupPortfolioDtos(LocalDate period) {
        ConnectionGroupPortfolioDto connectionGroupPortfolioDto1 = new ConnectionGroupPortfolioDto("brp.usef-example.com",
                USEFRoleDto.BRP);
        ConnectionGroupPortfolioDto connectionGroupPortfolioDto2 = new ConnectionGroupPortfolioDto("ean.111111111111",
                USEFRoleDto.DSO);
        PowerContainerDto powerContainerDto1 = new PowerContainerDto(period, 1);
        PowerDataDto powerDataDto1 = new PowerDataDto();
        powerDataDto1.setAverageConsumption(BigInteger.valueOf(1000L));
        powerContainerDto1.setObserved(powerDataDto1);
        PowerContainerDto powerContainerDto2 = new PowerContainerDto(period, 1);
        PowerDataDto powerDataDto2 = new PowerDataDto();
        powerDataDto1.setAverageConsumption(BigInteger.valueOf(-2000L));
        connectionGroupPortfolioDto1.getConnectionGroupPowerPerPTU().put(1, powerContainerDto1);
        connectionGroupPortfolioDto2.getConnectionGroupPowerPerPTU().put(1, powerContainerDto2);
        return Arrays.asList(connectionGroupPortfolioDto1, connectionGroupPortfolioDto2);

    }

    private Map<Connection, List<PowerContainer>> buildConnectionPowerContainers(Connection connection) {
        Map<Connection, List<PowerContainer>> result = new HashMap<>();

        result.put(connection, buildPowerContainerList());
        return result;
    }

    private Map<Connection, List<Udi>> buildUdiPowerContainerMap(Connection connection,
            Map<Udi, List<PowerContainer>> udiPowerContainers) {
        Map<Connection, List<Udi>> result = new HashMap<>();
        List<Udi> udiList = new ArrayList<>();

        Udi udi = new Udi();
        udi.setId(1l);
        udi.setDtuSize(15);
        udi.setEndpoint("udi.endpoint");
        udiPowerContainers.put(udi, buildUdiPowerContainerList());
        udiList.add(udi);
        result.put(connection, udiList);
        return result;
    }

    private List<PowerContainer> buildPowerContainerList() {
        List<PowerContainer> powerContainers = new ArrayList<>();
        PowerContainer powerContainer = new PowerContainer();
        powerContainer.setTimeIndex(SET_TIME_INDEX);
        powerContainers.add(powerContainer);

        return powerContainers;
    }

    private List<PowerContainer> buildUdiPowerContainerList() {
        List<PowerContainer> powerContainers = new ArrayList<>();
        UdiPowerContainer powerContainer = new UdiPowerContainer();
        powerContainer.setTimeIndex(SET_TIME_INDEX);
        powerContainers.add(powerContainer);

        return powerContainers;
    }

    private Map<ConnectionGroup, List<Connection>> buildConnectionGroupWithConnectionsMap() {
        Map<ConnectionGroup, List<Connection>> map = new HashMap<>();

        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(USEF_IDENTIFIER);

        List<Connection> connections = Collections.singletonList(new Connection(""));

        map.put(connectionGroup, connections);
        return map;
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDTOList() {
        LocalDate period = DateTimeUtil.getCurrentDate();
        ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto("ean.0000000001");
        connectionPortfolioDTO.getUdis().add(new UdiPortfolioDto("udi.endpoint", 15, "BEMS"));
        for (int i = 1; i <= 96; i++) {
            connectionPortfolioDTO.getConnectionPowerPerPTU().put(i, buildPowerContainerDto(period, i));
            connectionPortfolioDTO.getUdis().get(0).getUdiPowerPerDTU().put(i, buildPowerContainerDto(period, i));
        }
        return Collections.singletonList(connectionPortfolioDTO);
    }

    private PowerContainerDto buildPowerContainerDto(LocalDate period, int index) {
        PowerContainerDto powerContainerDto = new PowerContainerDto(period, index);
        PowerDataDto powerDataDto = new PowerDataDto();
        powerDataDto.setUncontrolledLoad(BigInteger.valueOf(index));
        powerContainerDto.setObserved(powerDataDto);

        return powerContainerDto;
    }

    private PowerContainer buildPowerContainer(LocalDate period, int index) {
        PowerContainer powerContainer = new PowerContainer(period, index);
        PowerData powerData = new PowerData();
        powerData.setUncontrolledLoad(BigInteger.valueOf(index));
        powerContainer.setObserved(powerData);
        return powerContainer;
    }

    private List<ConnectionPortfolioDto> buildConnectionForecastList(LocalDate period, String connectionEntityAddress,
            String udiEndpoint) {
        final ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto(connectionEntityAddress);
        connectionPortfolioDTO.getConnectionPowerPerPTU().putAll(IntStream.rangeClosed(1, 12).mapToObj(index -> {
            PowerContainerDto powerContainerDto = new PowerContainerDto(period, index);
            powerContainerDto.setForecast(new ForecastPowerDataDto(TEN, TEN, ZERO, TEN, ZERO, TEN, ZERO));
            return powerContainerDto;
        }).collect(Collectors.toMap(PowerContainerDto::getTimeIndex, Function.identity())));
        UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto(udiEndpoint, 12, null);
        udiPortfolioDto.getUdiPowerPerDTU().putAll(IntStream.rangeClosed(1, 12).mapToObj(index -> {
            PowerContainerDto powerContainerDto = new PowerContainerDto(period, index);
            powerContainerDto.setForecast(new ForecastPowerDataDto(TEN, TEN, ZERO, TEN, ZERO, TEN, ZERO));
            return powerContainerDto;
        }).collect(Collectors.toMap(PowerContainerDto::getTimeIndex, Function.identity())));
        List<ConnectionPortfolioDto> connectionPortfolioDtos = Collections.singletonList(connectionPortfolioDTO);
        connectionPortfolioDTO.getUdis().add(udiPortfolioDto);
        connectionPortfolioDTO.getUdis().add(new UdiPortfolioDto("udi://endpoint2", 12, null));
        return connectionPortfolioDtos;
    }

    private void stubRepositoriesForConnectionForecast(LocalDate period, String connectionEntityAddress, String endpoint) {
        when(powerContainerRepository.findConnectionPowerContainers(eq(period), eq(Optional.empty()), eq(Optional.empty())))
                .thenReturn(Collections.singletonMap(new Connection(connectionEntityAddress),
                        IntStream.rangeClosed(1, 12).mapToObj(index -> {
                            PowerContainer powerContainer = new PowerContainer(period, index);
                            powerContainer.setProfile(new PowerData());
                            return powerContainer;
                        }).collect(Collectors.toList())));

        when(corePlanboardBusinessService.findActiveConnections(eq(period), eq(Optional.empty())))
                .thenReturn(Collections.singletonList(new Connection(connectionEntityAddress)));
        when(udiRepository.findActiveUdisMappedPerEndpoint(eq(period))).thenReturn(Collections.singletonMap(endpoint, new Udi()));
    }

}
