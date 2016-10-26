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

package nl.energieprojecthoogdalem.brp;

import info.usef.brp.workflow.plan.connection.forecast.ReceivedAPlanWorkflowParameter.IN;
import info.usef.brp.workflow.plan.connection.forecast.ReceivedAPlanWorkflowParameter.OUT;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.PrognosisDto;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem Workflow implementation of the 'BRP received A-Plan and genetare flex request' workflow. This
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
 * The list of received A-Plan DTOs is always accepted no processed A-Plans will be created.
 */
public class ReceivedAPlan implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceivedAPlan.class);

    @SuppressWarnings("unchecked") public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.info("ReceivedAPlan invoked");

        List<PrognosisDto> receivedAplanDtos = (List<PrognosisDto>) context.getValue(IN.RECEIVED_A_PLAN_DTO_LIST.name());
        List<PrognosisDto> processedAPlans = new ArrayList<>();
        LOGGER.debug("Input: [{}] A-Plans (with RECEIVED status)", receivedAplanDtos.size());

        context.setValue(OUT.ACCEPTED_A_PLAN_DTO_LIST.name(), receivedAplanDtos);
        context.setValue(OUT.PROCESSED_A_PLAN_DTO_LIST.name(), processedAPlans);

        LOGGER.debug("Output: Accepted [{}] A-Plans (status will be changed to ACCEPTED)", receivedAplanDtos.size());
        LOGGER.debug("Output: Process [{}] A-Plans (status will be changed to PROCESSED)", processedAPlans.size());

        return context;
    }


}
