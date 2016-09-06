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

package energy.usef.core.workflow.util;

import java.math.BigInteger;

/**
 * A Util class to assist with some general calculations related to the settlement process.
 */
public class SettlementUtil {

    /**
     * Default private constructor to prevent instantiation.
     */
    private SettlementUtil() {
        // empty constructor to prevent instantiation.
    }

    /**
     * Calculates the Delivered Flex Power.
     * The ActualPrognosisPower(N) is calculated by subtracting the prognosis from the allocatedPower.
     * Then in combination with orderedFlexPower (F) the deliveredFlexPower is calculated.
     *
     * @param orderedFlexPower
     * @param prognosisPower
     * @param allocatedPower
     * @return {@link BigInteger} The delivered Flex Power
     */
    public static BigInteger calculateDeliveredFlexPower(BigInteger orderedFlexPower, BigInteger prognosisPower,
            BigInteger allocatedPower) {
        //N = ActualPrognosis
        //F = OrderedFlexPower
        BigInteger actualPrognosis = allocatedPower.subtract(prognosisPower);
        BigInteger deliveredFlexPower;
        if (orderedFlexPower.compareTo(BigInteger.ZERO) >= 0) {
            //F >= 0: IF(N >= F, MIN(N, F), MAX(0, N))
            if (actualPrognosis.compareTo(orderedFlexPower) >= 0) {
                deliveredFlexPower = actualPrognosis.min(orderedFlexPower);
            } else {
                deliveredFlexPower = BigInteger.ZERO.max(actualPrognosis);
            }
        } else {
            //F < 0: ABS(IF(N <= F, MAX(N, F), MIN(0, N)))
            if (actualPrognosis.compareTo(orderedFlexPower) <= 0) {
                deliveredFlexPower = actualPrognosis.max(orderedFlexPower).abs();
            } else {
                deliveredFlexPower = BigInteger.ZERO.min(actualPrognosis).abs();
            }
        }
        return deliveredFlexPower;
    }
}
