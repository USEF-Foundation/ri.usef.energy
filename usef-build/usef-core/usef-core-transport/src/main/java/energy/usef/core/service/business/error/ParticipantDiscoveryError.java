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

package energy.usef.core.service.business.error;

import energy.usef.core.exception.BusinessError;

/**
 * Enumeration of the {@link BusinessError}s that could occur during the participant discovery.
 */
public enum ParticipantDiscoveryError implements BusinessError {
    EMPTY_PARTICIPANT_LIST("The participant should not be empty. Complete the YAML file."),
    DNS_NOT_FOUND("Unable to reach the DNS"),
    PARTICIPANT_NOT_FOUND("The participant has not been found"),
    SENDER_ROLE_NOT_PROVIDED("Sender role has not been provided"),
    SENDER_DOMAIN_NOT_PROVIDED("Sender domain has not been provided"),
    RECIPIENT_ROLE_NOT_PROVIDED("Recipient role has not been provided"),
    RECIPIENT_DOMAIN_NOT_PROVIDED("Recipient domain has not been provided");

    private String error;

    ParticipantDiscoveryError(String errorMessage) {
        this.error = errorMessage;
    }

    @Override
    public String getError() {
        return error;
    }

}
