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

import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOffer;
import energy.usef.core.data.xml.bean.message.FlexOfferResponse;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.XMLUtil;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Incoming FlexOffer controller.
 */
@Stateless
public class FlexOfferController extends BaseIncomingMessageController<FlexOffer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexOfferController.class);

    @Inject
    private JMSHelperService jmsService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Inject
    private Config config;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(FlexOffer request, Message savedMessage) throws BusinessException {
        LOGGER.info("FlexOffer received.");
        try {
            // validate
            corePlanboardValidatorService.validateCurrency(request.getCurrency());
            corePlanboardValidatorService.validateTimezone(request.getTimeZone());
            corePlanboardValidatorService.validatePTUDuration(request.getPTUDuration());
            if (!request.getPTU().isEmpty()) {
                corePlanboardValidatorService.validatePTUsForPeriod(request.getPTU(), request.getPeriod(),
                        false);
            }
            corePlanboardValidatorService.validateDomain(request.getFlexRequestOrigin());

            corePlanboardValidatorService
                    .validatePlanboardMessageExpirationDate(request.getFlexRequestSequence(), DocumentType.FLEX_REQUEST,
                            request.getMessageMetadata().getSenderDomain());

            String usefIdentifier = request.getMessageMetadata().getSenderDomain();

            // The Period the offer applies to should have at least one PTU that is not already pending settlement.
            corePlanboardValidatorService.validateIfPTUForPeriodIsNotInPhase(usefIdentifier,
                    request.getPeriod(), PtuContainerState.PendingSettlement, PtuContainerState.Settled);

            // store
            corePlanboardBusinessService.storeFlexOffer(usefIdentifier, request,
                    DocumentStatus.ACCEPTED, request.getMessageMetadata().getSenderDomain());
            // send response
            sendResponse(request, null);
        } catch (BusinessValidationException exception) {
            sendResponse(request, exception);
        }

    }

    private void sendResponse(FlexOffer request, BusinessValidationException exception) {
        FlexOfferResponse response = new FlexOfferResponse();

        MessageMetadata messageMetadata = MessageMetadataBuilder
                .build(request.getMessageMetadata().getSenderDomain(), request.getMessageMetadata().getSenderRole(),
                        config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.BRP, ROUTINE)
                .conversationID(request.getMessageMetadata().getConversationID()).build();
        response.setMessageMetadata(messageMetadata);
        response.setSequence(request.getSequence());
        if (exception == null) {
            response.setResult(DispositionAcceptedRejected.ACCEPTED);
        } else {
            response.setResult(DispositionAcceptedRejected.REJECTED);
            response.setMessage(exception.getMessage());
        }

        // send the response xml to the out queue.
        jmsService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(response));

        LOGGER.info("FlexOfferResponse with conversation-id {} is sent to the outgoing queue.",
                response.getMessageMetadata().getConversationID());
    }
}
