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

package energy.usef.brp.workflow.altstep;

import energy.usef.brp.pbcfeederimpl.PbcFeederService;
import energy.usef.brp.workflow.plan.connection.forecast.PrepareFlexRequestWorkflowParameter;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the PBC in charge of handling received A-Plans and asking the generation of flex requests. This step will
 * receive the list of A-Plan data per ptu for a given period (day).
 * This stub does not create flex requests or accepts A-Plans.
 *
 * This implementation expects to find the following parameters as input:
 * <ul>
 * <li>PTU_DURATION: PTU duration ({@link Integer})</li>
 * <li>PROCESSED_A_PLAN_DTO_LIST: A-Plan DTO list ({@link java.util.List}) of {@link PrognosisDto}</li>
 * </ul>
 * This implementation must return the following parameters as input:
 * <ul>
 * <li>FLEX_REQUEST_DTO_LIST: List of flex requests ({@link java.util.List}) of {@link FlexRequestDto}</li>
 * <li>ACCEPTED_A_PLAN_DTO_LIST: List of accepted A-Plans ({@link java.util.List}) of {@link PrognosisDto}</li>
 * </ul>
 */
public class BrpPrepareFlexRequestsStubNotPrepare implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrpPrepareFlexRequestsStubNotPrepare.class);


    @Inject
    private PbcFeederService pbcFeederService;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("BrpPrepareFlexRequests Stub invoked");

        List<FlexRequestDto> flexRequestDtos = new ArrayList<>();
        List<PrognosisDto> acceptedAPlans = new ArrayList<>();

        List<PrognosisDto> aPlanDtos = (List<PrognosisDto>) context
                .getValue(PrepareFlexRequestWorkflowParameter.IN.PROCESSED_A_PLAN_DTO_LIST.name());
        Integer ptuDuration = (Integer) context.getValue(PrepareFlexRequestWorkflowParameter.IN.PTU_DURATION.name());
        LOGGER.debug("Input: [{}] A-Plans (with PROCESSED status).", aPlanDtos.size());

        context.setValue(PrepareFlexRequestWorkflowParameter.OUT.FLEX_REQUEST_DTO_LIST.name(), flexRequestDtos);
        context.setValue(PrepareFlexRequestWorkflowParameter.OUT.ACCEPTED_A_PLAN_DTO_LIST.name(), acceptedAPlans);

        LOGGER.debug("Output: Accepted [{}] A-Plans (status will be changed to ACCEPTED)", acceptedAPlans.size());
        LOGGER.debug("Output: Flex Requested for [{}] A-Plans (status will be changed to PENDING_FLEX_TRADING)",
                flexRequestDtos.size());

        return context;
    }
}
