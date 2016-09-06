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

package energy.usef.dso.controller;

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
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.XMLUtil;
import energy.usef.dso.workflow.coloring.ColoringProcessEvent;

import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Incoming FlexRequestResponse controller.
 */
@Stateless
@Transactional
public class FlexOfferController extends BaseIncomingMessageController<FlexOffer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexOfferController.class);

    @Inject
    private JMSHelperService jmsService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Inject
    private Event<ColoringProcessEvent> coloringEventManager;

    @Inject
    private Config config;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(FlexOffer flexOffer, Message savedMessage) throws BusinessException {
        LOGGER.info("FlexOffer received.");

        try {
            PlanboardMessage flexRequestMessage = corePlanboardValidatorService
                    .validatePlanboardMessageExpirationDate(flexOffer.getFlexRequestSequence(), DocumentType.FLEX_REQUEST,
                            flexOffer.getMessageMetadata().getSenderDomain());

            // do some extra validation
            corePlanboardValidatorService.validateCurrency(flexOffer.getCurrency());
            corePlanboardValidatorService.validateTimezone(flexOffer.getTimeZone());
            corePlanboardValidatorService.validatePTUDuration(flexOffer.getPTUDuration());
            if (!flexOffer.getPTU().isEmpty()) {
                corePlanboardValidatorService.validatePTUsForPeriod(flexOffer.getPTU(), flexOffer.getPeriod(), false);
            }
            corePlanboardValidatorService.validateDomain(flexOffer.getFlexRequestOrigin());

            // The Period the offer applies to should have at least one PTU that is not already pending settlement.
            String usefIdentifier = flexOffer.getCongestionPoint();
            corePlanboardValidatorService.validateIfPTUForPeriodIsNotInPhase(usefIdentifier, flexOffer.getPeriod(),
                    PtuContainerState.PendingSettlement, PtuContainerState.Settled);

            // store the flex offer
            corePlanboardBusinessService.storeFlexOffer(usefIdentifier, flexOffer, DocumentStatus.ACCEPTED,
                    flexOffer.getMessageMetadata().getSenderDomain());

            // update status (RECEIVED_OFFER or RECEIVED_EMPTY_OFFER) of the flex requests
            updateFlexRequestsStatus(flexOffer, flexRequestMessage);

            // eventually start the coloring process
            if (isAllowedToStartColoringProcess(flexOffer)) {
                startColoringProcess(flexOffer);
            }

            // send response
            sendResponse(flexOffer, null);
        } catch (BusinessValidationException exception) {
            sendResponse(flexOffer, exception);
        }

    }

    /**
     * Check if the coloring process needs to be called. Only needed if there are only non-processed FLEX_REQUESTS with
     * DocumentStatus RECEIVED_EMPTY_OFFER for the same day and congestion point.
     * 
     * @param flexOffer
     * @return
     */
    private boolean isAllowedToStartColoringProcess(FlexOffer flexOffer) {
        // retrieve a list with all flex requests planboard messages of the same day and congestion point.
        List<PlanboardMessage> flexRequests = corePlanboardBusinessService.findPlanboardMessagesForConnectionGroup(
                flexOffer.getCongestionPoint(), null, DocumentType.FLEX_REQUEST, flexOffer.getPeriod(), null);

        // remove all flex requests planboard messages with status RECEIVED_EMPTY_OFFER or REJECTED.
        List<PlanboardMessage> flexRequestsWithOffers = flexRequests
                .stream()
                .filter(flexRequest -> flexRequest.getDocumentStatus() != DocumentStatus.RECEIVED_EMPTY_OFFER
                        && flexRequest.getDocumentStatus() != DocumentStatus.REJECTED)
                .collect(Collectors.toList());

        return flexRequestsWithOffers.isEmpty();
    }

    /**
     * Start the coloring process to determine if PTU(s) become orange.
     */
    private void startColoringProcess(FlexOffer flexOffer) {
        coloringEventManager.fire(new ColoringProcessEvent(flexOffer.getPeriod(), flexOffer.getCongestionPoint()));
    }

    /**
     * Updates planboard message with status RECEIVED_OFFER (flex offer received with PTU data available) or RECEIVED_EMPTY_OFFER
     * (empty flex offer received without PTU data).
     * 
     * @param flexOffer
     */
    private void updateFlexRequestsStatus(FlexOffer flexOffer, PlanboardMessage flexRequestMessage) {
        if (flexOffer.getPTU().isEmpty()) {
            flexRequestMessage.setDocumentStatus(DocumentStatus.RECEIVED_EMPTY_OFFER);
        } else {
            flexRequestMessage.setDocumentStatus(DocumentStatus.RECEIVED_OFFER);
        }
    }

    private void sendResponse(FlexOffer request, BusinessValidationException exception) {
        FlexOfferResponse response = new FlexOfferResponse();

        MessageMetadata messageMetadata = MessageMetadataBuilder
                .build(request.getMessageMetadata().getSenderDomain(), request.getMessageMetadata().getSenderRole(),
                        config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.DSO, ROUTINE)
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
