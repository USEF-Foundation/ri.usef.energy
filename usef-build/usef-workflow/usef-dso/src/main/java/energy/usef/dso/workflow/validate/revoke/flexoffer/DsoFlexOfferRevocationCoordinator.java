/*
 * Copyright 2015 USEF Foundation
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

package energy.usef.dso.workflow.validate.revoke.flexoffer;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOfferRevocation;
import energy.usef.core.data.xml.bean.message.FlexOfferRevocationResponse;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.XMLUtil;

import java.util.List;
import java.util.Map;

import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class for the workflow 'Revoke Flex Offer' (DSO side).
 */
@Singleton
public class DsoFlexOfferRevocationCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoFlexOfferRevocationCoordinator.class);

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private CorePlanboardBusinessService planboardBusinessService;

    @Inject
    private CorePlanboardValidatorService planboardValidatorService;

    @Inject
    private Config config;

    /**
     * {@inheritDoc}
     */
    public void handleEvent(@Observes FlexOfferRevocationEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        FlexOfferRevocation flexOfferRevocation = event.getFlexOfferRevocation();

        DispositionAcceptedRejected responseResult = DispositionAcceptedRejected.ACCEPTED;
        String responseResultMessage = "Flex Offer has been revoked.";

        List<PlanboardMessage> flexOfferMessages = null;

        try {
            // Retrieving related flex offer details.
            Map<Integer, PtuFlexOffer> flexOffers = planboardBusinessService.findPtuFlexOffer(flexOfferRevocation.getSequence(),
                    flexOfferRevocation.getMessageMetadata().getSenderDomain());

            // Checking whether flex offers exist
            planboardValidatorService.checkRelatedFlexOffersExist(flexOfferRevocation, flexOffers);

            // Retrieving related plan board messages
            flexOfferMessages = planboardBusinessService
                    .findPlanboardMessages(flexOfferRevocation.getSequence(), DocumentType.FLEX_OFFER,
                            flexOfferRevocation.getMessageMetadata().getSenderDomain());

            // Checking whether plan board messages exist
            planboardValidatorService.checkRelatedPlanboardMessagesExist(flexOfferRevocation, flexOfferMessages);

            // Checking that no flex offer has a PTU in the operate or later phase.
            // Making sure that all PTUs are not in the operate or later phase we make sure that all PTUs are in the future
            planboardValidatorService.checkPtuPhase(flexOfferRevocation, flexOffers);
        } catch (BusinessValidationException e) {
            LOGGER.warn("Validation error for a flex offer: " + e.getMessage(), e);
            responseResult = DispositionAcceptedRejected.REJECTED;
            responseResultMessage = e.getMessage();
        }

        // Setting the status REVOKED
        if (responseResult == DispositionAcceptedRejected.ACCEPTED) {
            setPlanboardMessageStatus(flexOfferMessages);
        }

        // Sending the response
        FlexOfferRevocationResponse response = buildResponse(flexOfferRevocation);
        response.setResult(responseResult);
        response.setMessage(responseResultMessage);
        jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(response));
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void setPlanboardMessageStatus(List<PlanboardMessage> flexOfferMessages) {
        for (PlanboardMessage flexOfferMessage : flexOfferMessages) {
            flexOfferMessage.setDocumentStatus(DocumentStatus.REVOKED);
        }
    }

    private FlexOfferRevocationResponse buildResponse(FlexOfferRevocation flexOfferRevocation) {
        FlexOfferRevocationResponse response = new FlexOfferRevocationResponse();
        response.setSequence(flexOfferRevocation.getSequence());
        MessageMetadata metadata = new MessageMetadataBuilder().messageID().timeStamp()
                .conversationID(flexOfferRevocation.getMessageMetadata().getConversationID())
                .senderDomain(config.getProperty(ConfigParam.HOST_DOMAIN)).senderRole(USEFRole.DSO)
                .recipientDomain(flexOfferRevocation.getMessageMetadata().getSenderDomain())
                .recipientRole(flexOfferRevocation.getMessageMetadata().getSenderRole()).precedence(ROUTINE)
                .build();
        response.setMessageMetadata(metadata);
        return response;
    }
}
