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

package energy.usef.mdc.service.business;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.mdc.repository.AggregatorConnectionRepository;
import energy.usef.mdc.repository.CommonReferenceQueryStateRepository;

/**
 * Service class in charge of housekeeping operations for the Meter Data Company.
 */
public class MeterDataCompanyHousekeepingBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeterDataCompanyHousekeepingBusinessService.class);

    @Inject
    private AggregatorConnectionRepository aggregatorConnectionRepository;
    @Inject
    private CommonReferenceQueryStateRepository commonReferenceQueryStateRepository;

    /**
     * Cleanup database for a given period.
     *
     * @param period
     */
    public void cleanup(LocalDate period) {

        int aggregatorConnectionCount = aggregatorConnectionRepository.cleanup(period);
        LOGGER.info("Cleaned up {} AggregatorConnection objects.", aggregatorConnectionCount);

        int commonReferenceQueryStateCount = commonReferenceQueryStateRepository.cleanup(period);
        LOGGER.info("Cleaned up {} CommonReferenceQueryState objects.", commonReferenceQueryStateCount);
    }
}
