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

package energy.usef.dso.exception;

import energy.usef.core.exception.BusinessError;

/**
 * Enumeration of the DSO Business Errors.
 */
public enum DsoBusinessError implements BusinessError {
    FLEX_OFFER_NOT_AS_REQUESTED("FlexOffer should offer flexibility as requested"),
    NON_EXISTING_CONGESTION_POINT("The grid point has not been found."),
    INVALID_SENDER("The aggregator is not connected to this grid point."),
    POWER_VALUE_TOO_BIG("Power value too big."),
    PTUS_INCOMPLETE("Missing PTU entries."),
    INVALID_PERIOD("Invalid period."),
    DOCUMENT_SEQUENCE_NUMBER_IS_TOO_SMALL("The sequence number of the document should be bigger than the previous one.");

    private final String errorMessage;

    DsoBusinessError(String errorMessage) {
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
