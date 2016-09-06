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
import energy.usef.agr.workflow.operate.deviation.DetectDeviationFromPrognosisStepParameter;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a workflow step "AGRDetectDeviationFromPrognosis".
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
public class AgrDetectDeviationFromPrognosesStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrDetectDeviationFromPrognosesStub.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext workflowContext) {
        LOGGER.debug("Started");

        // retrieve the PBC input
        LocalDate period = workflowContext.get(DetectDeviationFromPrognosisStepParameter.IN.PERIOD.name(), LocalDate.class);
        int ptuDuration = workflowContext.get(DetectDeviationFromPrognosisStepParameter.IN.PTU_DURATION.name(), Integer.class);
        int currentPtuIndex = workflowContext.get(DetectDeviationFromPrognosisStepParameter.IN.CURRENT_PTU_INDEX.name(), Integer.class);
        String usefIdentifier = workflowContext.get(DetectDeviationFromPrognosisStepParameter.IN.USEF_IDENTIFIER.name(), String.class);
        List<ConnectionPortfolioDto> connectionPortfolioDtos = workflowContext
                .get(DetectDeviationFromPrognosisStepParameter.IN.CONNECTION_PORTFOLIO_DTO.name(), List.class);
        PrognosisDto latestPrognosis = workflowContext.get(DetectDeviationFromPrognosisStepParameter.IN.LATEST_PROGNOSIS.name(), PrognosisDto.class);

        int numberOfPtusPerDay = PtuUtil.getNumberOfPtusPerDay(period, ptuDuration);
        PtuPrognosisDto[] ptuPrognosisDto = latestPrognosis.getPtus().stream().toArray(PtuPrognosisDto[]::new);
        BigInteger[] forecastArray = createPowerArray(connectionPortfolioDtos, period, numberOfPtusPerDay, ptuDuration);

        List<Integer> deviationIndexList = new ArrayList<>();

        for (int ptuIndex = currentPtuIndex - 1; ptuIndex < forecastArray.length; ptuIndex++) {
            if (forecastArray[ptuIndex] == null || ptuPrognosisDto[ptuIndex].getPower() == null) {
                continue;
            }

            // Determine a min and a max (+5% -5% of the prognosis) check if forecast is not in between
            BigInteger allowedDeviation = ptuPrognosisDto[ptuIndex].getPower().divide(BigInteger.valueOf(20)).abs(); // 5%
            BigInteger maxForecastMargin = ptuPrognosisDto[ptuIndex].getPower().add(allowedDeviation);
            BigInteger minForecastMargin = ptuPrognosisDto[ptuIndex].getPower().subtract(allowedDeviation);

            if (forecastArray[ptuIndex].compareTo(minForecastMargin) < 0
                    || forecastArray[ptuIndex].compareTo(maxForecastMargin) > 0) {
                // Deviation detected
                Integer deviationPtuIndex = ptuIndex + 1;
                LOGGER.debug("Deviation detected for PTU Index {}. Forecast {} and Prognosis {} with allowed deviation {}",
                        deviationPtuIndex, forecastArray[ptuIndex], ptuPrognosisDto[ptuIndex].getPower().doubleValue(),
                        allowedDeviation);
                deviationIndexList.add(deviationPtuIndex);
            }
        }

        workflowContext.setValue(DetectDeviationFromPrognosisStepParameter.OUT.DEVIATION_INDEX_LIST.name(), deviationIndexList);

        return workflowContext;
    }

    /**
     * Creates an array of BigIntegers containing the averageConsumption - averageProduction + uncontrolledLoad per ptu.
     */
    private BigInteger[] createPowerArray(List<ConnectionPortfolioDto> connectionPortfolioDtos, LocalDate period,
            int numberOfPtusPerDay, int ptuDuration) {
        BigInteger[] powerArray = new BigInteger[numberOfPtusPerDay];

        // add the power values from the connection level
        connectionPortfolioDtos.stream().map(ConnectionPortfolioDto::getConnectionPowerPerPTU).forEach(
                connectionPowerPerPtuMap -> connectionPowerPerPtuMap.forEach(
                        (ptuIndex, powerContainerDto) -> powerArray[ptuIndex - 1] = calculatePower(powerArray[ptuIndex - 1],
                                powerContainerDto)));

        // add the power values from the UDI level
        connectionPortfolioDtos.stream()
                .flatMap(connectionPortfolioDTO -> connectionPortfolioDTO.getUdis().stream())
                .map(udi -> PowerContainerDtoUtil.average(udi, period, ptuDuration)).forEach(
                powerContainerPerPtu -> powerContainerPerPtu.forEach(
                        (ptuIndex, powerContainerDto) -> powerArray[ptuIndex - 1] = calculatePower(powerArray[ptuIndex - 1],
                                powerContainerDto)));

        return powerArray;
    }

    private BigInteger calculatePower(BigInteger power, PowerContainerDto powerContainerDto) {
        BigInteger result = powerContainerDto.getForecast().calculatePower();
        if (power != null) {
            result = result.add(power);
        }
        return result;
    }

}
