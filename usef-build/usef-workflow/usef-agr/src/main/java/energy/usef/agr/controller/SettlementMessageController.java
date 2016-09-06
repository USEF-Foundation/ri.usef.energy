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

import energy.usef.agr.workflow.settlement.receive.ReceiveSettlementMessageEvent;
import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.SettlementMessage;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message controller class for the {@link SettlementMessage}.
 */
public class SettlementMessageController extends BaseIncomingMessageController<SettlementMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementMessageController.class);

    @Inject
    private Event<ReceiveSettlementMessageEvent> eventManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public void action(SettlementMessage message, Message savedMessage) throws BusinessException {
        LOGGER.info(" Received SettlementMessage.");
        eventManager.fire(new ReceiveSettlementMessageEvent(message, savedMessage));
    }

}
