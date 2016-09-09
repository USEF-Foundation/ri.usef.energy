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

package energy.usef.dso.workflow.altstep;

import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PtuFlexRequestDto;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import energy.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestStepParameter.IN;
import energy.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestStepParameter.OUT;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Workflow step implementation for the Workflow 'Create Flex Requests'.
 * This stub creates one flex request.
 *
 * This implementation expects to find the following parameters as input:
 * <ul>
 * <li>CONGESTION_POINT_ENTITY_ADDRESS: the entity address of the congestion point ({@link String})</li>
 * <li>PERIOD: day for which one wants to send flex requests ({@link LocalDate})</li>
 * <li>GRID_SAFETY_ANALYSIS_LIST: Array of {@link GridSafetyAnalysisDto} containing the data of the Grid
 * Safety Analysis for a day.</li>
 * </ul>
 */
public class DsoCreateFlexRequestStubOne implements WorkflowStep {
    private static final int MINIMUM_REQUESTED_PTUS = 0;
    private static final int FLEX_REQUEST_EXPIRATION_DAYS = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoCreateFlexRequestStubOne.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        String entityAddress = context.get(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), String.class);
        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);

        GridSafetyAnalysisDto gridSafetyAnalysis = context.get(IN.GRID_SAFETY_ANALYSIS_DTO.name(), GridSafetyAnalysisDto.class);

        long numberOfRequestedPtus = gridSafetyAnalysis.getPtus().stream()
                .filter(ptu -> DispositionTypeDto.REQUESTED.equals(ptu.getDisposition()))
                .count();

        List<FlexRequestDto> flexRequestDtos = new ArrayList<>();
        if (numberOfRequestedPtus >= MINIMUM_REQUESTED_PTUS) {
            addFlexRequests(flexRequestDtos, entityAddress, period, gridSafetyAnalysis);
        } else {
            LOGGER.debug(" No Flex Requests needed, number of REQUESTED ptus is {}", numberOfRequestedPtus);
        }

        context.setValue(OUT.FLEX_REQUESTS_DTO_LIST.name(), flexRequestDtos);
        return context;
    }

    private void addFlexRequests(List<FlexRequestDto> flexRequestDtos, String entityAddress, LocalDate period,
            GridSafetyAnalysisDto gridSafetyAnalysis) {
        int numberOfRequests = 1;
        switch (numberOfRequests) {
        case 0:
            LOGGER.debug("No Flex Requests will be generated!");
            break;
        case 1:
            LOGGER.debug("One 100% Flex Requests will be generated!");
            flexRequestDtos.add(buildFlexRequest(entityAddress, period, gridSafetyAnalysis, BigDecimal.valueOf(1d)));
            break;
        case 2:
            LOGGER.debug("Two 60%/40% Flex Requests will be generated!");
            flexRequestDtos.add(buildFlexRequest(entityAddress, period, gridSafetyAnalysis, BigDecimal.valueOf(0.6d)));
            flexRequestDtos.add(buildFlexRequest(entityAddress, period, gridSafetyAnalysis, BigDecimal.valueOf(0.4d)));
            break;
        }
    }

    private FlexRequestDto buildFlexRequest(String entityAddress, LocalDate period, GridSafetyAnalysisDto gridSafetyAnalysis,
            BigDecimal powerPercentageFactor) {

        FlexRequestDto newFlexRequestDto = new FlexRequestDto();
        newFlexRequestDto.setConnectionGroupEntityAddress(entityAddress);
        newFlexRequestDto.setPeriod(period);
        newFlexRequestDto.setExpirationDateTime(DateTimeUtil.getCurrentDateTime().plusDays(FLEX_REQUEST_EXPIRATION_DAYS));
        // use grid safety analysis to determine flex request
        for (PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto : gridSafetyAnalysis.getPtus()) {
            PtuFlexRequestDto ptuFlexRequestDto = new PtuFlexRequestDto();
            ptuFlexRequestDto.setPtuIndex(BigInteger.valueOf(ptuGridSafetyAnalysisDto.getPtuIndex()));

            BigDecimal power = powerPercentageFactor.multiply(BigDecimal.valueOf(ptuGridSafetyAnalysisDto.getPower()));
            BigInteger roundedPower = power.setScale(0, BigDecimal.ROUND_HALF_UP).toBigInteger();
            ptuFlexRequestDto.setPower(roundedPower);

            if (ptuGridSafetyAnalysisDto.getDisposition() == DispositionTypeDto.AVAILABLE) {
                ptuFlexRequestDto.setDisposition(DispositionTypeDto.AVAILABLE);
            } else {
                ptuFlexRequestDto.setDisposition(DispositionTypeDto.REQUESTED);
            }

            newFlexRequestDto.getPtus().add(ptuFlexRequestDto);
        }
        return newFlexRequestDto;
    }
}
