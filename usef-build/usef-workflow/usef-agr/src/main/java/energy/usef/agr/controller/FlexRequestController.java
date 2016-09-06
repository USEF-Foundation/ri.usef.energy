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

import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.agr.service.business.AgrValidationBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexRequest;
import energy.usef.core.data.xml.bean.message.FlexRequestResponse;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.XMLUtil;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes flex request from DSO and BRP.
 */
@Stateless
public class FlexRequestController extends BaseIncomingMessageController<FlexRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlexRequestController.class);

    @Inject
    private Config config;

    @Inject
    private JMSHelperService jmsService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Inject
    private AgrValidationBusinessService agrValidationBusinessService;

    /**
     * {@inheritDoc}
     */
    public void action(FlexRequest request, Message savedMessage) throws BusinessException {
        LOGGER.info("FlexRequest received with conversation-id: {}", request.getMessageMetadata().getConversationID());

        FlexRequestResponse response = new FlexRequestResponse();
        MessageMetadata requestMetadata = request.getMessageMetadata();
        MessageMetadataBuilder messageMetadataBuilder = MessageMetadataBuilder.build(requestMetadata.getSenderDomain(),
                requestMetadata.getSenderRole(), config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.AGR, ROUTINE)
                .conversationID(requestMetadata.getConversationID());

        response.setMessageMetadata(messageMetadataBuilder.build());
        response.setSequence(request.getSequence());

        List<PTU> normalizedPtus = PtuListConverter.normalize(request.getPTU());
        LocalDate period = request.getPeriod();

        try {
            // Validation
            corePlanboardValidatorService.validatePTUsForPeriod(normalizedPtus, period, true);

            // Determine UsefIdentifer
            String usefIdentifier = determineUsefIdentifier(request);

            agrValidationBusinessService.validateConnectionGroup(usefIdentifier);

            agrValidationBusinessService.validatePTUsContainsDispositionRequested(normalizedPtus);
            agrValidationBusinessService.validatePrognosis(request.getPrognosisOrigin(), request.getPrognosisSequence());

            // Store request
            corePlanboardBusinessService.storeFlexRequest(usefIdentifier, request, DocumentStatus.ACCEPTED, request
                    .getMessageMetadata().getSenderDomain());

            if (USEFRole.BRP == requestMetadata.getSenderRole()) {
                // Update A-Plan status as superseded
                corePlanboardBusinessService.updatePrognosisStatus(request.getPrognosisSequence(), request.getMessageMetadata()
                        .getSenderDomain(), DocumentType.A_PLAN, DocumentStatus.PENDING_FLEX_TRADING);
            }

            // Send response
            sendResponse(response, null);
        } catch (BusinessValidationException validationException) {
            LOGGER.warn("The flex request with conversationId {} has errors and will be rejected: \n{}",
                    request.getMessageMetadata().getConversationID(), validationException.getMessage());

            sendResponse(response, validationException);
        }

    }

    private String determineUsefIdentifier(FlexRequest request) {
        String usefIdentifier = request.getCongestionPoint();
        // for the DSO its scongestionPoint, but for flexOrders from BRP it is the senderDomain.
        if (USEFRole.BRP.equals(request.getMessageMetadata().getSenderRole())) {
            usefIdentifier = request.getMessageMetadata().getSenderDomain();
        }
        return usefIdentifier;
    }

    private void sendResponse(FlexRequestResponse response, BusinessValidationException exception) {
        if (exception == null) {
            response.setResult(DispositionAcceptedRejected.ACCEPTED);
        } else {
            response.setResult(DispositionAcceptedRejected.REJECTED);
            response.setMessage(exception.getMessage());
        }

        // send the response xml to the out queue.
        jmsService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(response));

        LOGGER.info("Flex request response with conversation-id {} is sent to the outgoing queue.",
                response.getMessageMetadata().getConversationID());
    }

}
