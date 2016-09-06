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

package energy.usef.core.util;

import java.math.BigDecimal;

/**
 * Util class to do calculi with BigDecimals.
 */
public class BigDecimalUtil {

    private BigDecimalUtil() {
        // hide implicit constructor.
    }
    /**
     * Verifies if 2 {@link BigDecimal}s are identical (both null or both the same integer value).
     *
     * @param value1 a {@link BigDecimal}.
     * @param value2 a {@link BigDecimal}.
     * @return boolean indicating equality.
     */
    public static boolean identical (BigDecimal value1, BigDecimal value2) {
        return value1 != null ? value1.equals(value2) : value2 == null;
    }
}
