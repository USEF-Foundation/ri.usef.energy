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

package energy.usef.agr.workflow.validate.flexoffer;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.CRITICAL;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOfferRevocation;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.XMLUtil;

import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class for the Aggregator, handling the 'Revoke Flex Offers' workflow.
 */
@Stateless
public class AgrFlexOfferRevocationCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrFlexOfferRevocationCoordinator.class);

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private Config config;

    /**
     * Handles FlexOfferRevocation event for the aggregator.
     * 
     * Validation is done in EndPoint (@link FlexOfferRevocationEndPoint)
     * 
     * {@inheritDoc}
     */
    public void handleEvent(@Observes FlexOfferRevocationEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        // build Flex Offer Revocation message based on the flex offer sequence number.
        FlexOfferRevocation flexOfferRevocation = buildFlexOfferRevocation(event.getUsefRole(), event.getFlexOfferSequenceNumber(),
                event.getRecipientDomainName());

        // send the flex offer revocation to the dso or brp
        jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(flexOfferRevocation));
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Create FlexOfferRevocation object.
     * 
     * @param usefRole the usef role of the recipient (BRP or DSO)
     * @param flexOfferSequence sequence number of the original flex offer
     * @param recipientDomain domain of the recipient
     */
    private FlexOfferRevocation buildFlexOfferRevocation(USEFRole usefRole, Long flexOfferSequence, String recipientDomain) {
        FlexOfferRevocation flexOfferRevocation = new FlexOfferRevocation();
        flexOfferRevocation.setSequence(flexOfferSequence);
        MessageMetadata metadata = new MessageMetadataBuilder().messageID().conversationID().timeStamp()
                .senderDomain(config.getProperty(ConfigParam.HOST_DOMAIN)).senderRole(USEFRole.AGR)
                .recipientDomain(recipientDomain).recipientRole(usefRole).precedence(CRITICAL)
                .build();
        flexOfferRevocation.setMessageMetadata(metadata);
        return flexOfferRevocation;
    }
}
