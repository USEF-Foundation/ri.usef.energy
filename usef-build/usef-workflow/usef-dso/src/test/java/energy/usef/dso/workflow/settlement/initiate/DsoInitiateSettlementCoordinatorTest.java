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

package energy.usef.dso.workflow.settlement.initiate;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import energy.usef.core.config.Config;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter;
import energy.usef.core.workflow.settlement.CoreSettlementBusinessService;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;
import energy.usef.dso.model.MeterDataCompany;
import energy.usef.dso.model.PtuGridMonitor;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.dto.GridMonitoringDto;
import energy.usef.dso.workflow.settlement.send.CheckInitiateSettlementDoneEvent;

/**
 * Test class in charge of the unit tests related to the {@link DsoInitiateSettlementCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class DsoInitiateSettlementCoordinatorTest {
    private static final LocalDate START_DATE = new LocalDate(2014, 11, 1);
    private static final LocalDate END_DATE = new LocalDate(2014, 11, 30);

    private DsoInitiateSettlementCoordinator coordinator;
    @Mock
    private CoreSettlementBusinessService coreSettlementBusinessService;
    @Mock
    private ConfigDso configDso;
    @Mock
    private Config config;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private Event<FinalizeInitiateSettlementEvent> finalizeInitiateSettlementEventManager;
    @Mock
    private Event<CheckInitiateSettlementDoneEvent> checkInitiateSettlementDoneEvent;

    @Before
    public void setUp() {
        coordinator = new DsoInitiateSettlementCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        Whitebox.setInternalState(coordinator, coreSettlementBusinessService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configDso);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, dsoPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);
        Whitebox.setInternalState(coordinator, "finalizeInitiateSettlementEventManager", finalizeInitiateSettlementEventManager);
        Whitebox.setInternalState(coordinator, "checkInitiateSettlementDoneEvent", checkInitiateSettlementDoneEvent);
        when(coreSettlementBusinessService.findRelevantPrognoses(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(coreSettlementBusinessService.findRelevantFlexRequests(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(coreSettlementBusinessService.findRelevantFlexOffers(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(coreSettlementBusinessService.findRelevantFlexOrders(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(configDso.getIntegerProperty(ConfigDsoParam.DSO_METER_DATA_QUERY_EXPIRATION_IN_HOURS)).thenReturn(12);
        when(workflowStepExecuter.invoke(eq(DsoWorkflowStep.DSO_INITIATE_SETTLEMENT.name()),
                any(WorkflowContext.class))).then(call -> {
            WorkflowContext inContext = (WorkflowContext) call.getArguments()[1];
            inContext.setValue(CoreInitiateSettlementParameter.OUT.SETTLEMENT_DTO.name(),
                    new SettlementDto(START_DATE, END_DATE));
            return inContext;
        });
        when(workflowStepExecuter.invoke(eq(DsoWorkflowStep.DSO_REQUEST_PENALTY_DATA.name()),
                any(WorkflowContext.class))).then(call -> {
            WorkflowContext inContext = (WorkflowContext) call.getArguments()[1];
            inContext.setValue(RequestPenaltyDataParameter.OUT.UPDATED_SETTLEMENT_DTO.name(),
                    new SettlementDto(START_DATE, END_DATE));
            return inContext;
        });
    }

    @Test
    public void testHandleCollectSmartMeterDataEvent() {
        // mocking and variables
        CollectSmartMeterDataEvent collectSmartMeterDataEvent = new CollectSmartMeterDataEvent();
        collectSmartMeterDataEvent.setPeriodInMonth(START_DATE);
        when(corePlanboardBusinessService
                .findConnectionGroupWithConnectionsWithOverlappingValidity(any(LocalDate.class), any(LocalDate.class)))
                .then(call -> {
                    Map<LocalDate, Map<ConnectionGroup, List<Connection>>> result = new HashMap<>();
                    HashMap<ConnectionGroup, List<Connection>> connectionGroupMap = new HashMap<>();
                    connectionGroupMap.put(
                            new CongestionPointConnectionGroup("ean.1111111111"),
                            Collections.singletonList(new Connection("ean.000000000001")));
                    result.put((LocalDate) call.getArguments()[0], connectionGroupMap);
                    return result;
                });
        when(corePlanboardBusinessService
                .findPlanboardMessages(eq(DocumentType.FLEX_ORDER), any(LocalDate.class), any(LocalDate.class),
                        eq(DocumentStatus.ACCEPTED))).then(call -> {
            PlanboardMessage planboardMessage = new PlanboardMessage();
            planboardMessage.setPeriod((LocalDate) call.getArguments()[1]);
            return Collections.singletonList(planboardMessage);
        });
        when(dsoPlanboardBusinessService.findAllMDCs()).then(call -> {
            MeterDataCompany mdc = new MeterDataCompany();
            mdc.setDomain("mdc.usef-example.com");
            return Collections.singletonList(mdc);
        });
        // invocation
        coordinator.handleCollectSmartMeterDataEvent(collectSmartMeterDataEvent);
        // verifications and assertions
        verify(jmsHelperService, times(1)).sendMessageToOutQueue(any(String.class));
    }

    @Test
    public void testFinalizeInitiateSettlement() {
        final LocalDate period = new LocalDate(2015, 11, 10);
        when(corePlanboardBusinessService.findPlanboardMessagesOlderThan(any(LocalDateTime.class),
                eq(DocumentType.METER_DATA_QUERY_USAGE), eq(DocumentStatus.SENT))).then(call -> {
            PlanboardMessage planboardMessage = new PlanboardMessage();
            planboardMessage.setPeriod(period);
            return Collections.singletonList(planboardMessage);
        });
        coordinator.finalizeUnfinishedInitiateSettlements(new FinalizeUnfinishedInitiateSettlementEvent());
        ArgumentCaptor<FinalizeInitiateSettlementEvent> captor = ArgumentCaptor.forClass(FinalizeInitiateSettlementEvent.class);
        verify(finalizeInitiateSettlementEventManager, times(1)).fire(captor.capture());
        FinalizeInitiateSettlementEvent event = captor.getValue();
        Assert.assertEquals(period, event.getStartDate());
        Assert.assertEquals(period, event.getEndDate());
        Assert.assertTrue(event.getMeterDataPerCongestionPoint().isEmpty());
    }

    @Test
    public void testHandleDsoInitiateSettlementWithGridMonitoringData() {
        when(dsoPlanboardBusinessService.findGridMonitoringData(any(LocalDate.class), any(LocalDate.class))).thenReturn(
                buildGridMonitoringData());
        when(dsoPlanboardBusinessService.findAggregatorsWithOverlappingActivityForPeriod(eq(START_DATE), eq(END_DATE)))
                .thenReturn(buildAggregatorsOnConnections());
        coordinator.handleDsoInitiateSettlement(new FinalizeInitiateSettlementEvent(START_DATE, END_DATE, null));
        verify(dsoPlanboardBusinessService, times(1)).findAggregatorsWithOverlappingActivityForPeriod(eq(START_DATE), eq(END_DATE));
        verify(dsoPlanboardBusinessService, times(1)).findGridMonitoringData(eq(START_DATE), eq(END_DATE));
        ArgumentCaptor<WorkflowContext> contextCaptor = ArgumentCaptor.forClass(WorkflowContext.class);
        verify(workflowStepExecuter, times(1)).invoke(eq(DsoWorkflowStep.DSO_INITIATE_SETTLEMENT.name()), contextCaptor.capture());
        List<GridMonitoringDto> gridMonitoringDtos = contextCaptor.getValue()
                .get(DsoInitiateSettlementParameter.IN.GRID_MONITORING_DATA.name(), List.class);
        Assert.assertEquals(1, gridMonitoringDtos.size());
        Assert.assertEquals(START_DATE, gridMonitoringDtos.get(0).getPeriod());
        Assert.assertEquals("ean.111111111111", gridMonitoringDtos.get(0).getCongestionPointEntityAddress());
        Assert.assertEquals(96, gridMonitoringDtos.get(0).getPtuGridMonitoringDtos().size());
        Assert.assertEquals(2, gridMonitoringDtos.get(0).getConnectionCountPerAggregator().size());
    }

    @Test
    public void testhandleDsoInitiateSettlement() {
        when(coreSettlementBusinessService.isEachFlexOrderReadyForSettlement(any(Integer.class), any(Integer.class)))
                .thenReturn(Boolean.TRUE);
        when(coreSettlementBusinessService.findRelevantFlexOffers(Matchers.eq(START_DATE), Matchers.eq(END_DATE)))
                .thenReturn(buildPtuFlexOfferList());
        when(workflowStepExecuter.invoke(
                Matchers.eq(DsoWorkflowStep.DSO_INITIATE_SETTLEMENT.name()), any(WorkflowContext.class))).then(call -> {
            WorkflowContext inContext = ((WorkflowContext) call.getArguments()[1]);
            inContext.setValue(
                    CoreInitiateSettlementParameter.OUT.SETTLEMENT_DTO.name(),
                    buildSettlementDto());
            return inContext;
        });

        coordinator.handleDsoInitiateSettlement(new FinalizeInitiateSettlementEvent(START_DATE, END_DATE, null));
        verify(workflowStepExecuter, times(1))
                .invoke(eq(DsoWorkflowStep.DSO_INITIATE_SETTLEMENT.name()), Matchers.any(WorkflowContext.class));
        verify(workflowStepExecuter, times(1))
                .invoke(eq(DsoWorkflowStep.DSO_REQUEST_PENALTY_DATA.name()), Matchers.any(WorkflowContext.class));
        verify(coreSettlementBusinessService, times(1)).createFlexOrderSettlements(any(List.class));
        verify(checkInitiateSettlementDoneEvent, times(1)).fire(any(CheckInitiateSettlementDoneEvent.class));
    }

    @Test
    public void testGetWorkflowName() throws Exception {
        Assert.assertEquals(DsoWorkflowStep.DSO_INITIATE_SETTLEMENT.name(), coordinator.getWorkflowName());
    }

    @Test
    public void testInitiateWorkflowContext() throws Exception {
        WorkflowContext context = coordinator.initiateWorkflowContext(START_DATE, END_DATE);
        Assert.assertNotNull(context);
        Assert.assertNotNull(context.getValue(CoreInitiateSettlementParameter.IN.START_DATE.name()));
        Assert.assertNotNull(context.getValue(CoreInitiateSettlementParameter.IN.END_DATE.name()));
        Assert.assertNotNull(context.getValue(CoreInitiateSettlementParameter.IN.FLEX_ORDER_DTO_LIST.name()));
        Assert.assertNotNull(context.getValue(CoreInitiateSettlementParameter.IN.FLEX_OFFER_DTO_LIST.name()));
        Assert.assertNotNull(context.getValue(CoreInitiateSettlementParameter.IN.FLEX_REQUEST_DTO_LIST.name()));
        Assert.assertNotNull(context.getValue(CoreInitiateSettlementParameter.IN.PROGNOSIS_DTO_LIST.name()));
    }

    private List<PtuFlexOffer> buildPtuFlexOfferList() {
        List<PtuFlexOffer> ptuFlexOfferList = new ArrayList<>();
        PtuFlexOffer ptuFlexOffer = new PtuFlexOffer();
        ptuFlexOffer.setPrice(BigDecimal.TEN);
        ptuFlexOffer.setSequence(1l);
        ptuFlexOffer.setParticipantDomain("agr.usef-example.com");
        ptuFlexOffer.setConnectionGroup(new CongestionPointConnectionGroup("cp1"));
        ptuFlexOffer.setPtuContainer(new PtuContainer(START_DATE, 1));
        ptuFlexOfferList.add(ptuFlexOffer);
        return ptuFlexOfferList;
    }

    private SettlementDto buildSettlementDto() {
        SettlementDto settlementDto = new SettlementDto(START_DATE, END_DATE);
        FlexOrderSettlementDto flexOrderSettlementDto = new FlexOrderSettlementDto(new LocalDate(2014, 1, 15));
        PtuSettlementDto ptuSettlementDto = new PtuSettlementDto();
        ptuSettlementDto.setDeliveredFlexPower(BigInteger.valueOf(5));
        ptuSettlementDto.setOrderedFlexPower(BigInteger.TEN);
        ptuSettlementDto.setPtuIndex(BigInteger.ONE);
        flexOrderSettlementDto.getPtuSettlementDtos().add(ptuSettlementDto);
        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setFlexOfferSequenceNumber(1l);
        flexOrderSettlementDto.setFlexOrder(flexOrderDto);
        flexOrderDto.setParticipantDomain("agr.usef-example.com");
        settlementDto.getFlexOrderSettlementDtos().add(flexOrderSettlementDto);
        return settlementDto;
    }

    private List<PtuGridMonitor> buildGridMonitoringData() {
        final ConnectionGroup connectionGroup = new CongestionPointConnectionGroup("ean.111111111111");
        return IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuGridMonitor ptuGridMonitor = new PtuGridMonitor();
            ptuGridMonitor.setPtuContainer(new PtuContainer(START_DATE, index));
            ptuGridMonitor.setConnectionGroup(connectionGroup);
            ptuGridMonitor.setActualPower(1000L + index);
            return ptuGridMonitor;
        }).collect(Collectors.toList());
    }

    private List<AggregatorOnConnectionGroupState> buildAggregatorsOnConnections() {
        final CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup("ean.111111111111");
        return IntStream.rangeClosed(1, 2).mapToObj(index -> {
            Aggregator aggregator = new Aggregator();
            aggregator.setDomain("agr" + index + ".usef-example.com");
            AggregatorOnConnectionGroupState state = new AggregatorOnConnectionGroupState();
            state.setValidFrom(START_DATE);
            state.setValidUntil(START_DATE.plusDays(3));
            state.setAggregator(aggregator);
            state.setConnectionCount(BigInteger.valueOf(2));
            state.setCongestionPointConnectionGroup(congestionPoint);
            return state;
        }).collect(Collectors.toList());
    }

}
