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

package energy.usef.core.workflow.settlement;

/**
 * Core workflow parameters for initiating settlement (cross-USEF-role).
 */
public final class CoreInitiateSettlementParameter {

    public enum IN {
        PROGNOSIS_DTO_LIST,
        FLEX_REQUEST_DTO_LIST,
        FLEX_OFFER_DTO_LIST,
        FLEX_ORDER_DTO_LIST,
        START_DATE,
        END_DATE,
        PTU_DURATION
    }

    public enum OUT {
        SETTLEMENT_DTO
    }

}
