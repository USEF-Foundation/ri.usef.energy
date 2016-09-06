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

import java.math.BigInteger;

/**
 * Util class to do calculi with BigIntegers.
 */
public class BigIntegerUtil {

    private BigIntegerUtil() {
        // hide implicit constructor.
    }

    /**
     * Computes the average of an array of BigIntegers (ignoring null values).
     *
     * @param values an array of {@link BigInteger}.
     * @return a {@link BigInteger}.
     */
    public static BigInteger average(BigInteger... values) {
        int count = 0;
        BigInteger total = BigInteger.ZERO;
        for (BigInteger value : values) {
            if (value == null) {
                continue;
            }
            count++;
            total = total.add(value);
        }
        if (count > 0) {
            return total.divide(BigInteger.valueOf(count));
        }
        return null;
    }

    /**
     * Sums an array of BigIntegers, ignoring null values.
     *
     * @param values an array of {@link BigInteger}.
     * @return a {@link BigInteger}.
     */
    public static BigInteger sum(BigInteger... values) {
        int count = 0;
        BigInteger total = BigInteger.ZERO;
        for (BigInteger value : values) {
            if (value == null) {
                continue;
            }
            count++;
            total = total.add(value);
        }
        if (count > 0) {
            return total;
        }
        return null;
    }

    /**
     * Compares if 2 {@link BigInteger}s are identical (both null or both the same integer value).
     *
     * @param value1 a {@link BigInteger}.
     * @param value2 a {@link BigInteger}.
     * @return boolean indicating equality.
     */
    public static boolean identical (BigInteger value1, BigInteger value2) {
        return value1 != null ? value1.equals(value2) : value2 == null;
    }

}
