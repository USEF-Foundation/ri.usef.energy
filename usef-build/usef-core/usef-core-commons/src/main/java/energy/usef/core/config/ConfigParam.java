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

package energy.usef.core.config;

/**
 * The Config parameter contains all configuration parameters which can be used in the core libraries of USEF. Parameters which are
 * participant specific are defined in the classes Config<Participant>Param. The property class is used to do some validations on
 * the parameter. Also a user defined class can be entered with user specific validations, e.g. a parameter should be integer and
 * positive. An enumeration is used to configure the parameters to define the config parameter more type safe (before a string value
 * was used).
 */
public enum ConfigParam {
    RECIPIENT_ENDPOINT(String.class),
    HOST_DOMAIN(String.class),
    HOST_ROLE(String.class),
    ROUTINE_HTTP_REQUEST_MAX_RETRIES(Integer.class),

    BYPASS_SCHEDULED_EVENTS(String.class),

    ROUTINE_EXPONENTIAL_BACKOFF_INITIAL_INTERVAL_MILLIS(Integer.class),
    ROUTINE_EXPONENTIAL_BACKOFF_MAX_ELAPSED_TIME_MILLIS(Integer.class),
    ROUTINE_EXPONENTIAL_BACKOFF_MAX_INTERVAL_MILLIS(Integer.class),
    ROUTINE_EXPONENTIAL_BACKOFF_MULTIPLIER(Double.class),
    ROUTINE_EXPONENTIAL_BACKOFF_RANDOMIZATION_FACTOR(Double.class),

    TRANSACTIONAL_HTTP_REQUEST_MAX_RETRIES(Integer.class),
    TRANSACTIONAL_EXPONENTIAL_BACKOFF_INITIAL_INTERVAL_MILLIS(Integer.class),
    TRANSACTIONAL_EXPONENTIAL_BACKOFF_MAX_ELAPSED_TIME_MILLIS(Integer.class),
    TRANSACTIONAL_EXPONENTIAL_BACKOFF_MAX_INTERVAL_MILLIS(Integer.class),
    TRANSACTIONAL_EXPONENTIAL_BACKOFF_MULTIPLIER(Double.class),
    TRANSACTIONAL_EXPONENTIAL_BACKOFF_RANDOMIZATION_FACTOR(Double.class),
    TRANSACTIONAL_MESSAGE_NOTIFICATION_FACTOR(Double.class),

    CRITICAL_HTTP_REQUEST_MAX_RETRIES(Integer.class),
    CRITICAL_EXPONENTIAL_BACKOFF_INITIAL_INTERVAL_MILLIS(Integer.class),
    CRITICAL_EXPONENTIAL_BACKOFF_MAX_ELAPSED_TIME_MILLIS(Integer.class),
    CRITICAL_EXPONENTIAL_BACKOFF_MAX_INTERVAL_MILLIS(Integer.class),
    CRITICAL_EXPONENTIAL_BACKOFF_MULTIPLIER(Double.class),
    CRITICAL_EXPONENTIAL_BACKOFF_RANDOMIZATION_FACTOR(Double.class),
    CRITICAL_MESSAGE_NOTIFICATION_FACTOR(Double.class),

    KEYSTORE_FILENAME(String.class),
    KEYSTORE_PASSWORD(String.class),
    KEYSTORE_PRIVATE_KEY_ALIAS(String.class),
    KEYSTORE_PRIVATE_KEY_PASSWORD(String.class),

    PARTICIPANT_DNS_INFO_FILENAME(String.class),
    SENDER_ALLOW_LIST_FORCED(String.class),
    SENDER_ALLOW_LIST_FILENAME(String.class),
    SENDER_DENY_LIST_FILENAME(String.class),

    BYPASS_DNS_VERIFICATION(Boolean.class),
    BYPASS_TLS_VERIFICATION(Boolean.class),
    RETRY_HTTP_ERROR_CODES(String.class),
    MAX_ERROR_MESSAGE_LENGTH(String.class),

    PTU_DURATION(Integer.class),
    TIME_SERVER(String.class),
    TIME_SERVER_PORT(Integer.class),
    TIME_ZONE(String.class),
    CURRENCY(String.class),
    INTRADAY_GATE_CLOSURE_PTUS(Integer.class),

    VALIDATE_OUTGOING_XML(Boolean.class),
    VALIDATE_INCOMING_XML(Boolean.class),

    DAY_AHEAD_GATE_CLOSURE_TIME(String.class),
    DAY_AHEAD_GATE_CLOSURE_PTUS(Integer.class);

    private Class<?> propertyClass;

    /**
     * Configuration parameter constructor.
     *
     * @param propertyClass the property class describes the type of parameter and what validations can be done on the parameter.
     */
    ConfigParam(Class<?> propertyClass) {
        this.propertyClass = propertyClass;
    }

    public Class<?> getPropertyClass() {
        return propertyClass;
    }

}
