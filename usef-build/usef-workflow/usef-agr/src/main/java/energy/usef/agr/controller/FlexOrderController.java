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
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioEvent;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.data.xml.bean.message.FlexOrderResponse;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.Message;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.transformer.PtuListConverter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.util.XMLUtil;

import java.math.BigInteger;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process flexibility orders from DSO and BRP.
 */
@Stateless
public class FlexOrderController extends BaseIncomingMessageController<FlexOrder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexOrderController.class);

    @Inject
    private JMSHelperService jmsService;

    @Inject
    private AgrValidationBusinessService agrValidationBusinessService;

    @Inject
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private Config config;

    @Inject
    private Event<ReOptimizePortfolioEvent> eventManager;

    /**
     * {@inheritDoc}
     */
    public void action(FlexOrder order, Message savedMessage) {
        LOGGER.info("Flexorder received.");
        try {
            // 1. Validate semantics of order
            corePlanboardValidatorService.validateCurrency(order.getCurrency());
            corePlanboardValidatorService.validateTimezone(order.getTimeZone());
            corePlanboardValidatorService.validatePTUDuration(order.getPTUDuration());
            corePlanboardValidatorService.validateDomain(order.getFlexOfferOrigin());

            // determine UsefIdentifer
            String usefIdentifier = determineUsefIdentifier(order);

            // 2. Find corresponding FlexOffer and validate match on PTU basis
            agrValidationBusinessService.validateCorrespondingFlexOffer(order);

            // 3. FlexOrder timing validation
            agrValidationBusinessService.validateFlexOrderTiming(order);

            // 4. Store syntactically and semantically correct orders on plan board. Going back to plan state if in the other state
            corePlanboardBusinessService.storeFlexOrder(usefIdentifier, order, DocumentStatus.ACCEPTED,
                    order.getMessageMetadata().getSenderDomain(), AcknowledgementStatus.ACCEPTED, PtuContainerState.PlanValidate);

            // 5a. Send Response
            LOGGER.debug("Sending FlexOrderResponse with order reference [{}] to [{}]", order.getOrderReference(),
                    order.getMessageMetadata().getRecipientDomain());
            sendResponse(order, null);

            LOGGER.debug("Triggering ReOptimizePortfolio workflow.");
            if ( isReOptimizeNeeded(order)) {
                eventManager.fire(new ReOptimizePortfolioEvent(order.getPeriod()));
            }

        } catch (BusinessValidationException validationException) {
            // 5b. Send response with exception
            sendResponse(order, validationException);
        }
    }

    private boolean isReOptimizeNeeded(FlexOrder order) {
        boolean reOptimize = false;
        LocalDate currentDate = DateTimeUtil.getCurrentDate();
        if (order.getPeriod().isAfter(currentDate)) {
            reOptimize = true;
        } else if(order.getPeriod().isEqual(currentDate)) {
            int currentPtu = PtuUtil
                    .getPtuIndex(DateTimeUtil.getCurrentDateTime(), config.getIntegerProperty(ConfigParam.PTU_DURATION));

            //if order is for current period check and changes are after current ptu.
            reOptimize = PtuListConverter.normalize(order.getPTU()).stream()
                    .filter(ptu -> ptu.getPower() != null)
                    .filter(ptu -> !BigInteger.ZERO.equals(ptu.getPower()))
                    .filter(ptu -> ptu.getStart().intValue() > currentPtu)
                    .findAny().isPresent();
        }
        return reOptimize;
    }

    private String determineUsefIdentifier(FlexOrder order) {
        String usefIdentifier = order.getCongestionPoint();
        // for the DSO its scongestionPoint, but for flexOrders from BRP it is the senderDomain.
        if (USEFRole.BRP.equals(order.getMessageMetadata().getSenderRole())) {
            usefIdentifier = order.getMessageMetadata().getSenderDomain();
        }
        return usefIdentifier;
    }

    /**
     * This method builds a FlexOrderResponse object and puts it in the Message out Queue.
     *
     * @param order     is the flexorder to respond to.
     * @param exception to determine whether the Response shows Accepted or Rejected
     */
    private void sendResponse(FlexOrder order, Exception exception) {
        // 1. Initialize FlexOrderResponse
        FlexOrderResponse response = new FlexOrderResponse();

        MessageMetadata orderMetadata = order.getMessageMetadata();
        MessageMetadataBuilder messageMetadataBuilder = MessageMetadataBuilder.build(orderMetadata.getSenderDomain(),
                orderMetadata.getSenderRole(), orderMetadata.getRecipientDomain(), orderMetadata.getRecipientRole(),
                ROUTINE);

        response.setMessageMetadata(messageMetadataBuilder.conversationID(orderMetadata.getConversationID()).build());

        // 2. Check result of validations
        if (exception == null) {
            response.setResult(DispositionAcceptedRejected.ACCEPTED);
        } else {
            response.setResult(DispositionAcceptedRejected.REJECTED);
            response.setMessage(exception.getMessage());
        }
        response.setSequence(order.getSequence());

        // 3. send the response XML to the out queue.
        jmsService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(response));

        LOGGER.info("FlexOrderResponse with conversation ID [{}] and status [{}] is being sent.",
                response.getMessageMetadata().getConversationID(), response.getResult());

    }

}
