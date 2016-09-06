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

package energy.usef.agr.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Util class for doing operations with List objects.
 */
public class ListUtil {

    private ListUtil() {
        // do nothing. Prevent instantiation.
    }

    /**
     * Transforms a List of {@link Integer}s representing non-zero bits into a {@Link BigInteger} of the represented value.
     * <p/>
     * Example: a list with {@Link Integer} values 2, 3 and 5 is transformed into a {@Link BigInteger} with value 22.
     *
     * @param list
     * @return
     */
    public static BigInteger listToBigInteger(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return BigInteger.ZERO;
        }
        list.sort((i1, i2) -> i1 > i2 ? 1 : -1);
        int max = list.get(list.size() - 1);
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < max; ++i) {
            buffer.append('0');
        }
        for (Integer integer : list) {
            buffer.setCharAt(max - integer, '1');
        }
        return new BigInteger(buffer.toString(), 2);
    }

    /**
     * Transforms a {@link BigInteger} number to a List of {@link Integer}.
     * <p/>
     * Example: the BigInteger 68 is transformed to its binary representation <code>1000100</code> witch is transformed to a list
     * with integers 3 and 7, since only the 3rd and 7th bit are true.
     *
     * @param number
     * @return
     */
    public static List<Integer> bigIntegerToList(BigInteger number) {
        if (number == null) {
            return new ArrayList<>(0);
        }
        String bitArray = number.toString(2);
        List<Integer> result = new ArrayList<>();
        int length = bitArray.length();
        for (int index = length - 1; index >= 0; --index) {
            if (bitArray.charAt(index) == '1') {
                result.add(length - index);
            }
        }
        return result;
    }
}
