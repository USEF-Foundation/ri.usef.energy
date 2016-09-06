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

package energy.usef.core.workflow.coordinator;

import static energy.usef.core.model.DocumentType.FLEX_ORDER;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.core.workflow.settlement.CoreSettlementBusinessService;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import energy.usef.core.model.DocumentStatus;

/**
 * Test class in charge of the unit tests related to the {@link AbstractSettlementCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class AbstractSettlementCoordinatorTest {

    private static final LocalDate START_DATE = new LocalDate(2014, 11, 1);
    private static final LocalDate END_DATE = new LocalDate(2014, 11, 30);
    private static final LocalDateTime DATE_TIME = new LocalDateTime(2014, 11, 1, 12, 0, 0, 0);

    @Mock
    protected Config config;
    @Mock
    protected CoreSettlementBusinessService coreSettlementBusinessService;
    @Mock
    protected CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    protected WorkflowStepExecuter workflowStepExecuter;

    private AbstractSettlementCoordinator coordinator;

    private List<FlexOfferDto> relevantFlexOffers = new ArrayList<>();

    @Before
    public void init() {
        coordinator = new AbstractSettlementCoordinator() {
            @Override
            protected String getWorkflowName() {
                return null;
            }

            @Override
            protected WorkflowContext initiateWorkflowContext(LocalDate startDate, LocalDate endDate) {
                return null;
            }
        };
        FlexOfferDto flexOfferDto = new FlexOfferDto();
        flexOfferDto.setSequenceNumber(2L);
        flexOfferDto.setFlexRequestSequenceNumber(1L);
        flexOfferDto.setPeriod(START_DATE);
        flexOfferDto.setConnectionGroupEntityAddress("usef.energy");
        flexOfferDto.setParticipantDomain("usefdev.org");
        flexOfferDto.getPtus().add(createPtuFlexOfferDto(1, 1, 1));
        flexOfferDto.getPtus().add(createPtuFlexOfferDto(2, 2, 2));
        flexOfferDto.getPtus().add(createPtuFlexOfferDto(3, 3, 3));
        flexOfferDto.getPtus().add(createPtuFlexOfferDto(4, 4, 4));
        flexOfferDto.getPtus().add(createPtuFlexOfferDto(5, 5, 5));
        flexOfferDto.getPtus().add(createPtuFlexOfferDto(6, 6, 6));

        relevantFlexOffers.add(flexOfferDto);

        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, coreSettlementBusinessService);
        Whitebox.setInternalState(coordinator, config);

        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        PowerMockito.when(config.getProperty(ConfigParam.TIME_ZONE)).thenReturn("EUROPE/AMSTERDAM");
    }

    @Test
    public void testFetchRelevantPrognoses() throws Exception {
        List<PrognosisDto> prognosisDtos = coordinator.fetchRelevantPrognoses(START_DATE, END_DATE);
        Assert.assertNotNull(prognosisDtos);
        verify(coreSettlementBusinessService, times(1)).findRelevantPrognoses(START_DATE, END_DATE);
    }

    @Test
    public void testFetchRelevantFlexRequests() throws Exception {
        List<FlexRequestDto> flexRequestDtos = coordinator.fetchRelevantFlexRequests(START_DATE, END_DATE);
        Assert.assertNotNull(flexRequestDtos);
        verify(coreSettlementBusinessService, times(1)).findRelevantFlexRequests(START_DATE, END_DATE);
    }

    @Test
    public void testFetchRelevantFlexOffers() throws Exception {
        List<FlexOfferDto> flexOfferDtos = coordinator.fetchRelevantFlexOffers(START_DATE, END_DATE);
        Assert.assertNotNull(flexOfferDtos);
        verify(coreSettlementBusinessService, times(1)).findRelevantFlexOffers(START_DATE, END_DATE);
    }

    @Test
    public void testFetchRelevantFlexOrders() throws Exception {
        List<FlexOrderDto> orderDtos = coordinator.fetchRelevantFlexOrders(START_DATE, END_DATE, new ArrayList<>());
        Assert.assertNotNull(orderDtos);
        verify(coreSettlementBusinessService, times(1)).findRelevantFlexOrders(START_DATE, END_DATE);
    }

    @Test
    public void testSaveSettlement() throws Exception {
        coordinator.saveSettlement(buildSettlementDto());
        verify(coreSettlementBusinessService, times(1))
                .createFlexOrderSettlements(Matchers.anyListOf(FlexOrderSettlementDto.class));
    }

    @Test
    public void testCalculateSettlementPrice() throws Exception {
        SettlementDto settlementDto = coordinator.calculateSettlementPrice(buildSettlementDto(), buildPtuFlexOfferList());

        Assert.assertNotNull(settlementDto);
        Assert.assertEquals(new BigDecimal("5.0000"), settlementDto.getFlexOrderSettlementDtos().get(0).getPtuSettlementDtos().get(0).getPrice());
    }

    private List<FlexOfferDto> buildPtuFlexOfferList() {
        List<FlexOfferDto> flexOffers = new ArrayList<>();
        FlexOfferDto flexOffer = new FlexOfferDto();

        PtuFlexOfferDto ptuFlexOffer = new PtuFlexOfferDto();
        ptuFlexOffer.setPrice(BigDecimal.TEN);
        ptuFlexOffer.setPtuIndex(BigInteger.ONE);
        ptuFlexOffer.setPower(BigInteger.TEN);

        flexOffer.getPtus().add(ptuFlexOffer);
        flexOffer.setParticipantDomain("agr.usef-example.com");
        flexOffer.setSequenceNumber(1l);
        flexOffer.setConnectionGroupEntityAddress("cp1");

        flexOffers.add(flexOffer);

        return flexOffers;
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

    @Test
    public void testFetchRelevantFlexOrders2() throws Exception {
        // Use 4 hour PTU Duration resulting in 6 PTU's per day
        when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(240);
        when(coreSettlementBusinessService.findRelevantFlexOrders(any(LocalDate.class), any(LocalDate.class))).then(call -> {
            List<PtuFlexOrder> ptuFlexOrderList = new ArrayList<>();
            for (int i = 1; i <=6; i++)
                ptuFlexOrderList.add(createPtuFlexOrder((LocalDate) call.getArguments()[0], i));
            return ptuFlexOrderList;
        });

        when(corePlanboardBusinessService
                .findPlanboardMessages(eq(FLEX_ORDER), any(LocalDate.class), any(LocalDate.class), Matchers
                        .eq(DocumentStatus.ACCEPTED))).then(call -> {
            PlanboardMessage planboardMessage = new PlanboardMessage();
            planboardMessage.setPeriod((LocalDate) call.getArguments()[2]);
            planboardMessage.setCreationDateTime(DATE_TIME);
            planboardMessage.setSequence(3L);
            planboardMessage.setParticipantDomain("usefdev.org");
            return Collections.singletonList(planboardMessage);
        });
        List<FlexOrderDto> orderDtos = coordinator.fetchRelevantFlexOrders(START_DATE, END_DATE, relevantFlexOffers);
        Assert.assertNotNull(orderDtos);
        verify(coreSettlementBusinessService, times(1)).findRelevantFlexOrders(START_DATE, END_DATE);
        verify(corePlanboardBusinessService, times(1)).findPlanboardMessages(FLEX_ORDER, START_DATE, END_DATE, DocumentStatus.ACCEPTED);
        verify(corePlanboardBusinessService, times(1)).findPlanboardMessages(FLEX_ORDER, START_DATE, END_DATE, DocumentStatus.PROCESSED);

        Assert.assertEquals(BigInteger.valueOf(0), orderDtos.get(0).getPtus().get(0).getPower());
        Assert.assertEquals(BigInteger.valueOf(0), orderDtos.get(0).getPtus().get(1).getPower());
        Assert.assertEquals(BigInteger.valueOf(0), orderDtos.get(0).getPtus().get(2).getPower());
        Assert.assertEquals(BigInteger.valueOf(0), orderDtos.get(0).getPtus().get(3).getPower());
        Assert.assertEquals(BigInteger.valueOf(5), orderDtos.get(0).getPtus().get(4).getPower());
        Assert.assertEquals(BigInteger.valueOf(6), orderDtos.get(0).getPtus().get(5).getPower());
    }


    private PtuFlexOfferDto createPtuFlexOfferDto(int ptuIndex, int power, int price) {
        PtuFlexOfferDto ptuFlexOfferDto = new PtuFlexOfferDto();
        ptuFlexOfferDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
        ptuFlexOfferDto.setPower(BigInteger.valueOf(power));
        ptuFlexOfferDto.setPrice(BigDecimal.valueOf(price));
        return ptuFlexOfferDto;
    }

    private PtuFlexOrder createPtuFlexOrder (LocalDate ptuDate, int ptuIndex){
        ConnectionGroup connectionGroup = new BrpConnectionGroup();
        connectionGroup.setUsefIdentifier("usef.energy");

        PtuFlexOrder ptuFlexOrder = new PtuFlexOrder();
        ptuFlexOrder.setParticipantDomain("usefdev.org");
        ptuFlexOrder.setSequence(3L);
        ptuFlexOrder.setConnectionGroup(connectionGroup);
        ptuFlexOrder.setFlexOfferSequence(2L);
        ptuFlexOrder.setPtuContainer(createPtuContainer(ptuDate, ptuIndex));
        return ptuFlexOrder;
    }

    private PtuContainer createPtuContainer(LocalDate ptuDate, int ptuIndex) {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuDate(ptuDate);
        ptuContainer.setPtuIndex(ptuIndex);
        return ptuContainer;
    }
}
