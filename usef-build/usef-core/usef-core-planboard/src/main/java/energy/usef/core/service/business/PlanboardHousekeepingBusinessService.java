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

package energy.usef.core.service.business;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.repository.ConnectionGroupStateRepository;
import energy.usef.core.repository.FlexOrderSettlementRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuFlexOfferRepository;
import energy.usef.core.repository.PtuFlexOrderRepository;
import energy.usef.core.repository.PtuFlexRequestRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.repository.PtuSettlementRepository;
import energy.usef.core.repository.PtuStateRepository;

/**
 * Business service class in charge of housekeeping operations concerning the common planboard.
 */
public class PlanboardHousekeepingBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanboardHousekeepingBusinessService.class);

    @Inject
    private FlexOrderSettlementRepository flexOrderSettlementRepository;
    @Inject
    private PlanboardMessageRepository planboardMessageRepository;
    @Inject
    private PtuContainerRepository ptuContainerRepository;
    @Inject
    private PtuFlexOfferRepository ptuFlexOfferRepository;
    @Inject
    private PtuFlexOrderRepository ptuFlexOrderRepository;
    @Inject
    private PtuPrognosisRepository ptuPrognosisRepository;
    @Inject
    private PtuFlexRequestRepository ptuFlexRequestRepository;
    @Inject
    private PtuSettlementRepository ptuSettlementRepository;
    @Inject
    private PtuStateRepository ptuStateRepository;
    @Inject
    private ConnectionGroupStateRepository connectionGroupStateRepository;

    /**
     * Cleanup core planboard data for a certain date.
     *
     * @param period
     * @return the number of {@link PtuFlexOrder}s deleted.
     */
    public void cleanup(LocalDate period) {

        int connectionGroupStateCount = connectionGroupStateRepository.cleanup(period);
        LOGGER.info("Cleaned up {} ConnectionGroupState objects", connectionGroupStateCount);

        int ptuSettlementCount = ptuSettlementRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PtuSettlement objects", ptuSettlementCount);

        int flexOrderSettlementCount = flexOrderSettlementRepository.cleanup(period);
        LOGGER.info("Cleaned up {} FlexOrderSettlement objects", flexOrderSettlementCount);

        int ptuFlexOrderCount = ptuFlexOrderRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PtuFlexOrder objects", ptuFlexOrderCount);

        int ptuFlexOfferCount = ptuFlexOfferRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PtuFlexOffer objects", ptuFlexOfferCount);

        int ptuFlexRequestCount = ptuFlexRequestRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PtuFlexRequest objects", ptuFlexRequestCount);

        int ptuPrognosisCount = ptuPrognosisRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PtuPrognosis objects", ptuPrognosisCount);

        int ptuStateCount = ptuStateRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PtuState objects", ptuStateCount);

        int ptuContainerCount = ptuContainerRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PtuContainer objects", ptuContainerCount);

        int planboardMessageCount = planboardMessageRepository.cleanup(period);
        LOGGER.info("Cleaned up {} PlanBoardMessage objects", planboardMessageCount);
    }
}
