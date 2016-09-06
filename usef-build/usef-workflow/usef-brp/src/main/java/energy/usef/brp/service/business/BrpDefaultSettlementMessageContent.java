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

package energy.usef.brp.service.business;

/**
 * Enumeration of the default values which can be passed in a dummy settlement message (i.e. when no flex trading occurred during
 * a period but that a settlement message has to be sent nonetheless).
 */
public enum BrpDefaultSettlementMessageContent {

    ORDER_SETTLEMENT_ORDER_REFERENCE("0"),
    ORDER_SETTLEMENT_PROGNOSIS_SEQUENCE("0"),
    PTU_SETTLEMENT_ORDERED_FLEX_POWER("0"),
    PTU_SETTLEMENT_DELIVERED_FLEX_POWER("0"),
    PTU_SETTLEMENT_ACTUAL_POWER("0"),
    PTU_SETTLEMENT_PROGNOSIS_POWER("0"),
    PTU_SETTLEMENT_START("0"),
    PTU_SETTLEMENT_PRICE("0"),
    PTU_SETTLEMENT_NET_SETTLEMENT("0");

    private final String value ;

    BrpDefaultSettlementMessageContent(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
