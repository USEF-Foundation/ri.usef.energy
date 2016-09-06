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

package energy.usef.cro.controller;

import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdate;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdateResponse;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.XMLUtil;
import energy.usef.cro.service.business.CommonReferenceUpdateBusinessService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes incoming CRO common reference updates.
 */
@Startup
@Singleton
public class CommonReferenceUpdateController extends BaseIncomingMessageController<CommonReferenceUpdate> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonReferenceUpdateController.class);

    @Inject
    private JMSHelperService jmsService;

    @Inject
    private CommonReferenceUpdateBusinessService commonReferenceUpdateBusinessService;

    /**
     * {@inheritDoc}
     */
    @Lock(LockType.WRITE)
    public void action(CommonReferenceUpdate message, Message savedMessage) throws BusinessException {
        LOGGER.debug("Start processing message {}", message.getMessageMetadata().getMessageID());
        final List<String> errors = new ArrayList<>();
        // execute the business logic

        if (CommonReferenceEntityType.CONGESTION_POINT.equals(message.getEntity())) {
            commonReferenceUpdateBusinessService.updateCongestionPoints(message, errors);
        } else if (CommonReferenceEntityType.AGGREGATOR.equals(message.getEntity())) {
            commonReferenceUpdateBusinessService.updateAggregatorConnections(message, errors);
        } else if (CommonReferenceEntityType.BRP.equals(message.getEntity())) {
            commonReferenceUpdateBusinessService.updateBalanceResponsiblePartyConnections(message, errors);
        } else {
            LOGGER.error("Unknown CommonReferenceEntityType: " + message.getEntity());
        }

        // create the response
        CommonReferenceUpdateResponse response = createCommonReferenceUpdateResponse(message, errors);

        String xml = XMLUtil.messageObjectToXml(response);
        jmsService.sendMessageToOutQueue(xml);

        LOGGER.info("Common reference update response is sent to the outgoing queue");
        LOGGER.debug("Finished processing message {}", message.getMessageMetadata().getMessageID());
    }

    private CommonReferenceUpdateResponse createCommonReferenceUpdateResponse(CommonReferenceUpdate message, List<String> errors) {
        CommonReferenceUpdateResponse response = new CommonReferenceUpdateResponse();
        response.setEntityAddress(message.getEntityAddress());

        // Setting metadata
        MessageMetadata messageMetadata = MessageMetadataBuilder.build(message.getMessageMetadata().getSenderDomain(),
                message.getMessageMetadata().getSenderRole(),
                message.getMessageMetadata().getRecipientDomain(), USEFRole.CRO, ROUTINE).
                conversationID(message.getMessageMetadata().getConversationID()).messageID(UUID.randomUUID().toString()).build();

        messageMetadata.setTimeStamp(DateTimeUtil.getCurrentDateTime());

        response.setMessageMetadata(messageMetadata);

        if (errors.isEmpty()) {
            // No error
            response.setResult(DispositionAcceptedRejected.ACCEPTED);
            LOGGER.info("No error found, accepted CommonReferenceUpdateResponse will be sent");
        } else {
            // Errors
            response.setResult(DispositionAcceptedRejected.REJECTED);
            response.setMessage(errors.stream().collect(Collectors.joining("\n")));
            LOGGER.warn("Errors found, rejected CommonReferenceUpdateResponse will be sent");
        }

        return response;
    }

}
