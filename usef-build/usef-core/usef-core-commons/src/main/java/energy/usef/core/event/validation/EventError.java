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

package energy.usef.core.event.validation;

import energy.usef.core.exception.BusinessError;

/**
 * Enumeration of event specific errors
 */
public enum EventError implements BusinessError {
    INVALID_PERIOD("Invalid period, period empty for event {}."),
    PERIOD_IN_PAST("Invalid period, period in the past for event {}"),
    PERIOD_TODAY_OR_IN_PAST("Invalid period, period today or in the past for event {}"),
    PERIOD_IN_FUTURE("Invalid period, period in the future for event {}"),
    PERIOD_IN_PAST_OR_FUTURE("Invalid period, period not today for event {}");

    private final String errorMessage;

    EventError(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getError() {
        return errorMessage;
    }
}
