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

package energy.usef.agr.workflow.settlement.initiate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter;
import energy.usef.core.workflow.settlement.CoreSettlementBusinessService;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class AgrInitiateSettlementCoordinatorTest {
    @Mock
    private CoreSettlementBusinessService coreSettlementBusinessService;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private Config config;

    private AgrInitiateSettlementCoordinator coordinator;
    public static final LocalDate END_DATE = new LocalDate(2014, 11, 30);
    public static final LocalDate START_DATE = new LocalDate(2014, 11, 1);

    @Before
    public void setUp() throws Exception {
        coordinator = new AgrInitiateSettlementCoordinator();
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, coreSettlementBusinessService);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, config);
        when(coreSettlementBusinessService.findRelevantPrognoses(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(coreSettlementBusinessService.findRelevantFlexRequests(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(coreSettlementBusinessService.findRelevantFlexOffers(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(buildPtuFlexOfferList());
        when(coreSettlementBusinessService.findRelevantFlexOrders(any(LocalDate.class),
                any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(agrPortfolioBusinessService.findConnectionPortfolioDto(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new HashMap<>());
    }

    @Test
    public void testHandleAgrInitiateSettlementEventHappyFlow() throws Exception {
        final InitiateSettlementEvent event = new InitiateSettlementEvent();
        event.setPeriodInMonth(new LocalDate(2014, 11, 26));
        when(workflowStepExecuter.invoke(
                Matchers.eq(AgrWorkflowStep.AGR_INITIATE_SETTLEMENT.name()), any(WorkflowContext.class))).then(call -> {
            WorkflowContext inContext = ((WorkflowContext) call.getArguments()[1]);
            inContext.setValue(
                    CoreInitiateSettlementParameter.OUT.SETTLEMENT_DTO.name(),
                    buildSettlementDto());
            return inContext;
        });
        // actual invocation
        coordinator.handleAgrInitiateSettlementEvent(event);
        // verification
        verify(workflowStepExecuter, times(1)).invoke(
                Matchers.eq(AgrWorkflowStep.AGR_INITIATE_SETTLEMENT.name()),
                any(WorkflowContext.class));
        verify(coreSettlementBusinessService, times(1)).createFlexOrderSettlements(any(List.class));
    }

    @Test
    public void testInvokePluggableBusinessComponent() throws Exception {
        final WorkflowContext inContext = new DefaultWorkflowContext();
        when(workflowStepExecuter.invoke(
                Matchers.eq(AgrWorkflowStep.AGR_INITIATE_SETTLEMENT.name()), any(WorkflowContext.class))).then(call -> {
            ((WorkflowContext) call.getArguments()[1]).setValue(CoreInitiateSettlementParameter.OUT.SETTLEMENT_DTO.name(),
                    new SettlementDto(START_DATE, END_DATE));
            return call.getArguments()[1];
        });
        coordinator.invokeInitiateSettlementPbc(inContext);
        verify(workflowStepExecuter, times(1))
                .invoke(Matchers.eq(AgrWorkflowStep.AGR_INITIATE_SETTLEMENT.name()), any(WorkflowContext.class));
    }

    @Test
    public void testGetWorkflowName() throws Exception {
        Assert.assertEquals(AgrWorkflowStep.AGR_INITIATE_SETTLEMENT.name(), coordinator.getWorkflowName());
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

    // settlement dto with ordered power = 10, delivered power = 5 and flex offer price = 10
    private SettlementDto buildSettlementDto() {
        SettlementDto settlementDto = new SettlementDto(START_DATE, END_DATE);
        FlexOrderSettlementDto flexOrderSettlementDto = new FlexOrderSettlementDto(START_DATE);
        PtuSettlementDto ptuSettlementDto = new PtuSettlementDto();
        ptuSettlementDto.setDeliveredFlexPower(BigInteger.valueOf(5));
        ptuSettlementDto.setOrderedFlexPower(BigInteger.TEN);
        ptuSettlementDto.setPtuIndex(BigInteger.ONE);
        flexOrderSettlementDto.getPtuSettlementDtos().add(ptuSettlementDto);
        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setFlexOfferSequenceNumber(1l);
        flexOrderDto.setParticipantDomain("agr.usef-example.com");
        flexOrderSettlementDto.setFlexOrder(flexOrderDto);
        settlementDto.getFlexOrderSettlementDtos().add(flexOrderSettlementDto);
        return settlementDto;
    }

}
