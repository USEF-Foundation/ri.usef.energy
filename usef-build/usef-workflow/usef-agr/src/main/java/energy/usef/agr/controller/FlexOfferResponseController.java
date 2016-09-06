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

package energy.usef.agr.controller;

import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.FlexOfferResponse;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DocumentStatusUtil;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process flexibility orders from DSO.
 */
@Stateless
public class FlexOfferResponseController extends BaseIncomingMessageController<FlexOfferResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexOfferResponseController.class);

    @Inject
    private CorePlanboardBusinessService planboardBusinessService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(FlexOfferResponse message, Message savedMessage) throws BusinessException {
        List<PlanboardMessage> flexOffers = planboardBusinessService.findPlanboardMessages(message.getSequence(),
                DocumentType.FLEX_OFFER, message.getMessageMetadata().getSenderDomain());

        // if document is no longer new, document should not be updated
        flexOffers.stream()
                .filter(flexOffer -> DocumentStatus.SENT.equals(flexOffer.getDocumentStatus()))
                .forEach(flexOffer -> flexOffer.setDocumentStatus(DocumentStatusUtil.toDocumentStatus(message.getResult())));

        LOGGER.debug("Corresponding plan board message records updated for the flex offer sequence number: {}",
                message.getSequence());
    }
}
