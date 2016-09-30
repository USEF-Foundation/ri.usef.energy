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

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.operate.PlaceOperateFlexOrdersStepParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Workflow step implementation for the Workflow 'Place Operate Flex Orders'.
 * This stub orders all flex offers.
 *
 * This implementation expects to find the following parameters as input:
 * <ul>
 * <li>FLEX_OFFER_DTO_LIST ({@link List<FlexOfferDto>}): Flex offer DTO list.</li>
 * <li>GRID_SAFETY_ANALYSIS_DTO ({@link List<FlexOfferDto>}): Grid safety analysis DTO.</li>
 * </ul>
 * <p>
 * parameters as output:
 * <ul>
 * <li>ACCEPTED_FLEX_OFFER_DTO_LIST: Accepted flex offer DTO list.</li>
 * </ul>
 * <p>
 * Flex offer DTO combination is accepted based on optimal price.
 */
public class DsoPlaceOperateFlexOrdersStubOrder implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoPlaceFlexOrdersStubOrder.class);

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        List<FlexOfferDto> flexOfferDtos = (List<FlexOfferDto>) context
                .getValue(PlaceOperateFlexOrdersStepParameter.IN.FLEX_OFFER_DTO_LIST.name());
        GridSafetyAnalysisDto gridSafetyAnalysisDto = (GridSafetyAnalysisDto) context
                .getValue(PlaceOperateFlexOrdersStepParameter.IN.GRID_SAFETY_ANALYSIS_DTO.name());

        LOGGER.debug("Starting workflow step 'Place Flex Orders' for Entity Address {}, PTU Date {}.",
                gridSafetyAnalysisDto.getEntityAddress(), gridSafetyAnalysisDto.getPtuDate());

        // remove all flex offers with empty ptu data
        FlexOfferDto[] flexOfferDtoArray = flexOfferDtos.stream()
                .filter(flexOfferDto -> flexOfferDto.getPtus() != null && !flexOfferDto.getPtus().isEmpty())
                .toArray(FlexOfferDto[]::new);

        List<FlexOfferDto> acceptedFlexOfferDtos = new ArrayList<>();

        if (flexOfferDtoArray.length > 0) {
            acceptedFlexOfferDtos = findAcceptedFlexOfferDtos(flexOfferDtoArray);
        }

        context.setValue(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name(), acceptedFlexOfferDtos);

        LOGGER.debug("Ending successfully workflow step 'Place Flex Orders'.");
        return context;
    }

    private List<FlexOfferDto> findAcceptedFlexOfferDtos(FlexOfferDto[] flexOfferDtoArray) {
        List<FlexOfferDto> acceptedFlexOfferDtos = new ArrayList<>();

        acceptedFlexOfferDtos.addAll(Arrays.asList(flexOfferDtoArray));

        return acceptedFlexOfferDtos;
    }

}
