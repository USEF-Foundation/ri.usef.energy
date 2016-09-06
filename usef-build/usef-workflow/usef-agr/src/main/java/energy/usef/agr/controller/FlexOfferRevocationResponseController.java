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

import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOfferRevocationResponse;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for {@link FlexOfferRevocationResponse} message.
 */
public class FlexOfferRevocationResponseController extends BaseIncomingResponseMessageController<FlexOfferRevocationResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlexOfferRevocationResponseController.class);

    @Inject
    private CorePlanboardBusinessService planboardBusinessService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(FlexOfferRevocationResponse message, Message savedMessage) throws BusinessException {
        long flexOfferSequenceNumber = message.getSequence();
        String conversationID = message.getMessageMetadata().getConversationID();
        USEFRole senderRole = message.getMessageMetadata().getSenderRole();
        String senderDomain = message.getMessageMetadata().getSenderDomain();

        if (message.getResult() == DispositionAcceptedRejected.REJECTED) {
            LOGGER.warn("FlexOfferRevocation message from conversation {} has been rejected by {} {}.",
                    conversationID, senderRole, senderDomain);
            return;
        }

        LOGGER.debug("FlexOfferRevocation message from conversation {} has been accepted by {} {}.",
                conversationID, senderRole, senderDomain);

        PlanboardMessage flexOfferMessage = planboardBusinessService.findSinglePlanboardMessage(flexOfferSequenceNumber,
                DocumentType.FLEX_OFFER, senderDomain);

        // Validation
        if (flexOfferMessage == null) {
            LOGGER.error("No corresponding plan board message found for the flex offer sequence number: {}, nothing was updated",
                    flexOfferSequenceNumber);
            return;
        }

        if (DocumentStatus.ACCEPTED != flexOfferMessage.getDocumentStatus()) {
            LOGGER.error("Related plan board message for the flex offer sequence number: {} was never ACCEPTED, nothing was updated",
                    flexOfferSequenceNumber);
            return;
        }

        // Setting status revoked
        flexOfferMessage.setDocumentStatus(DocumentStatus.REVOKED);

        LOGGER.debug("Corresponding plan board message records updated for the flex offer sequence number: {}",
                flexOfferSequenceNumber);
    }

}
