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

package energy.usef.core.workflow.settlement;

import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.model.FlexOrderSettlement;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.model.PtuFlexRequest;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.repository.ConnectionGroupRepository;
import energy.usef.core.repository.ConnectionGroupStateRepository;
import energy.usef.core.repository.FlexOrderSettlementRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuFlexOfferRepository;
import energy.usef.core.repository.PtuFlexOrderRepository;
import energy.usef.core.repository.PtuFlexRequestRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.repository.PtuSettlementRepository;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.transformer.SettlementTransformer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.joda.time.LocalDate;

/**
 * Business Service class presenting core operations for Settlement.
 */
@Stateless
public class CoreSettlementBusinessService {

    @Inject
    private PtuContainerRepository ptuContainerRepository;
    @Inject
    private ConnectionGroupStateRepository connectionGroupStateRepository;
    @Inject
    private PlanboardMessageRepository planboardMessageRepository;
    @Inject
    private PtuPrognosisRepository ptuPrognosisRepository;
    @Inject
    private PtuFlexRequestRepository ptuFlexRequestRepository;
    @Inject
    private PtuFlexOfferRepository ptuFlexOfferRepository;
    @Inject
    private PtuFlexOrderRepository ptuFlexOrderRepository;
    @Inject
    private FlexOrderSettlementRepository flexOrderSettlementRepository;
    @Inject
    private PtuSettlementRepository ptuSettlementRepository;
    @Inject
    private ConnectionGroupRepository connectionGroupRepository;
    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * Finds the active connection groups and their connections.
     *
     * @param startDate {@link LocalDate} start date of validity.
     * @param endDate {{@link LocalDate} end date of validity (inclusive).
     * @return a {@link Map} with the connection group as key ({@link ConnectionGroup}) and a {@link List} of {@link Connection} as
     * value.
     */
    public Map<ConnectionGroup, Set<Connection>> findActiveConnectionGroupsWithConnections(LocalDate startDate,
            LocalDate endDate) {
        return connectionGroupStateRepository.findConnectionGroupStatesWithOverlappingValidity(startDate, endDate).stream().collect(
                Collectors.groupingBy(ConnectionGroupState::getConnectionGroup,
                        Collectors.mapping(ConnectionGroupState::getConnection, Collectors.toSet())));
    }

    /**
     * Finds the prognoses relevant for the settlement. Prognoses with a period within the two given dates and with status
     * ACCEPTED, FINAL and ARCHIVED will be returned.
     *
     * @param startDate {@link LocalDate} start date of the settlement period (inclusive).
     * @param endDate {@link LocalDate} end date of the settlement period (inclusive).
     * @return a {@link List} of {@link PtuPrognosis}.
     */
    public List<PtuPrognosis> findRelevantPrognoses(LocalDate startDate, LocalDate endDate) {
        return ptuPrognosisRepository.findPrognosesForSettlement(startDate, endDate);
    }

    /**
     * Finds all flex requests candidate to settlement process (period of the flex request within the interval defined by the two
     * given dates and status ACCEPTED).
     *
     * @param startDate {@link LocalDate} start date of the settlement (inclusive).
     * @param endDate {@link LocalDate} end date of the settlement (inclusive).
     * @return a {@link List} of {@link PtuFlexRequest}.
     */
    public List<PtuFlexRequest> findRelevantFlexRequests(LocalDate startDate, LocalDate endDate) {
        return ptuFlexRequestRepository.findFlexRequestsForSettlement(startDate, endDate);
    }

    /**
     * Finds all accepted flex offers for the period defined by the two given dates.
     *
     * @param startDate {@link LocalDate} start date of the settlement period (inclusive).
     * @param endDate {@link LocalDate} end date of the settlement period (inclusive).
     * @return a {@link List} of {@link PtuFlexOffer}.
     */
    public List<PtuFlexOffer> findRelevantFlexOffers(LocalDate startDate, LocalDate endDate) {
        return ptuFlexOfferRepository.findFlexOffersForSettlement(startDate, endDate);
    }

    /**
     * Finds all acknowledged flex orders for the period defined by the two given dates.
     *
     * @param startDate {@link LocalDate} start date of the settlement period (inclusive).
     * @param endDate {@link LocalDate} end date of the settlement period (inclusive).
     * @return a {@link List} of {@link PtuFlexOrder}.
     */
    public List<PtuFlexOrder> findRelevantFlexOrders(LocalDate startDate, LocalDate endDate) {
        return ptuFlexOrderRepository.findFlexOrdersForSettlement(startDate, endDate);
    }

    /**
     * Stores Flex Order Settlement items in the database.
     *
     * @param flexOrderSettlementDtos a {@link List} of {@link FlexOrderSettlementDto} items, which will be transformed to {@link
     * FlexOrderSettlement} entities.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createFlexOrderSettlements(List<FlexOrderSettlementDto> flexOrderSettlementDtos) {
        Set<LocalDate> periods = flexOrderSettlementDtos.stream()
                .map(FlexOrderSettlementDto::getPeriod)
                .collect(Collectors.toSet());
        Map<LocalDate, Map<Integer, PtuContainer>> ptuContainersPerPeriod = periods.stream().collect(
                Collectors.toMap(Function.identity(), ptuContainerRepository::findPtuContainersMap));
        flexOrderSettlementDtos.stream()
                .map(flexOrderSettlementDto -> transformFlexOrderSettlement(flexOrderSettlementDto,
                        ptuContainersPerPeriod.get(flexOrderSettlementDto.getPeriod())))
                .forEach(flexOrderSettlement -> {
                    flexOrderSettlementRepository.persist(flexOrderSettlement);
                    flexOrderSettlement.getPtuSettlements().forEach(ptuSettlementRepository::persist);
                });
    }

    /**
     * Checks if each accepted flex order in the given month of the given year has a related settlement item in the database.
     *
     * @param year {@link Integer} year.
     * @param month {@link Integer} month.
     * @return <code>true</code> if each ACCEPTED flex order planboard message has a related FlexOrderSettlement entity;
     * <code>false</code> otherwise.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Boolean isEachFlexOrderReadyForSettlement(Integer year, Integer month) {
        return flexOrderSettlementRepository.isEachFlexOrderReadyForSettlement(year, month);
    }

    private FlexOrderSettlement transformFlexOrderSettlement(FlexOrderSettlementDto flexOrderSettlementDto,
            Map<Integer, PtuContainer> ptuContainersPerIndex) {
        final Long sequenceNumber = sequenceGeneratorService.next();
        FlexOrderSettlement flexOrderSettlement = new FlexOrderSettlement();
        flexOrderSettlement.setFlexOrder(planboardMessageRepository.findSinglePlanboardMessage(
                flexOrderSettlementDto.getFlexOrder().getConnectionGroupEntityAddress(),
                flexOrderSettlementDto.getFlexOrder().getSequenceNumber(),
                flexOrderSettlementDto.getFlexOrder().getParticipantDomain()));
        flexOrderSettlement.setSequence(sequenceNumber);
        flexOrderSettlement.setConnectionGroup(
                connectionGroupRepository.find(flexOrderSettlementDto.getFlexOrder().getConnectionGroupEntityAddress()));
        flexOrderSettlement.setPeriod(flexOrderSettlementDto.getPeriod());
        flexOrderSettlement.getPtuSettlements()
                .addAll(flexOrderSettlementDto.getPtuSettlementDtos()
                        .stream()
                        .map(ptuSettlementDto -> SettlementTransformer.transformPtuSettlementDto(ptuSettlementDto,
                                ptuContainersPerIndex.get(ptuSettlementDto.getPtuIndex().intValue()), flexOrderSettlement))
                        .collect(Collectors.toList()));
        return flexOrderSettlement;
    }

    /**
     * Finds the Flex Order Settlement entities for the given period.
     *
     * @param startDate {@link LocalDate} start date of the period (inclusive).
     * @param endDate {@link LocalDate} end date of the period (inclusive).
     * @param connectionGroup {@link Optional} USEF identifier of the connection group of the flex order settlement.
     * @param participantDomain {@link Optional} participant domain of the flex order settlement.
     * @return a {@link List} of {@link FlexOrderSettlement} entities.
     */
    public List<FlexOrderSettlement> findFlexOrderSettlementsForPeriod(LocalDate startDate, LocalDate endDate,
            Optional<String> connectionGroup, Optional<String> participantDomain) {
        return flexOrderSettlementRepository
                .findFlexOrderSettlementsForPeriod(startDate, endDate, connectionGroup, participantDomain);
    }

}
