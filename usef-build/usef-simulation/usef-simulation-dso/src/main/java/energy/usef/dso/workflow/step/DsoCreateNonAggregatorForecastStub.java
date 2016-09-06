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

import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.plan.connection.forecast.DsoCreateNonAggregatorForecastParameter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a workflow step related to "Create forecasts for non-Aggregator connections".
 */
public class DsoCreateNonAggregatorForecastStub implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoCreateNonAggregatorForecastStub.class);

    @Inject
    private PbcFeederService pbcFeederService;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("DsoNonAggregatorForecast workflow with parameters: {}", context);

        LocalDate date = context.get(DsoCreateNonAggregatorForecastParameter.IN.PTU_DATE.name(), LocalDate.class);
        int ptuDuration = context.get(DsoCreateNonAggregatorForecastParameter.IN.PTU_DURATION.name(), Integer.class);
        String congestionPoint = context.get(DsoCreateNonAggregatorForecastParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), String.class);
        List<String> aggregators = context.get(DsoCreateNonAggregatorForecastParameter.IN.AGR_DOMAIN_LIST.name(), List.class);
        List<Long> counts = context.get(DsoCreateNonAggregatorForecastParameter.IN.AGR_CONNECTION_COUNT_LIST.name(), List.class);

        // Determine total number of non-aggregator connections we are handling.
        long nonAgrConnectionCount = 0;
        for (int i = 0; i < counts.size(); ++i) {
            if ((i >= aggregators.size()) || (aggregators.get(i) == null)) {
                nonAgrConnectionCount = counts.get(i);
                break;
            }
        }

        List<Long> powerList = new ArrayList<>();
        List<Long> maxLoadList = new ArrayList<>();

        if (nonAgrConnectionCount == 0) {
            LOGGER.debug("No non-aggregator connections found, so nothing to do.");
        } else {
            LOGGER.debug("Counted {} non-aggregator connections for congestion point {}.", nonAgrConnectionCount, congestionPoint);
            int nrOfPtus = PtuUtil.getNumberOfPtusPerDay(date, ptuDuration);

            LOGGER.debug("Retrieving UncontrolledLoad for congestion point {} from PBC Feeder.", congestionPoint);
            Map<Integer, BigDecimal> uncontrolledLoad = pbcFeederService.getUncontrolledLoadPerPtu(congestionPoint, date, 1,
                    nrOfPtus);

            LOGGER.debug("Retrieving PvLoadForecast from PBC Feeder.");
            Map<Integer, BigDecimal> pvLoadForecast = pbcFeederService.getPvLoadForecastPerPtu(date, 1, nrOfPtus);

            if (uncontrolledLoad.size() != nrOfPtus || pvLoadForecast.size() != nrOfPtus) {
                LOGGER.error("Error while retrieving uncontrolled load and pvLoad forecast from PBC feeder!");
            }

            for (Integer ptuIndex = 1; ptuIndex <= nrOfPtus; ptuIndex++) {
                BigDecimal forecast = (uncontrolledLoad.get(ptuIndex).add(pvLoadForecast.get(ptuIndex)))
                        .multiply(BigDecimal.valueOf(nonAgrConnectionCount));

                powerList.add(forecast.longValue());
                maxLoadList.add(forecast.longValue());
            }
        }

        context.setValue(DsoCreateNonAggregatorForecastParameter.OUT.POWER.name(), powerList);
        context.setValue(DsoCreateNonAggregatorForecastParameter.OUT.MAXLOAD.name(), maxLoadList);

        LOGGER.info("DsoNonAggregatorForecast workflow step complete");
        return context;
    }
}
