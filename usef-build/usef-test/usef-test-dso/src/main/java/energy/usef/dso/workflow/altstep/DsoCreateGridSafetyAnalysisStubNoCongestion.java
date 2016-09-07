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
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.NonAggregatorForecastDto;
import energy.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.PtuNonAggregatorForecastDto;
import energy.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

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
public class DsoCreateGridSafetyAnalysisStubNoCongestion implements WorkflowStep {
    //Adjusted to never have congestion
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoCreateGridSafetyAnalysisStubNoCongestion.class);
    private static final int MAX_LOAD_CONGESTION_POINT = 100000; //original : 100

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("Started invoke method");

        String congestionPointEntityAddres = (String) context.getValue(
                CreateGridSafetyAnalysisStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name());
        NonAggregatorForecastDto nonAggregatorForecastDto = (NonAggregatorForecastDto) context.getValue(
                CreateGridSafetyAnalysisStepParameter.IN.NON_AGGREGATOR_FORECAST.name());
        List<PrognosisDto> dPrognosisInputList = (List<PrognosisDto>) context.getValue(
                CreateGridSafetyAnalysisStepParameter.IN.D_PROGNOSIS_LIST.name());

        GridSafetyAnalysisDto gridSafetyAnalysisDto = generateGridSafety(nonAggregatorForecastDto, dPrognosisInputList,
                congestionPointEntityAddres);

        context.setValue(CreateGridSafetyAnalysisStepParameter.OUT.GRID_SAFETY_ANALYSIS.name(), gridSafetyAnalysisDto);

        return context;
    }

    private GridSafetyAnalysisDto generateGridSafety(NonAggregatorForecastDto nonAggregatorForecastDto,
            List<PrognosisDto> prognosisList, String entityAddress) {

        LOGGER.debug(" ### DEBUG ### input list (Non-Aggregator Forecast) size: [{}].", nonAggregatorForecastDto.getPtus().size());

        LOGGER.debug(" ### DEBUG ### input list (D-Prognosis) size: [{}].", prognosisList.size());

        // Go through all Non-Aggregator Forecast records, because these contain all possible records
        GridSafetyAnalysisDto gridSafetyAnalysisDto = new GridSafetyAnalysisDto();
        gridSafetyAnalysisDto.setEntityAddress(entityAddress);
        gridSafetyAnalysisDto.setPtuDate(nonAggregatorForecastDto.getPtuDate());

        for (PtuNonAggregatorForecastDto ptuNonAggregatorForecastDto : nonAggregatorForecastDto.getPtus()) {

            long nonAggregatorForecastPower = ptuNonAggregatorForecastDto.getPower();
            long dPrognosisPower = 0;
            long gridSafetyAnalysisPower = 0;
            LocalDate ptuDate = nonAggregatorForecastDto.getPtuDate();
            int ptuIndex = ptuNonAggregatorForecastDto.getPtuIndex();
            PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto = new PtuGridSafetyAnalysisDto();
            gridSafetyAnalysisDto.getPtus().add(ptuGridSafetyAnalysisDto);

            // find power value(s) in d-prognosis
            for (PrognosisDto prognosisDto : prognosisList) {
                if (!prognosisDto.getPeriod().equals(ptuDate)) {
                    continue;
                }
                for (PtuPrognosisDto ptuPrognosisDto : prognosisDto.getPtus()) {
                    if (BigInteger.valueOf(ptuIndex).compareTo(ptuPrognosisDto.getPtuIndex()) == 0) {
                        dPrognosisPower = dPrognosisPower + ptuPrognosisDto.getPower().longValue();
                    }

                }
            }

            LOGGER.debug(" ### DEBUG ### nonAgrPower [{}], dProgPower [{}], ptuDate [{}], ptuIndex [{}]",
                    nonAggregatorForecastPower, dPrognosisPower, ptuDate, ptuIndex);

            // calculate total power for Grid Safety Analysis
            gridSafetyAnalysisPower = nonAggregatorForecastPower + dPrognosisPower;

            ptuGridSafetyAnalysisDto.setPtuIndex(ptuIndex);

            // Determine requested power, either requested reduction of consumption or requested reduction of production
            if (Math.abs(gridSafetyAnalysisPower) > MAX_LOAD_CONGESTION_POINT) {
                long requestedPower = 0;

                if (gridSafetyAnalysisPower >= 0) {
                    requestedPower = MAX_LOAD_CONGESTION_POINT - gridSafetyAnalysisPower;
                } else {
                    requestedPower = -MAX_LOAD_CONGESTION_POINT - gridSafetyAnalysisPower;
                }
                LOGGER.debug(" ### DEBUG ### RequestedPower [{}]", requestedPower);

                ptuGridSafetyAnalysisDto.setPower(requestedPower);
                ptuGridSafetyAnalysisDto.setDisposition(DispositionTypeDto.REQUESTED);

            } else if (gridSafetyAnalysisPower >= 0) {
                // Check available consumption
                long availableConsumptionPower = 0;
                availableConsumptionPower = MAX_LOAD_CONGESTION_POINT - gridSafetyAnalysisPower;
                LOGGER.debug(" ### DEBUG ### Available Consumption Power [{}]", availableConsumptionPower);

                ptuGridSafetyAnalysisDto.setPower(availableConsumptionPower);
                ptuGridSafetyAnalysisDto.setDisposition(DispositionTypeDto.AVAILABLE);

            } else if (gridSafetyAnalysisPower < 0) {
                // Check available production
                long availableProductionPower = 0;
                availableProductionPower = -MAX_LOAD_CONGESTION_POINT - gridSafetyAnalysisPower;
                LOGGER.debug(" ### DEBUG ### Available Production Power [{}]", availableProductionPower);

                ptuGridSafetyAnalysisDto.setPower(availableProductionPower);
                ptuGridSafetyAnalysisDto.setDisposition(DispositionTypeDto.AVAILABLE);
            }
        }
        return gridSafetyAnalysisDto;
    }

}
