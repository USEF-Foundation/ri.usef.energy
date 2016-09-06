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

package energy.usef.brp.config;

/**
 * The Config brp parameter contains all configuration parameters which can be used in BRP component of USEF. The property class is
 * used to do some validations on the parameter. Also a user defined class can be entered with user specific validations, e.g. a
 * parameter should be integer and positive. An enumeration is used to configure the parameters to define the config parameter more
 * type safe (before a string value was used).
 */
public enum ConfigBrpParam {
    BRP_COMMON_REFERENCE_UPDATE_TIME_OF_DAY(String.class),
    BRP_INITIALIZE_PLANBOARD_TIME(String.class),
    BRP_INITIALIZE_PLANBOARD_DAYS_INTERVAL(Integer.class),
    BRP_INITIALIZE_PLANBOARD_DAYS_AHEAD(Integer.class),
    BRP_FLEXORDER_INITIAL_DELAY_IN_SECONDS(Long.class),
    BRP_FLEXORDER_INTERVAL_IN_SECONDS(Long.class),
    BRP_FINALIZE_APLANS_PTUS_BEFORE_GATE_CLOSURE(Integer.class),
    BRP_INITIATE_SETTLEMENT_TIME(String.class),
    BRP_INITIATE_SETTLEMENT_DAY_OF_MONTH(Integer.class),
    BRP_METER_DATA_QUERY_EXPIRATION_CHECK_INTERVAL_IN_MINUTES(Integer.class),
    BRP_METER_DATA_QUERY_EXPIRATION_IN_HOURS(Integer.class),
    BRP_SETTLEMENT_MESSAGE_DISPOSAL_TIME(String.class),
    BRP_SETTLEMENT_RESPONSE_WAITING_DURATION(Integer.class);

    private Class<?> propertyClass;

    /**
     * AGR Configuration parameter.
     * 
     * @param propertyClass the property class describes the type of parameter and what validations can be done on the parameter.
     */
    ConfigBrpParam(Class<?> propertyClass) {
        this.propertyClass = propertyClass;
    }

    public Class<?> getPropertyClass() {
        return propertyClass;
    }

}
