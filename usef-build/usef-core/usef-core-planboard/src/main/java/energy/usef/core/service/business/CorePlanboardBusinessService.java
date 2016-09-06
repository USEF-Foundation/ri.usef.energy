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
import energy.usef.core.data.xml.bean.message.CongestionPoint;
import energy.usef.core.data.xml.bean.message.Connection;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.data.xml.bean.message.FlexOffer;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.data.xml.bean.message.FlexOrderSettlement;
import energy.usef.core.data.xml.bean.message.FlexRequest;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.SettlementMessage;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PhaseType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
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
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global BusinessService class to help with methods used in multiple roles.
 */
@Stateless
public class CorePlanboardBusinessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CorePlanboardBusinessService.class);

    @Inject
    private AgrConnectionGroupRepository agrConnectionGroupRepository;
    @Inject
    private BrpConnectionGroupRepository brpConnectionGroupRepository;
    @Inject
    private Config config;
    @Inject
    private ConnectionGroupRepository connectionGroupRepository;
    @Inject
    private ConnectionGroupStateRepository connectionGroupStateRepository;
    @Inject
    private CongestionPointConnectionGroupRepository congestionPointConnectionGroupRepository;
    @Inject
    private ConnectionRepository connectionRepository;
    @Inject
    private PlanboardMessageRepository planboardMessageRepository;
    @Inject
    private PtuContainerRepository ptuContainerRepository;
    @Inject
    private PtuFlexOfferRepository ptuFlexOfferRepository;
    @Inject
    private PtuFlexOrderRepository ptuFlexOrderRepository;
    @Inject
    private PtuFlexRequestRepository ptuFlexRequestRepository;
    @Inject
    private PtuPrognosisRepository ptuPrognosisRepository;
    @Inject
    private PtuStateRepository ptuStateRepository;
    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * Store the FlexRequest to all the correct ptu's.
     *
     * @param usefIdentifier The usefIdentifier of the ConnectionGroup.
     * @param flexRequestMessage - The {@link FlexRequest} message.
     * @param initialStatus The status of the flex request which is stored.
     * @param participantDomain - The domain of the party which is being communicated with
     */
    public void storeFlexRequest(String usefIdentifier, FlexRequest flexRequestMessage, DocumentStatus initialStatus,
            String participantDomain) {
        LocalDate period = flexRequestMessage.getPeriod();
        List<PTU> ptus = PtuListConverter.normalize(flexRequestMessage.getPTU());

        ConnectionGroup connectionGroup = connectionGroupRepository.find(usefIdentifier);
        PlanboardMessage planboardMessage = new PlanboardMessage(DocumentType.FLEX_REQUEST, flexRequestMessage.getSequence(),
                initialStatus, participantDomain, period, flexRequestMessage.getPrognosisSequence(), connectionGroup,
                flexRequestMessage.getExpirationDateTime());
        planboardMessageRepository.persist(planboardMessage);

        Map<Integer, PtuContainer> ptuContainers = ptuContainerRepository.findPtuContainersMap(period);
        for (PTU ptu : ptus) {
            PtuContainer ptuContainer = ptuContainers.get(ptu.getStart().intValue());

            PtuFlexRequest ptuFlexRequest = new PtuFlexRequest();
            ptuFlexRequest.setPtuContainer(ptuContainer);
            ptuFlexRequest.setConnectionGroup(connectionGroup);
            ptuFlexRequest.setDisposition(DispositionAvailableRequested.valueOf(ptu.getDisposition().name()));
            ptuFlexRequest.setParticipantDomain(participantDomain);
            ptuFlexRequest.setPower(ptu.getPower() == null ? BigInteger.ZERO : ptu.getPower());
            ptuFlexRequest.setSequence(flexRequestMessage.getSequence());
            ptuFlexRequest.setPrognosisSequence(flexRequestMessage.getPrognosisSequence());
            // persist the flex request
            ptuFlexRequestRepository.persist(ptuFlexRequest);
        }
    }

    /**
     * Store the FlexOffer to all the ptu's in the period.
     *
     * @param usefIdentifier The usefIdentifier of the ConnectionGroup.
     * @param flexOfferRequest The {@link PtuFlexOffer} to be stored in the planboard.
     * @param initialStatus The status of the flex offer which is stored.
     * @param participantDomain - The domain of the party which is being communicated with
     */
    public void storeFlexOffer(String usefIdentifier, FlexOffer flexOfferRequest, DocumentStatus initialStatus,
            String participantDomain) {
        if (flexOfferRequest.getPTU().isEmpty()) {
            // Empty flex offer
            LOGGER.info("Saving an empty flex offer the sequence number: {}", flexOfferRequest.getSequence());
        }

        LocalDate period = flexOfferRequest.getPeriod();
        List<PTU> ptus = PtuListConverter.normalize(flexOfferRequest.getPTU());

        ConnectionGroup connectionGroup = connectionGroupRepository.find(usefIdentifier);
        PlanboardMessage planboardMessage = new PlanboardMessage(DocumentType.FLEX_OFFER, flexOfferRequest.getSequence(),
                initialStatus, participantDomain, period, flexOfferRequest.getFlexRequestSequence(), connectionGroup,
                flexOfferRequest.getExpirationDateTime());
        planboardMessageRepository.persist(planboardMessage);

        Map<Integer, PtuContainer> ptuContainers = ptuContainerRepository.findPtuContainersMap(period);
        for (PTU ptu : ptus) {
            PtuContainer ptuContainer = ptuContainers.get(ptu.getStart().intValue());

            PtuFlexOffer flexOffer = new PtuFlexOffer();
            flexOffer.setPtuContainer(ptuContainer);
            flexOffer.setConnectionGroup(connectionGroup);
            flexOffer.setSequence(flexOfferRequest.getSequence());
            flexOffer.setParticipantDomain(participantDomain);
            flexOffer.setFlexRequestSequence(flexOfferRequest.getFlexRequestSequence());
            flexOffer.setPower(ptu.getPower());
            flexOffer.setPrice(ptu.getPrice());

            ptuFlexOfferRepository.persist(flexOffer);
        }
    }

    /**
     * Store the FlexOrder to all the ptu's in the period.
     *
     * @param usefIdentifier The usefIdentifier of the ConnectionGroup.
     * @param flexOrderMessage The {@link FlexOrder} to be stored in the planboard.
     * @param initialStatus The status of the flex order which is stored.
     * @param participantDomain The domain of the party which is being communicated with
     * @param acknowledgementStatus Acknowledgement status
     * @param state PTU Container state
     */
    public void storeFlexOrder(String usefIdentifier, FlexOrder flexOrderMessage, DocumentStatus initialStatus,
            String participantDomain, AcknowledgementStatus acknowledgementStatus, PtuContainerState state) {
        LocalDate period = flexOrderMessage.getPeriod();
        List<PTU> ptus = PtuListConverter.normalize(flexOrderMessage.getPTU());

        ConnectionGroup connectionGroup = connectionGroupRepository.find(usefIdentifier);
        PlanboardMessage planboardMessage = new PlanboardMessage(DocumentType.FLEX_ORDER, flexOrderMessage.getSequence(),
                initialStatus, participantDomain, period, flexOrderMessage.getFlexOfferSequence(), connectionGroup,
                flexOrderMessage.getExpirationDateTime());
        planboardMessageRepository.persist(planboardMessage);

        Map<Integer, PtuContainer> ptuContainers = ptuContainerRepository.findPtuContainersMap(period);
        for (PTU ptu : ptus) {
            PtuContainer ptuContainer = ptuContainers.get(ptu.getStart().intValue());

            PtuFlexOrder flexOrder = new PtuFlexOrder();
            flexOrder.setPtuContainer(ptuContainer);
            flexOrder.setConnectionGroup(connectionGroup);
            flexOrder.setParticipantDomain(participantDomain);
            flexOrder.setSequence(flexOrderMessage.getSequence());
            flexOrder.setFlexOfferSequence(flexOrderMessage.getFlexOfferSequence());
            flexOrder.setAcknowledgementStatus(acknowledgementStatus);

            ptuFlexOrderRepository.persist(flexOrder);

            if (state != null) {
                PtuState ptuState = ptuStateRepository.findOrCreatePtuState(ptuContainer, connectionGroup);
                ptuState.setState(state);
            }
        }
    }

    /**
     * Store the FlexOrder to all the ptu's in the period.
     *
     * @param usefIdentifier The usefIdentifier of the ConnectionGroup.
     * @param flexOrderMessage The {@link FlexOrder} to be stored in the planboard.
     * @param initialStatus The status of the flex order which is stored.
     * @param participantDomain The domain of the party which is being communicated with
     * @param acknowledgementStatus Acknowledgement status
     */
    public void storeFlexOrder(String usefIdentifier, FlexOrder flexOrderMessage, DocumentStatus initialStatus,
            String participantDomain, AcknowledgementStatus acknowledgementStatus) {
        storeFlexOrder(usefIdentifier, flexOrderMessage, initialStatus, participantDomain, acknowledgementStatus, null);
    }

    /**
     * Stores a new Prognosis in the planboard.
     *
     * @param usefIdentifier The usefIdentifier of the ConnectionGroup.
     * @param prognosisMessage The {@link Prognosis} message which has to be stored in the planboard.
     * @param prognosisType {@link DocumentType} prognosis type
     * @param initialStatus The initial status of the document.
     * @param participantDomain The participant domain as a {@link String}.
     * @param message saved message
     * @param isSubstitute whether or not we are creating a Missing Prognosis.
     * @return The list of stored PtuPrognosis.
     */

    public List<PtuPrognosis> storePrognosis(String usefIdentifier, Prognosis prognosisMessage, DocumentType prognosisType,
            DocumentStatus initialStatus, String participantDomain, Message message, boolean isSubstitute) {
        ConnectionGroup connectionGroup = connectionGroupRepository.find(usefIdentifier);
        return storePrognosis(prognosisMessage, connectionGroup, prognosisType, initialStatus, participantDomain, message,
                isSubstitute);
    }

    /**
     * Stores a new Prognosis in the planboard.
     *
     * @param prognosisMessage The {@link Prognosis} message which has to be stored in the planboard.
     * @param connectionGroup The {ConnectionGroup} connection group
     * @param prognosisType {@link DocumentType} prognosis type
     * @param initialStatus The initial status of the document.
     * @param participantDomain The participant domain as a {@link String}.
     * @param message saved message
     * @param isSubstitute whether or not we are creating a Missing Prognosis.
     */
    public List<PtuPrognosis> storePrognosis(Prognosis prognosisMessage, ConnectionGroup connectionGroup,
            DocumentType prognosisType, DocumentStatus initialStatus, String participantDomain, Message message,
            boolean isSubstitute) {
        LocalDate period = prognosisMessage.getPeriod();
        List<PTU> ptus = PtuListConverter.normalize(prognosisMessage.getPTU());

        PlanboardMessage planboardMessage = new PlanboardMessage(prognosisType, prognosisMessage.getSequence(), initialStatus,
                participantDomain, period, null, connectionGroup, null);
        planboardMessage.setMessage(message);
        planboardMessageRepository.persist(planboardMessage);

        List<PtuPrognosis> storedPrognosis = new ArrayList<>();
        Map<Integer, PtuContainer> ptuContainers = ptuContainerRepository.findPtuContainersMap(period);
        for (PTU ptu : ptus) {
            PtuContainer ptuContainer = ptuContainers.get(ptu.getStart().intValue());
            PtuPrognosis prognosis = new PtuPrognosis();
            prognosis.setPtuContainer(ptuContainer);
            prognosis.setSequence(prognosisMessage.getSequence());
            prognosis.setType(PrognosisType.valueOf(prognosisMessage.getType().name()));
            prognosis.setPower(ptu.getPower());
            prognosis.setParticipantDomain(participantDomain);
            prognosis.setConnectionGroup(connectionGroup);
            prognosis.setSubstitute(isSubstitute);

            ptuPrognosisRepository.persist(prognosis);
            storedPrognosis.add(prognosis);
            ptuStateRepository.findOrCreatePtuState(ptuContainer, connectionGroup);
        }
        return storedPrognosis;
    }

    /**
     * Stores a {@link SettlementMessage} as a {@link PlanboardMessage} entity.
     *
     * @param flexOrderSettlements flex order settlements being sent.
     * @param daysBeforeExpiration Days before expiration
     * @param status status
     * @param participantDomain the domain of the other participant.
     * @param message message
     */
    public void storeFlexOrderSettlementsPlanboardMessage(List<energy.usef.core.model.FlexOrderSettlement> flexOrderSettlements,
            Integer daysBeforeExpiration, DocumentStatus status, String participantDomain, Message message) {
        if (flexOrderSettlements == null || flexOrderSettlements.isEmpty()) {
            return;
        }
        LocalDateTime validUntil = null;
        if (daysBeforeExpiration != null) {
            validUntil = DateTimeUtil.getCurrentDateTime().plusDays(daysBeforeExpiration);
        }
        for (energy.usef.core.model.FlexOrderSettlement flexOrderSettlement : flexOrderSettlements) {
            PlanboardMessage planboardMessage = new PlanboardMessage(DocumentType.FLEX_ORDER_SETTLEMENT,
                    flexOrderSettlement.getSequence(), status, participantDomain, flexOrderSettlement.getPeriod(),
                    flexOrderSettlement.getFlexOrder().getSequence(), flexOrderSettlement.getConnectionGroup(), null);
            planboardMessage.setMessage(message);
            planboardMessage.setExpirationDate(validUntil);
            planboardMessageRepository.persist(planboardMessage);
        }

    }

    /**
     * Stores a {@link SettlementMessage} as a {@link PlanboardMessage} entity.
     *
     * @param settlementMessage Settlement message
     * @param daysBeforeExpiration Days before expiration
     * @param status status
     * @param participantDomain the domain of the other participant.
     * @param message message
     */
    public void storeIncomingFlexOrderSettlementsPlanboardMessage(SettlementMessage settlementMessage, Integer daysBeforeExpiration,
            DocumentStatus status, String participantDomain, Message message) {
        if (settlementMessage == null) {
            return;
        }
        LocalDateTime validUntil = null;
        if (daysBeforeExpiration != null) {
            validUntil = DateTimeUtil.getCurrentDateTime().plusDays(daysBeforeExpiration);
        }
        for (FlexOrderSettlement flexOrderSettlement : settlementMessage.getFlexOrderSettlement()) {
            String usefIdentifier;
            if (USEFRole.BRP == settlementMessage.getMessageMetadata().getSenderRole()) {
                usefIdentifier = participantDomain;
            } else {
                usefIdentifier = flexOrderSettlement.getCongestionPoint();
            }

            // Connection Group can be null in case of dummy settlement (no flex occurred)
            ConnectionGroup connectionGroup = null;
            if (usefIdentifier != null) {
                connectionGroup = connectionGroupRepository.find(usefIdentifier);
            }

            PlanboardMessage planboardMessage = new PlanboardMessage(DocumentType.FLEX_ORDER_SETTLEMENT,
                    sequenceGeneratorService.next(), status, participantDomain, flexOrderSettlement.getPeriod(),
                    Long.valueOf(flexOrderSettlement.getOrderReference()), connectionGroup, null);
            planboardMessage.setMessage(message);
            planboardMessage.setExpirationDate(validUntil);
            planboardMessageRepository.persist(planboardMessage);
        }

    }

    /**
     * Finds the connections for a {@link ConnectionGroup} and a certain point in time.
     *
     * @param usefIdentifier the connection group entity address
     * @param date period ({@link LocalDate})
     * @return A {@link List} of {@link energy.usef.core.model.Connection} objects
     */
    public List<energy.usef.core.model.Connection> findConnectionsForConnectionGroup(String usefIdentifier, LocalDate date) {
        return connectionRepository.findConnectionsForConnectionGroup(usefIdentifier, date);
    }

    /**
     * Find all {@link energy.usef.core.model.Connection} at a given point in time.
     *
     * @param date period ({@link LocalDate})
     * @param connectionEntityList
     * @return A {@link List} of {@link energy.usef.core.model.Connection} objects
     */
    public List<energy.usef.core.model.Connection> findActiveConnections(LocalDate date, Optional<List<String>> connectionEntityList) {
        return connectionRepository.findActiveConnections(date, connectionEntityList);
    }

    /**
     * Finds connections by start/end dates and regimes.
     *
     * @param startDate start date
     * @param endDate end date
     * @param regimes regimes
     * @return connection entity address list
     */
    public Map<ConnectionGroup, List<energy.usef.core.model.Connection>> findConnections(LocalDate startDate, LocalDate endDate, RegimeType... regimes) {
        List<PtuState> ptuStates = ptuStateRepository.findPtuStates(startDate, endDate, regimes);
        List<ConnectionGroupState> connectionGroupStates = connectionGroupStateRepository
                .findActiveConnectionGroupStates(startDate, endDate);
        Map<ConnectionGroup, List<PtuState>> orangePtuStatesMap = ptuStates.stream()
                .collect(Collectors.groupingBy(PtuState::getConnectionGroup));
        return connectionGroupStates.stream().collect(Collectors.groupingBy(ConnectionGroupState::getConnectionGroup)).entrySet()
                .stream()
                .filter(statesPerConnectionGroup -> orangePtuStatesMap.containsKey(statesPerConnectionGroup.getKey()))
                .flatMap(statesPerConnectionGroup -> statesPerConnectionGroup.getValue().stream())
                .filter(connectionGroupState -> orangePtuStatesMap.get(connectionGroupState.getConnectionGroup())
                        .stream()
                        .anyMatch(orangePtuState -> isPtuContainerValidForConnectionGroup(orangePtuState, connectionGroupState)))
                .collect(Collectors.groupingBy(ConnectionGroupState::getConnectionGroup,
                        Collectors.mapping(ConnectionGroupState::getConnection, Collectors.toList())));

    }

    private boolean isPtuContainerValidForConnectionGroup(PtuState ptuState, ConnectionGroupState connectionGroupState) {
        return !connectionGroupState.getValidFrom().isAfter(ptuState.getPtuContainer().getPtuDate()) &&
                !connectionGroupState.getValidUntil().isBefore(ptuState.getPtuContainer().getPtuDate());
    }

    /**
     * Finds the connection groups and their connections having partial or full overlap with the given period.
     *
     * @param startDate {@link LocalDate} start date of the period (inclusive).
     * @param endDate {@link LocalDate} end date of the period (inclusive).
     * @return a {@link Map} of Connection Groups and their Connection per day (for each day of the period).
     */
    public Map<LocalDate, Map<ConnectionGroup, List<energy.usef.core.model.Connection>>> findConnectionGroupWithConnectionsWithOverlappingValidity(
            LocalDate startDate, LocalDate endDate) {
        List<ConnectionGroupState> connectionGroupStates = connectionGroupStateRepository
                .findConnectionGroupStatesWithOverlappingValidity(startDate, endDate);
        Map<LocalDate, Map<ConnectionGroup, List<energy.usef.core.model.Connection>>> result = new HashMap<>();
        DateTimeUtil.generateDatesOfInterval(startDate, endDate).stream()
                .forEach(day -> result.put(day, connectionGroupStates.stream()
                        .filter(cgs -> !cgs.getValidFrom().isAfter(day) && cgs.getValidUntil().isAfter(day))
                        .collect(Collectors.groupingBy(ConnectionGroupState::getConnectionGroup,
                                Collectors.mapping(ConnectionGroupState::getConnection, Collectors.toList())))));
        return result;
    }

    /**
     * Finds plan board messages.
     *
     * @param sequence corresponding document sequence
     * @param documentType document type
     * @param participantDomain {@link String} participant domain.
     * @return plan board messages
     */
    public List<PlanboardMessage> findPlanboardMessages(Long sequence, DocumentType documentType, String participantDomain) {
        return planboardMessageRepository.findPlanboardMessages(sequence, documentType, participantDomain);
    }

    /**
     * Finds plan board messages.
     *
     * @param sequence corresponding document sequence
     * @param congestionPoint the congetion point entity address
     * @param documentType document type
     * @return plan board messages
     */
    public List<PlanboardMessage> findPlanboardMessages(Long sequence, String congestionPoint, DocumentType documentType) {
        return planboardMessageRepository.findPlanboardMessages(sequence, congestionPoint, documentType);
    }

    /**
     * Finds plan board messages.
     *
     * @param sequence corresponding document sequence
     * @param documentType document type
     * @param participantDomain the domain name of the participant
     * @param congestionPoint congestionpoint entity address
     * @param documentStatus document status
     * @return plan board messages
     */
    public List<PlanboardMessage> findPlanboardMessages(Long sequence, DocumentType documentType, String participantDomain,
            String congestionPoint, DocumentStatus... documentStatus) {
        return planboardMessageRepository.findPlanboardMessages(sequence, documentType, participantDomain, congestionPoint,
                documentStatus);
    }

    /**
     * Finds a single plan board messages.
     *
     * @param sequence corresponding document sequence
     * @param documentType document type
     * @param participantDomain - The participantDomain which is being communicated with.
     * @return a single planboard message or <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public PlanboardMessage findSinglePlanboardMessage(Long sequence, DocumentType documentType, String participantDomain) {
        return planboardMessageRepository.findSinglePlanboardMessage(sequence, documentType, participantDomain);
    }

    /**
     * Finds a single plan board message by period.
     *
     * @param period The period
     * @param documentType document type
     * @param participantDomain - The participantDomain which is being communicated with.
     * @return a single planboard message or <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public PlanboardMessage findSinglePlanboardMessage(LocalDate period, DocumentType documentType, String participantDomain) {
        return planboardMessageRepository.findSinglePlanboardMessage(period, documentType, participantDomain);
    }


    /**
     * Find plan board messages.
     *
     * @param type document type of the message
     * @param period period
     * @param documentStatus document status
     * @return plan board message list
     */
    public List<PlanboardMessage> findPlanboardMessages(DocumentType type, LocalDate period, DocumentStatus documentStatus) {
        return planboardMessageRepository.findPlanboardMessages(type, period, documentStatus);
    }

    /**
     * Find all A-Plans and D-Prognosis PlanboardMessages for a given date.
     *
     * @param period period ({@link LocalDate})
     * @param connectionGroupIdentifier the connection group identifier
     * @return A {@link List} of {@link PlanboardMessage} objects
     */
    public List<PlanboardMessage> findPrognosismessagesForDate(LocalDate period, String connectionGroupIdentifier) {
        return planboardMessageRepository.findPrognosisRelevantForDate(period, connectionGroupIdentifier);
    }

    /**
     * Find plan board messages.
     *
     * @param usefIdentifier The usefIdentifier of the ConnectionGroup
     * @param participantDomain The participant.
     * @param type document type of the message
     * @param period period
     * @param documentStatus document status
     * @return plan board message list
     */
    public List<PlanboardMessage> findPlanboardMessagesForConnectionGroup(String usefIdentifier, String participantDomain,
            DocumentType type, LocalDate period, DocumentStatus documentStatus) {
        return planboardMessageRepository.findPlanboardMessages(type, participantDomain, usefIdentifier, documentStatus, period,
                period);
    }

    /**
     * Find plan board messages.
     *
     * @param type document type of the message
     * @param startDate start date
     * @param endDate end date
     * @param documentStatus document status
     * @return plan board message list
     */
    public List<PlanboardMessage> findPlanboardMessages(DocumentType type, LocalDate startDate, LocalDate endDate,
            DocumentStatus documentStatus) {
        return planboardMessageRepository.findPlanboardMessages(type, startDate, endDate, documentStatus);
    }

    /**
     * Finds a planboard message by an origin sequence.
     *
     * @param originSequence {@link Long} origin sequence number
     * @param type {@link DocumentType}
     * @param documentStatus {@link DocumentStatus}
     * @return pland board message
     */
    public PlanboardMessage findPlanboardMessageByOrigin(Long originSequence, DocumentType type, DocumentStatus documentStatus) {
        return planboardMessageRepository.findPlanboardMessagesWithOriginSequence(originSequence, type, documentStatus);
    }

    /**
     * Find all the planboard messages of a given type for a given participant with the specified origin sequence number.
     *
     * @param originSequence {@link Long} Origin Sequence Number.
     * @param documentType {@link DocumentType} document type of the planboard message.
     * @param participantDomain {@link String} participant domain.
     * @return a {@link List} of {@link PlanboardMessage}.
     */
    @SuppressWarnings("unchecked")
    public List<PlanboardMessage> findPlanboardMessagesWithOriginSequence(Long originSequence, DocumentType documentType,
            String participantDomain) {
        return planboardMessageRepository.findPlanboardMessagesWithOriginSequence(originSequence, documentType, participantDomain);
    }

    /**
     * This method finds {@link PlanboardMessage} based on {@link DocumentType} and {@link DocumentStatus}.
     *
     * @param localDateTime The LocalDateTime the message should be before.
     * @param documentType The type of document, like request, offer or order.
     * @param documentStatus The status of document, like new, submitted or rejected.
     * @return The list of {@link PlanboardMessage} which have a specific {@link DocumentType} and {@link DocumentStatus}.
     */
    public List<PlanboardMessage> findPlanboardMessagesOlderThan(LocalDateTime localDateTime, DocumentType documentType,
            DocumentStatus documentStatus) {
        return planboardMessageRepository.findPlanboardMessagesOlderThan(localDateTime, documentType, documentStatus);
    }

    /**
     * Finds all the active connection groups at given moment.
     *
     * @param date {@link org.joda.time.LocalDate}
     * @param connectionGroupType {@link Class} optional type of {@link ConnectionGroup} wanted
     * @return a {@link java.util.List} of {@link ConnectionGroupState}
     */
    public List<ConnectionGroupState> findActiveConnectionGroupStates(LocalDate date, Class<? extends
            ConnectionGroup> connectionGroupType) {
        return connectionGroupStateRepository.findActiveConnectionGroupStatesOfType(date, connectionGroupType);
    }

    /**
     * Find or create the PTU containers for a given period. If some ptu containers already exist in the database, nothing new will
     * be created (even though such a situation should not happen).
     *
     * @param period {@link LocalDate} period.
     * @return a {@link List} of {@link PtuContainer}.
     */
    public List<PtuContainer> findOrCreatePtuContainersForPeriod(LocalDate period) {
        List<PtuContainer> ptuContainers = new ArrayList<>(ptuContainerRepository.findPtuContainersMap(period).values());
        if (ptuContainers.isEmpty()) {
            for (int i = 1; i <= PtuUtil.getNumberOfPtusPerDay(period, config.getIntegerProperty(ConfigParam.PTU_DURATION)); ++i) {
                PtuContainer ptuContainer = new PtuContainer();
                ptuContainer.setPhase(PhaseType.Plan);
                ptuContainer.setPtuDate(period);
                ptuContainer.setPtuIndex(i);
                ptuContainerRepository.persist(ptuContainer);
                ptuContainers.add(ptuContainer);
            }
        }
        return ptuContainers;
    }

    /**
     * This will update the phase of all the {@link PtuContainer} entities with the given period. The new {@link PtuContainer#phase}
     * will be 'Validate'.
     *
     * @param period {@link LocalDate} date of change.
     * @return boolean
     */
    public boolean processMoveToValidateEvent(LocalDate period) {
        List<PlanboardMessage> lastAPlanMessages = planboardMessageRepository.findLastAPlanPlanboardMessages(period);
        List<PlanboardMessage> lastApprovedAPlanMessages = lastAPlanMessages.stream()
                .filter(aPlanMessage -> DocumentStatus.ACCEPTED == aPlanMessage.getDocumentStatus()
                        || DocumentStatus.FINAL == aPlanMessage.getDocumentStatus())
                .collect(Collectors.toList());

        List<BrpConnectionGroup> brpConnectionGroups = brpConnectionGroupRepository.findActiveBrpConnectionGroups(period);

        if (brpConnectionGroups.size() == lastApprovedAPlanMessages.size()) {
            LOGGER.debug("All A-Plans are approved, move to Validate phase");
            ptuContainerRepository.updatePtuContainersPhase(PhaseType.Validate, period, null);
            return true;
        }
        return false;
    }

    /**
     * This will update the phase of all the {@link PtuContainer} entities with the given period. The new {@link PtuContainer#phase}
     * will be 'Plan'.
     *
     * @param period {@link LocalDate} date of change.
     */
    public void processBackToPlanEvent(LocalDate period) {
        if (ptuContainerRepository.findPtuContainers(period, PhaseType.Operate, PhaseType.Settlement).isEmpty()) {
            LOGGER.info("Moving back to the Plan phase for the period {}", period);
            ptuContainerRepository.updatePtuContainersPhase(PhaseType.Plan, period, null);
        } else {
            LOGGER.warn("Can not move back to the Plan phase for the period {}, PTUs are in Operate or Settlement phases", period);
        }
    }

    /**
     * This will update the phase of all the {@link PtuContainer} entities with the given period. The new phase will be {@link
     * PtuContainerState#DayAheadClosedValidate}.
     *
     * @param period {@link LocalDate} date of change.
     */
    public void processDayAheadClosureEvent(LocalDate period) {
        ptuContainerRepository.updatePtuContainersState(PtuContainerState.DayAheadClosedValidate, period, null);
    }

    /**
     * This will update the phase of all the {@link PtuContainer} entities with the given period and ptu index. The new phase will
     * be {@link PtuContainerState#IntraDayClosedValidate}.
     *
     * @param period {@link LocalDate} date of change.
     * @param ptuIndex {@link Integer} index of the PTU.
     */
    public void processIntraDayClosureEvent(LocalDate period, Integer ptuIndex) {
        ptuContainerRepository.updatePtuContainersState(PtuContainerState.IntraDayClosedValidate, period, ptuIndex);
    }

    /**
     * This will update the phase of all the {@link PtuContainer} entities with the given period and given ptu index. The new phase
     * will be {@link PtuContainerState#Operate} for the specified {@link PtuContainer}. The previous {@link PtuContainer} will be
     * set to {@link PtuContainerState#PendingSettlement}.
     *
     * @param period {@link LocalDate} date of change.
     * @param ptuIndex {@link Integer} index of the PTU.
     */
    public void processMoveToOperateEvent(LocalDate period, Integer ptuIndex) {
        ptuContainerRepository.updatePtuContainersState(PtuContainerState.Operate, period, ptuIndex);
        ptuContainerRepository.updatePtuContainersPhase(PhaseType.Operate, period, ptuIndex);
        LocalDate pendingSettlementDate = ptuIndex == 1 ? period.minusDays(1) : period;
        Integer pendingSettlementPtuIndex = ptuIndex == 1 ?
                PtuUtil.getNumberOfPtusPerDay(pendingSettlementDate, config.getIntegerProperty(ConfigParam.PTU_DURATION)) :
                ptuIndex - 1;
        LOGGER.info("PTU Container with period={} and ptu index={} will move to Pending_Settlement.", pendingSettlementDate,
                pendingSettlementPtuIndex);
        ptuContainerRepository.updatePtuContainersState(PtuContainerState.PendingSettlement, pendingSettlementDate,
                pendingSettlementPtuIndex);
        ptuContainerRepository.updatePtuContainersPhase(PhaseType.Settlement, pendingSettlementDate, pendingSettlementPtuIndex);
    }

    /**
     * Find the connectionGroup based on the UsefIdentifier.
     *
     * @param usefIdentifier the identifier for the {@link ConnectionGroup}
     * @return the {@link ConnectionGroup} matching the usefIdentifier specified
     */
    public ConnectionGroup findConnectionGroup(String usefIdentifier) {
        return connectionGroupRepository.find(usefIdentifier);
    }

    /**
     * Finds all the ConnectionGroups related to the connectionAdresses for a specific time.
     *
     * @param connectionAdresses {@link List} of connection entity addresses {@link String}
     * @param period {@link LocalDate} validity period.
     * @return A {@link List} of {@link ConnectionGroup} objects
     */
    public List<ConnectionGroup> findConnectionGroupsWithConnections(List<String> connectionAdresses, LocalDate period) {
        List<ConnectionGroup> connectionGroups;
        if (connectionAdresses.isEmpty()) {
            connectionGroups = connectionGroupRepository.findAllForDateTime(period);
        } else {
            connectionGroups = connectionGroupRepository.findConnectionGroupsWithConnections(connectionAdresses, period);
        }
        return connectionGroups;
    }

    /**
     * Finds the list of connection entities for each given connection group usef identifier at the given period.
     *
     * @param connectionGroupIdentifiers {@link List} of {@link String} which are the USEF identifiers of the ConnectionGroups.
     * @param period {@link LocalDate} period of validity of the relationship between the connection and the connection group.
     * @return a {@link Map} with {@link ConnectionGroup} as key and a {@link List} of {@link energy.usef.core.model.Connection} as value.
     */
    public Map<ConnectionGroup, List<energy.usef.core.model.Connection>> findConnectionsWithConnectionGroups(List<String> connectionGroupIdentifiers,
            LocalDate period) {
        return connectionGroupStateRepository.findConnectionsWithConnectionGroups(connectionGroupIdentifiers, period);
    }

    /**
     * Finds the active connection groups and their connections.
     *
     * @param period {@link LocalDate} period of validity.
     * @return a {@link Map} with the connection group as key ({@link ConnectionGroup}) and a {@link List} of {@link energy.usef.core.model.Connection} as
     * value.
     */
    public Map<ConnectionGroup, List<energy.usef.core.model.Connection>> findActiveConnectionGroupsWithConnections(LocalDate period) {
        return findActiveConnectionGroupsWithConnections(period, period);
    }

    /**
     * Finds all active connection groups and their connections and returns them in a Map where the connection group identifier
     * ({@link String}) is mapped to a list of connection entity addresses ({@link String}).
     *
     * @param period the period {@link LocalDate}
     * @return Map with connection group identifier ({@link String}) is mapped to a list of connection entity addresses ({@link
     * String}).
     */
    public Map<String, List<String>> buildConnectionGroupsToConnectionsMap(LocalDate period) {
        Map<String, List<String>> connectionGroupToConnectionsMap = new HashMap<>();

        findActiveConnectionGroupsWithConnections(period).forEach(
                (connectionGroup, connectionList) -> connectionGroupToConnectionsMap.put(connectionGroup.getUsefIdentifier(),
                        connectionList.stream().map(energy.usef.core.model.Connection::getEntityAddress).collect(Collectors.toList())));

        return connectionGroupToConnectionsMap;
    }

    /**
     * Finds the active connection groups and their connections.
     *
     * @param startDate {@link LocalDate} start date of validity.
     * @param endDate {{@link LocalDate} end date of validity (inclusive).
     * @return a {@link Map} with the connection group as key ({@link ConnectionGroup}) and a {@link List} of {@link energy.usef.core.model.Connection} as
     * value.
     */
    public Map<ConnectionGroup, List<energy.usef.core.model.Connection>> findActiveConnectionGroupsWithConnections(LocalDate startDate,
            LocalDate endDate) {
        return connectionGroupStateRepository.findActiveConnectionGroupsWithConnections(startDate, endDate);
    }

    /**
     * Returns the list of dates (present and future, not past) for which the planboard has been initialized (i.e. for which ptu
     * containers exist).
     *
     * @return {@link List} of {@link LocalDate}.
     */
    public List<LocalDate> findInitializedDaysOfPlanboard() {
        return ptuContainerRepository.findInitializedDaysOfPlanboard();
    }

    /**
     * Update prognosis status.
     *
     * @param prognosisSequence {@link Long} prognosis sequence
     * @param participantDomain {@link String} participant domain
     * @param prognosisType {@link DocumentType} prognosis type
     * @param status {@link DocumentStatus} status
     * @return updated item count
     */
    public int updatePrognosisStatus(Long prognosisSequence, String participantDomain, DocumentType prognosisType,
            DocumentStatus status) {
        LOGGER.info("Updating planboard messages.");
        List<PlanboardMessage> planboardMessages = findPlanboardMessages(prognosisSequence, prognosisType, participantDomain);
        return updatePlanboardMessageStatus(planboardMessages, status);
    }

    /**
     * Finalize A-Plans.
     *
     * @param period {@link LocalDate} period
     */
    public void finalizeAPlans(LocalDate period) {
        LOGGER.info("Finalizing A-Plans for {} with status SENT or PENDING_FLEX_TRADING.", period);
        updatePlanboardMessageStatus(findPlanboardMessages(DocumentType.A_PLAN, period, null).stream()
                        .filter(a -> a.getDocumentStatus() == DocumentStatus.SENT
                                || a.getDocumentStatus() == DocumentStatus.PENDING_FLEX_TRADING).collect(Collectors.toList()),
                DocumentStatus.FINAL);
    }

    /**
     * Archive A-Plans.
     *
     * @param usefIdentifier {@link String} usefIdentifier
     * @param period {@link LocalDate} period
     */
    public void archiveAPlans(String usefIdentifier, LocalDate period) {
        LOGGER.info("Archiving A-Plans from {} on {} with status {} or {}.", usefIdentifier, period, DocumentStatus.ACCEPTED,
                DocumentStatus.PENDING_FLEX_TRADING);
        findPlanboardMessages(DocumentType.A_PLAN, period, null).stream()
                .filter(a -> a.getParticipantDomain().equals(usefIdentifier))
                .filter(a -> a.getDocumentStatus() == DocumentStatus.ACCEPTED
                        || a.getDocumentStatus() == DocumentStatus.PENDING_FLEX_TRADING)
                .forEach(a -> a.setDocumentStatus(DocumentStatus.ARCHIVED));
    }


    /**
     * Returns the last prognosis of type {@link PrognosisType} for period, usefIdentifier and documentstatus {@link
     * DocumentStatus}.
     *
     * @param period the period {@link LocalDate}
     * @param type {@link PrognosisType}
     * @param usefIdentifier the usefIdentifier
     * @param documentStatus the requested {@link DocumentStatus} of the prognosis.
     * @return a {@link List} of {@link PtuPrognosis} objects.
     */
    public List<PtuPrognosis> findLastPrognoses(LocalDate period, PrognosisType type, String usefIdentifier,
            DocumentStatus documentStatus) {
        return ptuPrognosisRepository
                .findLastPrognoses(period, Optional.of(type), Optional.of(usefIdentifier), Optional.of(documentStatus));
    }

    /**
     * Returns the last prognosis of type {@link PrognosisType} for period and usefIdentifier.
     *
     * @param period the period {@link LocalDate}
     * @param type {@link PrognosisType}
     * @param usefIdentifier the usefIdentifier
     * @return a {@link List} of {@link PtuPrognosis} objects.
     */
    public List<PtuPrognosis> findLastPrognoses(LocalDate period, PrognosisType type, String usefIdentifier) {
        return ptuPrognosisRepository.findLastPrognoses(period, Optional.of(type), Optional.of(usefIdentifier), Optional.empty());
    }

    /**
     * Returns the last prognosis of type {@link PrognosisType} for period and documentStatus {@link DocumentStatus}.
     *
     * @param period the period {@link LocalDate}
     * @param type {@link PrognosisType}
     * @param documentStatus the requested {@link DocumentStatus} of the prognosis.
     * @return a {@link List} of {@link PtuPrognosis} objects.
     */
    public List<PtuPrognosis> findLastPrognoses(LocalDate period, PrognosisType type, DocumentStatus documentStatus) {
        return ptuPrognosisRepository.findLastPrognoses(period, Optional.of(type), Optional.empty(), Optional.of(documentStatus));
    }

    /**
     * Returns the last prognosis of type {@link PrognosisType} for period and usefIdentifier.
     *
     * @param period the period {@link LocalDate}
     * @param type {@link PrognosisType}
     * @return a {@link List} of {@link PtuPrognosis} objects.
     */
    public List<PtuPrognosis> findLastPrognoses(LocalDate period, PrognosisType type) {
        return ptuPrognosisRepository.findLastPrognoses(period, Optional.of(type), Optional.empty(), Optional.empty());
    }

    /**
     * Returns the last prognosis of any type {@link PrognosisType} for specified period.
     *
     * @param period the period {@link LocalDate}
     * @return a {@link List} of {@link PtuPrognosis} objects.
     */
    public List<PtuPrognosis> findLastPrognoses(LocalDate period) {
        return ptuPrognosisRepository.findLastPrognoses(period, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Returns the last prognosis of any type {@link PrognosisType} for a specified period and usefIdentifier.
     *
     * @param period the period {@link LocalDate}
     * @param usefIdentifier the usefIdentifier
     * @return a {@link List} of {@link PtuPrognosis} objects.
     */
    public List<PtuPrognosis> findLastPrognoses(LocalDate period, String usefIdentifier) {
        return ptuPrognosisRepository.findLastPrognoses(period, Optional.empty(), Optional.of(usefIdentifier), Optional.empty());
    }

    /**
     * Finds the accepted prognoses for the given period for the given connection group.
     *
     * @param prognosisType {@link DocumentType} type of prognosis: {@link DocumentType#D_PROGNOSIS} or {@link
     * DocumentType#A_PLAN}.
     * @param period {@link LocalDate} period of the message.
     * @param connectionGroupIdentifier {@link String} USEF identifier of the connection group.
     * @return a {@link List} of {@link PlanboardMessage}.
     */
    public List<PlanboardMessage> findAcceptedPrognosisMessages(DocumentType prognosisType, LocalDate period,
            String connectionGroupIdentifier) {
        return planboardMessageRepository.findAcceptedPlanboardMessagesForConnectionGroup(prognosisType, period,
                connectionGroupIdentifier);
    }

    private int updatePlanboardMessageStatus(List<PlanboardMessage> planboardMessages, DocumentStatus status) {
        int updatedCount = 0;
        if (planboardMessages == null || planboardMessages.isEmpty()) {
            return updatedCount;
        }
        for (PlanboardMessage planboardMessage : planboardMessages) {
            // Updating plan board
            updatePlanboardMessageStatus(planboardMessage, status);

            // Updating PTU State
            updatePrognosisPtuState(planboardMessage, status);
        }
        return planboardMessages.size();
    }

    private void updatePlanboardMessageStatus(PlanboardMessage planboardMessage, DocumentStatus status) {
        // If the planboard message status is: ACCEPTED, REJECTED, FINAL nothing to do
        if (DocumentStatus.SENT == planboardMessage.getDocumentStatus()
                || DocumentStatus.PENDING_FLEX_TRADING == planboardMessage.getDocumentStatus()) {
            planboardMessage.setDocumentStatus(status);
        }
    }

    private void updatePrognosisPtuState(PlanboardMessage aPlanMessage, DocumentStatus status) {
        if (DocumentType.A_PLAN == aPlanMessage.getDocumentType() && (DocumentStatus.ACCEPTED == status
                || DocumentStatus.FINAL == status)) {
            List<PtuState> ptuStates = ptuStateRepository.findPtuStates(aPlanMessage.getPeriod(),
                    aPlanMessage.getParticipantDomain());
            ptuStates.stream()
                    .filter(ptuState -> PtuContainerState.PlanValidate == ptuState.getState())
                    .forEach(ptuState -> ptuState.setState(PtuContainerState.DayAheadClosedValidate));
        }
    }

    /**
     * Stores the connections if response is succesfull.
     *
     * @param message message
     * @param type type
     * @param initializationDate the date to initialize {@link LocalDate}
     * @param validityDuration {@link Integer} amount days of validity of the common reference query response (used to close the
     * valid_until date of the created entities).
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void storeCommonReferenceQueryResponse(CommonReferenceQueryResponse message, CommonReferenceEntityType type,
            LocalDate initializationDate, Integer validityDuration) {
        if (DispositionSuccessFailure.FAILURE.equals(message.getResult())) {
            return;
        }

        if (initializationDate == null) {
            throw new TechnicalException("The date of the initialization of the planboard is null. This cannot happen.");
        }
        if (validityDuration == null || validityDuration < 1) {
            throw new TechnicalException("The initialization duration of the planboard must be specified and greater than 0.");
        }
        // impossible to have both the connections and the congestion points in the same message.
        if (type == CommonReferenceEntityType.CONGESTION_POINT) {
            storeCongestionPoints(message, initializationDate, validityDuration);
        } else {
            storeConnections(message, message.getMessageMetadata().getRecipientRole(), initializationDate, validityDuration);
        }
    }

    /*
     * Stores the congestion points into the CongestionPointConnectionGroups.
     */
    private void storeCongestionPoints(CommonReferenceQueryResponse message, LocalDate initializationDate,
            Integer validityDuration) {

        Map<String, CongestionPoint> congestionPointToCongestionPoint = message.getCongestionPoint()
                .stream()
                .collect(Collectors.toMap(CongestionPoint::getEntityAddress, Function.identity()));

        Map<String, List<String>> congestionPointToConnections = message.getCongestionPoint()
                .stream()
                .collect(Collectors.toMap(CongestionPoint::getEntityAddress, cp -> cp.getConnection()
                        .stream()
                        .map(Connection::getEntityAddress)
                        .collect(Collectors.toList())));

        List<ConnectionGroupState> activeConnectionGroupStates = connectionGroupStateRepository
                .findActiveConnectionGroupStatesOfType(
                        initializationDate, CongestionPointConnectionGroup.class);
        List<ConnectionGroupState> endingConnectionGroupStates = connectionGroupStateRepository.findEndingConnectionGroupStates(
                initializationDate, CongestionPointConnectionGroup.class);

        /*
         * get the list of connection group states which can be extended. They must have the same data has the incoming information
         * and no active state can be open at the specified date.
         */
        List<ConnectionGroupState> connectionGroupStatesToExtend = endingConnectionGroupStates.stream()
                .filter(cgs -> congestionPointToConnections.keySet().contains(cgs.getConnectionGroup().getUsefIdentifier()))
                .filter(cgs -> congestionPointToConnections.get(cgs.getConnectionGroup().getUsefIdentifier())
                        .stream()
                        .anyMatch(entityAddress -> entityAddress.equals(cgs.getConnection().getEntityAddress())))
                .collect(Collectors.toList());

        // extends the validity of an ending ConnectionGroupState if data is unchanged in the CRQR
        connectionGroupStatesToExtend.stream().forEach(cgs -> {
            LOGGER.debug("Extending VALID_UNTIL of [{}/{}] from [{}] to [{}].", cgs.getConnectionGroup().getUsefIdentifier(),
                    cgs.getConnection().getEntityAddress(), cgs.getValidUntil(), cgs.getValidUntil().plusDays(validityDuration));
            cgs.setValidUntil(cgs.getValidUntil().plusDays(validityDuration));
        });

        /*
         * for each incoming congestion-point/connection pair which is not in the ending ConnectionGroupStates and which is not in
         * the active connection group states
         */
        congestionPointToConnections.entrySet().forEach(entry -> entry.getValue().stream()
                .filter(connection -> connectionGroupStatesToExtend.stream().noneMatch(
                        cgs -> cgs.getConnection().getEntityAddress().equals(connection) && cgs.getConnectionGroup()
                                .getUsefIdentifier().equals(entry.getKey())))
                .filter(connection -> activeConnectionGroupStates.stream().noneMatch(
                        activeCgs -> activeCgs.getConnectionGroup().getUsefIdentifier().equals(entry.getKey()) && activeCgs
                                .getConnection().getEntityAddress().equals(connection))).forEach(
                        connection -> createConnectionGroupState(connection, entry.getKey(), congestionPointToCongestionPoint,
                                initializationDate, validityDuration)));
    }

    private void createConnectionGroupState(String connection, String congestionPoint,
            Map<String, CongestionPoint> congestionPointToCongestionPoint, LocalDate initializationDate, Integer validityDuration) {
        LOGGER.debug("Creating new ConnectionGroupState for [{}/{}] from [{}] until [{}].", congestionPoint, connection,
                initializationDate, initializationDate.plusDays(validityDuration));
        createGroupState(congestionPointConnectionGroupRepository
                        .findOrCreate(congestionPoint, congestionPointToCongestionPoint.get(congestionPoint).getDSODomain()),
                connection, initializationDate, validityDuration);
    }

    /*
     * Stores the connection into the AgrConnectionGroups and BrpConnectionGroups
     */
    private void storeConnections(CommonReferenceQueryResponse message, USEFRole selfRole, LocalDate initializationDate,
            Integer validityDuration) {
        // declaration of the function which uses the right ConnectionGroupRepository to find or create ConnectionGroup
        final Function<String, ConnectionGroup> connectionGroupFinder = (USEFRole.AGR == selfRole) ?
                brpConnectionGroupRepository::findOrCreate :
                agrConnectionGroupRepository::findOrCreate;

        final Function<USEFRole, Class<? extends ConnectionGroup>> connectionGroupClassFinder = usefRole -> (usefRole
                == USEFRole.AGR) ? BrpConnectionGroup.class : AgrConnectionGroup.class;

        // declaration of the function which uses the AGR domain or the BRP domain of the connection to determine the usef
        // identifier of the ConnectionGroup.
        final Function<energy.usef.core.data.xml.bean.message.Connection, String> usefIdentifierProvider = USEFRole.AGR == selfRole ?
                Connection::getBRPDomain :
                Connection::getAGRDomain;

        List<ConnectionGroupState> currentConnectionGroupStates = connectionGroupStateRepository
                .findActiveConnectionGroupStatesOfType(
                        initializationDate, connectionGroupClassFinder.apply(selfRole));

        Map<String, energy.usef.core.data.xml.bean.message.Connection> messageConnections = message.getConnection()
                .stream()
                .collect(Collectors.toMap(Connection::getEntityAddress, Function.identity()));

        List<ConnectionGroupState> connectionGroupStatesToExtend = connectionGroupStateRepository
                .findEndingConnectionGroupStates(
                        initializationDate, selfRole == USEFRole.AGR ? BrpConnectionGroup.class : AgrConnectionGroup.class)
                .stream()
                .filter(cgs -> messageConnections
                        .entrySet()
                        .stream()
                        .anyMatch(
                                entry -> entry.getKey().equals(cgs.getConnection().getEntityAddress()) && cgs.getConnectionGroup()
                                        .getUsefIdentifier()
                                        .equals(usefIdentifierProvider.apply(entry.getValue()))))
                .collect(Collectors.toList());

        // extend the connection group states to extend
        connectionGroupStatesToExtend.stream().forEach(cgs -> {
            LOGGER.debug("Extending VALID_UNTIL of [{}/{}] from [{}] to [{}].", cgs.getConnectionGroup().getUsefIdentifier(),
                    cgs.getConnection().getEntityAddress(), cgs.getValidUntil(), cgs.getValidUntil().plusDays(validityDuration));
            cgs.setValidUntil(cgs.getValidUntil().plusDays(validityDuration));
        });

        // create new records for connections of the message that are not part of the connection groups to extend.
        messageConnections.entrySet()
                .stream()
                .filter(entry -> connectionGroupStatesToExtend.stream()
                        .noneMatch(cgs -> cgs.getConnection().getEntityAddress().equals(entry.getKey()) && cgs.getConnectionGroup()
                                .getUsefIdentifier()
                                .equals(usefIdentifierProvider.apply(entry.getValue()))))
                .filter(entry -> currentConnectionGroupStates.stream()
                        .noneMatch(cgs -> cgs.getConnection().getEntityAddress().equals(entry.getKey())))
                .forEach(entry -> {
                    LOGGER.trace("Creating new ConnectionGroupState for [{}/{}] valid from [{}] until [{}].",
                            usefIdentifierProvider.apply(entry.getValue()), entry.getKey(), initializationDate,
                            initializationDate.plusDays(validityDuration));
                    createGroupState(connectionGroupFinder.apply(usefIdentifierProvider.apply(entry.getValue())), entry.getKey(),
                            initializationDate, validityDuration);
                });
    }

    private void createGroupState(ConnectionGroup connectionGroup, String connectionEntityAddress, LocalDate modificationDate,
            Integer validityDuration) {
        // find or create Connection.
        energy.usef.core.model.Connection connection = connectionRepository.findOrCreate(connectionEntityAddress);

        // create new state
        ConnectionGroupState newConnectionGroupState = new ConnectionGroupState();
        newConnectionGroupState.setConnection(connection);
        newConnectionGroupState.setConnectionGroup(connectionGroup);
        newConnectionGroupState.setValidFrom(modificationDate);
        newConnectionGroupState.setValidUntil(modificationDate.plusDays(validityDuration));
        connectionGroupStateRepository.persist(newConnectionGroupState);
    }

    /**
     * Find and return a Map of the PtuFlexOffer based on a the sequence, the domain and the period..
     *
     * @param flexOfferSequence The sequence of the flex Offer.
     * @param domain The Domain of the other participant
     * @return ptuFlexOrder that corresponds with given PTU of flexOrder
     */
    public Map<Integer, PtuFlexOffer> findPtuFlexOffer(Long flexOfferSequence, String domain) {
        return ptuFlexOfferRepository.findPtuFlexOffer(flexOfferSequence, domain);
    }

    /**
     * Update given planboard message.
     *
     * @param planboardMessage the planboard message to update
     */
    public void updatePlanboardMessage(PlanboardMessage planboardMessage) {
        planboardMessageRepository.persist(planboardMessage);
    }

    /**
     * Retrieve a list of all accepted FlexOrders for a usefIdentifier for a certain date.
     *
     * @param usefIdentifier optional usef identifier
     * @param ptuDate the period ({@link LocalDate})
     * @return A {@link List} of {@link PtuFlexOrder} objects
     */
    public List<PtuFlexOrder> findAcceptedFlexOrdersForUsefIdentifierOnDate(Optional<String> usefIdentifier, LocalDate ptuDate) {
        return ptuFlexOrderRepository.findAcceptedFlexOrdersByDateAndUsefIdentifier(usefIdentifier, ptuDate);
    }

    /**
     * Finds all the flex offers which are referenced by a flex order during the given period.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link LocalDate} of all the {@link PtuFlexOffer} entities.
     */
    public List<PtuFlexOffer> findFlexOffersWithOrderInPeriod(LocalDate period) {
        return ptuFlexOfferRepository.findFlexOffersWithOrderInPeriod(period);
    }

    /**
     * Finds all the placed flex offers for a period (i.e. valid, active and non-revoked).
     *
     * @param period {@link LocalDate} period.
     * @return a {@link List} of {@link PtuFlexOffer}, ordered by participant domain, sequence number and ptu index.
     */
    public List<PtuFlexOffer> findPlacedFlexOffers(LocalDate period) {
        return ptuFlexOfferRepository.findPlacedFlexOffers(period);
    }

    /**
     * Finds all the prognoses that are linked to a flex order during the given period.
     *
     * @param period {@link LocalDate} period.
     * @return a {@link LocalDate} of the {@link PtuPrognosis} entities.
     */
    public List<PtuPrognosis> findPrognosesWithOrderInPeriod(LocalDate period) {
        return ptuPrognosisRepository.findPrognosesWithOrderInPeriod(period);
    }

    /**
     * Retrieve a PtuPrognosis based on a PtuFlexOffer sequences.
     *
     * @param flexOfferSequence sequence number of the flex offer
     * @param participantDomain participant domain
     * @return {@link List} of {@link PtuPrognosis} objects
     */
    public List<PtuPrognosis> findPtuPrognosisForPtuFlexOfferSequence(Long flexOfferSequence, String participantDomain) {
        Long ptuPrognosisSequence = ptuFlexRequestRepository.findPtuPrognosisSequenceByFlexOfferSequence(flexOfferSequence,
                participantDomain);
        return ptuPrognosisRepository.findPtuPrognosisForSequence(ptuPrognosisSequence, participantDomain);
    }

    /**
     * Intializes ptu's for the current day and index.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void initialisePtuContainers() {
        LocalDateTime timestamp = DateTimeUtil.getCurrentDateTime();
        LocalDate period = timestamp.toLocalDate();
        int ptuIndex = PtuUtil.getPtuIndex(timestamp, config.getIntegerProperty(ConfigParam.PTU_DURATION));
        LOGGER.error("Initialising PTUs at startup, current PTU is {} {}", period, ptuIndex);
        int updated = ptuContainerRepository.initialisePtuContainers(period, ptuIndex);
        LOGGER.debug("Updated {} PTUs", updated);
    }

    /**
     * Finds active congestion point address list.
     *
     * @param period period
     * @return active congestion point address list
     */
    public List<String> findActiveCongestionPointAddresses(LocalDate period) {
        return congestionPointConnectionGroupRepository.findActiveCongestionPointConnectionGroup(period).stream()
                .map(ConnectionGroup::getUsefIdentifier).collect(Collectors.toList());
    }

    /**
     * Finds {@link CongestionPointConnectionGroup} matching the given congestionPoint entity address.
     *
     * @param congestionPoint the congestion point entity address
     * @return {@link CongestionPointConnectionGroup}
     */
    public CongestionPointConnectionGroup findCongestionPointConnectionGroup(String congestionPoint) {
        return congestionPointConnectionGroupRepository.find(congestionPoint);
    }

    /**
     * Finds A-Plans related to the flex offer.
     *
     * @param flexOfferSequenceNumber flex offer sequence number
     * @param participantDomain participant domain
     * @return A-Plans related to the flex offer
     */
    public PlanboardMessage findAPlanRelatedToFlexOffer(Long flexOfferSequenceNumber, String participantDomain) {
        return planboardMessageRepository.findAPlanRelatedToFlexOffer(flexOfferSequenceNumber, participantDomain);
    }

    /**
     * Finds a {@Link PtuState} and create if it does not exist.
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
     * Finds a {@Link Connection} for a given entity address.
     *
     * @param entityAddress the entity address on the connection
     * @return Connection
     */
    public energy.usef.core.model.Connection findConnection(String entityAddress) {
        return connectionRepository.find(entityAddress);
    }

    /**
     * Store a {@Link PlanboardMessage}.
     *
     * @param planboardMessage Planboard Message
     */

    public void storePlanboardMessage(PlanboardMessage planboardMessage) {
        planboardMessageRepository.persist(planboardMessage);
    }

    /**
     * Store a {@Link PtuState}.
     *
     * @param ptuState PTU State
     */

    public void storePtuState(PtuState ptuState) {
        ptuStateRepository.persist(ptuState);
    }
}
