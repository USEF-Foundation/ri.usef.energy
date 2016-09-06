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

package energy.usef.brp.workflow.plan.flexoffer.revoke;

import energy.usef.core.exception.BusinessError;

/**
 * Enumeration of the business errors related to 'Revoke Flex Offers' workflow (BRP side).
 */
public enum BrpFlexOfferRevocationError implements BusinessError {
    NO_FLEX_OFFER_RELATED("Impossible to find a flex offer with the sequence {} for participant {}."),
    NO_PLAN_BOARD_MESSAGE_RELATED("Impossible to find a plan board message with the sequence {}."),
    FLEX_OFFER_HAS_PTU_IN_OPERATE_OR_LATER_PHASE(
            "The flex offer {} for participant {} has PTU {} in operate (or later) phase already.");

    private final String error;

    /**
     * Private constructor.
     */
    BrpFlexOfferRevocationError(String error) {
        this.error = error;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getError() {
        return error;
    }

}
