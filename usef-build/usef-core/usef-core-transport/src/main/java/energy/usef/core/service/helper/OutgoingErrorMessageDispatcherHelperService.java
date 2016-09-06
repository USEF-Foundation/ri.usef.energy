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

package energy.usef.core.service.helper;

import energy.usef.core.controller.error.OutgoingErrorMessageController;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.factory.ControllerClassFactoryBuilder;
import energy.usef.core.service.business.error.MessageControllerError;
import energy.usef.core.util.CDIUtil;
import energy.usef.core.util.XMLUtil;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is designed to route outgoing error XML messages to corresponding controllers for processing.
 */
@Stateless
public class OutgoingErrorMessageDispatcherHelperService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingErrorMessageDispatcherHelperService.class);

    /**
     * The method routes an outgoing error message to a corresponding controller and invokes a required action.
     *
     * @param xml xml message
     * @throws BusinessException
     */
    public <T> void dispatch(String xml) throws BusinessException {
        // transform
        Object xmlObject = XMLUtil.xmlToMessage(xml);

        if (!(xmlObject instanceof Message)) {
            throw new BusinessException(MessageControllerError.XML_NOT_CONVERTED_TO_OBJECT);
        }
        process((Message) xmlObject);
    }

    private <T> void process(Message xmlMessage) throws BusinessException {
        OutgoingErrorMessageController<Message> controller = null;
        Class<?> clazz = ControllerClassFactoryBuilder.getBuilder().getOutgoingErrorMessageControllerFactory()
                .getControllerClass(xmlMessage.getClass());

        if (clazz != null) {
            controller = CDIUtil.getBean(clazz);
            if (controller != null) {
                controller.execute(xmlMessage);
            } else {
                LOGGER.warn("Can not find the corresponding controller instance for the class {}", clazz);
            }
        } else {
            LOGGER.debug("Can not find the corresponding message error controller for the class {}", xmlMessage.getClass());
        }

    }

}
