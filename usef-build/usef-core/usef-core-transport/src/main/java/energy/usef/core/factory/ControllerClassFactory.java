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
 * Generic Controller class factory. The factory creates a map (xml class -> corresponding generic controller class). The generic
 * controller classes can be used to find corresponding instances in the container context.
 * 
 * @param <T> base controller class/interface
 */
public class ControllerClassFactory<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerClassFactory.class);

    private final Map<Class<? extends Message>, Class<? extends T>> xmlClassControllerClassMap =
            new HashMap<>();

    private final Class<T> baseControllerClass;
    private final String basePackage;
    private final String controllerClassPattern;

    /**
     * Constructor.
     *
     * @param baseControllerClass
     * @param basePackage
     * @param controllerClassPattern
     */
    public ControllerClassFactory(Class<T> baseControllerClass, String basePackage, String controllerClassPattern) {
        this.baseControllerClass = baseControllerClass;
        this.basePackage = basePackage;
        this.controllerClassPattern = controllerClassPattern;

        initXmlClassToControllerClassMap();
    }

    /**
     * Gets controller class by xml object class.
     *
     * @param xmlClass xml object class
     * @return controller class
     */
    public Class<? extends T> getControllerClass(Class<? extends Message> xmlClass) {
        return xmlClassControllerClassMap.get(xmlClass);
    }

    @SuppressWarnings("unchecked")
    private void initXmlClassToControllerClassMap() {

        Reflections reflections = new Reflections(ClasspathHelper.forPackage(basePackage),
                new FilterBuilder().include(controllerClassPattern));

        Set<Class<? extends T>> subTypes = reflections.getSubTypesOf(baseControllerClass);

        for (Class<? extends T> controllerClass : subTypes) {
            if (Modifier.isAbstract(controllerClass.getModifiers())) {
                continue;
            }
            Type genericSuperClass = controllerClass.getGenericSuperclass();

            if (genericSuperClass instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericSuperClass;
                Type[] fieldArgTypes = pt.getActualTypeArguments();
                if (fieldArgTypes != null && fieldArgTypes.length > 0) {
                    Class<? extends Message> xmlClass = (Class<? extends Message>) fieldArgTypes[0];
                    xmlClassControllerClassMap.put(xmlClass, controllerClass);
                }
            }
        }
        LOGGER.info("Successfully inited xml class -> controller class map");
    }
}
