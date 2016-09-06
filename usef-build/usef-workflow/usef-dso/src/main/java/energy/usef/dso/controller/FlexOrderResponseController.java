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

import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOrderResponse;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.Message;
import energy.usef.dso.workflow.validate.acknowledgement.flexorder.FlexOrderAcknowledgementEvent;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Incoming FlexOrderResponse controller.
 */
@Stateless
public class FlexOrderResponseController extends BaseIncomingResponseMessageController<FlexOrderResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexOrderResponseController.class);

    @Inject
    private Event<FlexOrderAcknowledgementEvent> eventManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(FlexOrderResponse message, Message savedMessage) throws BusinessException {
        LOGGER.info("FlexOrderResponse received.");
        if (DispositionAcceptedRejected.REJECTED == message.getResult()) {
            // Update rejected
            LOGGER.warn("FlexOrder was rejected.");
            FlexOrderAcknowledgementEvent event = new FlexOrderAcknowledgementEvent(message.getSequence(),
                    AcknowledgementStatus.REJECTED, message.getMessageMetadata().getSenderDomain());
            eventManager.fire(event);
        } else {
            LOGGER.debug("FlexOrder accepted.");
            FlexOrderAcknowledgementEvent event = new FlexOrderAcknowledgementEvent(message.getSequence(),
                    AcknowledgementStatus.ACCEPTED, message.getMessageMetadata().getSenderDomain());
            eventManager.fire(event);
        }
    }

}
