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

package energy.usef.cro.config;

/**
 * The Config common reference parameter contains all configuration parameters which can be used in common reference component of
 * USEF. The property class is used to do some validations on the parameter. Also a user defined class can be entered with user
 * specific validations, e.g. a parameter should be integer and positive. An enumeration is used to configure the parameters to
 * define the config parameter more type safe (before a string value was used).
 */
public enum ConfigCroParam {
    COMMON_REFERENCE_MODE(String.class);

    private Class<?> propertyClass;

    /**
     * CRO Configuration parameter.
     *
     * @param propertyClass the property class describes the type of parameter and what validations can be done on the parameter.
     */
    ConfigCroParam(Class<?> propertyClass) {
        this.propertyClass = propertyClass;
    }

    public Class<?> getPropertyClass() {
        return propertyClass;
    }

}
