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

package energy.usef.agr.workflow.operate.reoptimize;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_NON_UDI_REOPTIMIZE_PORTFOLIO;
import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_REOPTIMIZE_PORTFOLIO;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ForecastPowerDataDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.dto.device.request.ReduceRequestDto;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.event.Event;

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
import org.slf4j.Logger;

/**
 * Test class in charge of the unit tests related to the {@link AgrReOptimizePortfolioCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class AgrReOptimizePortfolioCoordinatorTest {

    public static final String PARTICIPANT_DOMAIN = "agr.usef-example.com";
    public static final int PTUS_PER_DAY = 12;
    private final String ENTITY_ADDRESS = "testAddress";
    private AgrReOptimizePortfolioCoordinator coordinator;
    @Mock
    private Logger LOGGER;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private Config config;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Mock
    private AgrPlanboardBusinessService agrPlanboardBusinessService;

    @Mock
    private Event<ReCreatePrognosesEvent> reCreatePrognosesEventManager;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private ReOptimizeFlagHolder reOptimizeFlagHolder;

    @Before
    public void init() throws Exception {
        coordinator = new AgrReOptimizePortfolioCoordinator();
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, agrPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configAgr);
        Whitebox.setInternalState(coordinator, reOptimizeFlagHolder);

        Whitebox.setInternalState(coordinator, "reCreatePrognosesEventManager", reCreatePrognosesEventManager);
        Whitebox.setInternalState(coordinator, "corePlanboardBusinessService", corePlanboardBusinessService);
        Mockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        Mockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Mockito.anyLong(), Mockito.any(), Mockito.any()))
                .thenReturn(new PlanboardMessage());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvokeWorkflowUdi() {
        Mockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(false);

        // variables and mocking
        final LocalDate period = new LocalDate();
        List<PtuFlexOrder> ptuFlexOrderList = buildPtuFlexOrderList(period);
        List<PtuFlexOffer> ptuFlexOfferList = buildPtuFlexOfferList(period);
        List<PtuPrognosis> ptuPrognosisList = buildPtuPrognosisList(period);
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(period)).thenReturn(
                buildConnectionPortfolio());

        Mockito.when(corePlanboardBusinessService.findAcceptedFlexOrdersForUsefIdentifierOnDate(
                Matchers.<Optional<String>>any(), Mockito.eq(period))).thenReturn(ptuFlexOrderList);
        Mockito.when(corePlanboardBusinessService.findFlexOffersWithOrderInPeriod(Matchers.any(LocalDate.class)))
                .thenReturn(ptuFlexOfferList);
        Mockito.when(corePlanboardBusinessService.findPrognosesWithOrderInPeriod(Matchers.any(LocalDate.class)))
                .thenReturn(ptuPrognosisList);
        Mockito.when(corePlanboardBusinessService.findFlexOffersWithOrderInPeriod(period))
                .thenReturn(buildPtuFlexOfferList(period));

        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_REOPTIMIZE_PORTFOLIO.name()), Mockito.any()))
                .thenReturn(buildWorkflowContextUdiOut());

        Mockito.when(corePlanboardBusinessService.findActiveConnectionGroupsWithConnections(Matchers.any(LocalDate.class)))
                .thenReturn(buildConnectionGroupToConnections());
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(buildConnectionPortfolio());

        // actual invocation
        coordinator.execute(new ExecuteReOptimizePortfolioEvent(period));
        // verifications
        ArgumentCaptor<ReCreatePrognosesEvent> reCreatePrognosesEventCaptor = ArgumentCaptor.forClass(ReCreatePrognosesEvent.class);
        Mockito.verify(agrPortfolioBusinessService, Mockito.times(1)).findConnectionPortfolioDto(Matchers.eq(period));
        Mockito.verify(agrPlanboardBusinessService, Mockito.times(12))
                .changeStatusOfPtuFlexOrder(Matchers.any(PtuFlexOrder.class), Mockito.eq(AcknowledgementStatus.PROCESSED));
        Mockito.verify(reCreatePrognosesEventManager, Mockito.times(1)).fire(reCreatePrognosesEventCaptor.capture());
        Mockito.verify(agrPortfolioBusinessService, Mockito.times(1))
                .storeDeviceMessages(Matchers.any(List.class), Matchers.any(Map.class));

        ReCreatePrognosesEvent capturedEvent = reCreatePrognosesEventCaptor.getValue();
        Assert.assertNotNull(capturedEvent);
        Assert.assertEquals(period, capturedEvent.getPeriod());
    }

    @Test
    public void testInvokeWorkflowNonUdi() {
        Mockito.when(configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)).thenReturn(true);

        // variables and mocking
        final LocalDate period = new LocalDate();
        List<PtuFlexOrder> ptuFlexOrderList = buildPtuFlexOrderList(period);
        List<PtuFlexOffer> ptuFlexOfferList = buildPtuFlexOfferList(period);
        List<PtuPrognosis> ptuPrognosisList = buildPtuPrognosisList(period);
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(period)).thenReturn(buildConnectionPortfolio());

        Mockito.when(corePlanboardBusinessService.findAcceptedFlexOrdersForUsefIdentifierOnDate(
                Matchers.<Optional<String>>any(), Mockito.eq(period))).thenReturn(ptuFlexOrderList);
        Mockito.when(corePlanboardBusinessService.findFlexOffersWithOrderInPeriod(Matchers.any(LocalDate.class)))
                .thenReturn(ptuFlexOfferList);
        Mockito.when(corePlanboardBusinessService.findPrognosesWithOrderInPeriod(Matchers.any(LocalDate.class)))
                .thenReturn(ptuPrognosisList);
        Mockito.when(corePlanboardBusinessService.findFlexOffersWithOrderInPeriod(period))
                .thenReturn(buildPtuFlexOfferList(period));

        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_NON_UDI_REOPTIMIZE_PORTFOLIO.name()), Mockito.any()))
                .thenReturn(buildWorkflowContextOut());

        Mockito.when(corePlanboardBusinessService.findActiveConnectionGroupsWithConnections(Matchers.any(LocalDate.class)))
                .thenReturn(buildConnectionGroupToConnections());
        Mockito.when(agrPortfolioBusinessService.findConnectionPortfolioDto(Matchers.any(LocalDate.class)))
                .thenReturn(buildConnectionPortfolio());

        // actual invocation
        coordinator.execute(new ExecuteReOptimizePortfolioEvent(period));

        // verifications
        ArgumentCaptor<ReCreatePrognosesEvent> reCreatePrognosesEventCaptor = ArgumentCaptor.forClass(ReCreatePrognosesEvent.class);
        Mockito.verify(agrPortfolioBusinessService, Mockito.times(1)).findConnectionPortfolioDto(Matchers.eq(period));
        Mockito.verify(agrPlanboardBusinessService, Mockito.times(12))
                .changeStatusOfPtuFlexOrder(Matchers.any(PtuFlexOrder.class), Mockito.eq(AcknowledgementStatus.PROCESSED));
        Mockito.verify(reCreatePrognosesEventManager, Mockito.times(1)).fire(reCreatePrognosesEventCaptor.capture());

        ReCreatePrognosesEvent capturedEvent = reCreatePrognosesEventCaptor.getValue();
        Assert.assertNotNull(capturedEvent);
        Assert.assertEquals(period, capturedEvent.getPeriod());
    }

    private List<PtuFlexOrder> buildPtuFlexOrderList(LocalDate period) {
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup(ENTITY_ADDRESS);
        return IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
            PtuFlexOrder ptuFlexOrder = new PtuFlexOrder();
            ptuFlexOrder.setFlexOfferSequence(1000L);
            ptuFlexOrder.setParticipantDomain(PARTICIPANT_DOMAIN);
            ptuFlexOrder.setPtuContainer(new PtuContainer(period, index));
            ptuFlexOrder.setConnectionGroup(connectionGroup);
            ptuFlexOrder.setSequence(1001L);
            AcknowledgementStatus acknowledgementStatus = AcknowledgementStatus.ACCEPTED;
            ptuFlexOrder.setAcknowledgementStatus(acknowledgementStatus);
            return ptuFlexOrder;
        }).collect(Collectors.toList());
    }

    private List<PtuFlexOffer> buildPtuFlexOfferList(LocalDate period) {
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup(ENTITY_ADDRESS);
        return IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
            PtuFlexOffer ptuFlexOffer = new PtuFlexOffer();
            ptuFlexOffer.setPower(BigInteger.valueOf(index * 10 + 100));
            ptuFlexOffer.setPrice(BigDecimal.valueOf(index * 3 + 50));
            ptuFlexOffer.setSequence(1000L);
            ptuFlexOffer.setPtuContainer(new PtuContainer(period, index));
            ptuFlexOffer.setParticipantDomain(PARTICIPANT_DOMAIN);
            ptuFlexOffer.setConnectionGroup(connectionGroup);
            return ptuFlexOffer;
        }).collect(Collectors.toList());
    }

    private List<PtuPrognosis> buildPtuPrognosisList(LocalDate period) {
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup(ENTITY_ADDRESS);
        return IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
            PtuPrognosis ptuPrognosis = new PtuPrognosis();
            ptuPrognosis.setPower(BigInteger.valueOf(index * 10 + 100));
            ptuPrognosis.setSequence(998L);
            ptuPrognosis.setPtuContainer(new PtuContainer(period, index));
            ptuPrognosis.setParticipantDomain(PARTICIPANT_DOMAIN);
            ptuPrognosis.setConnectionGroup(connectionGroup);
            return ptuPrognosis;
        }).collect(Collectors.toList());
    }

    private Map<ConnectionGroup, List<Connection>> buildConnectionGroupToConnections() {
        final ConnectionGroup congestionPoint = new CongestionPointConnectionGroup("ean.123456789012345678");
        final Connection connection1 = new Connection(ENTITY_ADDRESS);
        return Collections.singletonMap(congestionPoint, Collections.singletonList(connection1));
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolio() {
        ConnectionPortfolioDto connectionDto = new ConnectionPortfolioDto(ENTITY_ADDRESS);

        ForecastPowerDataDto forecastPowerDataDto = new ForecastPowerDataDto();
        forecastPowerDataDto.setAverageConsumption(BigInteger.ONE);
        forecastPowerDataDto.setUncontrolledLoad(BigInteger.ONE);

        PowerContainerDto powerContainerDto = new PowerContainerDto(new LocalDate(), 1);
        powerContainerDto.setForecast(forecastPowerDataDto);

        connectionDto.getConnectionPowerPerPTU().put(1, powerContainerDto);

        return Collections.singletonList(connectionDto);
    }

    private WorkflowContext buildWorkflowContextOut() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name(),
                Collections.singletonList(new ConnectionPortfolioDto(ENTITY_ADDRESS)));
        return context;
    }

    private WorkflowContext buildWorkflowContextUdiOut() {
        ReduceRequestDto reduceRequest = new ReduceRequestDto();
        reduceRequest.setPower(BigInteger.TEN);

        List<DeviceMessageDto> deviceMessageDtos = new ArrayList<>();
        DeviceMessageDto deviceMessageDto = new DeviceMessageDto();
        deviceMessageDto.setEndpoint("endpoint");
        deviceMessageDto.getReduceRequestDtos().add(reduceRequest);
        deviceMessageDtos.add(deviceMessageDto);

        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name(),
                Collections.singletonList(new ConnectionPortfolioDto(ENTITY_ADDRESS)));
        context.setValue(ReOptimizePortfolioStepParameter.OUT.DEVICE_MESSAGES_OUT.name(), deviceMessageDtos);

        return context;
    }
}
