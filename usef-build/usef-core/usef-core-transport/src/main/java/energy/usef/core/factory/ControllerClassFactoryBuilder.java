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

package energy.usef.core.factory;

import energy.usef.core.constant.USEFConstants;
import energy.usef.core.controller.IncomingMessageController;
import energy.usef.core.controller.error.OutgoingErrorMessageController;
import energy.usef.core.data.xml.bean.message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller class factory builder.
 */
public class ControllerClassFactoryBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerClassFactoryBuilder.class);

    private static ControllerClassFactoryBuilder builder;
    private static ControllerClassFactory<OutgoingErrorMessageController<Message>> outgoingErrorMessageControllerFactory;
    private static ControllerClassFactory<IncomingMessageController<Message>> incomingMessageControllerFactory;

    private ControllerClassFactoryBuilder() {

    }

    /**
     * This method returns the builder to create Controllers.
     *
     * @return {@link ControllerClassFactoryBuilder}
     */
    public static ControllerClassFactoryBuilder getBuilder() {
        if (builder == null) {
            builder = new ControllerClassFactoryBuilder();
        }
        return builder;
    }

    /**
     * Gets OutgoingErrorMessageControllerFactory instance.
     * 
     * @return OutgoingErrorMessageControllerFactory
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final ControllerClassFactory<OutgoingErrorMessageController<Message>> getOutgoingErrorMessageControllerFactory() {
        if (outgoingErrorMessageControllerFactory == null) {
            outgoingErrorMessageControllerFactory = new ControllerClassFactory(OutgoingErrorMessageController.class,
                    USEFConstants.BASE_PACKAGE, USEFConstants.OUTGOING_ERROR_MESSAGE_CONTROLLER_PACKAGE_PATTERN);
            LOGGER.debug("Created OutgoingErrorMessageControllerFactory");
        }
        return outgoingErrorMessageControllerFactory;
    }

    /**
     * Gets IncomingMessageControllerFactory instance.
     * 
     * @return IncomingMessageControllerFactory
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final ControllerClassFactory<IncomingMessageController<Message>> getIncomingMessageControllerFactory() {
        if (incomingMessageControllerFactory == null) {
            incomingMessageControllerFactory = new ControllerClassFactory(IncomingMessageController.class,
                    USEFConstants.BASE_PACKAGE, USEFConstants.INCOMING_MESSAGE_CONTROLLER_PACKAGE_PATTERN);
            LOGGER.debug("Created IncomingMessageControllerFactoryFactory");
        }
        return incomingMessageControllerFactory;
    }

}
