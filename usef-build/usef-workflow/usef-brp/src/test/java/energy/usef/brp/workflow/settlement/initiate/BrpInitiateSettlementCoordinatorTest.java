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

package energy.usef.brp.workflow.settlement.initiate;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.brp.model.MeterDataCompany;
import energy.usef.brp.service.business.BrpBusinessService;
import energy.usef.brp.workflow.BrpWorkflowStep;
import energy.usef.brp.workflow.settlement.send.CheckInitiateSettlementDoneEvent;
import energy.usef.core.config.Config;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter;
import energy.usef.core.workflow.settlement.CoreSettlementBusinessService;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

/**
 * Test class to test BrpInitiateSettlementCoordinator.
 */
@RunWith(PowerMockRunner.class)
public class BrpInitiateSettlementCoordinatorTest {

    private static final LocalDate START_DATE = new LocalDate(2014, 11, 1);
    private static final LocalDate END_DATE = new LocalDate(2014, 11, 30);

    private BrpInitiateSettlementCoordinator coordinator;
    @Mock
    private CoreSettlementBusinessService coreSettlementBusinessService;
    @Mock
    private ConfigBrp configDso;
    @Mock
    private Config config;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private Event<CheckInitiateSettlementDoneEvent> checkInitiateSettlementDoneEvent;
    @Mock
    private BrpBusinessService brpBusinessService;

    @Before
    public void setUp() {
        coordinator = new BrpInitiateSettlementCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();

        Whitebox.setInternalState(coordinator, coreSettlementBusinessService);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configDso);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, brpBusinessService);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);
        Whitebox.setInternalState(coordinator, "checkInitiateSettlementDoneEvent", checkInitiateSettlementDoneEvent);
        when(coreSettlementBusinessService.findRelevantPrognoses(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(coreSettlementBusinessService.findRelevantFlexRequests(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(coreSettlementBusinessService.findRelevantFlexOffers(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(coreSettlementBusinessService.findRelevantFlexOrders(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(configDso.getIntegerProperty(ConfigBrpParam.BRP_METER_DATA_QUERY_EXPIRATION_IN_HOURS)).thenReturn(12);
        when(workflowStepExecuter.invoke(eq(BrpWorkflowStep.BRP_INITIATE_SETTLEMENT.name()),
                any(WorkflowContext.class))).then(call -> {
            WorkflowContext inContext = (WorkflowContext) call.getArguments()[1];
            inContext.setValue(CoreInitiateSettlementParameter.OUT.SETTLEMENT_DTO.name(),
                    new SettlementDto(START_DATE, END_DATE));
            return inContext;
        });
        when(workflowStepExecuter.invoke(eq(BrpWorkflowStep.BRP_REQUEST_PENALTY_DATA.name()),
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
        CollectSmartMeterDataEvent collectSmartMeterDataEvent = new CollectSmartMeterDataEvent(START_DATE);
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
        when(brpBusinessService.findAllMDCs()).then(call -> {
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
    public void testhandleBrpInitiateSettlement() {
        when(coreSettlementBusinessService.isEachFlexOrderReadyForSettlement(any(Integer.class), any(Integer.class)))
                .thenReturn(Boolean.TRUE);
        coordinator.handleBrpInitiateSettlement(new FinalizeInitiateSettlementEvent(START_DATE, END_DATE, null));
        verify(workflowStepExecuter, times(1))
                .invoke(eq(BrpWorkflowStep.BRP_INITIATE_SETTLEMENT.name()), Matchers.any(WorkflowContext.class));
        verify(workflowStepExecuter, times(1))
                .invoke(eq(BrpWorkflowStep.BRP_REQUEST_PENALTY_DATA.name()), Matchers.any(WorkflowContext.class));
        verify(checkInitiateSettlementDoneEvent, times(1)).fire(any(CheckInitiateSettlementDoneEvent.class));
    }

    @Test
    public void testGetWorkflowName() throws Exception {
        Assert.assertEquals(BrpWorkflowStep.BRP_INITIATE_SETTLEMENT.name(), coordinator.getWorkflowName());
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

    @Test
    public void testSaveSettlement() throws Exception {
        coordinator.saveSettlement(buildSettlementDto());
        verify(coreSettlementBusinessService, times(1))
                .createFlexOrderSettlements(Matchers.anyListOf(FlexOrderSettlementDto.class));
    }

    private SettlementDto buildSettlementDto() {
        SettlementDto settlementDto = new SettlementDto(START_DATE, END_DATE);
        FlexOrderSettlementDto flexOrderSettlementDto = new FlexOrderSettlementDto(new LocalDate(2014, 1, 15));
        settlementDto.getFlexOrderSettlementDtos().add(flexOrderSettlementDto);
        return settlementDto;
    }

}
