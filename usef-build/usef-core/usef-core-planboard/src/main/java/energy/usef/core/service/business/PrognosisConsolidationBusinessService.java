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
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.repository.ConnectionGroupRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.util.PlanboardMessageUtil;
import energy.usef.core.util.PtuUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class in charge of operations related to the Prognosis Consolidation.
 */
@Stateless
public class PrognosisConsolidationBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrognosisConsolidationBusinessService.class);

    @Inject
    private PlanboardMessageRepository planboardMessageRepository;

    @Inject
    private PtuPrognosisRepository ptuPrognosisRepository;

    @Inject
    private Config config;

    @Inject
    private PtuContainerRepository ptuContainerRepository;

    @Inject
    private ConnectionGroupRepository connectionGroupRepository;

    /**
     * Consolidate prognosis that were received for a certain date in order to have the correct data for settlement.
     *
     * @param period
     * @param connectionGroupIdentifier
     * @param participantDomain
     * @return
     */
    public List<PtuPrognosis> consolidatePrognosisForDate(LocalDate period, String connectionGroupIdentifier,
            String participantDomain) {

        // 1. Fetch all relevant prognosis from the Planboard for the period
        List<PlanboardMessage> planboardMessageList = fetchRelevantPrognosisOrderedByCreationTime(period, connectionGroupIdentifier,
                participantDomain);

        // 2. Initialize PtuPrognosis list and loop over PlanboardMessages to fill list, starting with most recent
        List<PtuPrognosis> consolidatedPtuPrognosisList = initializePrognosisList(period, connectionGroupIdentifier,
                participantDomain);

        if (consolidatedPtuPrognosisList.isEmpty()) {
            return consolidatedPtuPrognosisList;
        }

        for (PlanboardMessage planboardMessage : planboardMessageList) {
            if (doAllPtusHavePower(consolidatedPtuPrognosisList, planboardMessage)) {
                break;
            }
        }
        return consolidatedPtuPrognosisList;
    }

    private boolean doAllPtusHavePower(List<PtuPrognosis> consolidatedPtuPrognosisList, PlanboardMessage planboardMessage) {
        LOGGER.debug("Processing relevant prognosis [{}] for [{}] with PMID=[{}]", planboardMessage.getSequence(),
                planboardMessage.getParticipantDomain(), planboardMessage.getId());
        // 2a. Find PtuPrognosis based on sequence number of planboardMessage
        List<PtuPrognosis> ptuPrognosisList = ptuPrognosisRepository.findBySequence(planboardMessage.getSequence());
        LOGGER.debug("Fetched [{}] PtuPrognoses for the sequence [{}]", ptuPrognosisList.size(), planboardMessage.getSequence());
        // 2b. Filter out valid PtuPrognosis with regard to intra-day gate closure
        List<PtuPrognosis> validPtuPrognosisList = filterPtuPrognosisForIntradayGateClosureValidity(ptuPrognosisList,
                planboardMessage.getCreationDateTime());
        LOGGER.debug("[{}] PtuPrognoses after filterPtuPrognosisForIntradayGateClosureValidity", validPtuPrognosisList.size());
        if (!validPtuPrognosisList.isEmpty()) {
            // sort validPtuPrognosisList to accelerate the matching process in next step
            validPtuPrognosisList.stream()
                    .sorted((e1, e2) -> e2.getPtuContainer().getPtuIndex().compareTo(e1.getPtuContainer().getPtuIndex()))
                    .collect(Collectors.toList());
            // 2c. For each empty PTU: Find Power and Disposition and add to new list
            consolidatedPtuPrognosisList.stream().filter(consolidatedPtu -> consolidatedPtu.getPower() == null)
                    .map(consolidatedPtu -> {
                        // find corresponding ptu in validPtuPrognosisList
                        return validPtuPrognosisList.stream().map(validPtuPrognosis -> {
                            if (validPtuPrognosis.getPtuContainer().getPtuIndex()
                                    .equals(consolidatedPtu.getPtuContainer().getPtuIndex())) {
                                return matchValidAndConsolidatePtuPrognosis(validPtuPrognosis, consolidatedPtu);
                            }
                            return validPtuPrognosis;
                        }).collect(Collectors.toList());
                    }).collect(Collectors.toList());
            // 2d. Validate completeness of consolidated list, i.e. all PTUs have power, if not, continue with next
            // PlanboardMessage
            if (consolidatedPtuPrognosisList.stream()
                    .noneMatch(consolidatedPtuPrognosis -> consolidatedPtuPrognosis.getPower() == null)) {
                LOGGER.debug("All the PTUs in the ConsolidatedPtuPrognoses have a power");
                consolidatedPtuPrognosisList.stream().forEach(ptuPrognosis -> LOGGER
                        .trace("Date of the consolidated prognosis: PTU index=[{}] for Power=[{}]",
                                ptuPrognosis.getPtuContainer().getPtuIndex(), ptuPrognosis.getPower()));
                return true;
            }
        }
        return false;
    }

    private List<PtuPrognosis> initializePrognosisList(LocalDate period, String connectionGroupIdentifier,
            String participantDomain) {
        ConnectionGroup connectionGroup = connectionGroupRepository.find(connectionGroupIdentifier);
        int ptusPerDay = PtuUtil.getNumberOfPtusPerDay(period, config.getIntegerProperty(ConfigParam.PTU_DURATION));
        final Map<Integer, PtuContainer> ptuContainers = ptuContainerRepository.findPtuContainersMap(period);

        return IntStream.rangeClosed(1, ptusPerDay).mapToObj(index -> {
            PtuPrognosis ptuPrognosis = new PtuPrognosis();
            PtuContainer ptuContainer = ptuContainers.get(index);
            ptuPrognosis.setPtuContainer(ptuContainer);
            ptuPrognosis.setParticipantDomain(participantDomain);
            ptuPrognosis.setConnectionGroup(connectionGroup);
            return ptuPrognosis;
        }).filter(ptuPrognosis -> ptuPrognosis.getPtuContainer() != null).collect(Collectors.toList());
    }

    private List<PlanboardMessage> fetchRelevantPrognosisOrderedByCreationTime(LocalDate period, String connectionGroupIdentifier,
            String participantDomain) {
        List<PlanboardMessage> planboardMessageList = planboardMessageRepository
                .findPrognosisRelevantForDateByUsefIdentifier(period, connectionGroupIdentifier, participantDomain);
        if (planboardMessageList.isEmpty()) {
            return new ArrayList<>();
        }
        LOGGER.debug("Fetched [{}] relevant prognoses for date [{}] in order to build a consolidated prognosis.",
                planboardMessageList.size(), period);

        // Reverse order by time of receipt
        return PlanboardMessageUtil.sortPlanboardMessageListDescByCreationTime(planboardMessageList);
    }

    private PtuPrognosis matchValidAndConsolidatePtuPrognosis(PtuPrognosis validPtuPrognosis, PtuPrognosis consolidatedPtu) {
        // REVIEW: any efficient way to deep copy values shorter/quicker?
        consolidatedPtu.setPower(validPtuPrognosis.getPower());
        consolidatedPtu.setPtuContainer(validPtuPrognosis.getPtuContainer());
        consolidatedPtu.setConnectionGroup(validPtuPrognosis.getConnectionGroup());
        consolidatedPtu.setSequence(validPtuPrognosis.getSequence());
        consolidatedPtu.setType(validPtuPrognosis.getType());
        consolidatedPtu.setParticipantDomain(validPtuPrognosis.getParticipantDomain());
        return consolidatedPtu;
    }

    private List<PtuPrognosis> filterPtuPrognosisForIntradayGateClosureValidity(List<PtuPrognosis> ptuPrognosisList,
            LocalDateTime planboardMessageCreationTime) {
        List<PtuPrognosis> filteredPrognoses = new ArrayList<>();
        LocalDate prognosisPeriod = ptuPrognosisList.get(0).getPtuContainer().getPtuDate();
        Integer gateClosurePtus = config.getIntegerProperty(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS);
        Integer ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        LocalDateTime creationTimePlusGateClosure = planboardMessageCreationTime.plusMinutes(gateClosurePtus * ptuDuration);
        Integer pivotIndex = PtuUtil.getPtuIndex(creationTimePlusGateClosure, ptuDuration);
        if (creationTimePlusGateClosure.toLocalDate().isEqual(prognosisPeriod)) {
            filteredPrognoses = ptuPrognosisList.stream()
                    .filter(ptuPrognosis -> ptuPrognosis.getPtuContainer().getPtuIndex() > pivotIndex)
                    .collect(Collectors.toList());
        } else if (creationTimePlusGateClosure.toLocalDate().isBefore(prognosisPeriod)) {
            filteredPrognoses = ptuPrognosisList.stream().collect(Collectors.toList());
        }
        return filteredPrognoses;
    }
}
