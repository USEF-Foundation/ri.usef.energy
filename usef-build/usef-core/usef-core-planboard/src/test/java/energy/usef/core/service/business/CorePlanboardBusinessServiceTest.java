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

package energy.usef.core.service.business;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.data.xml.bean.message.DispositionAvailableRequested;
import energy.usef.core.data.xml.bean.message.FlexOffer;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.data.xml.bean.message.FlexRequest;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.PrognosisType;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PhaseType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.model.PtuFlexRequest;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.model.PtuState;
import energy.usef.core.model.RegimeType;
import energy.usef.core.repository.AgrConnectionGroupRepository;
import energy.usef.core.repository.BrpConnectionGroupRepository;
import energy.usef.core.repository.CongestionPointConnectionGroupRepository;
import energy.usef.core.repository.ConnectionGroupRepository;
import energy.usef.core.repository.ConnectionGroupStateRepository;
import energy.usef.core.repository.ConnectionRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuFlexOfferRepository;
import energy.usef.core.repository.PtuFlexOrderRepository;
import energy.usef.core.repository.PtuFlexRequestRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.repository.PtuStateRepository;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.util.XMLUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import energy.usef.core.model.FlexOrderSettlement;

@RunWith(PowerMockRunner.class)
public class CorePlanboardBusinessServiceTest {

    public static final String TEST_DOMAIN = "test.com";
    public static final String ANOTHER_DOMAIN = "test2.com";
    private static final LocalDate PERIOD = new LocalDate("2014-10-10");
    private static final LocalDate ANOTHER_PERIOD = new LocalDate("2014-10-11");
    private static final String CONGESTION_POINT = "ea.23472834723849023";
    @Mock
    private PlanboardMessageRepository planboardMessageRepository;
    @Mock
    private PtuContainerRepository ptuContainerRepository;
    @Mock
    private PtuPrognosisRepository ptuPrognosisRepository;
    @Mock
    private PtuFlexRequestRepository ptuFlexRequestRepository;
    @Mock
    private PtuFlexOfferRepository ptuFlexOfferRepository;
    @Mock
    private PtuFlexOrderRepository ptuFlexOrderRepository;
    @Mock
    private PtuStateRepository ptuStateRepository;

    @Mock
    private ConnectionGroupRepository connectionGroupRepository;
    @Mock
    private CongestionPointConnectionGroupRepository congestionPointConnectionGroupRepository;
    @Mock
    private BrpConnectionGroupRepository brpConnectionGroupRepository;
    @Mock
    private AgrConnectionGroupRepository agrConnectionGroupRepository;
    @Mock
    private ConnectionGroupStateRepository connectionGroupStateRepository;
    @Mock
    private ConnectionRepository connectionRepository;
    @Mock
    private Config config;

    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Before
    public void init() {
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        corePlanboardBusinessService = new CorePlanboardBusinessService();
        Whitebox.setInternalState(corePlanboardBusinessService, sequenceGeneratorService);
        Whitebox.setInternalState(corePlanboardBusinessService, ptuContainerRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, ptuFlexOfferRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, ptuFlexOrderRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, ptuPrognosisRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, ptuFlexRequestRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, planboardMessageRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, connectionGroupRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, brpConnectionGroupRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, agrConnectionGroupRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, congestionPointConnectionGroupRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, connectionGroupStateRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, connectionRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, ptuStateRepository);
        Whitebox.setInternalState(corePlanboardBusinessService, config);

        PowerMockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.PTU_DURATION))).thenReturn(15);
        PowerMockito.when(ptuContainerRepository.findPtuContainersMap(Matchers.any(LocalDate.class)))
                .then(invocation -> IntStream.rangeClosed(1, 96).mapToObj(index -> {
                    PtuContainer ptu = new PtuContainer();
                    ptu.setPtuIndex(index);
                    ptu.setPtuDate((LocalDate) invocation.getArguments()[0]);
                    return ptu;
                }).collect(Collectors.toMap(PtuContainer::getPtuIndex, Function.identity())));
    }

    @Test
    public void testStoreFlexRequest() {
        FlexRequest flexRequest = new FlexRequest();
        PTU ptu1 = buildPTU(1);
        PTU ptu2 = buildPTU(2);
        flexRequest.getPTU().add(ptu1);
        flexRequest.getPTU().add(ptu2);

        corePlanboardBusinessService.storeFlexRequest(Mockito.anyString(), flexRequest, DocumentStatus.SENT, "usef-example.com");
        Mockito.verify(planboardMessageRepository, Mockito.times(1)).persist(Matchers.any(PlanboardMessage.class));
        Mockito.verify(ptuFlexRequestRepository, Mockito.times(2)).persist(Matchers.any(PtuFlexRequest.class));
    }

    @Test
    public void testStoreFlexOffer() {
        FlexOffer flexOffer = buildFlexOffer();
        PTU ptu1 = buildPTU(1);
        PTU ptu2 = buildPTU(2);
        flexOffer.getPTU().add(ptu1);
        flexOffer.getPTU().add(ptu2);

        corePlanboardBusinessService.storeFlexOffer(flexOffer.getCongestionPoint(), flexOffer, DocumentStatus.SENT,
                "usef-example.com");

        Mockito.verify(connectionGroupRepository, Mockito.times(1)).find(flexOffer.getCongestionPoint());
        Mockito.verify(ptuContainerRepository, Mockito.times(1)).findPtuContainersMap(Matchers.eq(PERIOD));

        Mockito.verify(ptuFlexOfferRepository, Mockito.times(2)).persist(Matchers.any(PtuFlexOffer.class));
    }

    @Test
    public void testStoreFlexOrder() {
        FlexOrder flexOrder = buildFlexOrder();
        PTU ptu1 = buildPTU(1);
        PTU ptu2 = buildPTU(2);
        flexOrder.getPTU().add(ptu1);
        flexOrder.getPTU().add(ptu2);

        PtuState ptuState = new PtuState();

        Mockito.when(ptuStateRepository.findOrCreatePtuState(Matchers.any(PtuContainer.class), Matchers.any(ConnectionGroup.class)))
                .thenReturn(ptuState);

        corePlanboardBusinessService.storeFlexOrder(flexOrder.getCongestionPoint(), flexOrder, DocumentStatus.SENT,
                "usef-example.com", AcknowledgementStatus.SENT, PtuContainerState.PlanValidate);
        Mockito.verify(connectionGroupRepository, Mockito.times(1)).find(flexOrder.getCongestionPoint());
        Mockito.verify(ptuContainerRepository, Mockito.times(1)).findPtuContainersMap(Matchers.eq(PERIOD));

        Mockito.verify(ptuFlexOrderRepository, Mockito.times(2)).persist(Matchers.any(PtuFlexOrder.class));

        Assert.assertEquals(PtuContainerState.PlanValidate, ptuState.getState());
    }

    @Test
    public void testStorePrognosis() {
        Prognosis prognosis = buildPrognosis();
        PTU ptu1 = buildPTU(1);
        PTU ptu2 = buildPTU(2);
        prognosis.getPTU().add(ptu1);
        prognosis.getPTU().add(ptu2);

        CongestionPointConnectionGroup cpcg = new CongestionPointConnectionGroup();
        cpcg.setUsefIdentifier(CONGESTION_POINT);
        Mockito.when(connectionGroupRepository.find(Matchers.any(String.class))).thenReturn(cpcg);

        corePlanboardBusinessService.storePrognosis(Mockito.anyString(), prognosis, DocumentType.D_PROGNOSIS, DocumentStatus.SENT,
                "usef-example.com", null, false);

        Mockito.verify(ptuContainerRepository, Mockito.times(1)).findPtuContainersMap(Matchers.eq(PERIOD));

        Mockito.verify(ptuPrognosisRepository, Mockito.times(2)).persist(Matchers.any(PtuPrognosis.class));
    }

    @Test
    public void testStorePtuState() {
        PtuState entity = new PtuState();
        corePlanboardBusinessService.storePtuState(entity);
        Mockito.verify(ptuStateRepository, Mockito.times(1)).persist(Matchers.eq(entity));
    }

    @Test
    public void testStorePlanboardMessage() {
        PlanboardMessage entity = new PlanboardMessage();
        corePlanboardBusinessService.storePlanboardMessage(entity);
        Mockito.verify(planboardMessageRepository, Mockito.times(1)).persist(Matchers.eq(entity));
    }

    @Test
    public void testStorePrognosisWithAplan() {
        // given
        Random random = new Random();
        Prognosis prognosis = new Prognosis();
        prognosis.getPTU().addAll(IntStream.rangeClosed(1, 96).mapToObj(elem -> {
            PTU ptuDto = new PTU();
            ptuDto.setPower(BigInteger.valueOf(random.nextInt(500)));
            ptuDto.setStart(BigInteger.valueOf(elem));
            return ptuDto;
        }).collect(Collectors.toList()));
        prognosis.setPeriod(new LocalDate());
        prognosis.setSequence(random.nextLong());
        prognosis.setType(PrognosisType.A_PLAN);

        PowerMockito.when(connectionGroupRepository.find(Matchers.anyString()))
                .thenReturn(PowerMockito.mock(ConnectionGroup.class));
        // when
        corePlanboardBusinessService.storePrognosis("agr.usef-example.com", prognosis, DocumentType.A_PLAN,
                DocumentStatus.PROCESSED, "agr.usef-example.com", null, false);

        // then
        Mockito.verify(ptuPrognosisRepository, Mockito.times(96)).persist(Matchers.any(PtuPrognosis.class));
    }

    @Test
    public void testStorePrognosisTest() {
        Prognosis prognosis = buildPrognosis();
        PTU ptu1 = buildPTU(1);
        PTU ptu2 = buildPTU(2);
        prognosis.getPTU().add(ptu1);
        prognosis.getPTU().add(ptu2);

        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(CONGESTION_POINT);

        corePlanboardBusinessService.storePrognosis(prognosis, connectionGroup, DocumentType.D_PROGNOSIS, DocumentStatus.SENT,
                "usef-example.com", null, false);
        // verify the call to repositories.
        Mockito.verify(ptuContainerRepository, Mockito.times(1)).findPtuContainersMap(Matchers.eq(PERIOD));
        Mockito.verify(ptuPrognosisRepository, Mockito.times(2)).persist(Matchers.any(PtuPrognosis.class));
    }

    @Test
    public void testFindPlanboardMessages() {
        Long sequence = 12345L;
        DocumentType documentType = DocumentType.FLEX_OFFER;
        String participantDomain = "usef-example.com";

        corePlanboardBusinessService.findPlanboardMessages(sequence, documentType, participantDomain);

        Mockito.verify(planboardMessageRepository, Mockito.times(1))
                .findPlanboardMessages(sequence, documentType, participantDomain);
    }

    @Test
    public void testProcessDayAheadClosureEvent() {
        corePlanboardBusinessService.processDayAheadClosureEvent(new LocalDate(2015, 1, 19));
        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .updatePtuContainersState(Matchers.eq(PtuContainerState.DayAheadClosedValidate),
                        Matchers.eq(new LocalDate(2015, 1, 19)), Matchers.isNull(Integer.class));
    }

    @Test
    public void testProcessBackToPlanEvent() {
        LocalDate period = new LocalDate(2015, 1, 19);
        List<PtuContainer> ptuContainers = new ArrayList<>();
        PowerMockito.when(ptuContainerRepository.findPtuContainers(period, PhaseType.Operate, PhaseType.Settlement))
                .thenReturn(ptuContainers);

        corePlanboardBusinessService.processBackToPlanEvent(period);
        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .updatePtuContainersPhase(Matchers.eq(PhaseType.Plan), Matchers.eq(period), Matchers.isNull(Integer.class));
    }

    @Test
    public void testProcessBackToPlanEventWhenInOperatePhase() {
        LocalDate period = new LocalDate(2015, 1, 19);
        List<PtuContainer> ptuContainers = new ArrayList<>();
        ptuContainers.add(new PtuContainer());

        PowerMockito.when(ptuContainerRepository.findPtuContainers(period, PhaseType.Operate, PhaseType.Settlement))
                .thenReturn(ptuContainers);

        corePlanboardBusinessService.processBackToPlanEvent(period);
        Mockito.verify(ptuContainerRepository, Mockito.times(0))
                .updatePtuContainersPhase(Matchers.eq(PhaseType.Plan), Matchers.eq(period), Matchers.isNull(Integer.class));
    }

    @Test
    public void testProcessMoveToValidateEvent() {
        LocalDate period = new LocalDate(2015, 1, 19);

        List<PlanboardMessage> planboardMessages = new ArrayList<>();
        PlanboardMessage planboardMessage1 = new PlanboardMessage();
        planboardMessage1.setDocumentStatus(DocumentStatus.ACCEPTED);
        planboardMessages.add(planboardMessage1);

        List<BrpConnectionGroup> brpConnectionGroups = new ArrayList<>();
        BrpConnectionGroup brpConnectionGroup = new BrpConnectionGroup();
        brpConnectionGroups.add(brpConnectionGroup);

        PowerMockito.when(planboardMessageRepository.findLastAPlanPlanboardMessages(period)).thenReturn(planboardMessages);
        PowerMockito.when(brpConnectionGroupRepository.findActiveBrpConnectionGroups(period))
                .thenReturn(brpConnectionGroups);

        corePlanboardBusinessService.processMoveToValidateEvent(period);

        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .updatePtuContainersPhase(Matchers.eq(PhaseType.Validate), Matchers.eq(period), Matchers.isNull(Integer.class));
    }

    @Test
    public void testProcessIntraDayClosureEvent() {
        corePlanboardBusinessService.processIntraDayClosureEvent(new LocalDate(2015, 1, 19), 25);
        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .updatePtuContainersState(Matchers.eq(PtuContainerState.IntraDayClosedValidate),
                        Matchers.eq(new LocalDate(2015, 1, 19)), Matchers.eq(25));
    }

    @Test
    public void testMoveToOperateEvent() {
        corePlanboardBusinessService.processMoveToOperateEvent(new LocalDate(2015, 1, 19), 25);
        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .updatePtuContainersState(Matchers.eq(PtuContainerState.Operate), Matchers.eq(new LocalDate(2015, 1, 19)),
                        Matchers.eq(25));
        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .updatePtuContainersState(Matchers.eq(PtuContainerState.PendingSettlement), Matchers.eq(new LocalDate(2015, 1, 19)),
                        Matchers.eq(24));
    }

    @Test
    public void testMoveToOperateEventWithDayShift() {
        corePlanboardBusinessService.processMoveToOperateEvent(new LocalDate(2015, 1, 19), 1);
        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .updatePtuContainersState(Matchers.eq(PtuContainerState.Operate), Matchers.eq(new LocalDate(2015, 1, 19)),
                        Matchers.eq(1));
        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .updatePtuContainersPhase(Matchers.eq(PhaseType.Operate), Matchers.eq(new LocalDate(2015, 1, 19)), Matchers.eq(1));
        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .updatePtuContainersState(Matchers.eq(PtuContainerState.PendingSettlement), Matchers.eq(new LocalDate(2015, 1, 18)),
                        Matchers.eq(96));
        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .updatePtuContainersPhase(Matchers.eq(PhaseType.Settlement), Matchers.eq(new LocalDate(2015, 1, 18)),
                        Matchers.eq(96));
    }

    @Test
    public void testStoreSettlementPlanboardMessageWithNull() {
        corePlanboardBusinessService.storeFlexOrderSettlementsPlanboardMessage(null, 4, DocumentStatus.SENT, null, null);
        Mockito.verify(planboardMessageRepository, Mockito.times(0)).persist(Matchers.any(PlanboardMessage.class));
    }

    @Test
    public void testStoreSettlementPlanboardMessage() {
        FlexOrderSettlement flexOrderSettlement = new FlexOrderSettlement();
        PlanboardMessage flexOrder = new PlanboardMessage();
        flexOrder.setSequence(1L);
        flexOrderSettlement.setFlexOrder(flexOrder);
        corePlanboardBusinessService.storeFlexOrderSettlementsPlanboardMessage(Collections.singletonList(flexOrderSettlement), 4,
                DocumentStatus.SENT, null, null);
        Mockito.verify(planboardMessageRepository, Mockito.times(1)).persist(Matchers.any(PlanboardMessage.class));
    }

    @Test
    public void testArchiveAPlan() {
        List<PlanboardMessage> messages = new ArrayList<>();
        messages.add(createPlanboardMessage(TEST_DOMAIN, DocumentStatus.ACCEPTED, PERIOD));
        messages.add(createPlanboardMessage(TEST_DOMAIN, DocumentStatus.PENDING_FLEX_TRADING, PERIOD));

        Mockito.when(planboardMessageRepository.findPlanboardMessages(DocumentType.A_PLAN, PERIOD, null)).thenReturn(messages);
        corePlanboardBusinessService.archiveAPlans(TEST_DOMAIN, PERIOD);

        messages.stream().forEach(a -> Assert.assertEquals(DocumentStatus.ARCHIVED, a.getDocumentStatus()));
    }

    @Test
    public void testArchiveAPlanWrongStatus() {
        List<PlanboardMessage> messages = new ArrayList<>();
        messages.add(createPlanboardMessage(TEST_DOMAIN, DocumentStatus.RECEIVED, PERIOD));
        messages.add(createPlanboardMessage(TEST_DOMAIN, DocumentStatus.PROCESSED, PERIOD));
        messages.add(createPlanboardMessage(TEST_DOMAIN, DocumentStatus.FINAL, PERIOD));

        Mockito.when(planboardMessageRepository.findPlanboardMessages(DocumentType.A_PLAN, PERIOD, null)).thenReturn(messages);
        corePlanboardBusinessService.archiveAPlans(TEST_DOMAIN, PERIOD);

        messages.stream().forEach(a -> Assert.assertNotEquals(DocumentStatus.ARCHIVED, a.getDocumentStatus()));
    }

    @Test
    public void testArchiveAPlanDifferentAggregator() {
        List<PlanboardMessage> messages = new ArrayList<>();
        messages.add(createPlanboardMessage(TEST_DOMAIN, DocumentStatus.ACCEPTED, PERIOD));
        messages.add(createPlanboardMessage(TEST_DOMAIN, DocumentStatus.PENDING_FLEX_TRADING, PERIOD));

        Mockito.when(planboardMessageRepository.findPlanboardMessages(DocumentType.A_PLAN, PERIOD, null)).thenReturn(messages);
        corePlanboardBusinessService.archiveAPlans(ANOTHER_DOMAIN, PERIOD);

        messages.stream().forEach(a -> Assert.assertNotEquals(DocumentStatus.ARCHIVED, a.getDocumentStatus()));
    }

    @Test
    public void testArchiveAPlanDifferentPeriod() {
        List<PlanboardMessage> messages = new ArrayList<>();
        messages.add(createPlanboardMessage(TEST_DOMAIN, DocumentStatus.ACCEPTED, PERIOD));
        messages.add(createPlanboardMessage(TEST_DOMAIN, DocumentStatus.PENDING_FLEX_TRADING, PERIOD));

        Mockito.when(planboardMessageRepository.findPlanboardMessages(DocumentType.A_PLAN, PERIOD, null)).thenReturn(messages);
        corePlanboardBusinessService.archiveAPlans(TEST_DOMAIN, ANOTHER_PERIOD);

        messages.stream().forEach(a -> Assert.assertNotEquals(DocumentStatus.ARCHIVED, a.getDocumentStatus()));
    }

    @Test
    public void testUpdateDPrognosisStatus() {
        Long prognosisSequence = 123l;
        String participantDomain = TEST_DOMAIN;
        List<PlanboardMessage> messages = new ArrayList<>();
        messages.add(new PlanboardMessage());

        Mockito.when(
                planboardMessageRepository.findPlanboardMessages(prognosisSequence, DocumentType.D_PROGNOSIS, participantDomain))
                .thenReturn(messages);

        int updatedCount = corePlanboardBusinessService.updatePrognosisStatus(prognosisSequence, participantDomain,
                DocumentType.D_PROGNOSIS, DocumentStatus.ACCEPTED);

        Assert.assertEquals(1, updatedCount);

    }

    @Test
    public void testFindConnectionGroupWithConnectionsWithOverlappingValidity() {
        LocalDate startDate = DateTimeUtil.parseDate("2015-07-01");
        LocalDate endDate = DateTimeUtil.parseDate("2015-07-31");
        // mocking
        PowerMockito.when(connectionGroupStateRepository.findConnectionGroupStatesWithOverlappingValidity(startDate, endDate))
                .thenReturn(buildConnectionGroupStates(startDate));
        // invocation
        Map<LocalDate, Map<ConnectionGroup, List<Connection>>> connectionGroupsToConnections =
                corePlanboardBusinessService.findConnectionGroupWithConnectionsWithOverlappingValidity(startDate, endDate);

        // verifications
        Assert.assertNotNull(connectionGroupsToConnections);
        Assert.assertEquals(31, connectionGroupsToConnections.keySet().size());
        Assert.assertEquals(1, connectionGroupsToConnections.get(startDate).keySet().size());
        Assert.assertEquals(1, connectionGroupsToConnections.get(startDate.plusDays(1)).keySet().size());
        Assert.assertEquals(2,
                connectionGroupsToConnections.get(startDate).get(new AgrConnectionGroup("agr1.usef-example.com")).size());
        Assert.assertEquals(2,
                connectionGroupsToConnections.get(startDate.plusDays(1)).get(new AgrConnectionGroup("agr2.usef-example.com"))
                        .size());
    }

    @Test
    public void testFinaliizeRejectedAPlan() {

        List<PlanboardMessage> messages = new ArrayList<>();
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentType(DocumentType.A_PLAN);
        planboardMessage.setPeriod(PERIOD);
        planboardMessage.setDocumentStatus(DocumentStatus.REJECTED);
        planboardMessage.setParticipantDomain(TEST_DOMAIN);
        messages.add(planboardMessage);

        List<PtuState> ptuStates = new ArrayList<>();
        PtuState ptuState = new PtuState();
        ptuState.setRegime(RegimeType.GREEN);
        ptuState.setState(PtuContainerState.PlanValidate);
        ptuStates.add(ptuState);

        Mockito.when(planboardMessageRepository.findPlanboardMessages(DocumentType.A_PLAN, PERIOD, null)).thenReturn(messages);

        Mockito.when(ptuStateRepository.findPtuStates(planboardMessage.getPeriod(), planboardMessage.getParticipantDomain()))
                .thenReturn(ptuStates);

        corePlanboardBusinessService.finalizeAPlans(PERIOD);

        Assert.assertEquals(PtuContainerState.PlanValidate, ptuState.getState());

    }

    @Test
    public void testFinalizeSentAPlan() {

        List<PlanboardMessage> messages = new ArrayList<>();
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentType(DocumentType.A_PLAN);
        planboardMessage.setDocumentStatus(DocumentStatus.SENT);
        planboardMessage.setPeriod(PERIOD);
        planboardMessage.setParticipantDomain(TEST_DOMAIN);
        messages.add(planboardMessage);

        List<PtuState> ptuStates = new ArrayList<>();
        PtuState ptuState = new PtuState();
        ptuState.setRegime(RegimeType.GREEN);
        ptuState.setState(PtuContainerState.PlanValidate);
        ptuStates.add(ptuState);

        Mockito.when(planboardMessageRepository.findPlanboardMessages(DocumentType.A_PLAN, PERIOD, null)).thenReturn(messages);

        Mockito.when(ptuStateRepository.findPtuStates(planboardMessage.getPeriod(), planboardMessage.getParticipantDomain()))
                .thenReturn(ptuStates);

        corePlanboardBusinessService.finalizeAPlans(PERIOD);

        Assert.assertEquals(PtuContainerState.DayAheadClosedValidate, ptuState.getState());

    }

    @Test
    public void testFindOrCreatePtuContainersForPeriodCreatesThemWhenNotFound() {
        Integer ptuDuration = 240;
        Integer ptusPerDay = 6;
        PowerMockito.when(ptuContainerRepository.findPtuContainersMap(Matchers.eq(PERIOD))).thenReturn(new HashMap<>());
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(ptuDuration);
        corePlanboardBusinessService.findOrCreatePtuContainersForPeriod(PERIOD);
        Mockito.verify(ptuContainerRepository, Mockito.times(1)).findPtuContainersMap(Matchers.eq(PERIOD));
        Mockito.verify(ptuContainerRepository, Mockito.times(ptusPerDay)).persist(Matchers.any(PtuContainer.class));
    }

    @Test
    public void testFindOrCreatePtuContainersForPeriodReturnsThemWhenFound() {
        PowerMockito.when(ptuContainerRepository.findPtuContainersMap(Matchers.eq(PERIOD)))
                .thenReturn(Collections.singletonMap(1, new PtuContainer()));
        List<PtuContainer> existingPtuContainers = corePlanboardBusinessService.findOrCreatePtuContainersForPeriod(PERIOD);
        Mockito.verify(ptuContainerRepository, Mockito.times(1)).findPtuContainersMap(Matchers.eq(PERIOD));
        Mockito.verify(ptuContainerRepository, Mockito.times(0)).persist(Matchers.any(PtuContainer.class));
        Assert.assertEquals(1, existingPtuContainers.size());
    }

    @Test
    public void testFindFlexOfferSucceeds() {
        corePlanboardBusinessService.findPtuFlexOffer(1l, "usef-example.com");
        Mockito.verify(ptuFlexOfferRepository, Mockito.times(1)).findPtuFlexOffer(Matchers.eq(1l), Matchers.eq("usef-example.com"));
    }

    @Test
    public void testFindConnectionGroupWithEmptyConnectionListQueriesAll() {
        List<String> connectionAddresses = new ArrayList<>();
        LocalDate today = DateTimeUtil.getCurrentDate();
        corePlanboardBusinessService.findConnectionGroupsWithConnections(connectionAddresses, today);
        Mockito.verify(connectionGroupRepository, Mockito.times(1)).findAllForDateTime(Matchers.eq(today));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStoreCongestionPoints() throws IOException {
        StringWriter xmlWriter = new StringWriter();
        String commonReferenceQueryResponseFile = "energy/usef/core/service/business/common_reference_query_response_dso.xml";
        IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream(commonReferenceQueryResponseFile),
                xmlWriter);
        CommonReferenceQueryResponse message = XMLUtil.xmlToMessage(xmlWriter.toString(), CommonReferenceQueryResponse.class);

        ConnectionGroup cg = new CongestionPointConnectionGroup();
        cg.setUsefIdentifier("ean.1111111111");

        PowerMockito.when(connectionGroupStateRepository.findActiveConnectionGroupStatesOfType(Matchers.any(LocalDate.class),
                Matchers.any(Class.class))).then(invocation -> {
            ConnectionGroup connectionGroup3 = new CongestionPointConnectionGroup();
            connectionGroup3.setUsefIdentifier("ean.3333333333");
            Connection connection = new Connection();
            connection.setEntityAddress("ean.1000000005");
            ConnectionGroupState connectionGroupState = new ConnectionGroupState();
            connectionGroupState.setConnectionGroup(connectionGroup3);
            connectionGroupState.setConnection(connection);
            connectionGroupState.setValidFrom(((LocalDate) invocation.getArguments()[0]).minusDays(1));
            connectionGroupState.setValidUntil(((LocalDate) invocation.getArguments()[0]).plusDays(2));
            return Collections.singletonList(connectionGroupState);
        });
        PowerMockito.when(connectionGroupStateRepository.findEndingConnectionGroupStates(Matchers.any(LocalDate.class),
                Matchers.eq(CongestionPointConnectionGroup.class)))
                .then(invocation -> IntStream.rangeClosed(1, 2).mapToObj(index -> {
                    ConnectionGroupState connectionGroupState = new ConnectionGroupState();
                    connectionGroupState.setValidFrom(((LocalDate) invocation.getArguments()[0]).minusDays(3));
                    connectionGroupState.setValidUntil((LocalDate) invocation.getArguments()[0]);
                    Connection connection = new Connection();
                    connection.setEntityAddress("ean.100000000" + index);
                    connectionGroupState.setConnection(connection);
                    connectionGroupState.setConnectionGroup(cg);
                    return connectionGroupState;
                }).collect(Collectors.toList()));
        PowerMockito.when(connectionRepository.findOrCreate(Matchers.any(String.class))).then(invocation -> {
            Connection connection = new Connection();
            connection.setEntityAddress((String) invocation.getArguments()[0]);
            return connection;
        });
        PowerMockito.when(
                congestionPointConnectionGroupRepository.findOrCreate(Matchers.any(String.class), Matchers.any(String.class)))
                .then(invocation -> {
                    CongestionPointConnectionGroup congestionPointConnectionGroup = new CongestionPointConnectionGroup();
                    congestionPointConnectionGroup.setUsefIdentifier((String) invocation.getArguments()[0]);
                    congestionPointConnectionGroup.setDsoDomain((String) invocation.getArguments()[1]);
                    return congestionPointConnectionGroup;
                });
        PowerMockito.when(connectionGroupRepository.find(Matchers.any(String.class))).thenReturn(null);
        corePlanboardBusinessService.storeCommonReferenceQueryResponse(message, CommonReferenceEntityType.CONGESTION_POINT,
                DateTimeUtil.getCurrentDate().plusDays(7), 3);
        ArgumentCaptor<ConnectionGroupState> connectionGroupStateCaptor = ArgumentCaptor.forClass(ConnectionGroupState.class);
        Mockito.verify(connectionGroupStateRepository, Mockito.times(6)).persist(connectionGroupStateCaptor.capture());

        List<ConnectionGroupState> persistedConnectionGroupStates = connectionGroupStateCaptor.getAllValues();
        Assert.assertEquals(6, persistedConnectionGroupStates.size());
        for (ConnectionGroupState cgs : persistedConnectionGroupStates) {
            Assert.assertNotNull(cgs);
            Assert.assertNotNull(cgs.getConnection());
            Assert.assertNotNull(cgs.getConnectionGroup());
            Assert.assertTrue(cgs.getConnectionGroup() instanceof CongestionPointConnectionGroup);
        }
    }

    @Test
    public void testStoreConnections() throws IOException {
        StringWriter xmlWriter = new StringWriter();
        String commonReferenceQueryResponseFile = "energy/usef/core/service/business/common_reference_query_response.xml";
        IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream(commonReferenceQueryResponseFile),
                xmlWriter);
        CommonReferenceQueryResponse message = XMLUtil.xmlToMessage(xmlWriter.toString(), CommonReferenceQueryResponse.class);

        ConnectionGroup brpConnectionGroup = new BrpConnectionGroup();
        brpConnectionGroup.setUsefIdentifier("brp1.usef-example.com");

        PowerMockito.when(connectionGroupStateRepository.findEndingConnectionGroupStates(Matchers.any(LocalDate.class),
                Matchers.eq(BrpConnectionGroup.class))).then(invocation -> IntStream.rangeClosed(1, 2).mapToObj(index -> {
            Connection connection = new Connection();
            connection.setEntityAddress("ean.10000000000" + index);
            return connection;
        }).map(connection -> {
            ConnectionGroupState connectionGroupState = new ConnectionGroupState();
            connectionGroupState.setConnection(connection);
            connectionGroupState.setConnectionGroup(brpConnectionGroup);
            connectionGroupState.setValidFrom(((LocalDate) invocation.getArguments()[0]).minusDays(3));
            connectionGroupState.setValidUntil((LocalDate) invocation.getArguments()[0]);
            return connectionGroupState;
        }).collect(Collectors.toList()));

        PowerMockito.when(connectionGroupStateRepository.findActiveConnectionGroupStatesOfType(Matchers.any(LocalDate.class),
                Matchers.eq(BrpConnectionGroup.class))).then(invocation -> {
            Connection connection = new Connection();
            connection.setEntityAddress("ean.100000000003");
            ConnectionGroupState connectionGroupState = new ConnectionGroupState();
            connectionGroupState.setConnection(connection);
            connectionGroupState.setConnectionGroup(brpConnectionGroup);
            connectionGroupState.setValidFrom(((LocalDate) invocation.getArguments()[0]).minusDays(1));
            connectionGroupState.setValidFrom(((LocalDate) invocation.getArguments()[0]).plusDays(2));
            return Collections.singletonList(connectionGroupState);
        });

        corePlanboardBusinessService.storeCommonReferenceQueryResponse(message, CommonReferenceEntityType.BRP,
                DateTimeUtil.getCurrentDate().plusDays(7), 3);
        Mockito.verify(connectionGroupStateRepository, Mockito.times(2)).persist(Matchers.any(ConnectionGroupState.class));
    }

    @Test
    public void testFindAcceptedPrognosisMessages() {
        corePlanboardBusinessService.findAcceptedPrognosisMessages(DocumentType.D_PROGNOSIS, new LocalDate("2015-07-02"),
                "brp.usef-example.com");
        Mockito.verify(planboardMessageRepository, Mockito.times(1))
                .findAcceptedPlanboardMessagesForConnectionGroup(Matchers.eq(DocumentType.D_PROGNOSIS),
                        Matchers.eq(new LocalDate("2015-07-02")), Matchers.eq("brp.usef-example.com"));
    }

    @Test
    public void testFindActiveConnectionGroupsWithConnections() {
        final LocalDate period = new LocalDate("2015-07-03");
        corePlanboardBusinessService.findActiveConnectionGroupsWithConnections(period);
        Mockito.verify(connectionGroupStateRepository, Mockito.times(1))
                .findActiveConnectionGroupsWithConnections(Matchers.eq(period), Matchers.eq(period));
    }

    /**
     * Test case for testing CorePlanboardBusinessService.findActiveConnections({@link LocalDateTime}).
     */
    @Test
    public void testFindActiveConnectionsLocalDateTime() {
        List<String> connectionEntityList = new ArrayList<>();

        List<Connection> connectionList = corePlanboardBusinessService
                .findActiveConnections(DateTimeUtil.getCurrentDate(), Optional.of(connectionEntityList));
        Assert.assertNotNull(connectionList);
        Mockito.verify(connectionRepository, Mockito.times(1))
                .findActiveConnections(Matchers.any(LocalDate.class), Matchers.any(Optional.class));
    }

    /**
     * Test case for testing CorePlanboardBusinessService.findConnections({@link LocalDate}, {@link LocalDate}, {@link RegimeType
     * ...}).
     */
    @Test
    public void testFindConnectionsByStartDateEndDateAndRegimes() {
        ConnectionGroup connectionGroup = new BrpConnectionGroup();
        connectionGroup.setUsefIdentifier("brp1.usef-example.com");

        PowerMockito.when(connectionGroupStateRepository.findActiveConnectionGroupStates(Matchers.any(LocalDate.class),
                Matchers.any(LocalDate.class))).then(invocation -> {
            Connection connection = new Connection();
            connection.setEntityAddress("ean.100000000003");
            ConnectionGroupState connectionGroupState = new ConnectionGroupState();
            connectionGroupState.setConnection(connection);
            connectionGroupState.setConnectionGroup(connectionGroup);
            connectionGroupState.setValidFrom(((LocalDate) invocation.getArguments()[0]).minusDays(2));
            connectionGroupState.setValidUntil(((LocalDate) invocation.getArguments()[0]).plusDays(2));
            return Collections.singletonList(connectionGroupState);
        });

        Mockito.when(
                ptuStateRepository.findPtuStates(Matchers.any(LocalDate.class), Matchers.any(LocalDate.class),
                        Matchers.any(RegimeType.class)))
                .then(invocation -> {
                    PtuState ptuState = new PtuState();
                    ptuState.setRegime(RegimeType.ORANGE);
                    PtuContainer ptuContainer = new PtuContainer();
                    ptuContainer.setPtuDate(DateTimeUtil.getCurrentDate());
                    ptuState.setPtuContainer(ptuContainer);
                    ptuState.setConnectionGroup(connectionGroup);
                    return Collections.singletonList(ptuState);
                });

        Map<ConnectionGroup, List<Connection>> connectionsMap = corePlanboardBusinessService.findConnections(
                DateTimeUtil.getCurrentDate().minusDays(1),
                DateTimeUtil.getCurrentDate().plusDays(1), RegimeType.ORANGE);

        Assert.assertEquals(1, connectionsMap.size());

    }

    /**
     * Test case for the CorePlanboardBusinessService.FindActiveConnectionGroupStates({@link LocalDateTime}, {@link Class}, {@link
     * String}) method.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFindActiveConnectionGroupStates() {
        connectionGroupStateRepository.findActiveConnectionGroupStatesOfType(DateTimeUtil.getCurrentDate(),
                CongestionPointConnectionGroup.class);

        Mockito.verify(connectionGroupStateRepository, Mockito.times(1))
                .findActiveConnectionGroupStatesOfType(Matchers.any(LocalDate.class), Matchers.any(Class.class));
    }

    private PTU buildPTU(int i) {
        PTU ptu = new PTU();
        ptu.setStart(BigInteger.valueOf(i));
        ptu.setDuration(BigInteger.ONE);
        ptu.setPower(BigInteger.valueOf(new Random().nextInt(9999)));
        ptu.setPrice(new BigDecimal(BigInteger.valueOf(new Random().nextInt(9999)), 2));
        ptu.setDisposition(DispositionAvailableRequested.REQUESTED);
        return ptu;
    }

    @Test
    public void testFindFlexOrdersForUsefIdentifierOnDate() throws Exception {
        List<PtuFlexOrder> ptuFlexOrderList = new ArrayList<>();
        Mockito.when(
                ptuFlexOrderRepository.findAcceptedFlexOrdersByDateAndUsefIdentifier(Matchers.<Optional<String>>any(),
                        Matchers.any(LocalDate.class)))
                .thenReturn(ptuFlexOrderList);
        corePlanboardBusinessService
                .findAcceptedFlexOrdersForUsefIdentifierOnDate(Matchers.<Optional<String>>any(), Matchers.any(LocalDate.class));
        Mockito.verify(ptuFlexOrderRepository, Mockito.times(1))
                .findAcceptedFlexOrdersByDateAndUsefIdentifier(Matchers.<Optional<String>>any(), Matchers.any(LocalDate.class));
    }

    @Test
    public void testFindPtuPrognosisForFlexOfferSequences() throws Exception {
        List<Long> sequences = new ArrayList<>();
        Mockito.when(ptuFlexRequestRepository
                .findPtuPrognosisSequenceByFlexOfferSequence(Matchers.any(Long.class), Matchers.anyString()))
                .thenReturn(1L);
        List<PtuPrognosis> ptuPrognosis = new ArrayList<>();
        Mockito.when(ptuPrognosisRepository.findPtuPrognosisForSequence(Matchers.any(Long.class),
                Matchers.anyString()))
                .thenReturn(ptuPrognosis);
        corePlanboardBusinessService.findPtuPrognosisForPtuFlexOfferSequence(Matchers.any(Long.class), Matchers.anyString());
        Mockito.verify(ptuFlexRequestRepository, Mockito.times(1))
                .findPtuPrognosisSequenceByFlexOfferSequence(Matchers.any(Long.class), Matchers.anyString());
        Mockito.verify(ptuPrognosisRepository, Mockito.times(1)).findPtuPrognosisForSequence(Matchers.any(Long.class),
                Matchers.anyString());
    }

    @Test
    public void testFindPrognosisMessagesForDate() {
        List<PlanboardMessage> planboardMessageList = new ArrayList<>();
        Mockito.when(
                planboardMessageRepository
                        .findPrognosisRelevantForDate(Matchers.any(LocalDate.class), Matchers.anyString()))
                .thenReturn(planboardMessageList);
        corePlanboardBusinessService.findPrognosismessagesForDate(Matchers.any(LocalDate.class), Matchers.anyString());
        Mockito.verify(planboardMessageRepository, Mockito.times(1))
                .findPrognosisRelevantForDate(Matchers.any(LocalDate.class), Matchers.anyString());
    }

    @Test
    public void testFindConnectionsWithConnectionGroups() {
        Mockito.when(connectionGroupStateRepository
                .findConnectionsWithConnectionGroups(Matchers.anyListOf(String.class), Matchers.any(LocalDate.class)))
                .thenReturn(new HashMap<>());
        Map<ConnectionGroup, List<Connection>> connectionsWithConnectionGroups = corePlanboardBusinessService
                .findConnectionsWithConnectionGroups(Collections.singletonList("ean.000000000001"),
                        DateTimeUtil.parseDate("2015-06-24"));
        Mockito.verify(connectionGroupStateRepository, Mockito.times(1)).findConnectionsWithConnectionGroups(
                Matchers.anyListOf(String.class), Matchers.eq(DateTimeUtil.parseDate("2015-06-24")));
        Assert.assertNotNull(connectionsWithConnectionGroups);
    }

    @Test
    public void testFindInitializedDaysOfPlanboard() {
        corePlanboardBusinessService.findInitializedDaysOfPlanboard();
        Mockito.verify(ptuContainerRepository, Mockito.times(1)).findInitializedDaysOfPlanboard();
    }

    @Test
    public void testFindFlexOffersWithOrderInPeriod() {
        // variables and mocking
        final LocalDate period = new LocalDate(2015, 2, 2);
        PowerMockito.when(ptuFlexOfferRepository.findFlexOffersWithOrderInPeriod(Matchers.any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new PtuFlexOffer()));
        // invocation
        List<PtuFlexOffer> flexOffersWithOrderInPeriod = corePlanboardBusinessService.findFlexOffersWithOrderInPeriod(period);
        // verifications
        Assert.assertEquals(1, flexOffersWithOrderInPeriod.size());
        Mockito.verify(ptuFlexOfferRepository, Mockito.times(1)).findFlexOffersWithOrderInPeriod(Matchers.eq(period));
    }

    @Test
    public void testFindPlacedFlexOffers() {
        // variables and mocking
        final LocalDate period = new LocalDate(2015, 11, 18);
        PowerMockito.when(ptuFlexOfferRepository.findPlacedFlexOffers(Matchers.any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new PtuFlexOffer()));
        // actual invocation
        List<PtuFlexOffer> placedFlexOffers = corePlanboardBusinessService.findPlacedFlexOffers(period);
        // assertions and verifications
        Assert.assertEquals("Size of the result is not correct. Business service should not modify the size of the result.", 1,
                placedFlexOffers.size());
        Mockito.verify(ptuFlexOfferRepository, Mockito.times(1)).findPlacedFlexOffers(Matchers.eq(period));
    }

    @Test
    public void testFindPrognosesWithOrderInPeriod() {
        // variables and mocking
        final LocalDate period = new LocalDate(2015, 2, 2);
        PowerMockito.when(ptuPrognosisRepository.findPrognosesWithOrderInPeriod(Matchers.any(LocalDate.class)))
                .thenReturn(Collections.singletonList(new PtuPrognosis()));
        // invocation
        List<PtuPrognosis> prognosesWithOrderInPeriod = corePlanboardBusinessService.findPrognosesWithOrderInPeriod(period);
        // verifications
        Assert.assertEquals(1, prognosesWithOrderInPeriod.size());
        Mockito.verify(ptuPrognosisRepository, Mockito.times(1)).findPrognosesWithOrderInPeriod(Matchers.eq(period));
    }

    @Test
    public void testFindLastPrognosesLocalDatePrognosisTypeStringDocumentStatus() throws Exception {
        final LocalDate period = new LocalDate(2015, 1, 1);
        final String usefIdentifier = "brp1.usef-example.com";

        Mockito.when(ptuPrognosisRepository.findLastPrognoses(Matchers.any(LocalDate.class),
                Matchers.eq(Optional.of(energy.usef.core.model.PrognosisType.A_PLAN)), Matchers.any(Optional.class),
                Matchers.eq(Optional.of(DocumentStatus.ACCEPTED)))).then(call -> IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuPrognosis ptuPrognosis = new PtuPrognosis();
            PtuContainer ptuContainer = new PtuContainer((LocalDate) call.getArguments()[0], index);
            ptuPrognosis.setType(energy.usef.core.model.PrognosisType.A_PLAN);
            ptuPrognosis.setConnectionGroup(new BrpConnectionGroup(usefIdentifier));
            ptuPrognosis.setPtuContainer(ptuContainer);
            return ptuPrognosis;
        }).collect(Collectors.toList()));

        // invocation
        List<PtuPrognosis> lastPrognoses = corePlanboardBusinessService
                .findLastPrognoses(period, energy.usef.core.model.PrognosisType.A_PLAN, usefIdentifier, DocumentStatus.ACCEPTED);
        // assertions
        Assert.assertNotNull(lastPrognoses);
        Assert.assertEquals(96, lastPrognoses.size());
        Mockito.verify(ptuPrognosisRepository, Mockito.times(1))
                .findLastPrognoses(Matchers.eq(period), Matchers.eq(Optional.of(energy.usef.core.model.PrognosisType.A_PLAN)),
                        Matchers.eq(Optional.of(usefIdentifier)), Matchers.eq(Optional.of(DocumentStatus.ACCEPTED)));

    }

    @Test
    public void testFindLastPrognosesLocalDatePrognosisTypeString() throws Exception {
        final LocalDate period = new LocalDate(2015, 1, 1);
        final String usefIdentifier = "brp1.usef-example.com";

        Mockito.when(ptuPrognosisRepository.findLastPrognoses(Matchers.any(LocalDate.class),
                Matchers.eq(Optional.of(energy.usef.core.model.PrognosisType.A_PLAN)), Matchers.any(Optional.class),
                Matchers.eq(Optional.empty()))).then(call -> IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuPrognosis ptuPrognosis = new PtuPrognosis();
            PtuContainer ptuContainer = new PtuContainer((LocalDate) call.getArguments()[0], index);
            ptuPrognosis.setType(energy.usef.core.model.PrognosisType.A_PLAN);
            ptuPrognosis.setConnectionGroup(new BrpConnectionGroup(usefIdentifier));
            ptuPrognosis.setPtuContainer(ptuContainer);
            return ptuPrognosis;
        }).collect(Collectors.toList()));

        // invocation
        List<PtuPrognosis> lastPrognoses = corePlanboardBusinessService
                .findLastPrognoses(period, energy.usef.core.model.PrognosisType.A_PLAN, usefIdentifier);
        // assertions
        Assert.assertNotNull(lastPrognoses);
        Assert.assertEquals(96, lastPrognoses.size());
        Mockito.verify(ptuPrognosisRepository, Mockito.times(1))
                .findLastPrognoses(Matchers.eq(period), Matchers.eq(Optional.of(energy.usef.core.model.PrognosisType.A_PLAN)),
                        Matchers.eq(Optional.of(usefIdentifier)), Matchers.eq(Optional.empty()));
    }

    @Test
    public void testFindLastPrognosesLocalDatePrognosisType() throws Exception {
        final LocalDate period = new LocalDate(2015, 1, 1);
        final String usefIdentifier = "brp1.usef-example.com";

        Mockito.when(ptuPrognosisRepository.findLastPrognoses(Matchers.any(LocalDate.class),
                Matchers.eq(Optional.of(energy.usef.core.model.PrognosisType.A_PLAN)), Matchers.eq(Optional.empty()),
                Matchers.eq(Optional.empty()))).then(call -> IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuPrognosis ptuPrognosis = new PtuPrognosis();
            PtuContainer ptuContainer = new PtuContainer((LocalDate) call.getArguments()[0], index);
            ptuPrognosis.setType(energy.usef.core.model.PrognosisType.A_PLAN);
            ptuPrognosis.setConnectionGroup(new BrpConnectionGroup(usefIdentifier));
            ptuPrognosis.setPtuContainer(ptuContainer);
            return ptuPrognosis;
        }).collect(Collectors.toList()));

        // invocation
        List<PtuPrognosis> lastPrognoses = corePlanboardBusinessService
                .findLastPrognoses(period, energy.usef.core.model.PrognosisType.A_PLAN);
        // assertions
        Assert.assertNotNull(lastPrognoses);
        Assert.assertEquals(96, lastPrognoses.size());
        Mockito.verify(ptuPrognosisRepository, Mockito.times(1))
                .findLastPrognoses(Matchers.eq(period), Matchers.eq(Optional.of(energy.usef.core.model.PrognosisType.A_PLAN)),
                        Matchers.eq(Optional.empty()), Matchers.eq(Optional.empty()));
    }

    @Test
    public void testFindLastPrognosesLocalDateString() throws Exception {
        final LocalDate period = new LocalDate(2015, 1, 1);
        final String usefIdentifier = "brp1.usef-example.com";

        Mockito.when(ptuPrognosisRepository.findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(Optional.empty()),
                Matchers.eq(Optional.of(usefIdentifier)), Matchers.eq(Optional.empty())))
                .then(call -> IntStream.rangeClosed(1, 96).mapToObj(index -> {
                    PtuPrognosis ptuPrognosis = new PtuPrognosis();
                    PtuContainer ptuContainer = new PtuContainer((LocalDate) call.getArguments()[0], index);
                    ptuPrognosis.setType(energy.usef.core.model.PrognosisType.A_PLAN);
                    ptuPrognosis.setConnectionGroup(new BrpConnectionGroup(usefIdentifier));
                    ptuPrognosis.setPtuContainer(ptuContainer);
                    return ptuPrognosis;
                }).collect(Collectors.toList()));

        // invocation
        List<PtuPrognosis> lastPrognoses = corePlanboardBusinessService.findLastPrognoses(period, usefIdentifier);
        // assertions
        Assert.assertNotNull(lastPrognoses);
        Assert.assertEquals(96, lastPrognoses.size());
        Mockito.verify(ptuPrognosisRepository, Mockito.times(1))
                .findLastPrognoses(Matchers.eq(period), Matchers.eq(Optional.empty()), Matchers.eq(Optional.of(usefIdentifier)),
                        Matchers.eq(Optional.empty()));

    }

    @Test
    public void testFindLastPrognosesLocalDate() throws Exception {
        final LocalDate period = new LocalDate(2015, 1, 1);
        final String usefIdentifier = "brp1.usef-example.com";

        Mockito.when(ptuPrognosisRepository
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(Optional.empty()), Matchers.eq(Optional.empty()),
                        Matchers.eq(Optional.empty()))).then(call -> IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuPrognosis ptuPrognosis = new PtuPrognosis();
            PtuContainer ptuContainer = new PtuContainer((LocalDate) call.getArguments()[0], index);
            ptuPrognosis.setType(energy.usef.core.model.PrognosisType.A_PLAN);
            ptuPrognosis.setConnectionGroup(new BrpConnectionGroup(usefIdentifier));
            ptuPrognosis.setPtuContainer(ptuContainer);
            return ptuPrognosis;
        }).collect(Collectors.toList()));

        // invocation
        List<PtuPrognosis> lastPrognoses = corePlanboardBusinessService.findLastPrognoses(period);
        // assertions
        Assert.assertNotNull(lastPrognoses);
        Assert.assertEquals(96, lastPrognoses.size());
        Mockito.verify(ptuPrognosisRepository, Mockito.times(1))
                .findLastPrognoses(Matchers.eq(period), Matchers.eq(Optional.empty()), Matchers.eq(Optional.empty()),
                        Matchers.eq(Optional.empty()));
    }

    @Test
    public void testBuildConnectionGroupsToConnectionsMap() {
        final LocalDate period = new LocalDate(2015, 2, 2);

        PowerMockito.when(connectionGroupStateRepository
                .findActiveConnectionGroupsWithConnections(Matchers.any(LocalDate.class), Matchers.any(LocalDate.class)))
                .thenReturn(buildConnectionGroupsWithConnections());

        Map<String, List<String>> connectionListMap = corePlanboardBusinessService.buildConnectionGroupsToConnectionsMap(period);

        Assert.assertNotNull(connectionListMap);
        Assert.assertEquals(2, connectionListMap.size());
        Assert.assertEquals(9, connectionListMap.get("brp.usef-example.com").size());
        Assert.assertEquals(9, connectionListMap.get("ean1.000001").size());
    }

    @Test
    public void testInitialisePtuContainers() {
        LocalDateTime timestamp = DateTimeUtil.getCurrentDateTime();
        LocalDate period = timestamp.toLocalDate();
        int ptuIndex = PtuUtil.getPtuIndex(timestamp, 15);

        corePlanboardBusinessService.initialisePtuContainers();

        Mockito.verify(ptuContainerRepository, Mockito.times(1))
                .initialisePtuContainers(Matchers.eq(period), Matchers.eq(ptuIndex));
    }

    @Test
    public void testFindActiveCongestionPointAddresses() {
        LocalDate period = new LocalDate();
        List<CongestionPointConnectionGroup> connectionGroups = new ArrayList<>();

        CongestionPointConnectionGroup connectionGroup1 = new CongestionPointConnectionGroup();
        connectionGroup1.setUsefIdentifier("test1");
        connectionGroups.add(connectionGroup1);

        CongestionPointConnectionGroup connectionGroup2 = new CongestionPointConnectionGroup();
        connectionGroup2.setUsefIdentifier("test2");
        connectionGroups.add(connectionGroup2);

        Mockito.when(
                congestionPointConnectionGroupRepository.findActiveCongestionPointConnectionGroup(Matchers.any(LocalDate.class)))
                .thenReturn(connectionGroups);

        List<String> result = corePlanboardBusinessService.findActiveCongestionPointAddresses(period);

        Assert.assertEquals(2, result.size());
    }

    @Test
    public void testFindCongestionPointConnectionGroup() {
        corePlanboardBusinessService.findCongestionPointConnectionGroup("brp.usef-example.com");

        Mockito.verify(congestionPointConnectionGroupRepository, Mockito.times(1)).find(Matchers.eq("brp.usef-example.com"));
    }

    @Test
    public void testFindConnection() {
        corePlanboardBusinessService.findConnection("brp.usef-example.com");
        Mockito.verify(connectionRepository, Mockito.times(1)).find(Matchers.eq("brp.usef-example.com"));
    }

    @Test
    public void testFindAPlanRelatedToFlexOffer() {
        PlanboardMessage aPlanRelatedToFlexOffer = corePlanboardBusinessService
                .findAPlanRelatedToFlexOffer(1l, "brp.usef-example.com");

        Mockito.verify(planboardMessageRepository, Mockito.times(1))
                .findAPlanRelatedToFlexOffer(Matchers.eq(1l), Matchers.eq("brp.usef-example.com"));
    }

    @Test
    public void testFindOrCreatePtuState() {
        PtuState ptuState = corePlanboardBusinessService
                .findOrCreatePtuState(new PtuContainer(new LocalDate(), 1), new BrpConnectionGroup("brp"));

        Mockito.verify(ptuStateRepository, Mockito.times(1))
                .findOrCreatePtuState(Matchers.any(PtuContainer.class), Matchers.any(BrpConnectionGroup.class));
    }

    @Test
    public void testFindSinglePlanboardMessageForPeriod() {
        corePlanboardBusinessService.findSinglePlanboardMessage(new LocalDate(), DocumentType.METER_DATA_QUERY_EVENTS,
                "mdc.usef-example.com");
        Mockito.verify(planboardMessageRepository, Mockito.times(1)).findSinglePlanboardMessage(Matchers.eq(new LocalDate()), Matchers.eq(DocumentType.METER_DATA_QUERY_EVENTS), Matchers.eq("mdc.usef-example.com"));
    }

    @Test
    public void testFindSinglePlanboardMessageForSequence() {
        corePlanboardBusinessService.findSinglePlanboardMessage(1L, DocumentType.METER_DATA_QUERY_EVENTS,
                "mdc.usef-example.com");
        Mockito.verify(planboardMessageRepository, Mockito.times(1)).findSinglePlanboardMessage(Matchers.eq(1L), Matchers.eq(DocumentType.METER_DATA_QUERY_EVENTS), Matchers.eq("mdc.usef-example.com"));
    }

    private Map<ConnectionGroup, List<Connection>> buildConnectionGroupsWithConnections() {
        Map<ConnectionGroup, List<Connection>> connectionGroupListMap = new HashMap<>();

        ConnectionGroup connectionGroup1 = new BrpConnectionGroup("brp.usef-example.com");
        ConnectionGroup connectionGroup2 = new CongestionPointConnectionGroup("ean1.000001");

        connectionGroupListMap.put(connectionGroup1, buildConnectionList());
        connectionGroupListMap.put(connectionGroup2, buildConnectionList());

        return connectionGroupListMap;
    }

    private List<Connection> buildConnectionList() {
        List<Connection> connectionList = new ArrayList<>();

        IntStream.rangeClosed(1, 9).forEach(i -> {
            Connection connection = new Connection();
            connection.setEntityAddress("ean1.000001.00" + i);
            connectionList.add(connection);
        });

        return connectionList;
    }

    private FlexOffer buildFlexOffer() {
        FlexOffer flexOffer = new FlexOffer();
        flexOffer.setCongestionPoint(CONGESTION_POINT);
        flexOffer.setFlexRequestSequence(11111L);
        flexOffer.setSequence(22222L);
        flexOffer.setPeriod(PERIOD);
        return flexOffer;
    }

    private FlexOrder buildFlexOrder() {
        FlexOrder flexOrder = new FlexOrder();
        flexOrder.setCongestionPoint(CONGESTION_POINT);
        flexOrder.setFlexOfferSequence(11111L);
        flexOrder.setSequence(22222L);
        flexOrder.setPeriod(PERIOD);
        return flexOrder;
    }

    private Prognosis buildPrognosis() {
        Prognosis prognosis = new Prognosis();
        prognosis.setCongestionPoint(CONGESTION_POINT);
        prognosis.setPeriod(PERIOD);
        prognosis.setType(PrognosisType.D_PROGNOSIS);
        prognosis.setSequence(22222l);
        return prognosis;
    }

    private List<ConnectionGroupState> buildConnectionGroupStates(LocalDate startDate) {
        ConnectionGroup congestionPoint1 = new CongestionPointConnectionGroup("agr1.usef-example.com");
        ConnectionGroup congestionPoint2 = new CongestionPointConnectionGroup("agr2.usef-example.com");
        Connection connection1 = new Connection("ean.0000000001");
        Connection connection2 = new Connection("ean.0000000002");
        ConnectionGroupState cgs1 = buildConnectionGroupState(congestionPoint1, connection1, startDate, startDate.plusDays(1));
        ConnectionGroupState cgs2 = buildConnectionGroupState(congestionPoint1, connection2, startDate, startDate.plusDays(1));
        ConnectionGroupState cgs3 = buildConnectionGroupState(congestionPoint2, connection1, startDate.plusDays(1),
                startDate.plusDays(2));
        ConnectionGroupState cgs4 = buildConnectionGroupState(congestionPoint2, connection2, startDate.plusDays(1),
                startDate.plusDays(2));
        return Arrays.asList(cgs1, cgs2, cgs3, cgs4);
    }

    private ConnectionGroupState buildConnectionGroupState(ConnectionGroup connectionGroup, Connection connection,
            LocalDate validFrom, LocalDate validUntil) {
        ConnectionGroupState cgs = new ConnectionGroupState();
        cgs.setConnection(connection);
        cgs.setConnectionGroup(connectionGroup);
        cgs.setValidFrom(validFrom);
        cgs.setValidUntil(validUntil);
        return cgs;
    }

    private PlanboardMessage createPlanboardMessage(String participantDomain, DocumentStatus documentStatus, LocalDate period) {
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentType(DocumentType.A_PLAN);
        planboardMessage.setDocumentStatus(documentStatus);
        planboardMessage.setPeriod(period);
        planboardMessage.setParticipantDomain(participantDomain);
        return planboardMessage;
    }
}
