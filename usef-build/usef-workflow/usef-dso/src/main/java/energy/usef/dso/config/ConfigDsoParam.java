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

package energy.usef.dso.config;

/**
 * The Config DSO parameter contains all configuration parameters which can be used in distribution service operator component of
 * USEF. The property class is used to do some validations on the parameter. Also a user defined class can be entered with user
 * specific validations, e.g. a parameter should be integer and positive. An enumeration is used to configure the parameters to
 * define the config parameter more type safe (before a string value was used).
 */
public enum ConfigDsoParam {
    DSO_COMMON_REFERENCE_UPDATE_TIME(String.class),
    DSO_INITIALIZE_PLANBOARD_TIME(String.class),
    DSO_INITIALIZE_PLANBOARD_DAYS_INTERVAL(String.class),
    DSO_CONNECTION_FORECAST_TIME(String.class),
    DSO_CONNECTION_FORECAST_DAYS_INTERVAL(Integer.class),
    DSO_GRID_SAFETY_ANALYSIS_EXPIRATION_IN_MINUTES(Long.class),
    DSO_FLEXORDER_INITIAL_DELAY_IN_SECONDS(Long.class),
    DSO_FLEXORDER_INTERVAL_IN_SECONDS(Long.class),
    DSO_INITIATE_SETTLEMENT_TIME(String.class),
    DSO_INITIATE_SETTLEMENT_DAY_OF_MONTH(Integer.class),
    DSO_METER_DATA_QUERY_EXPIRATION_CHECK_INTERVAL_IN_MINUTES(Integer.class),
    DSO_METER_DATA_QUERY_EXPIRATION_IN_HOURS(Integer.class),
    DSO_SETTLEMENT_MESSAGE_DISPOSAL_TIME(String.class),
    DSO_SETTLEMENT_RESPONSE_WAITING_DURATION(Integer.class),
    DSO_OPERATE_INTERVAL_IN_SECONDS(Integer.class),
    DSO_OPERATE_INITIAL_DELAY_IN_SECONDS(Integer.class),
    DSO_INITIATE_COLLECT_ORANGE_REGIME_DATA_DAY_OF_MONTH(Integer.class),
    DSO_INITIATE_COLLECT_ORANGE_REGIME_DATA_TIME(String.class);

    private Class<?> propertyClass;

    /**
     * DSO Configuration parameter.
     *
     * @param propertyClass the property class describes the type of parameter and what validations can be done on the parameter.
     */
    ConfigDsoParam(Class<?> propertyClass) {
        this.propertyClass = propertyClass;
    }

    public Class<?> getPropertyClass() {
        return propertyClass;
    }

}
