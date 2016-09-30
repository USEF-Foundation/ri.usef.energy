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

package energy.usef.brp.service.business;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.repository.ConnectionGroupRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuFlexOrderRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link BrpPlanboardBusinessService} class.
 */
@RunWith(PowerMockRunner.class)
public class BrpPlanboardBusinessServiceTest {

    @Mock
    private PtuPrognosisRepository ptuPrognosisRepository;
    @Mock
    private PlanboardMessageRepository planboardMessageRepository;
    @Mock
    private PtuFlexOrderRepository ptuFlexOrderRepository;
    @Mock
    private Config config;
    @Mock
    private ConnectionGroupRepository connectionGroupRepository;

    private BrpPlanboardBusinessService brpPlanboardBusinessService;
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Before
    public void init() {
        brpPlanboardBusinessService = new BrpPlanboardBusinessService();
        corePlanboardBusinessService = new CorePlanboardBusinessService();
        Whitebox.setInternalState(brpPlanboardBusinessService, ptuPrognosisRepository);
        Whitebox.setInternalState(brpPlanboardBusinessService, planboardMessageRepository);
        Whitebox.setInternalState(brpPlanboardBusinessService, ptuFlexOrderRepository);
        Whitebox.setInternalState(brpPlanboardBusinessService, config);

        Whitebox.setInternalState(corePlanboardBusinessService, ptuPrognosisRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, planboardMessageRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, ptuFlexOrderRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, connectionGroupRepository);

        PowerMockito.when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("brp.usef-example.com");

    }

    @Test
    public void tesfFindOrderableFlexOffers() {
        brpPlanboardBusinessService.findOrderableFlexOffers();
        Mockito.verify(planboardMessageRepository, Mockito.times(1))
                .findOrderableFlexOffers();
    }

    @Test
    public void testFindOrderableFlexOffersSomeExpired() {
        List<PlanboardMessage> allOrderableFlexOffers = buildPlanboardMessageList(DocumentType.FLEX_OFFER);
        Mockito.when(planboardMessageRepository.findOrderableFlexOffers()).thenReturn(allOrderableFlexOffers);

        List<PlanboardMessage> nonExpiredFlexOffers = brpPlanboardBusinessService.findOrderableFlexOffers();
        Assert.assertEquals(3, nonExpiredFlexOffers.size());
    }

    @Test
    public void testUpdateFlexOrdersWithAcknowledgementStatus() {
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentStatus(DocumentStatus.SENT);
        PowerMockito.when(planboardMessageRepository.findSinglePlanboardMessage(Matchers.any(Long.class),
                Matchers.eq(DocumentType.FLEX_ORDER), Matchers.any(String.class))).thenReturn(planboardMessage);
        List<PtuFlexOrder> mockedPtuFlexOrders = buildPtuFlexOrderList();
        PowerMockito.when(ptuFlexOrderRepository.findFlexOrdersBySequence(Matchers.any(Long.class))).thenReturn
                (mockedPtuFlexOrders);
        PtuFlexOrder mockedPtuFlexOrder = mockedPtuFlexOrders.get(0);
        brpPlanboardBusinessService.updateFlexOrdersWithAcknowledgementStatus(1l, AcknowledgementStatus.ACCEPTED,
                "agr.usef-example.com");
        Mockito.verify(planboardMessageRepository, Mockito.times(1))
                .findSinglePlanboardMessage(Matchers.eq(1l), Matchers.eq(DocumentType.FLEX_ORDER),
                        Matchers.eq("agr.usef-example.com"));
        Mockito.verify(ptuFlexOrderRepository, Mockito.times(1)).findFlexOrdersBySequence(Matchers.eq(1l));
        Mockito.verify(mockedPtuFlexOrder, Mockito.times(96)).setAcknowledgementStatus(AcknowledgementStatus.ACCEPTED);
    }

    @Test
    public void testFindAcceptedFlexOrders() {
        PowerMockito.when(corePlanboardBusinessService
                .findPlanboardMessages(DocumentType.FLEX_ORDER, new LocalDate().minusMonths(1), new LocalDate(),
                        DocumentStatus.ACCEPTED)).thenReturn(buildPlanboardMessageList(DocumentType.FLEX_ORDER));
        brpPlanboardBusinessService.findAcceptedFlexOrders(new LocalDate().minusMonths(1), new LocalDate());
        Mockito.verify(planboardMessageRepository, Mockito.times(1))
                .findPlanboardMessages(DocumentType.FLEX_ORDER, new LocalDate().minusMonths(1), new LocalDate(),
                        DocumentStatus.ACCEPTED);
    }

    @Test
    public void testFinalizePendingAPlans() {
        LocalDate date = new LocalDate();
        List<PlanboardMessage> planboardList = buildPlanboardMessageList(DocumentType.A_PLAN);
        planboardList.get(0).setDocumentStatus(DocumentStatus.ACCEPTED);
        planboardList.get(1).setDocumentStatus(DocumentStatus.RECEIVED);
        planboardList.get(2).setDocumentStatus(DocumentStatus.PENDING_FLEX_TRADING);
        planboardList.get(3).setDocumentStatus(DocumentStatus.PROCESSED);
        planboardList.get(4).setDocumentStatus(DocumentStatus.FINAL);

        Mockito.when(planboardMessageRepository.findPlanboardMessages(DocumentType.A_PLAN, date, null)).thenReturn(planboardList);

        brpPlanboardBusinessService.finalizePendingAPlans(date);

        // verify that status (only planboard messages with status RECEIVED and PENDING_FLEX_TRADING will be set to FINAL
        Assert.assertEquals(DocumentStatus.ACCEPTED, planboardList.get(0).getDocumentStatus());
        Assert.assertEquals(DocumentStatus.FINAL, planboardList.get(1).getDocumentStatus());
        Assert.assertEquals(DocumentStatus.FINAL, planboardList.get(2).getDocumentStatus());
        Assert.assertEquals(DocumentStatus.PROCESSED, planboardList.get(3).getDocumentStatus());
        Assert.assertEquals(DocumentStatus.FINAL, planboardList.get(4).getDocumentStatus());
    }

    private List<PlanboardMessage> buildPlanboardMessageList(DocumentType documentType) {
        List<PlanboardMessage> planboardMessageList = new ArrayList<>();
        for (long i = 1; i <= 5; ++i) {
            planboardMessageList.add(buildPlanboardMessage(documentType, i));
        }
        return planboardMessageList;
    }

    private PlanboardMessage buildPlanboardMessage(DocumentType documentType, Long sequence) {
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentStatus(DocumentStatus.SENT);
        planboardMessage.setDocumentType(documentType);
        planboardMessage.setSequence(sequence);
        if (documentType == DocumentType.FLEX_REQUEST || documentType == DocumentType.FLEX_OFFER || documentType == DocumentType.FLEX_ORDER) {
            planboardMessage.setExpirationDate(DateTimeUtil.getCurrentDateTime().plusDays(3).minusDays(sequence.intValue()).plusHours(2));
        }
        return planboardMessage;
    }

    private List<PtuFlexOrder> buildPtuFlexOrderList() {
        PtuFlexOrder mockedOrder = PowerMockito.mock(PtuFlexOrder.class);
        List<PtuFlexOrder> ptuFlexOrders = new ArrayList<>();
        for (int i = 1; i <= 96; ++i) {
            ptuFlexOrders.add(mockedOrder);
        }
        return ptuFlexOrders;
    }
}
