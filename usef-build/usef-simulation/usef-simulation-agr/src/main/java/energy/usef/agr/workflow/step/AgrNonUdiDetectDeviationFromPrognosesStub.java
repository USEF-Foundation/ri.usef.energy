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

import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.util.PowerContainerDtoUtil;
import energy.usef.agr.workflow.operate.deviation.NonUdiDetectDeviationFromPrognosisStepParameter;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a workflow step "AGRNonUdiDetectDeviationFromPrognosis".
 * <p>
 * This step makes dummy comparison of forecast usage obtained via the connection monitoring process with D-prognoses and A-plans.
 * <p>
 * This PBC takes the following parameters as input:
 * <ul>
 * <li>PERIOD</li> : the period the portfolio ({@link LocalDate}).
 * <li>PTU_DURATION</li> : the duration of PTU expressed in minutes ({@link Integer}).
 * <li>CURRENT_PTU_INDEX</li> : current ptu index ({@link Integer}).
 * <li>CONNECTION_PORTFOLIO_DTO</li> : a {@link ConnectionGroupPortfolioDto} containing the current portfolio.
 * <li>LATEST_PROGNOSIS</li> : a {@link PrognosisDto} containing the latest A-plans and/or D-prognoses.
 * </ul>
 * <p>
 * This PBC must output:
 * <ul>
 * <li>DEVIATION_INDEX_LIST</li> : a list of {@link Integer} which contains the ptu indexes with deviation.
 * </ul>
 */
public class AgrNonUdiDetectDeviationFromPrognosesStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrNonUdiDetectDeviationFromPrognosesStub.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext workflowContext) {
        // retrieve the PBC input
        LocalDate period = workflowContext.get(NonUdiDetectDeviationFromPrognosisStepParameter.IN.PERIOD.name(), LocalDate.class);
        int ptuDuration = workflowContext.get(NonUdiDetectDeviationFromPrognosisStepParameter.IN.PTU_DURATION.name(), Integer.class);
        int currentPtuIndex = workflowContext.get(NonUdiDetectDeviationFromPrognosisStepParameter.IN.CURRENT_PTU_INDEX.name(), Integer.class);
        String usefIdentifier = workflowContext.get(NonUdiDetectDeviationFromPrognosisStepParameter.IN.USEF_IDENTIFIER.name(), String.class);
        List<ConnectionPortfolioDto> connectionPortfolioDtos = workflowContext
                .get(NonUdiDetectDeviationFromPrognosisStepParameter.IN.CONNECTION_PORTFOLIO_DTO.name(), List.class);
        PrognosisDto latestPrognosis = workflowContext.get(NonUdiDetectDeviationFromPrognosisStepParameter.IN.LATEST_PROGNOSIS.name(), PrognosisDto.class);

        int numberOfPtusPerDay = PtuUtil.getNumberOfPtusPerDay(period, ptuDuration);
        PtuPrognosisDto[] ptuPrognosisDto = latestPrognosis.getPtus().stream().toArray(PtuPrognosisDto[]::new);
        BigInteger[] powerArray = createPowerArray(connectionPortfolioDtos, numberOfPtusPerDay, currentPtuIndex);

        List<Integer> deviationIndexList = new ArrayList<>();

        for (int ptuIndex = currentPtuIndex - 1; ptuIndex < powerArray.length; ptuIndex++) {
            if (powerArray[ptuIndex] == null || ptuPrognosisDto[ptuIndex].getPower() == null) {
                continue;
            }

            // Determine a min and a max (+5% -5% of the prognosis) check if forecast is not in between
            BigInteger allowedDeviation = ptuPrognosisDto[ptuIndex].getPower().divide(BigInteger.valueOf(20)).abs(); // 5%
            BigInteger maxForecastMargin = ptuPrognosisDto[ptuIndex].getPower().add(allowedDeviation);
            BigInteger minForecastMargin = ptuPrognosisDto[ptuIndex].getPower().subtract(allowedDeviation);

            if (powerArray[ptuIndex].compareTo(minForecastMargin) < 0 || powerArray[ptuIndex].compareTo(maxForecastMargin) > 0) {
                // Deviation detected
                Integer deviationPtuIndex = ptuIndex + 1;
                LOGGER.debug("Deviation detected for PTU Index {}. Forecast {} and Prognosis {} with allowed deviation {}",
                        deviationPtuIndex, powerArray[ptuIndex], ptuPrognosisDto[ptuIndex].getPower().doubleValue(),
                        allowedDeviation);
                deviationIndexList.add(deviationPtuIndex);
            }
        }

        workflowContext
                .setValue(NonUdiDetectDeviationFromPrognosisStepParameter.OUT.DEVIATION_INDEX_LIST.name(), deviationIndexList);

        return workflowContext;
    }

    /**
     * Creates an array of BigIntegers containing the averageConsumption - averageProduction + uncontrolledLoad per ptu.
     */
    private BigInteger[] createPowerArray(List<ConnectionPortfolioDto> connectionPortfolioDtos, int numberOfPtusPerDay,
            int currentPtuIndex) {
        BigInteger[] powerArray = new BigInteger[numberOfPtusPerDay];

        for (int ptuIndex = currentPtuIndex; ptuIndex <= numberOfPtusPerDay; ptuIndex++) {
            // List of all PowerContainerDto's for all connections for current ptu index
            final int finalPtuIndex = ptuIndex;
            List<PowerContainerDto> connectionPowerContainerDtoList = connectionPortfolioDtos.stream()
                    .map(connectionPortfolioDTO -> connectionPortfolioDTO.getConnectionPowerPerPTU().get(finalPtuIndex))
                    .collect(Collectors.toList());

            // Summed PowerContainerDto's for all connections for the current ptu index
            PowerContainerDto summedConnectionPowerContainer = PowerContainerDtoUtil.sum(connectionPowerContainerDtoList);

            // fetch the most accurate (metered, observed, forecast or profile) uncontrolled load, average consumption and average production
            BigInteger uncontrolledLoad = summedConnectionPowerContainer.getMostAccurateUncontrolledLoad();
            BigInteger averageConsumption = summedConnectionPowerContainer.getMostAccurateAverageConsumption();
            BigInteger averageProduction = summedConnectionPowerContainer.getMostAccurateAverageProduction();

            powerArray[ptuIndex - 1] = averageConsumption.subtract(averageProduction.abs()).add(uncontrolledLoad);
        }

        return powerArray;
    }

}
