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

package energy.usef.brp.workflow.step;

import energy.usef.brp.workflow.plan.connection.forecast.ReceivedAPlanWorkflowParameter.IN;
import energy.usef.brp.workflow.plan.connection.forecast.ReceivedAPlanWorkflowParameter.OUT;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.PrognosisDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow step implementation for the A-Plan received step of the 'BRP received A-Plan and genetare flex request' workflow. This
 * implementation expects to find the following parameters as input:
 * <ul>
 * <li>PTU_DURATION: PTU duration ({@link Integer})</li>
 * <li>A_PLAN_DTO_LIST: Full A-Plan DTO list ({@link List}) of {@link PrognosisDto} for this period</li>
 * * <li>RECEIVED_A_PLAN_DTO_LIST: A-Plan DTO list With status RECEIVED ({@link List}) of {@link PrognosisDto} received for this period</li>
 * </ul>
 * This implementation must return the following parameters as input:
 * <ul>
 * <li>ACCEPTED_A_PLAN_DTO_LIST: List of accepted A-Plans ({@link java.util.List}) of {@link PrognosisDto}</li>
 * <li>PROCESSED_A_PLAN_DTO_LIST: List of processed A-Plans ({@link java.util.List}) of {@link PrognosisDto}</li>
 * </ul>
 * The list of received A-Plan DTOs is iterated and for each item a random decision is taken: accepted, processed, no decision.
 */
public class BrpReceivedAPlanStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrpReceivedAPlanStub.class);
    private static final Random RANDOM = new Random();

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked") public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("BRPReceivedAPlan Stub invoked");

        List<PrognosisDto> acceptedAPlans = new ArrayList<>();
        List<PrognosisDto> processedAPlans = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<PrognosisDto> aPlanDtos = (List<PrognosisDto>) context.getValue(IN.A_PLAN_DTO_LIST.name());
        List<PrognosisDto> receivedAplanDtos = (List<PrognosisDto>) context.getValue(IN.RECEIVED_A_PLAN_DTO_LIST.name());
        LOGGER.debug("Input: [{}] A-Plans (with RECEIVED status)", receivedAplanDtos.size());

        //create countMap
        Map<String, Integer> aplanCountPerParticipant = aPlanDtos.stream()
                .collect(Collectors.groupingBy(PrognosisDto::getParticipantDomain))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));

        receivedAplanDtos.stream().forEach(aPlanDto -> {
            if (randomChance(aplanCountPerParticipant.get(aPlanDto.getParticipantDomain()))) {
                //already has an aplan
                acceptedAPlans.add(aPlanDto);
            } else {
                processedAPlans.add(aPlanDto);
            }

        });
        context.setValue(OUT.ACCEPTED_A_PLAN_DTO_LIST.name(), acceptedAPlans);
        context.setValue(OUT.PROCESSED_A_PLAN_DTO_LIST.name(), processedAPlans);

        LOGGER.debug("Output: Accepted [{}] A-Plans (status will be changed to ACCEPTED)", acceptedAPlans.size());
        LOGGER.debug("Output: Process [{}] A-Plans (status will be changed to PROCESSED)", processedAPlans.size());

        return context;
    }

    private int generateRandomNumber(int minValue, int maxValue) {
        return RANDOM.nextInt((maxValue - minValue) + 1) + minValue;
    }

    private boolean randomChance(Integer nrOfAPlans) {
        return generateRandomNumber(1, nrOfAPlans) == 1;
    }

}
