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
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Workflow step implementation for the Workflow 'Create Non Aggregator Forecast'.
 * This stub creates a non-AGR forecast without using the PBC feeder.
 *
 * This implementation expects to find the following parameters as input:
 * <ul>
 * <li>PTU_DATE: the date for which the non-AGR forecast should be created ({@link String})</li>
 * <li>PTU_DURATION: The duration of one PTU ({@link String})</li>
 * <li>CONGESTION_POINT_ENTITY_ADDRESS: the entity address of the congestion point ({@link String})</li>
 * <li>AGR_DOMAIN_LIST: List of Aggregators ({@link String})</li>
 * <li>AGR_CONNECTION_COUNT_LIST: Number of non-AGR connections ({@link LocalDate})</li>
 * </ul>
 */
public class DsoCreateNonAggregatorForecastStubWithoutFeeder implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoCreateNonAggregatorForecastStubWithoutFeeder.class);

    // Number of random PTU's which have BIG power consumption (x 2)
    private static final int NUM_BIG_PTUS = 5;

    // Parameters available to the workflow step.
    private static final String PTU_DATE = "PTU_DATE";
    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "CONGESTION_POINT_ENTITY_ADDRESS";
    private static final String AGR_DOMAIN_LIST = "AGR_DOMAIN_LIST";
    private static final String AGR_CONNECTION_COUNT_LIST = "AGR_CONNECTION_COUNT_LIST";
    private static final String PTU_DURATION = "PTU_DURATION";

    // Parameters to be produced by the workflow step. Each of them are expected to be arrays of long.

    // In Watt.
    private static final String POWER = "POWER";
    // In Watt.
    private static final String MAXLOAD = "MAXLOAD";

    private static final int MINUTES_PER_DAY = Minutes.standardMinutesIn(Days.ONE).getMinutes();

    // 8000 kWh/j, arbitrary value to return for each non-aggregator connection.
    private static final int POWER_VALUE = 8000000 / (365 * 24);
    // 9000 kWh/j, arbitrary value to return for each non-aggregator connection.
    private static final int MAXLOAD_VALUE = 9000000 / (365 * 24);

    private static final Random RANDOM = new Random(new Date().getTime());

    /*
     * (non-Javadoc)
     * 
     * @see WorkflowStep#invoke(WorkflowContext)
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {

        LocalDate date = (LocalDate) context.getValue(PTU_DATE);
        int ptuDuration = (int) context.getValue(PTU_DURATION);
        String congestionPoint = (String) context.getValue(CONGESTION_POINT_ENTITY_ADDRESS);
        @SuppressWarnings("unchecked")
        List<String> aggregators = (List<String>) context.getValue(AGR_DOMAIN_LIST);
        @SuppressWarnings("unchecked")
        List<Long> counts = (List<Long>) context.getValue(AGR_CONNECTION_COUNT_LIST);

        LOGGER.info("DsoNonAggregatorForecast workflow step for {} at {} started", congestionPoint, date);

        int nrOfPtus = MINUTES_PER_DAY / ptuDuration;

        /* Determine total number of non-aggregator connections we are handling. */
        long totalcount = 0;
        for (int i = 0; i < counts.size(); ++i) {
            if ((i >= aggregators.size()) || (aggregators.get(i) == null)) {
                totalcount = counts.get(i);
                break;
            }
        }

        LOGGER.info("Random maxload and power generated for {} non-aggregator connections on congestion point {}.", totalcount,
                congestionPoint);
        List<Long> power = new ArrayList<>();
        List<Long> maxload = new ArrayList<>();

        // Random deviation +/- 20%
        int correction = (RANDOM.nextInt(POWER_VALUE * 2) - POWER_VALUE) / 5;
        for (int i = 0; i < nrOfPtus; ++i) {
            power.add(i, (POWER_VALUE + correction) * totalcount);
            maxload.add(i, MAXLOAD_VALUE * totalcount);
        }
        // For NUM_BIG_PTUS randomly chosen PTU's, multiply by 2
        for (int i = 0; i < NUM_BIG_PTUS; ++i) {
            int randomPtuIndex = RANDOM.nextInt(nrOfPtus);
            power.set(randomPtuIndex, power.get(randomPtuIndex) * 2);
        }

        context.setValue(POWER, power);
        context.setValue(MAXLOAD, maxload);

        LOGGER.info("DsoNonAggregatorForecast workflow step complete");
        return context;
    }
}
