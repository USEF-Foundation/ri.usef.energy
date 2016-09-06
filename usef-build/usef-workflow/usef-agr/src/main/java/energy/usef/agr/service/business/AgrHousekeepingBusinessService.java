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

package energy.usef.agr.service.business;

import javax.inject.Inject;

import energy.usef.agr.repository.*;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.agr.repository.device.capability.DeviceCapabilityRepository;

/**
 * Service class in charge of housekeeping operations for the Aggregator.
 */
public class AgrHousekeepingBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrHousekeepingBusinessService.class);

    @Inject
    private PowerContainerRepository powerContainerRepository;

    @Inject
    private DeviceCapabilityRepository deviceCapabilityRepository;

    @Inject
    private UdiEventRepository udiEventRepository;

    @Inject
    private DeviceRequestRepository deviceRequestRepository;

    @Inject
    private DeviceMessageRepository deviceMessageRepository;

    @Inject
    private UdiRepository udiRepository;
    /**
     * Cleanup database for a given period.
     *
     * @param period
     */
    public void cleanup(LocalDate period) {
        int powerContainerCount = powerContainerRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PowerContainer objects.", powerContainerCount);

        int deviceCapabilityCount = deviceCapabilityRepository.cleanup(period);
        LOGGER.info("Cleaned up {} DeviceCapability objects.", deviceCapabilityCount);

        int udiEventRepositoryCount = udiEventRepository.cleanup(period);
        LOGGER.info("Cleaned up {} UdiEvent objects.", udiEventRepositoryCount);

        int deviceRequestRepositoryCount = deviceRequestRepository.cleanup(period);
        LOGGER.info("Cleaned up {} DeviceRequest objects.", deviceRequestRepositoryCount);

        int deviceMessageRepositoryCount = deviceMessageRepository.cleanup(period);
        LOGGER.info("Cleaned up {} DeviceMessage objects.", deviceMessageRepositoryCount);

        int udiRepositoryCount = udiRepository.cleanup(period);
        LOGGER.info("Cleaned up {} Udi objects.", udiRepositoryCount);
    }
}
