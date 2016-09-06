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

package energy.usef.dso.workflow.step;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import energy.usef.dso.workflow.operate.PlaceOperateFlexOrdersStepParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow step implementation for the Workflow 'Place Operate Flex Orders'. This implementation expects to find the following
 * parameters as input:
 * <ul>
 * <li>FLEX_OFFER_DTO_LIST ({@link List<FlexOfferDto>}): Flex offer DTO list.</li>
 * <li>GRID_SAFETY_ANALYSIS_DTO ({@link List<FlexOfferDto>}): Grid safety analysis DTO.</li>
 * </ul>
 * 
 * parameters as output:
 * <ul>
 * <li>ACCEPTED_FLEX_OFFER_DTO_LIST: Accepted flex offer DTO list.</li>
 * </ul>
 * 
 * Flex offer DTO combination is accepted based on optimal price.
 */
public class DsoPlaceOperateFlexOrdersStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoPlaceFlexOrdersStub.class);

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
            acceptedFlexOfferDtos = findAcceptedFlexOfferDtos(flexOfferDtoArray, gridSafetyAnalysisDto);
        }

        context.setValue(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name(), acceptedFlexOfferDtos);

        LOGGER.debug("Ending successfully workflow step 'Place Flex Orders'.");
        return context;
    }

    private List<FlexOfferDto> findAcceptedFlexOfferDtos(FlexOfferDto[] flexOfferDtoArray,
            GridSafetyAnalysisDto gridSafetyAnalysisDto) {
        List<FlexOfferDto> acceptedFlexOfferDtos = new ArrayList<>();

        List<FlexOfferCombination> flexOfferCombinations = generateFlexOfferCombinations(0, flexOfferDtoArray.length - 1,
                flexOfferDtoArray,
                gridSafetyAnalysisDto);
        double minPrice = -1;
        FlexOfferCombination optimalFlexOfferCombination = null;
        for (FlexOfferCombination flexOfferCombination : flexOfferCombinations) {
            if ((optimalFlexOfferCombination == null && flexOfferCombination.sufficient)
                    || (minPrice > flexOfferCombination.price && flexOfferCombination.sufficient)) {
                minPrice = flexOfferCombination.price;
                optimalFlexOfferCombination = flexOfferCombination;
            }
        }

        if (optimalFlexOfferCombination != null) {
            for (int i = 0; i < optimalFlexOfferCombination.combination.length; i++) {
                acceptedFlexOfferDtos.add(flexOfferDtoArray[optimalFlexOfferCombination.combination[i]]);
            }
        } else {
            acceptedFlexOfferDtos.addAll(Arrays.asList(flexOfferDtoArray));
        }

        return acceptedFlexOfferDtos;
    }

    private List<FlexOfferCombination> generateFlexOfferCombinations(int i, int j, FlexOfferDto[] flexOfferDtoArray,
            GridSafetyAnalysisDto gridSafetyAnalysisDto) {
        List<FlexOfferCombination> resultFlexOfferCombinations = new ArrayList<>();
        FlexOfferCombination firstFlexOfferCombination = new FlexOfferCombination(new int[] { i });
        resultFlexOfferCombinations.add(firstFlexOfferCombination);

        calculate(firstFlexOfferCombination, flexOfferDtoArray, gridSafetyAnalysisDto);

        if (i != j) {
            List<FlexOfferCombination> flexOfferCombinations = generateFlexOfferCombinations(i + 1, j, flexOfferDtoArray,
                    gridSafetyAnalysisDto);
            resultFlexOfferCombinations.addAll(flexOfferCombinations);

            for (FlexOfferCombination flexOfferCombination : flexOfferCombinations) {
                if (!flexOfferCombination.validated) {
                    calculate(flexOfferCombination, flexOfferDtoArray, gridSafetyAnalysisDto);
                }
                if (!firstFlexOfferCombination.sufficient && !flexOfferCombination.sufficient) {
                    resultFlexOfferCombinations.add(flexOfferCombination.add(i));
                }
            }

        }
        return resultFlexOfferCombinations;
    }

    private class FlexOfferCombination {
        boolean sufficient = false;
        boolean validated = false;
        int[] combination;
        double price;

        FlexOfferCombination(int[] combination) {
            this.combination = combination;
        }

        FlexOfferCombination add(int i) {
            int[] newCombination = Arrays.copyOf(combination, combination.length + 1);
            newCombination[combination.length] = i;

            return new FlexOfferCombination(newCombination);
        }
    }

    private void calculate(FlexOfferCombination flexOfferCombination, FlexOfferDto[] flexOfferDtoArray,
            GridSafetyAnalysisDto gridSafetyAnalysisDto) {
        int ptuArraySize = getBiggestPtuSize(flexOfferDtoArray);

        // nothing to do if there is no ptu data available
        if (ptuArraySize == 0) {
            flexOfferCombination.validated = true;
            return;
        }

        long[] totalPowerArray = new long[ptuArraySize];
        double totalPrice= fillPowerAndCalculateTotalPrice(flexOfferCombination, flexOfferDtoArray, totalPowerArray);

        for (PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto : gridSafetyAnalysisDto.getPtus()) {
            int index = ptuGridSafetyAnalysisDto.getPtuIndex() - 1;
            long power = (ptuGridSafetyAnalysisDto.getPower() != null) ? ptuGridSafetyAnalysisDto.getPower() : 0;
            if (totalPowerArray[index] < power) {
                flexOfferCombination.validated = true;
                return;
            }
        }

        flexOfferCombination.sufficient = true;
        flexOfferCombination.validated = true;
        flexOfferCombination.price = totalPrice;
    }

    private double fillPowerAndCalculateTotalPrice(FlexOfferCombination flexOfferCombination,
            FlexOfferDto[] flexOfferDtoArray, long[] totalPowerArray) {
        double totalPrice = 0;
        for (int i = 0; i < flexOfferCombination.combination.length; i++) {
            FlexOfferDto flexOfferDto = flexOfferDtoArray[flexOfferCombination.combination[i]];

            for (PtuFlexOfferDto ptuFlexOfferDto : flexOfferDto.getPtus()) {
                int index = ptuFlexOfferDto.getPtuIndex().intValue() - 1;
                totalPowerArray[index] += (ptuFlexOfferDto.getPower() != null) ? ptuFlexOfferDto.getPower().longValue() : 0L;
                totalPrice += (ptuFlexOfferDto.getPrice() != null) ? ptuFlexOfferDto.getPrice().doubleValue() : 0;
            }
        }
        return totalPrice;
    }

    private int getBiggestPtuSize(FlexOfferDto[] flexOfferDtoArray) {
        int biggestSize = 0;

        for (FlexOfferDto flexOfferDto : flexOfferDtoArray) {
            if (flexOfferDto.getPtus().size() > biggestSize) {
                biggestSize = flexOfferDto.getPtus().size();
            }
        }

        return biggestSize;
    }
}
