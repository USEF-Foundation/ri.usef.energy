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

package energy.usef.brp.service.business;

import energy.usef.core.config.Config;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuFlexOrderRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.DocumentStatusUtil;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class in charge of all the operations related to the BRP planboard.
 */
public class BrpPlanboardBusinessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrpPlanboardBusinessService.class);

    @Inject
    private PtuPrognosisRepository ptuPrognosisRepository;

    @Inject
    private PtuFlexOrderRepository ptuFlexOrderRepository;

    @Inject
    private PlanboardMessageRepository planboardMessageRepository;

    @Inject
    private Config config;


    /**
     * Find {@link PlanboardMessage} which have status ACCEPTED to know which offers needs to be processed.
     *
     * @return the {@link List} of {@link PlanboardMessage} of type {@link DocumentType#FLEX_OFFER} with status ACCEPTED.
     */
    public List<PlanboardMessage> findOrderableFlexOffers() {
        return planboardMessageRepository.findOrderableFlexOffers().stream().filter(this::validateOffer).collect(Collectors.toList());
    }

    /**
     * Update the flex orders in the planboard given.
     *
     * @param flexOrderSequence {@link Long} the flex order sequence number.
     * @param acknowledgementStatus the new {@link AcknowledgementStatus}
     * @param aggregatorDomain aggregator domain
     * @return Updated planboardMessage {@link PlanboardMessage} for flex order
     */
    public PlanboardMessage updateFlexOrdersWithAcknowledgementStatus(Long flexOrderSequence,
            AcknowledgementStatus acknowledgementStatus, String aggregatorDomain) {
        PlanboardMessage flexOrderMessage = planboardMessageRepository.findSinglePlanboardMessage(flexOrderSequence,
                DocumentType.FLEX_ORDER, aggregatorDomain);

        if (!DocumentStatus.SENT.equals(flexOrderMessage.getDocumentStatus())) {
            LOGGER.error("A response has already been processed for this flex order %s. Invalid response received " +
                            "from %s. ", flexOrderSequence, aggregatorDomain);
            return null;
        }

        flexOrderMessage.setDocumentStatus(DocumentStatusUtil.toDocumentStatus(acknowledgementStatus));

        List<PtuFlexOrder> ptuFlexOrders = ptuFlexOrderRepository.findFlexOrdersBySequence(flexOrderSequence);
        for (PtuFlexOrder ptuFlexOrder : ptuFlexOrders) {
            ptuFlexOrder.setAcknowledgementStatus(acknowledgementStatus);
        }

        return flexOrderMessage;
    }

    /**
     * Find all ACCEPTED flex orders within a period.
     *
     * @param startDate {@link LocalDate} start date of the period requested.
     * @param endDate {@link LocalDate} end date of the period requested.
     * @return a {@link List} of {@link PlanboardMessage}.
     */
    public List<PlanboardMessage> findAcceptedFlexOrders(LocalDate startDate, LocalDate endDate) {
        return planboardMessageRepository
                .findPlanboardMessages(DocumentType.FLEX_ORDER, startDate, endDate, DocumentStatus.ACCEPTED);
    }

    /**
     * Finalize pending A-Plans for the BRP by changing the document status of all planboard messages for certain period from
     * RECEIVED / PENDING_FLEX_TRADING into FINAL.
     *
     * @param period {@link LocalDate} period
     */
    public void finalizePendingAPlans(LocalDate period) {
        planboardMessageRepository.findPlanboardMessages(DocumentType.A_PLAN, period, null).stream().
                filter(planboardMessage -> planboardMessage.getDocumentStatus() == DocumentStatus.RECEIVED
                        || planboardMessage.getDocumentStatus() == DocumentStatus.PENDING_FLEX_TRADING).
                forEach(aPlan -> aPlan.setDocumentStatus(DocumentStatus.FINAL));
    }

    private boolean validateOffer(PlanboardMessage flexOfferMessage) {
        if (flexOfferMessage.getExpirationDate().isBefore(DateTimeUtil.getCurrentDateTime())) {
            flexOfferMessage.setDocumentStatus(DocumentStatus.EXPIRED);
            LOGGER.info("FlexOffer {} ignored, it expired {}.", flexOfferMessage.getSequence(),
                    flexOfferMessage.getExpirationDate());
            return false;
        }
        return true;
    }

}
