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

package energy.usef.brp.controller;

import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.FlexRequestResponse;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DocumentStatusUtil;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller class for incoming {@link FlexRequestResponse} messages.
 */
@Stateless
public class FlexRequestResponseController extends BaseIncomingResponseMessageController<FlexRequestResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexRequestResponseController.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(FlexRequestResponse message, Message savedMessage) throws BusinessException {
        LOGGER.info("Response for flex request from {} with sequence {} received.", message.getMessageMetadata().getSenderDomain()
                , message.getSequence());

        // Update the status of the corresponding flex request
        PlanboardMessage flexRequestMessage = corePlanboardBusinessService.findSinglePlanboardMessage(message.getSequence(),
                DocumentType.FLEX_REQUEST,
                message.getMessageMetadata().getSenderDomain());

        if (!DocumentStatus.SENT.equals(flexRequestMessage.getDocumentStatus())) {
            LOGGER.error("A response has already been processed for this flex request %s. Invalid response received " +
                            "from %s. ",
                    message.getSequence(), message.getMessageMetadata().getSenderDomain());
            return;
        }

        DocumentStatus documentStatus = DocumentStatusUtil.toDocumentStatus(message.getResult());
        flexRequestMessage.setDocumentStatus(documentStatus);
        if (DocumentStatus.REJECTED == documentStatus) {
            LOGGER.warn("Flex request from {} with sequence {} rejected.", message.getMessageMetadata().getSenderDomain()
                    , message.getSequence());
        }
    }
}
