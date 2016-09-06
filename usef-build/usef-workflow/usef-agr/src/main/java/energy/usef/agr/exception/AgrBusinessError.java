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

package energy.usef.agr.exception;

import energy.usef.core.exception.BusinessError;

/**
 * Enumeration of the Aggregator-specific Business Errors.
 */
public enum AgrBusinessError implements BusinessError {
    NON_EXISTING_GRID_POINT("The grid point has not been found."),
    NON_EXISTING_CONGESTION_POINT("Congestionpoint {} does not exist."),
    INVALID_PTUS("The request does not contain PTU elements where disposition is REQUESTED."),    
    NON_EXISTING_PROGNOSIS("The prognosis can not be found with origin {} and sequence {}."), 
    NO_MATCHING_OFFER_FOR_ORDER("No matching flexoffer could be found for flexorder {}."),
    WRONG_NUMBER_OF_PTUS("Wrong number of PTUs."),
    FLEX_ORDER_PERIOD_IN_THE_PAST("Period of flexorder {} is in the past."),
    FLEX_ORDER_WITHOUT_NON_ZERO_FUTURE_POWER("Flexorder {} has no future PTU's with non-zero power values."),
    FLEX_OFFER_REVOKED("Corresponding flexoffer is REVOKED."),
    NON_EXISTING_FLEX_OFFER("Could not find corresponding flex offer with sequence {}. Number of flex offers found {}.");
    
    private final String errorMessage;

    AgrBusinessError(String errorMessage) {
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
