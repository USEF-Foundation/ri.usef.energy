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
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link ListUtil} util class.
 */
public class ListUtilTest {

    @Test
    public void testListToBigIntegerWithSmallNumbers() throws Exception {
        BigInteger value = ListUtil.listToBigInteger(Arrays.asList(3,7));
        Assert.assertEquals(BigInteger.valueOf(68), value);
    }

    @Test
    public void testListToBigIntegerWithBiggerNumber() throws Exception {
        BigInteger value = ListUtil.listToBigInteger(Arrays.asList(6,8,15));
        Assert.assertEquals(BigInteger.valueOf(16544), value);
    }

    @Test
    public void testListToBigIntegerWithUnorderedList() throws Exception {
        BigInteger value = ListUtil.listToBigInteger(Arrays.asList(15,6,8));
        Assert.assertEquals(BigInteger.valueOf(16544), value);
    }

    /**
     * Tests with a small big integer.
     */
    @Test
    public void testBigIntegerToListWithSmallList() {
        final BigInteger number = new BigInteger("68");
        List<Integer> integers = ListUtil.bigIntegerToList(number);
        Assert.assertEquals(2, integers.size());
        Assert.assertEquals(3, integers.get(0).intValue());
        Assert.assertEquals(7, integers.get(1).intValue());
    }

    /**
     * Tests with a small big integer.
     */
    @Test
    public void testBigIntegerToListWithBigList() {
        final BigInteger number = new BigInteger("16544"); // binary : 100000010100000
        List<Integer> integers = ListUtil.bigIntegerToList(number);
        Assert.assertEquals(3, integers.size());
        Assert.assertEquals(6, integers.get(0).intValue());
        Assert.assertEquals(8, integers.get(1).intValue());
        Assert.assertEquals(15, integers.get(2).intValue());
    }

    @Test
    public void testBigIntegerToListWithNull() {
        List<Integer> integers = ListUtil.bigIntegerToList(null);
        Assert.assertTrue(integers.isEmpty());
    }

    @Test
    public void testListToBigIntegerWithEmptyList() {
        BigInteger result = ListUtil.listToBigInteger(new ArrayList<>());
        Assert.assertEquals(BigInteger.ZERO, result);
    }
}
