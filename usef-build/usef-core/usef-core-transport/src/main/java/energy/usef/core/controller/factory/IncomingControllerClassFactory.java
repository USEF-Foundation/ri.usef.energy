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

package energy.usef.core.controller.factory;

import energy.usef.core.constant.USEFConstants;
import energy.usef.core.controller.IncomingMessageController;
import energy.usef.core.data.xml.bean.message.Message;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller class factory. The factory creates a map (xml class -> corresponding controller class). The controller classes can be
 * used to find corresponding instances in the container context.
 */
public class IncomingControllerClassFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(IncomingControllerClassFactory.class);

    private static final Map<Class<? extends Message>, Class<? extends IncomingMessageController<? extends Message>>> XML_CLASS_CONTROLLER_CLASS_MAP =
            new HashMap<>();

    @SuppressWarnings("rawtypes")
    private static final Class BASE_CONTROLLER_CLASS = IncomingMessageController.class;

    private static IncomingControllerClassFactory factory = null;

    private IncomingControllerClassFactory() {
        initXmlClassToControllerClassMap();
    }

    private static void initFactory() {
        if (factory == null) {
            factory = new IncomingControllerClassFactory();
            LOGGER.info("Instance of the IncomingControllerClassFactory has been created");
        }
    }

    /**
     * Gets controller class by xml object class.
     *
     * @param xmlClass xml object class
     * @return controller class
     */
    public static Class<? extends IncomingMessageController<? extends Message>> getControllerClass(Class<? extends Message> xmlClass) {
        initFactory();
        return XML_CLASS_CONTROLLER_CLASS_MAP.get(xmlClass);
    }

    @SuppressWarnings("unchecked")
    private <T> void initXmlClassToControllerClassMap() {

        Reflections reflections = new Reflections(ClasspathHelper.forPackage(USEFConstants.BASE_PACKAGE),
                new FilterBuilder().include(USEFConstants.INCOMING_MESSAGE_CONTROLLER_PACKAGE_PATTERN));

        Set<Class<? extends IncomingMessageController<? extends Message>>> subTypes =
                reflections.getSubTypesOf(BASE_CONTROLLER_CLASS);

        for (Class<? extends IncomingMessageController<? extends Message>> controllerClass : subTypes) {
            if (Modifier.isAbstract(controllerClass.getModifiers())) {
                continue;
            }
            Type genericSuperClass = controllerClass.getGenericSuperclass();

            if (genericSuperClass instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericSuperClass;
                Type[] fieldArgTypes = pt.getActualTypeArguments();
                if (fieldArgTypes != null && fieldArgTypes.length > 0) {
                    Class<? extends Message> xmlClass = (Class<? extends Message>) fieldArgTypes[0];
                    XML_CLASS_CONTROLLER_CLASS_MAP.put(xmlClass, controllerClass);
                }
            }
        }
        LOGGER.info("Successfully inited xml class -> controller class map");
    }
}
