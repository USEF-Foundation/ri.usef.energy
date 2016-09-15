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

import energy.usef.agr.service.business.PrognosisResponseBusinessService;
import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.PrognosisResponse;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.event.RequestMoveToValidateEvent;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DocumentStatusUtil;

import java.util.List;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Response controller {@link PrognosisResponseController}.
 */
@Stateless
public class PrognosisResponseController extends BaseIncomingResponseMessageController<PrognosisResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrognosisResponseController.class);

    @Inject
    private PrognosisResponseBusinessService prognosisResponseBusinessService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private Event<RequestMoveToValidateEvent> moveToValidateEventManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(PrognosisResponse message, Message savedMessage) throws BusinessException {
        LOGGER.info("PrognosisResponse for Prognosis {} received.", message.getPrognosisSequence());
        List<Long> flexOrderSequences = prognosisResponseBusinessService.getNotValidatedSequences(message);

        if (!flexOrderSequences.isEmpty()) {
            reportPermanentError(message.getMessageMetadata().getConversationID(), flexOrderSequences);
        }

        String senderDomain = message.getMessageMetadata().getSenderDomain();
        DocumentStatus documentStatus = DocumentStatusUtil.toDocumentStatus(message.getResult());
        if (DocumentStatus.REJECTED == documentStatus) {
            LOGGER.warn("Prognosis {} was rejected by {}.", message.getPrognosisSequence(), senderDomain);
        } else {
            LOGGER.info("Prognosis {} accepted by {}.", message.getPrognosisSequence(), senderDomain);
        }

        DocumentType documentType = message.getMessageMetadata().getSenderRole() == USEFRole.DSO ? DocumentType.D_PROGNOSIS
                : DocumentType.A_PLAN;


        PlanboardMessage originalPrognosis = corePlanboardBusinessService.findSinglePlanboardMessage(
                message.getPrognosisSequence(), documentType, senderDomain);

        if (!DocumentStatus.SENT.equals(originalPrognosis.getDocumentStatus())) {
            LOGGER.error("A response has already been processed for this prognosis %s. Invalid response received " +
                    "from %s. ",  message.getPrognosisSequence(), senderDomain);
            return;
        }

        // Update status
        corePlanboardBusinessService.updatePrognosisStatus(message.getPrognosisSequence(), senderDomain, documentType,
                documentStatus);

        if ((originalPrognosis != null) && (documentType == DocumentType.A_PLAN)) {
           // Fire an event to move to Validate phase if possible
            moveToValidateEventManager.fire(new RequestMoveToValidateEvent(originalPrognosis.getPeriod()));
        }
    }

    /**
     * Stores a permanent error of a prognosis message which could not be validated by the dso.
     *
     * @param conversationId ConversationId of the orignal Message
     * @param sequences
     */
    private void reportPermanentError(String conversationId, List<Long> sequences) {
        String errorMessage = "Prognosis could not be validated. Flexoffer: " + sequences.get(0);

        for (int i = 1; i < sequences.size(); ++i) {
            errorMessage = errorMessage + ", " + sequences.get(i);
        }
        LOGGER.warn(errorMessage);
        messageService.storeMessageError(messageService.getInitialMessageOfConversation(conversationId), errorMessage, 0);
    }

}
