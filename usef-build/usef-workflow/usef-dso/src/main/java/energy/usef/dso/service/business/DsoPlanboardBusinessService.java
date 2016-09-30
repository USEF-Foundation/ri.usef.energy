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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CongestionPoint;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Exchange;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.model.PtuState;
import energy.usef.core.repository.CongestionPointConnectionGroupRepository;
import energy.usef.core.repository.ConnectionRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuFlexOfferRepository;
import energy.usef.core.repository.PtuFlexOrderRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.repository.PtuStateRepository;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.DocumentStatusUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.transformer.FlexOrderTransformer;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.model.AggregatorOnConnectionGroupState;
import energy.usef.dso.model.CommonReferenceOperator;
import energy.usef.dso.model.ConnectionCapacityLimitationPeriod;
import energy.usef.dso.model.ConnectionMeterEvent;
import energy.usef.dso.model.GridSafetyAnalysis;
import energy.usef.dso.model.MeterDataCompany;
import energy.usef.dso.model.NonAggregatorForecast;
import energy.usef.dso.model.PrognosisUpdateDeviation;
import energy.usef.dso.model.PtuGridMonitor;
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
import energy.usef.dso.workflow.transformer.GridSafetyAnalysisDtoTransformer;

/**
 * Service class in charge of operations and validations related to the DSO planboard.
 */
@Stateless
public class DsoPlanboardBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoPlanboardBusinessService.class);

    @Inject
    private Config config;

    @Inject
    private AggregatorRepository aggregatorRepository;

    @Inject
    private AggregatorOnConnectionGroupStateRepository aggregatorOnConnectionGroupStateRepository;

    @Inject
    private CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Inject
    private CongestionPointConnectionGroupRepository congestionPointConnectionGroupRepository;

    @Inject
    private ConnectionMeterEventRepository connectionMeterEventRepository;

    @Inject
    private ConnectionCapacityLimitationPeriodRepository connectionCapacityLimitationPeriodRepository;

    @Inject
    private ConnectionRepository connectionRepository;

    @Inject
    private GridSafetyAnalysisRepository gridSafetyAnalysisRepository;

    @Inject
    private MeterDataCompanyRepository meterDataCompanyRepository;

    @Inject
    private NonAggregatorForecastRepository nonAggregatorForecastRepository;

    @Inject
    private PlanboardMessageRepository planboardMessageRepository;

    @Inject
    private PrognosisUpdateDeviationRepository prognosisUpdateDeviationRepository;

    @Inject
    private PtuContainerRepository ptuContainerRepository;

    @Inject
    private PtuFlexOfferRepository ptuFlexOfferRepository;

    @Inject
    private PtuFlexOrderRepository ptuFlexOrderRepository;

    @Inject
    private PtuGridMonitorRepository ptuGridMonitorRepository;

    @Inject
    private PtuPrognosisRepository ptuPrognosisRepository;

    @Inject
    private PtuStateRepository ptuStateRepository;

    @Inject
    private SynchronisationCongestionPointRepository synchronisationCongestionPointRepository;

    @Inject
    private SynchronisationCongestionPointStatusRepository synchronisationCongestionPointStatusRepository;

    @Inject
    private SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * Gets last NonAggregatorForecasts with current date as start date.
     *
     * @param ptuDate ptu date
     * @return last non aggregator forecasts
     */
    public List<NonAggregatorForecast> findLastNonAggregatorForecasts(LocalDate ptuDate, Optional<String> usefIdentifier) {
        return nonAggregatorForecastRepository.getLastNonAggregatorForecasts(ptuDate, usefIdentifier);
    }

    /**
     * Gets aggregators by grid point entity adress.
     *
     * @param entityAddress grid entity address
     * @param date the period
     * @return aggregators
     */
    public List<Aggregator> getAggregatorsByEntityAddress(String entityAddress, LocalDate date) {
        return aggregatorOnConnectionGroupStateRepository.getAggregatorsByCongestionPointAddress(entityAddress, date);
    }

    /**
     * Counts the {@link AggregatorOnConnectionGroupState}s for this congestionPoint in a moment in time.
     *
     * @param entityAddress The congestion Point Entity Address.
     * @param date The day.
     * @return aggregators
     */
    public Long countActiveAggregatorsForCongestionPointOnDay(String entityAddress, LocalDate date) {
        return aggregatorOnConnectionGroupStateRepository.countActiveAggregatorsForCongestionPointOnDay(entityAddress, date);
    }

    /**
     * Find the {@link AggregatorOnConnectionGroupState}s for this congestionPoint in a moment in time.
     *
     * @param entityAddress grid entity address
     * @param date the period
     * @return aggregators
     */
    public List<AggregatorOnConnectionGroupState> findAggregatorOnConnectionGroupStateByCongestionPointAddress(
            String entityAddress,
            LocalDate date) {
        return aggregatorOnConnectionGroupStateRepository.findAggregatorOnConnectionGroupStateByCongestionPointAddress(
                entityAddress, date);
    }

    /**
     * Finds the Aggregators on a congestion point.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link Map} with the congestion point as key ({@link CongestionPointConnectionGroup}) and a {@link List} of {@link
     * AggregatorOnConnectionGroupState} as value.
     */
    public Map<CongestionPointConnectionGroup, List<AggregatorOnConnectionGroupState>> findConnectionGroupsWithAggregators(
            LocalDate period) {
        return aggregatorOnConnectionGroupStateRepository.findConnectionGroupsWithAggregators(period);
    }

    /**
     * Finds the {@link AggregatorOnConnectionGroupState} entities with an overlap over the given period.
     *
     * @param startDate {@link LocalDate} start date of the period.
     * @param endDate {@link LocalDate} end date of the period (inclusive).
     * @return a {@link List} of {@link AggregatorOnConnectionGroupState}.
     */
    public List<AggregatorOnConnectionGroupState> findAggregatorsWithOverlappingActivityForPeriod(LocalDate startDate,
            LocalDate endDate) {
        return aggregatorOnConnectionGroupStateRepository.findAggregatorsWithOverlappingActivityForPeriod(startDate, endDate);
    }

    /**
     * Finds the {@link GridSafetyAnalysis} entities for the given parameters with disposition requested for at least one of the
     * ptu's.
     *
     * @param congestionPointEntityAddress {@link String} Entity Address of the related congestion point.
     * @param ptuDate {@link LocalDate} PTU date (period).
     * @return a list of {@link GridSafetyAnalysis}.
     */
    public List<GridSafetyAnalysis> findLatestGridSafetyAnalysisWithDispositionRequested(String congestionPointEntityAddress,
            LocalDate ptuDate) {
        return gridSafetyAnalysisRepository.findGridSafetyAnalysisWithDispositionRequested(congestionPointEntityAddress, ptuDate);
    }

    /**
     * Finds the grid monitoring data for the given period of time.
     *
     * @param startDate {@link LocalDate} start date (inclusive).
     * @param endDate {@link LocalDate} end date (inclusive).
     * @return a {@link List} of {@link PtuGridMonitor}.
     */
    public List<PtuGridMonitor> findGridMonitoringData(LocalDate startDate, LocalDate endDate) {
        return ptuGridMonitorRepository.findPtuGridMonitorsByDates(startDate, endDate);
    }

    /**
     * Create the GridSafetyAnalysisDto Map used in calculations to find optimal flex offer combinations required to place flex
     * orders.
     *
     * @return Map: Congestion point entity address -> Map: PTU Date -> GridSafetyAnalysisDto
     */
    public Map<String, Map<LocalDate, GridSafetyAnalysisDto>> createGridSafetyAnalysisRelatedToFlexOffersDtoMap() {
        // Map: Congestion point entity address -> Map: PTU Date -> GridSafetyAnalysisDto
        Map<String, Map<LocalDate, GridSafetyAnalysisDto>> result = new HashMap<>();
        List<GridSafetyAnalysis> gsaList = gridSafetyAnalysisRepository.findGridSafetyAnalysisRelatedToFlexOffers(DateTimeUtil
                .getCurrentDate());

        for (GridSafetyAnalysis gsa : gsaList) {
            // Map: PTU Date -> GridSafetyAnalysisDto
            Map<LocalDate, GridSafetyAnalysisDto> dateToGridSafetyAnalysisDtoMap = result.get(gsa.getConnectionGroup()
                    .getUsefIdentifier());
            if (dateToGridSafetyAnalysisDtoMap == null) {
                dateToGridSafetyAnalysisDtoMap = new HashMap<>();
                result.put(gsa.getConnectionGroup().getUsefIdentifier(), dateToGridSafetyAnalysisDtoMap);
            }

            GridSafetyAnalysisDto gridSafetyAnalysisDto = dateToGridSafetyAnalysisDtoMap.get(gsa.getPtuContainer().getPtuDate());
            if (gridSafetyAnalysisDto == null) {
                gridSafetyAnalysisDto = new GridSafetyAnalysisDto();
                gridSafetyAnalysisDto.setEntityAddress(gsa.getConnectionGroup().getUsefIdentifier());
                gridSafetyAnalysisDto.setPtuDate(gsa.getPtuContainer().getPtuDate());
                dateToGridSafetyAnalysisDtoMap.put(gsa.getPtuContainer().getPtuDate(), gridSafetyAnalysisDto);
            }

            gridSafetyAnalysisDto.getPtus().add(GridSafetyAnalysisDtoTransformer.transform(gsa));
        }

        return result;
    }

    /**
     * Creates flex offers map required to place flex orders.
     *
     * @return Map: Congestion point entity address -> Map: PTU Date -> Flex Offer list
     */
    public Map<String, Map<LocalDate, List<PlanboardMessage>>> findOrderableFlexOffers() {
        List<PlanboardMessage> flexOffers = planboardMessageRepository.findOrderableFlexOffers();

        Map<String, List<PlanboardMessage>> flexOffersPerConnectionGroup = flexOffers.stream()
                .filter(this::validateOffer)
                .collect(Collectors.groupingBy(pm -> pm.getConnectionGroup().getUsefIdentifier()));

        return flexOffersPerConnectionGroup.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().collect(Collectors.groupingBy(PlanboardMessage::getPeriod))));
    }

    private boolean validateOffer(PlanboardMessage flexOfferMessage) {
        if (flexOfferMessage.getExpirationDate().isBefore(DateTimeUtil.getCurrentDateTime())) {
            flexOfferMessage.setDocumentStatus(DocumentStatus.EXPIRED);
            LOGGER.info("FlexOffer {} ignored, it expired {}.", flexOfferMessage.getSequence(),
                    flexOfferMessage.getExpirationDate());
            return false;
        }
        return true;
    }

    /**
     * Find the PTU's for the congestionpoint and period.
     *
     * @param period - The Day we want all {@link PtuContainer}'s for.
     * @return - The Map of {@link PtuContainer}'s mapped by ptu index.
     */
    public Map<Integer, PtuContainer> findPTUContainersForPeriod(LocalDate period) {
        return ptuContainerRepository.findPtuContainersMap(period);
    }

    /**
     * Find {@link PlanboardMessage} which have status ACCEPTED to know which offers needs to be processed.
     *
     * @return the {@link PlanboardMessage} with status ACCEPTED.
     */
    public List<PlanboardMessage> findNewOffers() {
        return planboardMessageRepository.findPlanboardMessages(DocumentType.FLEX_OFFER, DocumentStatus.ACCEPTED);
    }

    /**
     * Updates flexibility orders with acknowledgement status.
     *
     * @param sequence sequence number
     * @param acknowledgementStatus acknowledgement status
     * @param aggregatorDomain aggregator domain
     * @return flex order DTO containing the whole data set corresponding to the updated flex orders. In case there was found no
     * flex order to update the flex order DTO has empty properties
     */
    public FlexOrderDto updateFlexOrdersWithAcknowledgementStatus(Long sequence,
            final AcknowledgementStatus acknowledgementStatus, String aggregatorDomain) {
        PlanboardMessage flexOrderMessage = planboardMessageRepository.findSinglePlanboardMessage(sequence,
                DocumentType.FLEX_ORDER, aggregatorDomain);

        if (!DocumentStatus.SENT.equals(flexOrderMessage.getDocumentStatus())) {
            LOGGER.error("A response has already been processed for this flex order %s. Invalid response received " +
                            "from %s. ", sequence, aggregatorDomain);
            return null;
        }

        flexOrderMessage.setDocumentStatus(DocumentStatusUtil.toDocumentStatus(acknowledgementStatus));

        List<PtuFlexOrder> flexOrders = ptuFlexOrderRepository.findFlexOrdersBySequence(sequence);
        List<PtuFlexOrder> changedFlexOrders = new ArrayList<>();
        flexOrders.stream()
                .filter(flexOrder -> AcknowledgementStatus.SENT.equals(flexOrder.getAcknowledgementStatus())
                        || AcknowledgementStatus.NO_RESPONSE.equals(flexOrder.getAcknowledgementStatus()))
                .forEach(flexOrder -> {
                    changedFlexOrders.add(flexOrder);
                    flexOrder.setAcknowledgementStatus(acknowledgementStatus);
                });

        return FlexOrderTransformer.transformPtuFlexOrders(changedFlexOrders);
    }

    /**
     * Handles the business logic related to the receipt of an updated version of a prognosis.
     *
     * @param prognosis {@link Prognosis} message.
     * @param existingPtuPrognoses {@link List} of {@link PtuPrognosis}, which is a list of all existing d-prognosis for a
     * congestion point on a specific period.
     */
    public void handleUpdatedPrognosis(Prognosis prognosis, List<PtuPrognosis> existingPtuPrognoses) {
        String aggregatorDomain = prognosis.getMessageMetadata().getSenderDomain();
        Long latestPrognosisSequence = getDocumentLatestSequenceNumber(existingPtuPrognoses, aggregatorDomain);

        // 1. Find the flex order messages related to the updated prognosis.
        List<PlanboardMessage> planboardFlexOrders = planboardMessageRepository.findFlexOrdersRelatedToPrognosis(
                latestPrognosisSequence, aggregatorDomain);
        LOGGER.info("Found {} related flex orders for the prognosis with sequence {}.", planboardFlexOrders.size(),
                latestPrognosisSequence);

        // 2. Find all the PTU Flex Orders for each FlexOrder message.
        List<PtuFlexOrder> ptuFlexOrders = new ArrayList<>();
        for (PlanboardMessage planboardFlexOrder : planboardFlexOrders) {
            ptuFlexOrders.addAll(ptuFlexOrderRepository.findFlexOrdersBySequence(planboardFlexOrder.getSequence()));
        }

        // 3.1. If there are no flex orders at the moment, log it and persist the updated prognosis.
        if (ptuFlexOrders.isEmpty()) {
            LOGGER.info("No orders have been placed. Updated prognosis has no impact.");
        } else {
            // 3.2. If there are existing flex orders.
            // 3.2.1. Create a map of PTUIndex / List of flex orders for the ptu.
            Map<Integer, List<PtuFlexOrder>> ptuFlexOrdersMap = sortAndMapPtuFlexOrders(ptuFlexOrders);
            for (PTU ptu : PtuListConverter.normalize(prognosis.getPTU())) {
                // 3.2.2. For each ptu, sum the flex ordered.
                BigInteger power = sumPowerOfFlexOrders(ptuFlexOrdersMap.get(ptu.getStart().intValue()));
                PtuPrognosis previousPtuPrognosis = fetchPreviousPtuPrognosis(existingPtuPrognoses, aggregatorDomain,
                        ptu.getStart().intValue());
                /*
                 * 3.2.3. If there is a gap between the difference between previous prognosed power and order flex, and with the
                 * updated prognosed power, record the fact in the database.
                 */
                if (!(previousPtuPrognosis.getPower().add(power)).equals(ptu.getPower())) {
                    LOGGER.warn(
                            "Sum of the flex ordered summed to previous prognosis power does not match updated prognosed power "
                                    + "for the PTU with index {} on {} for prognosis {})!", ptu.getStart().intValue(),
                            prognosis.getPeriod(), prognosis.getSequence());
                    prognosisUpdateDeviationRepository.persist(
                            new PrognosisUpdateDeviation(prognosis.getSequence(), aggregatorDomain,
                                    prognosis.getPeriod().toDateMidnight().toDate(), ptu.getStart().intValue(), power,
                                    previousPtuPrognosis.getPower(), ptu.getPower()));
                }
            }
        }
    }

    /**
     * Gets the latest sequence number in a list of {@link Exchange}.
     *
     * @param exchanges {@link List} of {@link Exchange}.
     * @param participantDomain {@link String} domain name of the correspondent participant.
     * @return a {@link Long} with the highest sequence number;
     */
    private Long getDocumentLatestSequenceNumber(List<? extends Exchange> exchanges, String participantDomain) {
        Long result = -1L;
        for (Exchange exchange : exchanges) {
            if (participantDomain.equals(exchange.getParticipantDomain())) {
                continue;
            }
            if (result.compareTo(exchange.getSequence()) == -1) {
                result = exchange.getSequence();
            }
        }
        return result;
    }

    /**
     * Maps a list of {@link PtuFlexOrder} with their PTU index.
     *
     * @param ptuFlexOrders {@link List} of {@link PtuFlexOrder}.
     * @return a mapping between the PTU index and the list of {@link PtuFlexOrder} for this very PTU index.
     */
    private Map<Integer, List<PtuFlexOrder>> sortAndMapPtuFlexOrders(List<PtuFlexOrder> ptuFlexOrders) {
        Map<Integer, List<PtuFlexOrder>> result = new HashMap<>();
        Integer ptuIndex;
        for (PtuFlexOrder ptuFlexOrder : ptuFlexOrders) {
            ptuIndex = ptuFlexOrder.getPtuContainer().getPtuIndex();
            if (result.get(ptuIndex) == null) {
                result.put(ptuIndex, new ArrayList<>(Collections.singletonList(ptuFlexOrder)));
            } else {
                result.get(ptuIndex).add(ptuFlexOrder);
            }
        }
        return result;
    }

    /**
     * Sums the power of the Flex Orders.
     *
     * @param ptuFlexOrders {@link List} of {@link PtuFlexOrder}.
     * @return the sum of the power.
     */
    private BigInteger sumPowerOfFlexOrders(List<PtuFlexOrder> ptuFlexOrders) {
        BigInteger result = BigInteger.ZERO;
        for (PtuFlexOrder ptuFlexOrder : ptuFlexOrders) {
            if (AcknowledgementStatus.ACCEPTED != ptuFlexOrder.getAcknowledgementStatus()) {
                continue;
            }
            PtuFlexOffer ptuFlexOffer = getRelatedPtuFlexOffer(ptuFlexOrder);
            result = result.add(ptuFlexOffer.getPower());
        }
        return result;
    }

    /**
     * Gets the flex offer related to a flex order on a given PTU.
     *
     * @param ptuFlexOrder {@link PtuFlexOrder}.
     * @return a {@link PtuFlexOffer} or <code>null</code>.
     */
    private PtuFlexOffer getRelatedPtuFlexOffer(PtuFlexOrder ptuFlexOrder) {
        return ptuFlexOfferRepository.findPtuFlexOfferWithSequence(ptuFlexOrder.getFlexOfferSequence(),
                ptuFlexOrder.getParticipantDomain(), ptuFlexOrder.getPtuContainer().getPtuIndex());
    }

    /**
     * In the list of all previous prognoses for all aggregators, this method fetches the one for a given aggregator and a given ptu
     * index.
     *
     * @param lastPtuPrognoses {@link List} of previous {@link PtuPrognosis}.
     * @param aggregatorDomain {@link String} domain name of the aggregator.
     * @param ptuIndex {@link Integer} index of the PTU.
     * @return a {@link PtuPrognosis} or <code>null</code>.
     */
    private PtuPrognosis fetchPreviousPtuPrognosis(List<PtuPrognosis> lastPtuPrognoses, String aggregatorDomain, Integer ptuIndex) {
        for (PtuPrognosis prognosis : lastPtuPrognoses) {
            if (aggregatorDomain.equals(prognosis.getParticipantDomain()) && ptuIndex.equals(
                    prognosis.getPtuContainer().getPtuIndex())) {
                return prognosis;
            }
        }
        return null;
    }

    /**
     * Saves Non Aggregator Connection Forecast.
     *
     * @param congestionPointConnectionGroup the {@link CongestionPointConnectionGroup} of the forecast
     * @param ptuDate PTU date
     * @param power power array
     * @param maxload max load array
     */
    public void saveNonAggregatorConnectionForecast(CongestionPointConnectionGroup congestionPointConnectionGroup,
            LocalDate ptuDate, List<Long> power, List<Long> maxload) {

        int length = power.size();
        if (length == 0) {
            int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);
            length = PtuUtil.getNumberOfPtusPerDay(ptuDate, ptuDuration);
        }
        Map<Integer, PtuContainer> ptuContainers = ptuContainerRepository.findPtuContainersMap(ptuDate);
        for (int i = 0; i < length; i++) {
            Integer ptuIndex = i + 1;
            PtuContainer ptuContainer = ptuContainers.get(ptuIndex);
            if (ptuContainer == null) {
                ptuContainer = new PtuContainer(ptuDate, ptuIndex);
                ptuContainerRepository.persist(ptuContainer);
                LOGGER.trace("Persisted ptuContainer");
            } else {
                LOGGER.trace("Existing ptuContainer");
            }

            if (i < power.size()) {
                LOGGER.trace("Persisted nonAggregatorForecast");
                NonAggregatorForecast nonAggregatorForecast = new NonAggregatorForecast();
                nonAggregatorForecast.setMaxLoad(maxload.get(i));
                nonAggregatorForecast.setPower(power.get(i));
                nonAggregatorForecast.setSequence(sequenceGeneratorService.next());
                nonAggregatorForecast.setPtuContainer(ptuContainer);
                nonAggregatorForecast.setConnectionGroup(congestionPointConnectionGroup);
                nonAggregatorForecast.setCreationDate(ptuDate.toDateMidnight().toDate());
                nonAggregatorForecastRepository.persist(nonAggregatorForecast);
            }

        }

        LOGGER.info("Forecast saved {} PTU's, length = {}", power.size(), length);
    }

    /**
     * Finds all {@link CommonReferenceOperator}s and {@link SynchronisationConnection}s and put them in a Map, all {@link
     * SynchronisationConnection}s are send to all {@link CommonReferenceOperator}s.
     *
     * @return Map with CRO domain mapped to {@link List} of {@link SynchronisationCongestionPoint} objects.
     */
    public Map<String, List<SynchronisationCongestionPoint>> findConnectionsPerCRO() {
        List<SynchronisationCongestionPoint> congestionPoints = synchronisationCongestionPointRepository.findAll();

        List<CommonReferenceOperator> cros = commonReferenceOperatorRepository.findAll();

        Map<String, List<SynchronisationCongestionPoint>> congestionPointsPerCRO = new HashMap<>();

        if (congestionPoints.isEmpty() || cros.isEmpty()) {
            return congestionPointsPerCRO;
        }

        for (CommonReferenceOperator cro : cros) {
            congestionPointsPerCRO.put(cro.getDomain(), congestionPoints);
        }
        return congestionPointsPerCRO;
    }

    /**
     * Updates all CongestionPoints where the synchronisationTime is > as the lastModificationTime for this croDomain.
     *
     * @param congestionPointEntityAddress the entity address ({@link String}) of the congestion point
     * @param croDomain the domain name ({@link String}) of the CRO
     */
    @Transactional(value = TxType.REQUIRES_NEW)
    public void updateCongestionPointStatusForCRO(String congestionPointEntityAddress, String croDomain) {
        synchronisationCongestionPointRepository.updateCongestionPointStatusForCRO(congestionPointEntityAddress, croDomain);
    }

    /**
     * Clean's the synchronization table's if synchronization is complete.
     */
    public void cleanSynchronization() {
        long countModified = synchronisationCongestionPointStatusRepository.countSynchronisationConnectionStatusWithStatus(
                SynchronisationConnectionStatusType.MODIFIED);
        long countDeleted = synchronisationCongestionPointStatusRepository.countSynchronisationConnectionStatusWithStatus(
                SynchronisationConnectionStatusType.DELETED);
        if (countModified + countDeleted == 0L) {
            // everything is synchronized, time to remove everything.
            synchronisationCongestionPointStatusRepository.deleteAll();
            synchronisationConnectionRepository.deleteAll();
            synchronisationCongestionPointRepository.deleteAll();
        }
    }

    /**
     * Updates the aggregaters linked to the congestionPoint.
     *
     * @param xmlCongestionPoint {@link CongestionPoint} for which the aggregator count will be updated.
     * @param initializationDate {@link LocalDate} date of the initializtion of the aggregator count.
     * @param initializationDuration {@link Integer} duration of the initalization.
     */
    public void updateAggregatorsOnCongestionPointConnectionGroup(CongestionPoint xmlCongestionPoint, LocalDate initializationDate,
            Integer initializationDuration) {

        CongestionPointConnectionGroup congestionPoint = congestionPointConnectionGroupRepository.findOrCreate(
                xmlCongestionPoint.getEntityAddress(), config.getProperty(ConfigParam.HOST_DOMAIN));

        List<AggregatorOnConnectionGroupState> endingAtDate = aggregatorOnConnectionGroupStateRepository
                .findEndingAggregatorOnConnectionGroupStates(congestionPoint.getUsefIdentifier(), initializationDate);

        // for each ending aggregator/connection group state for which nothing is updated, extend the valid until date.
        endingAtDate.stream()
                .filter(aocgs -> xmlCongestionPoint.getAggregator()
                        .stream()
                        .anyMatch(aggregator -> aocgs.getConnectionCount().equals(aggregator.getConnectionCount())
                                && aocgs.getAggregator().getDomain().equals(aggregator.getDomain())
                                && aocgs.getCongestionPointConnectionGroup()
                                .getUsefIdentifier()
                                .equals(xmlCongestionPoint.getEntityAddress())))
                .forEach(aocgs -> aocgs.setValidUntil(aocgs.getValidUntil().plusDays(initializationDuration)));

        // create new records if anything changed
        xmlCongestionPoint
                .getAggregator()
                .stream()
                .filter(aggregator -> endingAtDate.stream()
                        .noneMatch(
                                aocgs -> aocgs.getConnectionCount().equals(aggregator.getConnectionCount())
                                        && aocgs.getAggregator()
                                        .getDomain()
                                        .equals(aggregator.getDomain()) && aocgs.getCongestionPointConnectionGroup()
                                        .getUsefIdentifier()
                                        .equals(xmlCongestionPoint.getEntityAddress())))
                .filter(xmlAggregator -> StringUtils.isNotEmpty(xmlAggregator.getDomain()))
                .forEach(xmlAggregator -> {
                    Aggregator dbAggregator = aggregatorRepository.findOrCreate(xmlAggregator.getDomain());
                    AggregatorOnConnectionGroupState newState = new AggregatorOnConnectionGroupState();
                    newState.setAggregator(dbAggregator);
                    newState.setConnectionCount(
                            xmlAggregator.getConnectionCount() == null ?
                                    BigInteger.ZERO :
                                    xmlAggregator.getConnectionCount());
                    newState.setValidFrom(initializationDate);
                    newState.setValidUntil(initializationDate.plusDays(initializationDuration));
                    newState.setCongestionPointConnectionGroup(congestionPoint);
                    aggregatorOnConnectionGroupStateRepository.persist(newState);
                });
    }

    /**
     * Gets last NonAggregatorForecast.
     */
    public NonAggregatorForecast getLastNonAggregatorForecast() {
        return nonAggregatorForecastRepository.getLastNonAggregatorForecast();
    }

    /**
     * Return a list of all MeterDataCompany entities.
     *
     * @return List of MeterDataCompany entities
     */
    public List<MeterDataCompany> findAllMDCs() {
        return meterDataCompanyRepository.findAll();
    }

    /**
     * Finds the current connection count for a specified usef identifier.
     *
     * @param usefIdentifier {@link String} usef identifier.
     * @return a {@link Long}.
     */
    public Long findConnectionCountByUsefIdentifier(String usefIdentifier) {
        return connectionRepository.findConnectionCountByUsefIdentifier(usefIdentifier, DateTimeUtil.getCurrentDate());
    }

    /**
     * Finds a PTU state and create if it does not exist.
     *
     * @param ptuContainer PTU Container
     * @param connectionGroup Connection Group
     * @return PtuState
     */
    @SuppressWarnings("unchecked")
    public PtuState findOrCreatePtuState(PtuContainer ptuContainer, ConnectionGroup connectionGroup) {
        return ptuStateRepository.findOrCreatePtuState(ptuContainer, connectionGroup);
    }

    /**
     * Retreive the limited power at this PTU.
     *
     * @param ptuContainer {@link PtuContainer} the specified PTU (not nullable).
     * @param connectionGroup {@link ConnectionGroup} a congestion point (not nullable).
     * @return an {@link Optional} of {@link Long} limited power for the given PTU and the given congestion point.
     */
    public Optional<Long> findLimitedPower(PtuContainer ptuContainer, ConnectionGroup connectionGroup) {
        return ptuGridMonitorRepository.findLimitedPower(ptuContainer, connectionGroup);
    }

    /**
     * Store the limited power at this PTU.
     *
     * @param ptuContainer
     * @param limitedPower
     * @param connectionGroup
     */
    public void setLimitedPower(PtuContainer ptuContainer, Long limitedPower, ConnectionGroup connectionGroup) {
        ptuGridMonitorRepository.setLimitedPower(ptuContainer, limitedPower, connectionGroup);
    }

    /**
     * Store the actual power at this PTU. If the power already exists, store the median of the current and the new value instead.
     *
     * @param ptuContainer
     * @param actualPower
     * @param connectionGroup
     */
    @SuppressWarnings("unchecked")
    public void setActualPower(PtuContainer ptuContainer, Long actualPower, ConnectionGroup connectionGroup) {
        ptuGridMonitorRepository.setActualPower(ptuContainer, actualPower, connectionGroup);
    }

    /**
     * Finds {@link PtuContainer} entity for given period and a given index. This method should only be called in a OPERATE context
     * and not in a loop. If in a loop, use {@link PtuContainerRepository#findPtuContainersMap(LocalDate)};
     *
     * @param period {@link LocalDate} period.
     * @param ptuIndex {@link Integer} The Ptu Index.
     * @return a {@link PtuContainer}.
     */
    public PtuContainer findPtuContainer(LocalDate period, Integer ptuIndex) {
        return ptuContainerRepository.findPtuContainer(period, ptuIndex);
    }

    /**
     * Finds all {@link CongestionPointConnectionGroup} active for the specific time.
     *
     * @param date date time
     * @return {@link CongestionPointConnectionGroup} list
     */
    public List<CongestionPointConnectionGroup> findActiveCongestionPointConnectionGroup(LocalDate date) {
        return congestionPointConnectionGroupRepository.findActiveCongestionPointConnectionGroup(date);
    }

    /**
     * Gets the entire list of {@link CommonReferenceOperator} known by this Balance Responsible Party.
     *
     * @return {@link List} of {@link CommonReferenceOperator}
     */
    @SuppressWarnings("unchecked")
    public List<CommonReferenceOperator> findAllCommonReferenceOperators() {
        return commonReferenceOperatorRepository.findAll();
    }

    /**
     * This method finds a Connection for a given connection entity address and a certain period.
     *
     * @param entityAddress connection entity address
     * @param date date
     * @return connection if exists, null otherwise
     */
    @SuppressWarnings("unchecked")
    public Connection findConnectionForConnectionMeterEventsPeriod(String entityAddress, LocalDate date) {
        return connectionMeterEventRepository.findConnectionForConnectionMeterEventsPeriod(entityAddress, date);
    }

    /**
     * Store a {@Link ConnectionMeterEvent}.
     *
     * @param entity Connection Meter Event
     */

    public void storeConnectionMeterEvent(ConnectionMeterEvent entity) {
        connectionMeterEventRepository.persist(entity);
    }

    /**
     * Finds connections not related to ConnectionMeterEvents.
     *
     * @param date date
     * @param connectionIncludeList connection include list
     * @return connection list
     */
    public List<Connection> findConnectionsNotRelatedToConnectionMeterEvents(LocalDate date, List<String> connectionIncludeList) {
        return connectionMeterEventRepository.findConnectionsNotRelatedToConnectionMeterEvents(date, connectionIncludeList);
    }

    /**
     * This method finds all ConnectionMeterEvents for a certain period that where active in that period.
     *
     * @param fromDate from date
     * @param endDate end date
     *
     * @return connection meter event list
     */
    @SuppressWarnings("unchecked")
    public List<ConnectionMeterEvent> findConnectionMeterEventsForPeriod(LocalDate fromDate, LocalDate endDate) {
        return connectionMeterEventRepository.findConnectionMeterEventsForPeriod(fromDate, endDate);
    }

    /**
     * Store a {@Link ConnectionCapacityLimitationPeriod}.
     *
     * @param entity Connection Capacity Limitation Period
     */

    public void storeConnectionMeterEventPeriod(ConnectionCapacityLimitationPeriod entity) {
        connectionCapacityLimitationPeriodRepository.persist(entity);
    }

    /**
     * Get Aggregators by CongestionPointAddress.
     *
     * @param congestionPointAddress {@link String} entity address of the congestion point.
     * @param dateTime {@link org.joda.time.LocalDateTime} validity moment of the {@link AggregatorOnConnectionGroupState}.
     * @return list of {@link Aggregator}.
     */
    public List<Aggregator> getAggregatorsByCongestionPointAddress(String congestionPointAddress, LocalDate dateTime) {
        return aggregatorOnConnectionGroupStateRepository.getAggregatorsByCongestionPointAddress(congestionPointAddress, dateTime);
    }

    /**
     * Store a {@Link GridSafetyAnalysis}.
     *
     * @param entity Grid Safety Analysis
     */

    public void storeGridSafetyAnalysis(GridSafetyAnalysis entity) {
        gridSafetyAnalysisRepository.persist(entity);
    }


    /**
     * Finds the {@link GridSafetyAnalysis} entities for a given day.
     *
     * @param congestionPointEntityAddress {@link String} related Congestion Point entity address.
     * @param ptuDate {@link LocalDate} Period of the PTU.
     * @return a {@link List} of {@link GridSafetyAnalysis}.
     */
    @SuppressWarnings("unchecked")
    public List<GridSafetyAnalysis> findGridSafetyAnalysis(String congestionPointEntityAddress, LocalDate ptuDate) {
        return gridSafetyAnalysisRepository.findGridSafetyAnalysis(congestionPointEntityAddress, ptuDate);
    }

    /**
     * Deletes the {@link GridSafetyAnalysis} entities for a given day and congestion point.
     *
     * @param entityAddress {@link String} related Congestion Point entity address.
     * @param period {@link LocalDate} Period of the PTU.
     */
    @SuppressWarnings("unchecked")
    public int deletePreviousGridSafetyAnalysis(String entityAddress, LocalDate period) {
        return gridSafetyAnalysisRepository.deletePreviousGridSafetyAnalysis(entityAddress, period);
    }
}


