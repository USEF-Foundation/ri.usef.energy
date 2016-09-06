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

package energy.usef.agr.config;

/**
 * The Config aggregator parameter contains all configuration parameters which can be used in aggregator component of USEF. The
 * property class is used to do some validations on the parameter. Also a user defined class can be entered with user specific
 * validations, e.g. a parameter should be integer and positive. An enumeration is used to configure the parameters to define the
 * config parameter more type safe (before a string value was used).
 */
public enum ConfigAgrParam {
    AGR_COMMON_REFERENCE_UPDATE_TIME(String.class),
    AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL(Integer.class),
    AGR_INITIALIZE_PLANBOARD_TIME(String.class),
    AGR_CONNECTION_FORECAST_TIME(String.class),
    AGR_CONNECTION_FORECAST_DAYS_INTERVAL(Integer.class),
    AGR_FLEXOFFER_INITIAL_DELAY_IN_SECONDS(Long.class),
    AGR_FLEXOFFER_INTERVAL_IN_SECONDS(Long.class),
    AGR_DETERMINE_NETDEMANDS_INITIAL_DELAY_IN_SECONDS(Long.class),
    AGR_DETERMINE_NETDEMANDS_INTERVAL_IN_SECONDS(Long.class),
    AGR_CONTROL_ADS_INITIAL_DELAY_IN_SECONDS(Long.class),
    AGR_CONTROL_ADS_INTERVAL_IN_SECONDS(Long.class),
    AGR_CONTROL_ADS_TIMEOUT_IN_SECONDS(Integer.class),
    AGR_INITIATE_SETTLEMENT_DAY_OF_MONTH(Integer.class),
    AGR_INITIATE_SETTLEMENT_TIME_OF_DAY(String.class),
    AGR_FINALIZE_A_PLAN_PTUS(Integer.class),
    AGR_IDENTIFY_CHANGE_IN_FORECAST_INITIAL_DELAY_IN_SECONDS(Integer.class),
    AGR_IDENTIFY_CHANGE_IN_FORECAST_INTERVAL_IN_SECONDS(Integer.class),
    AGR_POWERMATCHER_ENDPOINT_URI(String.class),
    AGR_INITIALIZE_NON_UDI_TIME_OF_DAY(String.class),
    AGR_IS_NON_UDI_AGGREGATOR(Boolean.class),
    AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION_INTERVAL_IN_MINUTES(Integer.class);

    private Class<?> propertyClass;

    /**
     * AGR Configuration parameter.
     *
     * @param propertyClass the property class describes the type of parameter and what validations can be done on the parameter.
     */
    ConfigAgrParam(Class<?> propertyClass) {
        this.propertyClass = propertyClass;
    }

    public Class<?> getPropertyClass() {
        return propertyClass;
    }

}
