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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import energy.usef.agr.model.CommonReferenceOperator;
import energy.usef.agr.model.SynchronisationConnection;
import energy.usef.agr.repository.CommonReferenceOperatorRepository;
import energy.usef.agr.repository.SynchronisationConnectionRepository;
import energy.usef.agr.repository.SynchronisationConnectionStatusRepository;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.model.PtuFlexRequest;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuFlexOrderRepository;
import energy.usef.core.repository.PtuFlexRequestRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.util.DateTimeUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrPlanboardBusinessService}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DateTimeUtil.class })
public class AgrPlanboardBusinessServiceTest {

    private AgrPlanboardBusinessService planboardService;

    @Mock
    private CommonReferenceOperatorRepository commonReferenceOperatorRepository;
    @Mock
    private SynchronisationConnectionRepository synchronisationConnectionRepository;
    @Mock
    private SynchronisationConnectionStatusRepository synchronisationConnectionStatusRepository;

    @Mock
    private PtuContainerRepository ptuContainerRepository;
    @Mock
    private PtuPrognosisRepository prognosisRepository;

    @Mock
    private PtuFlexRequestRepository flexRequestRepository;
    @Mock
    private PtuFlexOrderRepository ptuFlexOrderRepository;
    @Mock
    private PlanboardMessageRepository planboardMessageRepository;
    @Mock
    private EntityManager entityManager;

    @Before
    public void init() {
        planboardService = new AgrPlanboardBusinessService();
        Whitebox.setInternalState(planboardService, commonReferenceOperatorRepository);
        Whitebox.setInternalState(planboardService, synchronisationConnectionRepository);
        Whitebox.setInternalState(planboardService, synchronisationConnectionStatusRepository);

        Whitebox.setInternalState(planboardService, ptuContainerRepository);
        Whitebox.setInternalState(planboardService, prognosisRepository);
        Whitebox.setInternalState(planboardService, ptuFlexOrderRepository);

        Whitebox.setInternalState(planboardService, flexRequestRepository);
        Whitebox.setInternalState(planboardService, planboardMessageRepository);
    }

    private ConnectionGroup buildConnectionGroup() {
        CongestionPointConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier("ean.123456789012345678");
        connectionGroup.setDsoDomain("dso.usef-example.com");
        return connectionGroup;
    }

    /**
     * Tests AgrPlanboardBusinessService.findLastPrognoses method.
     */
    @Test
    public void testFindLastPrognoses() {
        String usefIdentifier = "brp.usef-example.com";
        LocalDate period = DateTimeUtil.parseDate("2015-03-03");

        planboardService.findLastPrognoses(period, PrognosisType.A_PLAN, Optional.of(usefIdentifier));

        verify(prognosisRepository, times(1)).findLastPrognoses(Matchers.eq(period), Matchers.eq(Optional.of(PrognosisType.A_PLAN)),
                Matchers.eq(Optional.of(usefIdentifier)), Matchers.eq(Optional.empty()));
    }

    /**
     * Tests AgrPlanboardBusinessService.findLastPrognoses method.
     */
    @Test
    public void testFindLastPrognosesWithNullUsefIdentifier() {
        LocalDate period = DateTimeUtil.parseDate("2015-03-03");

        planboardService.findLastPrognoses(period, PrognosisType.A_PLAN, Optional.empty());

        verify(prognosisRepository, times(1)).findLastPrognoses(Matchers.eq(period), Matchers.eq(Optional.of(PrognosisType.A_PLAN)),
                Matchers.eq(Optional.empty()), Matchers.eq(Optional.empty()));
    }

    /**
     * Tests AgrPlanboardBusinessService.findAcceptedRequests method.
     */
    @Test
    public void testFindAcceptedRequests() {
        List<PlanboardMessage> planboardMessageList = planboardService.findAcceptedRequests(DocumentType.A_PLAN);

        Assert.assertNotNull(planboardMessageList);
    }

    /**
     * Tests AgrPlanboardBusinessService.findPTUContainersForPeriod method.
     */
    @Test
    public void testFindPTUContainersForPeriod() {
        List<PtuContainer> ptuContainerList = planboardService.findPTUContainersForPeriod(DateTimeUtil.getCurrentDate());

        Assert.assertNotNull(ptuContainerList);
    }

    /**
     * Tests AgrPlanboardBusinessService.findLastFlexRequestDocumentWithDispositionRequested method.
     */
    @Test
    public void testFindLastFlexRequestDocumentWithDispositionRequested() {
        String usefIdentifier = "ean.123456789012345678";
        Long sequence = 123920350293l;

        @SuppressWarnings("unused")
        PtuFlexRequest ptuFlexRequest = planboardService.findLastFlexRequestDocumentWithDispositionRequested(usefIdentifier,
                DateTimeUtil.getCurrentDate(), sequence);

        ArgumentCaptor<String> usefIdentifierCapturer = ArgumentCaptor.forClass(String.class);
        Mockito.verify(flexRequestRepository, Mockito.times(1)).findLastFlexRequestDocumentWithDispositionRequested(
                usefIdentifierCapturer.capture(), Matchers.any(LocalDate.class), Matchers.any(Long.class));
        Assert.assertEquals(usefIdentifier, usefIdentifierCapturer.getValue());
    }

    @Test
    public void testCleanSynchronization() {
        Mockito.when(synchronisationConnectionStatusRepository.countSynchronisationConnectionStatusWithStatus(Matchers.any()))
                .thenReturn(0l);

        planboardService.cleanSynchronization();

        Mockito.verify(synchronisationConnectionStatusRepository, Mockito.times(1)).deleteAll();
        Mockito.verify(synchronisationConnectionRepository, Mockito.times(1)).deleteAll();
    }

    @Test
    public void testUpdateConnectionStatusForCRO() {
        planboardService.updateConnectionStatusForCRO("cro.usef-example.com");

        Mockito.verify(synchronisationConnectionRepository, Mockito.times(1))
                .updateConnectionStatusForCRO(Matchers.eq("cro.usef-example.com"));
    }

    @Test
    public void testFindConnectionsPerCRO() {
        SynchronisationConnection synchronisationConnection = new SynchronisationConnection();
        synchronisationConnection.setId(1l);
        synchronisationConnection.setCustomer(true);
        synchronisationConnection.setEntityAddress("agr.usef-example.com");

        CommonReferenceOperator commonReferenceOperator = new CommonReferenceOperator();
        commonReferenceOperator.setId(2l);
        commonReferenceOperator.setDomain("cro.usef-example.com");

        Mockito.when(synchronisationConnectionRepository.findAll()).thenReturn(Arrays.asList(synchronisationConnection));
        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(Arrays.asList(commonReferenceOperator));

        Map<String, List<SynchronisationConnection>> connectionsPerCRO = planboardService.findConnectionsPerCRO();

        Assert.assertEquals(1, connectionsPerCRO.size());
        Assert.assertEquals(1l, connectionsPerCRO.get("cro.usef-example.com").get(0).getId().longValue());
    }

    @Test
    public void testChangeStatusOfPtuFlexOrder() {
        planboardService.changeStatusOfPtuFlexOrder(new PtuFlexOrder(), AcknowledgementStatus.PROCESSED);

        Mockito.verify(ptuFlexOrderRepository, Mockito.times(1)).persist(Matchers.any(PtuFlexOrder.class));
    }

    @Test
    public void testFindAll() {
        List<CommonReferenceOperator> commonReferenceOperators = planboardService.findAll();

        Assert.assertNotNull(commonReferenceOperators);
        Mockito.verify(commonReferenceOperatorRepository, Mockito.times(1)).findAll();
    }

    @Test
    public void testFindRejectedPlanboardMessages() {
        List<PlanboardMessage> rejectedPlanboardMessages = planboardService
                .findRejectedPlanboardMessages(DocumentType.A_PLAN, new LocalDate("2015-01-01"));

        Assert.assertNotNull(rejectedPlanboardMessages);
        Mockito.verify(planboardMessageRepository, Mockito.times(1))
                .findRejectedPlanboardMessages(Matchers.eq(DocumentType.A_PLAN), Matchers.eq(new LocalDate("2015-01-01")));
    }
}
