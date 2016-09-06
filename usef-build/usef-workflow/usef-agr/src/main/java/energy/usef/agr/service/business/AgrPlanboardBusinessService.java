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

import static energy.usef.agr.model.SynchronisationConnectionStatusType.MODIFIED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.joda.time.LocalDate;

import energy.usef.agr.model.CommonReferenceOperator;
import energy.usef.agr.model.SynchronisationConnection;
import energy.usef.agr.repository.CommonReferenceOperatorRepository;
import energy.usef.agr.repository.SynchronisationConnectionRepository;
import energy.usef.agr.repository.SynchronisationConnectionStatusRepository;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.model.PtuFlexRequest;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuFlexOrderRepository;
import energy.usef.core.repository.PtuFlexRequestRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.transformer.PrognosisTransformer;

/**
 * Service class in charge of operations and validations related to the Aggregator planboard.
 */

@Stateless
public class AgrPlanboardBusinessService {
    public static final DocumentType A_PLAN = DocumentType.A_PLAN;

    @Inject
    private CommonReferenceOperatorRepository commonReferenceOperatorRepository;
    @Inject
    private PlanboardMessageRepository planboardMessageRepository;
    @Inject
    private PtuContainerRepository ptuContainerRepository;
    @Inject
    private PtuFlexOrderRepository ptuFlexOrderRepository;
    @Inject
    private PtuFlexRequestRepository ptuFlexRequestRepository;
    @Inject
    private PtuPrognosisRepository prognosisRepository;
    @Inject
    private SynchronisationConnectionRepository synchronisationConnectionRepository;
    @Inject
    private SynchronisationConnectionStatusRepository synchronisationConnectionStatusRepository;

    /**
     * Find proccessable requests for a certain type.
     *
     * @param type - The type of request searched for.
     * @return {@link List} of {@link PlanboardMessage} objects.
     */
    public List<PlanboardMessage> findAcceptedRequests(DocumentType type) {
        return planboardMessageRepository.findPlanboardMessages(type, DocumentStatus.ACCEPTED);
    }

    /**
     * Find the PTU's for the congestionpoint and period.
     *
     * @param period - The Day we want all {@link PtuContainer}'s for.
     * @return - The List of {@link PtuContainer}'s.
     */
    public List<PtuContainer> findPTUContainersForPeriod(LocalDate period) {
        return new ArrayList<>(ptuContainerRepository.findPtuContainersMap(period).values());
    }

    /**
     * Find the last FlexRequest Document for a day with DispositionRequested.
     *
     * @param usefIdentifier usefIdentifier ({@link String})
     * @param period the period ({@link LocalDate}
     * @param sequence sequence number ({@link Long})
     * @return {@link PtuFlexRequest}
     */
    public PtuFlexRequest findLastFlexRequestDocumentWithDispositionRequested(String usefIdentifier, LocalDate period,
            Long sequence) {
        return ptuFlexRequestRepository.findLastFlexRequestDocumentWithDispositionRequested(usefIdentifier, period, sequence);
    }

    /**
     * Finds the {@link PtuFlexRequest} with the given sequence number for a given participant. The size of the resulting list
     * should match the number of PTUs for the period of those PtuFlexRequest.
     *
     * @param connectionGroupUsefIdentifier {@link String} usef identifier of the connection group.
     * @param flexRequestSequenceNumber     {@link Long} sequence number.
     * @param participantDomain             {@link String} participant domain name.
     * @return a {@link List} of {@link PtuFlexRequest}.
     */
    public List<PtuFlexRequest> findPtuFlexRequestWithSequence(String connectionGroupUsefIdentifier, Long flexRequestSequenceNumber,
            String participantDomain) {
        return ptuFlexRequestRepository
                .findPtuFlexRequestWithSequence(connectionGroupUsefIdentifier, flexRequestSequenceNumber, participantDomain);
    }

    /**
     * Gets the entire list of {@link CommonReferenceOperator} known by this Balance Responsible Party.
     *
     * @return {@link List} of {@link CommonReferenceOperator}
     */
    public List<CommonReferenceOperator> findAll() {
        return commonReferenceOperatorRepository.findAll();
    }

    /**
     * Finds all {@link CommonReferenceOperator}s and {@link SynchronisationConnection}s and put them in a Map, all
     * {@link SynchronisationConnection}s are send to all {@link CommonReferenceOperator}s.
     *
     * @return Map containing cro domain mapped to {@link List} of {@link SynchronisationConnection} objects.
     */
    public Map<String, List<SynchronisationConnection>> findConnectionsPerCRO() {
        List<SynchronisationConnection> connections = synchronisationConnectionRepository.findAll();

        List<CommonReferenceOperator> cros = commonReferenceOperatorRepository.findAll();

        Map<String, List<SynchronisationConnection>> connectionsPerCRO = new HashMap<>();

        if (connections.isEmpty() || cros.isEmpty()) {
            return connectionsPerCRO;
        }

        for (CommonReferenceOperator cro : cros) {
            connectionsPerCRO.put(cro.getDomain(), connections);
        }
        return connectionsPerCRO;
    }

    /**
     * Updates connection status for CRO.
     *
     * @param croDomain CRO domain
     */
    @Transactional(value = TxType.REQUIRES_NEW)
    public void updateConnectionStatusForCRO(String croDomain) {
        synchronisationConnectionRepository.updateConnectionStatusForCRO(croDomain);
    }

    /**
     * Clean's the synchronization table's if synchronization is complete.
     */
    public void cleanSynchronization() {
        long count = synchronisationConnectionStatusRepository.countSynchronisationConnectionStatusWithStatus(MODIFIED);
        if (count == 0L) {
            // everything is synchronized, time to remove everything.
            synchronisationConnectionStatusRepository.deleteAll();
            synchronisationConnectionRepository.deleteAll();
        }
    }

    /**
     * Finds last prognosis list.
     *
     * @param period period
     * @param prognosisType prognosis type
     * @param usefIdentifier USEF identifier
     * @return last prognosis list in the {@link PrognosisDto} format.
     */
    public List<PrognosisDto> findLastPrognoses(LocalDate period, PrognosisType prognosisType, Optional<String> usefIdentifier) {

        List<PtuPrognosis> ptuPrognoses = prognosisRepository.findLastPrognoses(period, Optional.of(prognosisType),
                usefIdentifier, Optional.empty());

        Map<String, List<PtuPrognosis>> ptuPrognosesMap = ptuPrognoses.stream().collect(
                Collectors.groupingBy(ptuPrognosis -> "" + ptuPrognosis.getParticipantDomain() + ptuPrognosis.getSequence()));

        List<PrognosisDto> result = ptuPrognosesMap.entrySet().stream()
                .map(entry -> PrognosisTransformer.mapToPrognosis(entry.getValue())).collect(Collectors.toList());
        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    /**
     * Updates the acknowledgmentstatus of a ptuFlexOrder.
     *
     * @param ptuFlexOrder The {@link PtuFlexOrder} that needs to be updated
     * @param status The new {@link AcknowledgementStatus} of the ptuFlexOrder
     */
    public void changeStatusOfPtuFlexOrder(PtuFlexOrder ptuFlexOrder, AcknowledgementStatus status) {
        ptuFlexOrder.setAcknowledgementStatus(status);
        ptuFlexOrderRepository.persist(ptuFlexOrder);
    }

    /**
     * This method finds rejected {@link PlanboardMessage} based on {@link DocumentType} and {@link DocumentStatus} within a time
     * frame (startDate - endDate).
     *
     * @param documentType The type of document, like request, offer or order.
     * @param period The date of the planboard message
     * @return The list of {@link PlanboardMessage} which have a specific {@link DocumentType} and {@link DocumentStatus}.
     */
    public List<PlanboardMessage> findRejectedPlanboardMessages(DocumentType documentType, LocalDate period) {
        return planboardMessageRepository.findRejectedPlanboardMessages(documentType, period);
    }

}
