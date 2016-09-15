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

package energy.usef.dso.service.business;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import energy.usef.core.config.Config;
import energy.usef.core.data.xml.bean.message.Aggregator;
import energy.usef.core.data.xml.bean.message.CongestionPoint;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.repository.CongestionPointConnectionGroupRepository;
import energy.usef.core.repository.ConnectionRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuFlexOfferRepository;
import energy.usef.core.repository.PtuFlexOrderRepository;
import energy.usef.core.repository.PtuFlexRequestRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.repository.PtuStateRepository;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;
import energy.usef.dso.model.CommonReferenceOperator;
import energy.usef.dso.model.ConnectionCapacityLimitationPeriod;
import energy.usef.dso.model.ConnectionMeterEvent;
import energy.usef.dso.model.GridSafetyAnalysis;
import energy.usef.dso.model.NonAggregatorForecast;
import energy.usef.dso.model.PrognosisUpdateDeviation;
import energy.usef.dso.model.SynchronisationCongestionPoint;
import energy.usef.dso.model.SynchronisationConnection;
import energy.usef.dso.model.SynchronisationConnectionStatusType;
import energy.usef.dso.repository.AggregatorOnConnectionGroupStateRepository;
import energy.usef.dso.repository.AggregatorRepository;
import energy.usef.dso.repository.CommonReferenceOperatorRepository;
import energy.usef.dso.repository.ConnectionCapacityLimitationPeriodRepository;
import energy.usef.dso.repository.ConnectionMeterEventRepository;
import energy.usef.dso.repository.GridSafetyAnalysisRepository;
import energy.usef.dso.repository.MeterDataCompanyRepository;
import energy.usef.dso.repository.NonAggregatorForecastRepository;
import energy.usef.dso.repository.PrognosisUpdateDeviationRepository;
import energy.usef.dso.repository.PtuGridMonitorRepository;
import energy.usef.dso.repository.SynchronisationCongestionPointRepository;
import energy.usef.dso.repository.SynchronisationCongestionPointStatusRepository;
import energy.usef.dso.repository.SynchronisationConnectionRepository;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;

/**
 * Test class in charge of the unit tests related to the {@link DsoPlanboardBusinessService}.
 */
@RunWith(PowerMockRunner.class)
public class DsoPlanboardBusinessServiceTest {

    private static final String CONGESTION_POINT = "ean.123456789012345678";

    private DsoPlanboardBusinessService planboardService;

    @Mock
    private AggregatorOnConnectionGroupStateRepository aggregatorOnConnectionGroupStateRepository;
    @Mock
    private AggregatorRepository aggregatorRepository;
    @Mock
    private CommonReferenceOperatorRepository commonReferenceOperatorRepository;
    @Mock
    private Config config;
    @Mock
    private CongestionPointConnectionGroupRepository congestionPointConnectionGroupRepository;
    @Mock
    private ConnectionRepository connectionRepository;
    @Mock
    private ConnectionMeterEventRepository connectionMeterEventRepository;
    @Mock
    private ConnectionCapacityLimitationPeriodRepository connectionCapacityLimitationPeriodRepository;
    @Mock
    private CorePlanboardValidatorService planboardValidatorService;
    @Mock
    private EntityManager entityManager;
    @Mock
    private GridSafetyAnalysisRepository gridSafetyAnalysisRepository;
    @Mock
    private MeterDataCompanyRepository meterDataCompanyRepository;
    @Mock
    private NonAggregatorForecastRepository nonAggregatorForecastRepository;
    @Mock
    private PlanboardMessageRepository planboardMessageRepository;
    @Mock
    private PtuFlexRequestRepository ptuFlexRequestRepository;
    @Mock
    private PtuGridMonitorRepository ptuGridMonitorRepository;
    @Mock
    private PtuPrognosisRepository ptuPrognosisRepository;
    @Mock
    private PtuStateRepository ptuStateRepository;
    @Mock
    private PtuFlexOrderRepository ptuFlexOrderRepository;
    @Mock
    private PtuFlexOfferRepository ptuFlexOfferRepository;
    @Mock
    private PtuContainerRepository ptuContainerRepository;
    @Mock
    private PrognosisUpdateDeviationRepository prognosisUpdateDeviationRepository;
    @Mock
    private SynchronisationCongestionPointRepository synchronisationCongestionPointRepository;
    @Mock
    private SynchronisationCongestionPointStatusRepository synchronisationCongestionPointStatusRepository;
    @Mock
    private SynchronisationConnectionRepository synchronisationConnectionRepository;


    @Before
    public void init() {
        planboardService = new DsoPlanboardBusinessService();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        Whitebox.setInternalState(planboardService, "sequenceGeneratorService", sequenceGeneratorService);
        Whitebox.setInternalState(planboardService, "aggregatorRepository", aggregatorRepository);
        Whitebox.setInternalState(planboardService, "aggregatorOnConnectionGroupStateRepository",
                aggregatorOnConnectionGroupStateRepository);
        Whitebox.setInternalState(planboardService, "commonReferenceOperatorRepository", commonReferenceOperatorRepository);
        Whitebox.setInternalState(planboardService, "config", config);
        Whitebox.setInternalState(planboardService, "congestionPointConnectionGroupRepository",
                congestionPointConnectionGroupRepository);
        Whitebox.setInternalState(planboardService, "connectionRepository", connectionRepository);

        Whitebox.setInternalState(planboardService, "connectionCapacityLimitationPeriodRepository",
                connectionCapacityLimitationPeriodRepository);
        Whitebox.setInternalState(planboardService, "connectionMeterEventRepository", connectionMeterEventRepository);
        Whitebox.setInternalState(planboardService, "gridSafetyAnalysisRepository", gridSafetyAnalysisRepository);
        Whitebox.setInternalState(planboardService, "meterDataCompanyRepository", meterDataCompanyRepository);
        Whitebox.setInternalState(planboardService, "nonAggregatorForecastRepository", nonAggregatorForecastRepository);
        Whitebox.setInternalState(planboardService, "planboardMessageRepository", planboardMessageRepository);
        Whitebox.setInternalState(planboardService, "ptuContainerRepository", ptuContainerRepository);
        Whitebox.setInternalState(planboardService, "ptuFlexOfferRepository", ptuFlexOfferRepository);
        Whitebox.setInternalState(planboardService, "ptuFlexOrderRepository", ptuFlexOrderRepository);
        Whitebox.setInternalState(planboardService, "ptuGridMonitorRepository", ptuGridMonitorRepository);
        Whitebox.setInternalState(planboardService, "ptuPrognosisRepository", ptuPrognosisRepository);
        Whitebox.setInternalState(planboardService, "ptuStateRepository", ptuStateRepository);
        Whitebox.setInternalState(planboardService, "prognosisUpdateDeviationRepository", prognosisUpdateDeviationRepository);
        Whitebox.setInternalState(planboardService, "synchronisationCongestionPointRepository",
                synchronisationCongestionPointRepository);
        Whitebox.setInternalState(planboardService, "synchronisationCongestionPointStatusRepository",
                synchronisationCongestionPointStatusRepository);
        Whitebox.setInternalState(planboardService, "synchronisationConnectionRepository", synchronisationConnectionRepository);
    }

    @Test
    public void testInitiliazePlanboardService() {
        Assert.assertNotNull(planboardService);
    }

    /**
     * Tests DsoPlanboardBusinessService.findLastNonAggregatorForecasts method.
     */
    @Test
    public void testGetLastNonAggregatorForecasts() {
        LocalDate ptuDate = new LocalDate().plusDays(1);
        planboardService.findLastNonAggregatorForecasts(ptuDate, Optional.empty());
        verify(nonAggregatorForecastRepository, times(1)).getLastNonAggregatorForecasts(Matchers.any(LocalDate.class),
                Matchers.eq(Optional.empty()));
    }

    /**
     * Tests DsoPlanboardBusinessService.getAggregatorsByEntityAddress method.
     */
    @Test
    public void testGetAggregatorsByEntityAddress() {
        String entityAddress = "test.com";
        planboardService.getAggregatorsByEntityAddress(entityAddress, new LocalDate());
        verify(aggregatorOnConnectionGroupStateRepository, times(1)).getAggregatorsByCongestionPointAddress(
                Matchers.eq(entityAddress), Matchers.any(LocalDate.class));
    }

    @Test
    public void testFindGridSafetyAnalysisWithDispositionRequested() {
        LocalDate ptuDate = new LocalDate(2014, 11, 28);
        planboardService.findLatestGridSafetyAnalysisWithDispositionRequested(CONGESTION_POINT, ptuDate);

        verify(gridSafetyAnalysisRepository, times(1))
                .findGridSafetyAnalysisWithDispositionRequested(Matchers.eq(CONGESTION_POINT), Matchers.eq(ptuDate));
    }

    @Test
    public void testFindGridSafetyAnalysisRelatedToFlexOffers() {
        PowerMockito.when(
                gridSafetyAnalysisRepository.findGridSafetyAnalysisRelatedToFlexOffers(Matchers.any(LocalDate.class))).thenReturn(
                buildGridSafetyAnalysisList());
        Map<String, Map<LocalDate, GridSafetyAnalysisDto>> map = planboardService
                .createGridSafetyAnalysisRelatedToFlexOffersDtoMap();
        Assert.assertEquals(2, map.get(CONGESTION_POINT).get(new LocalDate()).getPtus().size());
    }

    @Test
    public void testCreateFlexOffersMapToPlaceFlexOrders() {
        planboardService.findOrderableFlexOffers();
        verify(planboardMessageRepository, times(1)).findOrderableFlexOffers();
    }

    @Test
    public void testFindPTUContainersForPeriod() {
        LocalDate period = new LocalDate();
        planboardService.findPTUContainersForPeriod(period);
        verify(ptuContainerRepository, times(1)).findPtuContainersMap(Matchers.eq(period));
    }

    @Test
    public void testFindOrderableFlexOffersSomeExpired() {
        List<PlanboardMessage> allOrderableFlexOffers = buildPlanboardMessagelist(DocumentType.FLEX_OFFER);
        Mockito.when(planboardMessageRepository.findOrderableFlexOffers()).thenReturn(allOrderableFlexOffers);

        Map<String, Map<LocalDate, List<PlanboardMessage>>> nonExpiredFlexOffers = planboardService.findOrderableFlexOffers();
        Assert.assertEquals(3, nonExpiredFlexOffers.get(CONGESTION_POINT).get(new LocalDate()).size());
    }

    @Test
    public void testFindNewOffers() {
        planboardService.findNewOffers();
        verify(planboardMessageRepository, times(1)).findPlanboardMessages(Matchers.eq(DocumentType.FLEX_OFFER),
                Matchers.eq(DocumentStatus.ACCEPTED));
    }

    @Test
    public void testUpdateFlexOrdersWithAcknowledgementStatus() {
        Long sequence = 1234567L;
        AcknowledgementStatus acknowledgementStatus = AcknowledgementStatus.ACCEPTED;
        String aggregatorDomain = "test.com";

        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentStatus(DocumentStatus.SENT);
        PowerMockito.when(planboardMessageRepository
                .findSinglePlanboardMessage(Matchers.eq(sequence), Matchers.eq(DocumentType.FLEX_ORDER),
                        Matchers.eq(aggregatorDomain))).thenReturn(planboardMessage);

        planboardService.updateFlexOrdersWithAcknowledgementStatus(sequence, acknowledgementStatus, aggregatorDomain);

        verify(ptuFlexOrderRepository, times(1)).findFlexOrdersBySequence(sequence);
    }

    @Test
    public void testUpdateAggregatorsOnCongestionPointConnectionGroup() {
        CongestionPoint xmlCongestionPoint = new CongestionPoint();
        xmlCongestionPoint.setEntityAddress("ean.1111111111");
        xmlCongestionPoint.setDSODomain("dso.usef-example.com");
        /*
         * Creating 3 aggregators on the connection count.
         */
        xmlCongestionPoint.getAggregator().addAll(IntStream.rangeClosed(1, 3).mapToObj(index -> {
            Aggregator xmlAggregator = new Aggregator();
            xmlAggregator.setDomain("agr" + index + ".usef-example.com");
            xmlAggregator.setConnectionCount(BigInteger.valueOf(index));
            return xmlAggregator;
        }).collect(Collectors.toList()));
        // stubing of the AggregatorRepository
        PowerMockito.when(aggregatorRepository.findOrCreate(Matchers.any(String.class))).then(invocation -> {
            energy.usef.dso.model.Aggregator agr = new energy.usef.dso.model.Aggregator();
            agr.setDomain((String) invocation.getArguments()[0]);
            return agr;
        });
        // stubbing of the CongestionPointConnectionGroupRepository
        PowerMockito.when(
                congestionPointConnectionGroupRepository.findOrCreate(Matchers.any(String.class), Matchers.any(String.class)))
                .then(invocation -> {
                    CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
                    congestionPoint.setUsefIdentifier((String) invocation.getArguments()[0]);
                    congestionPoint.setDsoDomain((String) invocation.getArguments()[1]);
                    return congestionPoint;
                });

        // stubbing of the AggregatorOnConnectionGroupStateRepository
        PowerMockito.when(
                aggregatorOnConnectionGroupStateRepository.findEndingAggregatorOnConnectionGroupStates(Matchers.any(String.class),
                        Matchers.any(LocalDate.class))).then(invocation -> {
            AggregatorOnConnectionGroupState endingState = new AggregatorOnConnectionGroupState();
            energy.usef.dso.model.Aggregator aggregator = new energy.usef.dso.model.Aggregator();
            aggregator.setDomain("agr2.usef-example.com");
            CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
            congestionPoint.setUsefIdentifier("ean.1111111111");
            endingState.setValidUntil((LocalDate) invocation.getArguments()[1]);
            endingState.setConnectionCount(BigInteger.valueOf(2));
            endingState.setAggregator(aggregator);
            endingState.setCongestionPointConnectionGroup(congestionPoint);
            return Collections.singletonList(endingState);
        });

        planboardService.updateAggregatorsOnCongestionPointConnectionGroup(xmlCongestionPoint,
                DateTimeUtil.getCurrentDate().plusDays(1), 1);

        verify(congestionPointConnectionGroupRepository, times(1)).findOrCreate(Matchers.any(String.class),
                Matchers.any(String.class));
        verify(aggregatorRepository, times(2)).findOrCreate(Matchers.any(String.class));
        // only the 2 first aggregators have a different or non-existing state.
        verify(aggregatorOnConnectionGroupStateRepository, times(2)).persist(Matchers.any(AggregatorOnConnectionGroupState.class));
    }

    @Test
    public void testInvokeForUpdatedPrognosis() throws BusinessException {
        PowerMockito.when(ptuPrognosisRepository
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.any(Optional.class), Matchers.any(Optional.class),
                        Matchers.eq(Optional.empty()))).thenReturn(buildPtuPrognosisList());
        PowerMockito.when(
                planboardMessageRepository.findFlexOrdersRelatedToPrognosis(Matchers.any(Long.class), Matchers.any(String.class)))
                .thenReturn(buildPlanboardMessages());
        PowerMockito.when(ptuFlexOrderRepository.findFlexOrdersBySequence(Matchers.any(Long.class))).thenReturn(buildFlexOrders());

        // 0. Previous prognosis prognosed 1000 Power
        // 1. Flex Offer for PTU 1 will contain -100 power, which will be OK for the 900 Power on updated Prognosis on PTU 1
        // (1000 + (-100) = 900)
        PowerMockito.when(ptuFlexOfferRepository.findPtuFlexOfferWithSequence(Matchers.any(Long.class), Matchers.any(String.class),
                Matchers.eq(1))).thenReturn(buildFlexOffer(1));
        // 2. Flex Offer for PTU 2 will contain -200 power which wil not be OK for the 900 Power on updated Prognosis on PTU 2
        // (1000 + (-200) != 900)
        PowerMockito.when(ptuFlexOfferRepository.findPtuFlexOfferWithSequence(Matchers.any(Long.class), Matchers.any(String.class),
                Matchers.eq(2))).thenReturn(buildFlexOffer(2));

        Prognosis prognosis = buildPrognosis();
        planboardService.handleUpdatedPrognosis(prognosis, buildPtuPrognosisList());

        ArgumentCaptor<PrognosisUpdateDeviation> prognosisUpdateErrorCaptor = ArgumentCaptor.forClass(
                PrognosisUpdateDeviation.class);
        // verify that we record a deviation in the planboard for the PTU 2
        verify(prognosisUpdateDeviationRepository, times(1)).persist(prognosisUpdateErrorCaptor.capture());
        PrognosisUpdateDeviation error = prognosisUpdateErrorCaptor.getValue();
        Assert.assertNotNull("Did not expect a null PrognosisUpdateError", error);
        Assert.assertEquals("PTU index mismatch.", 2, error.getPtuIndex().intValue());
    }

    @Test
    public void testfindAggregatorsWithOverlappingActivityForPeriod() {
        final LocalDate startDate = new LocalDate("2015-09-02");
        final LocalDate endDate = new LocalDate("2015-09-04");
        List<AggregatorOnConnectionGroupState> result = planboardService
                .findAggregatorsWithOverlappingActivityForPeriod(startDate, endDate);
        Assert.assertNotNull(result);
        Mockito.verify(aggregatorOnConnectionGroupStateRepository, Mockito.times(1))
                .findAggregatorsWithOverlappingActivityForPeriod(Matchers.eq(startDate), Matchers.eq(endDate));
    }

    private Prognosis buildPrognosis() {
        Prognosis dprognosis = new Prognosis();
        dprognosis.setMessageMetadata(new MessageMetadataBuilder().senderDomain("agr.usef-example.com")
                .senderRole(USEFRole.AGR)
                .recipientDomain("dso.usef-example.com")
                .recipientRole(USEFRole.DSO)
                .messageID()
                .conversationID()
                .timeStamp()
                .build());
        dprognosis.setSequence(2l);
        dprognosis.setCongestionPoint("ean.0123456789012345678");
        dprognosis.setPeriod(new LocalDate());

        PTU ptu1 = new PTU();
        ptu1.setStart(BigInteger.valueOf(1));
        ptu1.setPower(BigInteger.valueOf(900));

        PTU ptu2 = new PTU();
        ptu2.setStart(BigInteger.valueOf(2));
        ptu2.setPower(BigInteger.valueOf(900));
        dprognosis.getPTU().add(ptu1);
        dprognosis.getPTU().add(ptu2);
        return dprognosis;
    }

    private List<PtuPrognosis> buildPtuPrognosisList() {
        PtuContainer ptu1 = new PtuContainer();
        ptu1.setPtuIndex(1);
        PtuContainer ptu2 = new PtuContainer();
        ptu2.setPtuIndex(2);

        PtuPrognosis prognosis1 = new PtuPrognosis();
        prognosis1.setParticipantDomain("agr.usef-example.com");
        prognosis1.setSequence(1l);
        prognosis1.setPtuContainer(ptu1);
        prognosis1.setPower(BigInteger.valueOf(1000));

        PtuPrognosis prognosis2 = new PtuPrognosis();
        prognosis2.setParticipantDomain("agr.usef-example.com");
        prognosis2.setSequence(1l);
        prognosis2.setPtuContainer(ptu2);
        prognosis2.setPower(BigInteger.valueOf(1000));
        return Arrays.asList(prognosis1, prognosis2);
    }

    private List<PlanboardMessage> buildPlanboardMessages() {
        PlanboardMessage pm1 = new PlanboardMessage();
        pm1.setSequence(1l);
        return new ArrayList<>(Collections.singletonList(pm1));
    }

    private List<PtuFlexOrder> buildFlexOrders() {
        PtuFlexOrder order1 = new PtuFlexOrder();
        PtuFlexOrder order2 = new PtuFlexOrder();

        PtuContainer ptu1 = new PtuContainer();
        ptu1.setPtuIndex(1);
        ptu1.setPtuDate(new LocalDate());
        PtuContainer ptu2 = new PtuContainer();
        ptu2.setPtuIndex(2);
        ptu2.setPtuDate(new LocalDate());

        order1.setPtuContainer(ptu1);
        order1.setParticipantDomain("agr.usef-example.com");
        order1.setAcknowledgementStatus(AcknowledgementStatus.ACCEPTED);
        order1.setFlexOfferSequence(-1l);

        order2.setPtuContainer(ptu2);
        order2.setParticipantDomain("agr.usef-example.com");
        order2.setAcknowledgementStatus(AcknowledgementStatus.ACCEPTED);
        order2.setFlexOfferSequence(-1l);

        return new ArrayList<>(Arrays.asList(order1, order2));
    }

    private PtuFlexOffer buildFlexOffer(int ptuIndex) {
        PtuContainer ptu = new PtuContainer();
        ptu.setPtuIndex(ptuIndex);
        ptu.setPtuDate(new LocalDate());

        PtuFlexOffer offer = new PtuFlexOffer();
        offer.setPower(BigInteger.valueOf(-100).multiply(BigInteger.valueOf(ptuIndex)));
        return offer;
    }

    private List<GridSafetyAnalysis> buildGridSafetyAnalysisList() {

        GridSafetyAnalysis gsa1 = new GridSafetyAnalysis();
        GridSafetyAnalysis gsa2 = new GridSafetyAnalysis();

        PtuContainer ptu1 = new PtuContainer();
        ptu1.setPtuIndex(1);
        ptu1.setPtuDate(new LocalDate());

        PtuContainer ptu2 = new PtuContainer();
        ptu2.setPtuIndex(2);
        ptu2.setPtuDate(new LocalDate());

        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(CONGESTION_POINT);

        gsa1.setPtuContainer(ptu1);
        gsa1.setConnectionGroup(connectionGroup);
        gsa1.setDisposition(DispositionAvailableRequested.REQUESTED);

        gsa2.setPtuContainer(ptu2);
        gsa2.setConnectionGroup(connectionGroup);
        gsa2.setDisposition(DispositionAvailableRequested.REQUESTED);

        return new ArrayList<>(Arrays.asList(gsa1, gsa2));
    }

    @Test
    public void testSaveNonAggregatorConnectionForecast() {
        CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier("abc.test123.com");

        Aggregator agr = new Aggregator();
        agr.setDomain("abc.com");
        agr.setConnectionCount(BigInteger.TEN);

        LocalDate ptuDate = new LocalDate();
        List<Long> power = IntStream.rangeClosed(10, 12).mapToObj(Long::valueOf).collect(Collectors.toList());
        List<Long> maxload = IntStream.rangeClosed(1, 3).mapToObj(index -> (long) (index * 10)).collect(Collectors.toList());

        Mockito.when(congestionPointConnectionGroupRepository.getEntityManager()).thenReturn(entityManager);
        Mockito.when(ptuContainerRepository.getEntityManager()).thenReturn(entityManager);
        Mockito.when(nonAggregatorForecastRepository.getEntityManager()).thenReturn(entityManager);

        planboardService.saveNonAggregatorConnectionForecast(congestionPoint, ptuDate, power, maxload);

        verify(nonAggregatorForecastRepository, times(3)).persist(Matchers.any(NonAggregatorForecast.class));
    }

    @Test
    public void getLastNonAggregatorForecast() {
        planboardService.getLastNonAggregatorForecast();
        verify(nonAggregatorForecastRepository, times(1)).getLastNonAggregatorForecast();
    }

    @Test
    public void testFindConnectionsPerCRO() {
        Mockito.when(synchronisationCongestionPointRepository.findAll()).thenReturn(buildCongestionPoints());
        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(buildCRO());

        Map<String, List<SynchronisationCongestionPoint>> connectionsPerCRO = planboardService.findConnectionsPerCRO();

        Assert.assertNotNull(connectionsPerCRO);
        Assert.assertEquals(1, connectionsPerCRO.size());
        Assert.assertEquals(1, connectionsPerCRO.get("cro.usef-example.com").size());
        Assert.assertEquals(2, connectionsPerCRO.get("cro.usef-example.com").get(0).getConnections().size());
    }

    @Test
    public void testCleanSynchronization() {
        Mockito.when(synchronisationCongestionPointStatusRepository
                .countSynchronisationConnectionStatusWithStatus(SynchronisationConnectionStatusType.MODIFIED)).thenReturn(0l);
        Mockito.when(synchronisationCongestionPointStatusRepository
                .countSynchronisationConnectionStatusWithStatus(SynchronisationConnectionStatusType.DELETED)).thenReturn(0l);

        planboardService.cleanSynchronization();

        Mockito.verify(synchronisationCongestionPointStatusRepository, Mockito.times(1)).deleteAll();
        Mockito.verify(synchronisationConnectionRepository, Mockito.times(1)).deleteAll();
        Mockito.verify(synchronisationCongestionPointRepository, Mockito.times(1)).deleteAll();
    }

    @Test
    public void testUpdateCongestionPointStatusForCRO() {
        planboardService.updateCongestionPointStatusForCRO("ean1", "cro.usef-example.com");

        Mockito.verify(synchronisationCongestionPointRepository, Mockito.times(1))
                .updateCongestionPointStatusForCRO(Matchers.eq("ean1"), Matchers.eq("cro.usef-example.com"));
    }

    @Test
    public void testFindAllMDCs() {
        planboardService.findAllMDCs();

        Mockito.verify(meterDataCompanyRepository, Mockito.times(1)).findAll();
    }

    @Test
    public void testFindConnectionsPerCROEmptyCongestionPoints() {
        Mockito.when(synchronisationCongestionPointRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(commonReferenceOperatorRepository.findAll()).thenReturn(buildCRO());

        Map<String, List<SynchronisationCongestionPoint>> connectionsPerCRO = planboardService.findConnectionsPerCRO();

        Assert.assertNotNull(connectionsPerCRO);
        Assert.assertEquals(0, connectionsPerCRO.size());
    }

    private List<CommonReferenceOperator> buildCRO() {
        CommonReferenceOperator commonReferenceOperator = new CommonReferenceOperator();
        commonReferenceOperator.setDomain("cro.usef-example.com");
        commonReferenceOperator.setId(1l);
        return Arrays.asList(commonReferenceOperator);
    }

    private List<SynchronisationCongestionPoint> buildCongestionPoints() {
        SynchronisationConnection connection1 = new SynchronisationConnection();
        connection1.setEntityAddress("ean.1");
        SynchronisationConnection connection2 = new SynchronisationConnection();
        connection1.setEntityAddress("ean.2");

        SynchronisationCongestionPoint synchronisationCongestionPoint = new SynchronisationCongestionPoint();
        synchronisationCongestionPoint.setEntityAddress("dso.usef-example.com");
        synchronisationCongestionPoint.setConnections(Arrays.asList(connection1, connection2));

        return Arrays.asList(synchronisationCongestionPoint);
    }

    private List<PlanboardMessage> buildPlanboardMessagelist(DocumentType documentType) {
        List<PlanboardMessage> planboardMessageList = new ArrayList<>();
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(CONGESTION_POINT);
        for (long i = 1; i <= 6; ++i) {
            planboardMessageList.add(buildPlanboardMessage(documentType, i, connectionGroup));
        }
        return planboardMessageList;
    }

    private PlanboardMessage buildPlanboardMessage(DocumentType documentType, Long sequence, ConnectionGroup connectionGroup) {

        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentType(documentType);
        planboardMessage.setSequence(sequence);
        planboardMessage.setDocumentStatus(DocumentStatus.SENT);
        planboardMessage.setConnectionGroup(connectionGroup);
        planboardMessage.setPeriod(new LocalDate());
        if (documentType == DocumentType.FLEX_REQUEST || documentType == DocumentType.FLEX_OFFER
                || documentType == DocumentType.FLEX_ORDER) {
            planboardMessage
                    .setExpirationDate(DateTimeUtil.getCurrentDateTime().plusDays(3).minusDays(sequence.intValue()).plusHours(2));
        }
        return planboardMessage;
    }

    @Test
    public void testFindConnectionCountByUsefIdentifier() {
        planboardService.findConnectionCountByUsefIdentifier(CONGESTION_POINT);
        verify(connectionRepository, times(1))
                .findConnectionCountByUsefIdentifier(Matchers.eq(CONGESTION_POINT), Matchers.eq(DateTimeUtil.getCurrentDate()));
    }

    @Test
    public void testFindOrCreatePtuState() {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuIndex(1);
        ptuContainer.setPtuDate(new LocalDate());

        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(CONGESTION_POINT);

        planboardService.findOrCreatePtuState(ptuContainer, connectionGroup);

        verify(ptuStateRepository, times(1))
                .findOrCreatePtuState(Matchers.eq(ptuContainer), Matchers.eq(connectionGroup));
    }

    @Test
    public void testFindLimitedPower() {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuIndex(1);
        ptuContainer.setPtuDate(new LocalDate());

        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(CONGESTION_POINT);

        planboardService.findLimitedPower(ptuContainer, connectionGroup);

        verify(ptuGridMonitorRepository, times(1))
                .findLimitedPower(Matchers.eq(ptuContainer), Matchers.eq(connectionGroup));
    }

    @Test
    public void testSetLimitedPower() {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuIndex(1);
        ptuContainer.setPtuDate(new LocalDate());

        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(CONGESTION_POINT);

        planboardService.setLimitedPower(ptuContainer, 100L, connectionGroup);

        verify(ptuGridMonitorRepository, times(1))
                .setLimitedPower(Matchers.eq(ptuContainer), Matchers.eq(100L), Matchers.eq(connectionGroup));
    }

    @Test
    public void testSetActualPower() {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuIndex(1);
        ptuContainer.setPtuDate(new LocalDate());

        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(CONGESTION_POINT);

        planboardService.setActualPower(ptuContainer, 100L, connectionGroup);

        verify(ptuGridMonitorRepository, times(1))
                .setActualPower(Matchers.eq(ptuContainer), Matchers.eq(100L), Matchers.eq(connectionGroup));
    }

    @Test
    public void testFindPtuContainer() {
        LocalDate period = new LocalDate();

        planboardService.findPtuContainer(period, 12);
        verify(ptuContainerRepository, times(1))
                .findPtuContainer(Matchers.eq(period), Matchers.eq(12));
    }

    @Test
    public void testFindActiveCongestionPointConnectionGroup() {
        LocalDate date = new LocalDate();
        planboardService.findActiveCongestionPointConnectionGroup(date);
        verify(congestionPointConnectionGroupRepository, times(1))
                .findActiveCongestionPointConnectionGroup(Matchers.eq(date));
    }

    @Test
    public void testFindAllCommonReferenceOperators() {
        planboardService.findAllCommonReferenceOperators();
        verify(commonReferenceOperatorRepository, times(1)).findAll();
    }

    @Test
    public void testStorePlanboardMessage() {
        ConnectionMeterEvent entity = new ConnectionMeterEvent();
        planboardService.storeConnectionMeterEvent(entity);
        Mockito.verify(connectionMeterEventRepository, Mockito.times(1)).persist(Matchers.eq(entity));
    }

    @Test
    public void testFindConnectionForConnectionMeterEventsPeriod() {
        planboardService.findConnectionForConnectionMeterEventsPeriod(CONGESTION_POINT, new LocalDate());
        Mockito.verify(connectionMeterEventRepository, Mockito.times(1))
                .findConnectionForConnectionMeterEventsPeriod(Matchers.eq(CONGESTION_POINT), Matchers.eq(new LocalDate()));
    }

    @Test
    public void testStoreConnectionMeterEvent() {
        ConnectionMeterEvent entity = new ConnectionMeterEvent();
        planboardService.storeConnectionMeterEvent(entity);
        Mockito.verify(connectionMeterEventRepository, Mockito.times(1)).persist(Matchers.eq(entity));
    }

    @Test
    public void testFindConnectionsNotRelatedToConnectionMeterEvents() {
        List<String> connectionIncludeList = new ArrayList<>();
        planboardService.findConnectionsNotRelatedToConnectionMeterEvents(new LocalDate(), connectionIncludeList);

        Mockito.verify(connectionMeterEventRepository, Mockito.times(1))
                .findConnectionsNotRelatedToConnectionMeterEvents(Matchers.eq(new LocalDate()), Matchers.eq(connectionIncludeList));
    }

    @Test
    public void testFindConnectionMeterEventsForPeriod() {
        planboardService.findConnectionMeterEventsForPeriod(new LocalDate(), new LocalDate());

        Mockito.verify(connectionMeterEventRepository, Mockito.times(1))
                .findConnectionMeterEventsForPeriod(Matchers.eq(new LocalDate()), Matchers.eq(new LocalDate()));
    }

    @Test
    public void testStoreConnectionMeterEventPeriod() {
        ConnectionCapacityLimitationPeriod entity = new ConnectionCapacityLimitationPeriod();
        planboardService.storeConnectionMeterEventPeriod(entity);
        Mockito.verify(connectionCapacityLimitationPeriodRepository, Mockito.times(1)).persist(Matchers.eq(entity));
    }

    @Test
    public void testGetAggregatorsByCongestionPointAddress() {
        planboardService.getAggregatorsByCongestionPointAddress(CONGESTION_POINT, new LocalDate());
        Mockito.verify(aggregatorOnConnectionGroupStateRepository, Mockito.times(1))
                .getAggregatorsByCongestionPointAddress(Matchers.eq(CONGESTION_POINT), Matchers.eq(new LocalDate()));
    }

    @Test
    public void testStoreGridSafetyAnalysis() {
        GridSafetyAnalysis entity = new GridSafetyAnalysis();
        planboardService.storeGridSafetyAnalysis(entity);
        Mockito.verify(gridSafetyAnalysisRepository, Mockito.times(1)).persist(Matchers.eq(entity));
    }

    @Test
    public void testFindGridSafetyAnalysis() {
        LocalDate ptuDate = new LocalDate(2014, 11, 28);
        planboardService.findGridSafetyAnalysis(CONGESTION_POINT, ptuDate);
        verify(gridSafetyAnalysisRepository, times(1)).findGridSafetyAnalysis(Matchers.eq(CONGESTION_POINT), Matchers.eq(ptuDate));
    }

    @Test
    public void testDeletePreviousGridSafetyAnalysis() {
        LocalDate ptuDate = new LocalDate(2014, 11, 28);
        planboardService.deletePreviousGridSafetyAnalysis(CONGESTION_POINT, ptuDate);
        verify(gridSafetyAnalysisRepository, times(1)).deletePreviousGridSafetyAnalysis(Matchers.eq(CONGESTION_POINT), Matchers.eq(ptuDate));
    }
}
