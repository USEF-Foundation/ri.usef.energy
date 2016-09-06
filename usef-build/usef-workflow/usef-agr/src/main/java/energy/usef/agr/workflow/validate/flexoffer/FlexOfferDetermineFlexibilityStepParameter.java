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

package energy.usef.agr.workflow.validate.flexoffer;

/**
 * The defined parameters for the FlexOfferDetermineFlexibilityStepParameter.
 */
public final class FlexOfferDetermineFlexibilityStepParameter {

    private FlexOfferDetermineFlexibilityStepParameter() {
        // private constructor
    }

    /**
     * The ingoing parameters for this step.
     */
    public enum IN {
        PERIOD,
        PTU_DURATION,
        FLEX_REQUEST_DTO_LIST,
        FLEX_OFFER_DTO_LIST,
        LATEST_D_PROGNOSES_DTO_LIST,
        LATEST_A_PLANS_DTO_LIST,
        CONNECTION_PORTFOLIO_DTO,
        CONNECTION_GROUPS_TO_CONNECTIONS_MAP
    }

    /**
     * The outgoing parameters for this step.
     */
    public enum OUT {
        FLEX_OFFER_DTO_LIST
    }
}
