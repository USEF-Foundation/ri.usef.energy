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

package energy.usef.core.service.validation;

import energy.usef.core.exception.BusinessError;

/**
 * Core business errors used in the {@link CorePlanboardValidatorService}.
 */
public enum CoreBusinessError implements BusinessError {
    NOT_INITIALIZED_PLANBOARD("The planboard has not been initialized for the period [{}]."),

    INVALID_PTU_DURATION("The PTU duration is not the agreed value."),
    INVALID_TIMEZONE("The Time zone is not the agreed value."),
    INVALID_CURRENCY("The Currency is not the agreed value."),
    INVALID_DOMAIN("The domain {} does not equal the expected {}."),
    INVALID_DOMAIN_NAME("{} is not a valid domain name."),
    INVALID_ENTITY_ADDRESS("{} is not a valid domain name."),

    DOCUMENT_EXIRED("{} with sequence number {} expired at {}"),
    RELATED_MESSAGE_NOT_FOUND("The message with sequence {} does not exist. "),

    WRONG_NUMBER_OF_PTUS("The number of PTU's is {} instead of {}. The message will be rejected."),
    INCOMPLETE_PTUS("Incomplete set of PTU's missing {} "),
    PTUS_IN_WRONG_PHASE("PTU's for gridpoint {} and period {} are all in one of the following phases {}."),

    NO_FLEX_OFFER_RELATED("Impossible to find a flex offer with the sequence {} for participant {}."),
    NO_PLAN_BOARD_MESSAGE_RELATED("Impossible to find a plan board message with the sequence {}."),
    FLEX_OFFER_ALREADY_ORDERED("Flex Offer has already been ordered, can not be revoked {}."),
    FLEX_OFFER_HAS_PTU_IN_OPERATE_OR_LATER_PHASE(
            "The flex offer {} for participant {} has PTU {} in operate (or later) phase already."),

    UNRECOGNIZED_CONNECTION_GROUP("No connection group with usef identifier [{}] is active.");

    private final String errorMessage;

    CoreBusinessError(String errorMessage) {
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
