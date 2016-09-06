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

import energy.usef.core.controller.DefaultIncomingMessageController;
import energy.usef.core.controller.IncomingMessageController;
import energy.usef.core.controller.factory.IncomingControllerClassFactory;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.error.MessageControllerError;
import energy.usef.core.util.XMLUtil;

import java.util.Iterator;

import javax.ejb.Stateless;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is designed to route incoming XML messages to corresponding controllers for processing.
 */
@Stateless
public class DispatcherHelperService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherHelperService.class);

    @Inject
    private BeanManager beanManager;

    @Inject
    private DefaultIncomingMessageController defaultIncomingMessageController;

    /**
     * The method routes an incoming message to a corresponding controller and invokes a required action.
     *
     * @param xml xml message
     * @throws BusinessException
     */
    public void dispatch(String xml) throws BusinessException {
        // transform
        Object xmlObject = XMLUtil.xmlToMessage(xml);

        if (!(xmlObject instanceof Message)) {
            throw new BusinessException(MessageControllerError.XML_NOT_CONVERTED_TO_OBJECT);
        }

        process(xml, (Message) xmlObject);
    }

    private void process(String xml, Message message) throws BusinessException {
        IncomingMessageController<Message> controller = null;

        Class<?> controllerClass = IncomingControllerClassFactory.getControllerClass(message.getClass());
        controller = getController(controllerClass);

        if (controller == null) {
            LOGGER.error("No controller is found for the message of type: {}, default controller will be used", message.getClass());
            controller = defaultIncomingMessageController;
        }
        // process the message
        controller.execute(xml, message);
    }

    @SuppressWarnings("unchecked")
    private IncomingMessageController<Message> getController(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        Iterator<Bean<?>> iterator = beanManager.getBeans(clazz).iterator();
        if (iterator.hasNext()) {
            Bean<?> bean = iterator.next();
            CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
            return (IncomingMessageController<Message>) beanManager.getReference(bean, clazz, creationalContext);
        }
        return null;
    }

}
