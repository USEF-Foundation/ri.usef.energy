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

package energy.usef.agr.workflow.step;

import energy.usef.agr.workflow.nonudi.dto.CongestionManagementProfileDto;
import energy.usef.agr.workflow.nonudi.dto.ObjectiveAgentProfileDto;
import energy.usef.agr.workflow.nonudi.goals.AgrNonUdiSetAdsGoalsParameter;
import energy.usef.agr.workflow.nonudi.service.PowerMatcher;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PrognosisTypeDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Stub implementation of the PBC which is in charge of setting the ADS goals via the PowerMatcher.
 * <p>
 * The PBC receives the following parameters as input to take the decision:
 * <ul>
 * <li>PERIOD: the period {@link org.joda.time.LocalDate} the cluster needs to be initialized.</li>
 * <li>PTU_DURATION: the duration per ptu in minutes {@link Integer}.</li>
 * <li>PROGNOSIS_DTO: {@link PrognosisDto} dto containing the ptuPrognoses and other relevant prognoses information.</li>
 * </ul>
 * <p>
 * The step implementation of this PBC will set the ADS goals in the PowerMatcher using the input received.
 * <p>
 * This PBC requires a PowerMatcher to be available and will try forever in case the PowerMatcher isn't responding.
 */
public class AgrNonUdiSetAdsGoalsStub implements WorkflowStep {
    @Inject
    private PowerMatcher powerMatcher;

    private static List<ObjectiveAgentProfileDto> transformToObjectiveAgentProfileDto(PrognosisDto prognosisDto, int ptuDuration) {
        List<ObjectiveAgentProfileDto> objectiveAgentProfileDtos = new ArrayList<>();

        prognosisDto.getPtus().forEach(ptuPrognosisDto -> {
            ObjectiveAgentProfileDto objectiveAgentProfileDto = new ObjectiveAgentProfileDto();
            objectiveAgentProfileDto.setTimeInterval(
                    PowerMatcher.getInterval(prognosisDto.getPeriod(), ptuPrognosisDto.getPtuIndex().intValue(), ptuDuration, 1));
            objectiveAgentProfileDto.setTargetDemandWatt(new BigDecimal(ptuPrognosisDto.getPower()));

            objectiveAgentProfileDtos.add(objectiveAgentProfileDto);
        });

        return objectiveAgentProfileDtos;
    }

    private static List<CongestionManagementProfileDto> transformToCongestionManagementProfileDto(PrognosisDto prognosisDto,
            int ptuDuration) {
        List<CongestionManagementProfileDto> congestionManagementProfileDtos = new ArrayList<>();

        prognosisDto.getPtus().forEach(ptuPrognosisDto -> {
            CongestionManagementProfileDto congestionManagementProfileDto = new CongestionManagementProfileDto();
            congestionManagementProfileDto.setTimeInterval(
                    PowerMatcher.getInterval(prognosisDto.getPeriod(), ptuPrognosisDto.getPtuIndex().intValue(), ptuDuration, 1));
            congestionManagementProfileDto.setMinDemandWatt(new BigDecimal(ptuPrognosisDto.getPower()));
            congestionManagementProfileDto.setMaxDemandWatt(new BigDecimal(ptuPrognosisDto.getPower()));

            congestionManagementProfileDtos.add(congestionManagementProfileDto);
        });

        return congestionManagementProfileDtos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {

        PrognosisDto prognosisDto = context.get(AgrNonUdiSetAdsGoalsParameter.IN.PROGNOSIS_DTO.name(), PrognosisDto.class);
        int ptuDuration = context.get(AgrNonUdiSetAdsGoalsParameter.IN.PTU_DURATION.name(), Integer.class);

        if (prognosisDto.getType() == PrognosisTypeDto.A_PLAN) {
            // A-Plan's -> Objective Agent
            powerMatcher.postObjectiveAgent(prognosisDto.getConnectionGroupEntityAddress(),
                    transformToObjectiveAgentProfileDto(prognosisDto, ptuDuration));
        } else {
            // D-Prognoses -> Congestion Management
            powerMatcher.postCongestionManagement(prognosisDto.getConnectionGroupEntityAddress(),
                    transformToCongestionManagementProfileDto(prognosisDto, ptuDuration));
        }

        return context;
    }
}
