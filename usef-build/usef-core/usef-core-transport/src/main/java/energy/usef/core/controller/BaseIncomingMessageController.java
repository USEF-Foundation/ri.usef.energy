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

package energy.usef.core.controller;

import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.MessageDirection;
import energy.usef.core.service.business.MessageService;

import javax.inject.Inject;

/**
 * Base controller class. This class is responsible for the general operations related to incoming xml messages processing.
 *
 * @param <T> a subclass of the {@link Message} class (e.g. CommonReferenceUpdate).
 */
public abstract class BaseIncomingMessageController<T extends Message>
        implements IncomingMessageController<T> {

    @Inject
    protected MessageService messageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(String xml, T message) throws BusinessException {
        // persist message
        energy.usef.core.model.Message savedMessage = messageService.storeMessage(xml, message, MessageDirection.INBOUND);
        // corresponding controller action is invoked
        action(message, savedMessage);
    }

    /**
     * Takes an action based on an incoming message.
     * 
     * @param message a specific type of {@link Message}.
     * @param savedMessage the previously saved message.
     * @throws BusinessException
     */
    public abstract void action(T message, energy.usef.core.model.Message savedMessage) throws BusinessException;

}
