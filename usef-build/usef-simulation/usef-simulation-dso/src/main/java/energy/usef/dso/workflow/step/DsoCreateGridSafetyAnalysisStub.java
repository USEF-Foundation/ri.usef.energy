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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.NonAggregatorForecastDto;
import energy.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.PtuNonAggregatorForecastDto;
import energy.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter;
import energy.usef.pbcfeeder.dto.PbcPowerLimitsDto;

/**
 * Implementation of a workflow step generating the Grid Safety Analysis. The step works as follows: - The step retrieves the
 * Non-Aggregator forecast and D-prognosis forecast - The step goes through the Non-Aggregator forecast, because this forecast
 * contains all possible values for congestion point, PTU date and PTU index - The step combines the forecasted power of the
 * Non-Aggregator forecast with the forecast in the D-Prognosis - Based on the prognosis, the disposition is determined: - A Max
 * load for a congestion point can be set. If the total forecasted power is within this value (+ or -), there is no congestion.
 * Available power is calculated based on the max load and used for the grid safety analysis. - When the forecasted power is not
 * within the max load, the requested power is calculated in order to reduce production or consumption. This value is added to the
 * grid safety analysis.
 */
public class DsoCreateGridSafetyAnalysisStub implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoCreateGridSafetyAnalysisStub.class);

    @Inject
    private PbcFeederService pbcFeederService;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("Grid Safety Analysis invoked with context: {}", context);

        String congestionPointEntityAddress = context.get(CreateGridSafetyAnalysisStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), String.class);
        NonAggregatorForecastDto nonAggregatorForecastDto = context.get(CreateGridSafetyAnalysisStepParameter.IN.NON_AGGREGATOR_FORECAST.name(),
                NonAggregatorForecastDto.class);
        List<PrognosisDto> dPrognosisInputList = (List<PrognosisDto>) context.getValue(
                CreateGridSafetyAnalysisStepParameter.IN.D_PROGNOSIS_LIST.name());
        LocalDate period = context.get(CreateGridSafetyAnalysisStepParameter.IN.PERIOD.name(), LocalDate.class);

        PbcPowerLimitsDto powerLimitsDto = fetchPowerLimits(congestionPointEntityAddress);
        Map<Integer, BigInteger> totalLoadPerPtu = computeTotalLoad(nonAggregatorForecastDto, dPrognosisInputList);
        GridSafetyAnalysisDto gridSafetyAnalysisDto = buildGridSafetyAnalysis(congestionPointEntityAddress, period, powerLimitsDto,
                totalLoadPerPtu);

        context.setValue(CreateGridSafetyAnalysisStepParameter.OUT.GRID_SAFETY_ANALYSIS.name(), gridSafetyAnalysisDto);

        return context;
    }

    private PbcPowerLimitsDto fetchPowerLimits(String congestionPointEntityAddress) {
        return pbcFeederService.getCongestionPointPowerLimits(congestionPointEntityAddress);
    }

    /**
     * Computes the total load from the Non-Aggregator forecast and the different available prognoses.
     *
     * @param nonAggregatorForecastDto {@link NonAggregatorForecastDto} the non-aggregator forecast.
     * @param prognoses                {@link List} of {@link PrognosisDto}.
     * @return a {@link Map} with the PTU index as key ({@link Integer}) and the total load as value ({@link BigInteger}).
     */
    private Map<Integer, BigInteger> computeTotalLoad(NonAggregatorForecastDto nonAggregatorForecastDto,
                                                      List<PrognosisDto> prognoses) {
        Map<Integer, PtuNonAggregatorForecastDto> nonAggregatorForecastPerPtu = nonAggregatorForecastDto.getPtus()
                .stream()
                .collect(Collectors.toMap(PtuNonAggregatorForecastDto::getPtuIndex, Function.identity()));
        Map<Integer, Optional<BigInteger>> prognosisPowerPerPtu = prognoses.stream()
                .flatMap(prognosis -> prognosis.getPtus().stream())
                .collect(Collectors.groupingBy(ptuPrognosisDto -> ptuPrognosisDto.getPtuIndex().intValue(),
                        Collectors.mapping(PtuPrognosisDto::getPower, Collectors.reducing(BigInteger::add))));
        Map<Integer, BigInteger> totalPowerPerPtu = new HashMap<>();
        for (Integer ptuIndex : prognosisPowerPerPtu.keySet()) {
            totalPowerPerPtu.put(ptuIndex, prognosisPowerPerPtu.get(ptuIndex)
                    .orElse(BigInteger.ZERO)
                    .add(BigInteger.valueOf(nonAggregatorForecastPerPtu.get(ptuIndex).getPower())));
        }
        return totalPowerPerPtu;
    }

    private GridSafetyAnalysisDto buildGridSafetyAnalysis(String congestionPointEntityAddress, LocalDate period,
                                                          PbcPowerLimitsDto powerLimitsDto, Map<Integer, BigInteger> totalLoadPerPtu) {
        GridSafetyAnalysisDto gridSafetyAnalysisDto = new GridSafetyAnalysisDto();
        gridSafetyAnalysisDto.setEntityAddress(congestionPointEntityAddress);
        gridSafetyAnalysisDto.setPtuDate(period);
        for (Integer ptuIndex : totalLoadPerPtu.keySet()) {
            PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto = new PtuGridSafetyAnalysisDto();
            ptuGridSafetyAnalysisDto.setPtuIndex(ptuIndex);
            DispositionTypeDto dispositionTypeDto = fetchDisposition(powerLimitsDto, totalLoadPerPtu.get(ptuIndex));
            BigInteger power = fetchPowerValue(dispositionTypeDto, powerLimitsDto, totalLoadPerPtu.get(ptuIndex));
            LOGGER.trace("Disposition [{}] and power [{}]W for grid safety analysis for PTU [{}]", dispositionTypeDto, power,
                    ptuIndex);
            ptuGridSafetyAnalysisDto.setDisposition(dispositionTypeDto);
            ptuGridSafetyAnalysisDto.setPower(power.longValue());
            gridSafetyAnalysisDto.getPtus().add(ptuGridSafetyAnalysisDto);
        }
        return gridSafetyAnalysisDto;
    }

    /**
     * Fetches the required disposition for the grid safety analysis.
     * <p>
     * Disposition will be AVAILABLE if the total load is lesser than or equal to the Consumption Limit (Upper Limit) or if the
     * total load is bigger than or equal to the Production limit (Lower Limit).
     *
     * @param powerLimitsDto {@link PbcPowerLimitsDto} the power limits.
     * @param totalLoad      {@link BigInteger} the total load for the PTU.
     * @return {@link DispositionTypeDto} the disposition for the PTU.
     */
    private DispositionTypeDto fetchDisposition(PbcPowerLimitsDto powerLimitsDto, BigInteger totalLoad) {
        if (totalLoad.compareTo(powerLimitsDto.getUpperLimit().toBigInteger()) != 1
                && totalLoad.compareTo(powerLimitsDto.getLowerLimit().toBigInteger()) != -1) {
            return DispositionTypeDto.AVAILABLE;
        }
        return DispositionTypeDto.REQUESTED;
    }

    /**
     * Fetches the required power value for the grid safety analysis.
     * <p>
     * If the disposition is AVAILABLE, power will be the difference between Consumption Limit and Total Load (if load bigger
     * than 0) or the difference between Production Limit and Total Load (if load lesser than or equal to 0).
     * <p>
     * If the disposition is REQUESTED, power will be the difference between Total Load and Consumption Limit (if load bigger
     * than Consumption Limit) or the difference between Total Load and Production Limit (if load lesser than Production Limit).
     *
     * @param disposition    {@link DispositionTypeDto} the already known disposition for the PTU of the grid safety analysis.
     * @param powerLimitsDto {@link PbcPowerLimitsDto} the power limits.
     * @param totalLoad      {@link BigInteger} the total load for the PTU.
     * @return a {@link BigInteger} which is the power for the grid safety analysis.
     */
    private BigInteger fetchPowerValue(DispositionTypeDto disposition, PbcPowerLimitsDto powerLimitsDto, BigInteger totalLoad) {
        BigInteger power = null;
        if (disposition == DispositionTypeDto.AVAILABLE) {
            if (totalLoad.compareTo(BigInteger.ZERO) == 1) {
                power = powerLimitsDto.getUpperLimit().toBigInteger().subtract(totalLoad);
            } else {
                power = powerLimitsDto.getLowerLimit().toBigInteger().subtract(totalLoad);
            }
        }
        if (disposition == DispositionTypeDto.REQUESTED) {
            if (totalLoad.compareTo(powerLimitsDto.getUpperLimit().toBigInteger()) == 1) {
                power = totalLoad.subtract(powerLimitsDto.getUpperLimit().toBigInteger());
            } else {
                power = totalLoad.subtract(powerLimitsDto.getLowerLimit().toBigInteger());
            }
        }
        return power;
    }

}
