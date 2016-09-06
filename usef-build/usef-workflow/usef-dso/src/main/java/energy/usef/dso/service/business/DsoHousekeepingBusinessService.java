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

package energy.usef.dso.service.business;

import energy.usef.dso.repository.GridSafetyAnalysisRepository;
import energy.usef.dso.repository.NonAggregatorForecastRepository;
import energy.usef.dso.repository.PrognosisUpdateDeviationRepository;
import energy.usef.dso.repository.PtuGridMonitorRepository;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Service class in charge of housekeeping operations for the Distribution System Operator.
 */
public class DsoHousekeepingBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoHousekeepingBusinessService.class);

    @Inject
    private GridSafetyAnalysisRepository gridSafetyAnalysisRepository;
    @Inject
    private NonAggregatorForecastRepository nonAggregatorForecastRepository;
    @Inject
    private PrognosisUpdateDeviationRepository prognosisUpdateDeviationRepository;
    @Inject
    private PtuGridMonitorRepository ptuGridMonitorRepository;

    /**
     * Cleanup database for a given period.
     *
     * @param period
     */
    public void cleanup(LocalDate period) {
        int gridSafetyAnalysisCount = gridSafetyAnalysisRepository.cleanup(period);
        LOGGER.info("Cleaned up {} GridSafetyAnalysis objects.", gridSafetyAnalysisCount);

        int nonAggregatorForecastCount = nonAggregatorForecastRepository.cleanup(period);
        LOGGER.info("Cleaned up {} NonAggregatorForecast objects.", nonAggregatorForecastCount);

        int ptuGridMonitorCount = ptuGridMonitorRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PtuGridMonitor objects.", ptuGridMonitorCount);

        int prognosisUpdatedeviationCount = prognosisUpdateDeviationRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PrognosisUpdatedeviiation objects.", prognosisUpdatedeviationCount);
    }
}
