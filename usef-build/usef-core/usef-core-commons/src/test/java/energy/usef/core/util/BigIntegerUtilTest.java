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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link BigIntegerUtil} class.
 */
public class BigIntegerUtilTest {

    @Test
    public void testAverageRegularCase() throws Exception {
        BigInteger bi1 = BigInteger.valueOf(1L);
        BigInteger bi2 = BigInteger.valueOf(2L);
        BigInteger bi3 = BigInteger.valueOf(3L);
        BigInteger average = BigIntegerUtil.average(bi1, bi2, bi3);
        Assert.assertEquals(BigInteger.valueOf(2L), average);
    }

    @Test
    public void testAverageWithNegative() {
        BigInteger bi1 = BigInteger.valueOf(1L);
        BigInteger bi2 = BigInteger.valueOf(-2L);
        BigInteger average = BigIntegerUtil.average(bi1, bi2);
        Assert.assertEquals(BigInteger.valueOf(0), average);
    }

    @Test
    public void testAverageEmptyList() throws Exception {
        BigInteger average = BigIntegerUtil.average();
        Assert.assertNull(average);
    }

    @Test
    public void testSumRegularCase() {
        BigInteger bi1 = BigInteger.valueOf(1L);
        BigInteger bi2 = BigInteger.valueOf(2L);
        BigInteger bi3 = BigInteger.valueOf(3L);
        BigInteger sum = BigIntegerUtil.sum(bi1, bi2, bi3);
        Assert.assertEquals(BigInteger.valueOf(6L), sum);
    }
    @Test
    public void testSumWithNullValue() {
        BigInteger bi1 = BigInteger.valueOf(1L);
        BigInteger bi2 = null;
        BigInteger bi3 = BigInteger.valueOf(3L);
        BigInteger sum = BigIntegerUtil.sum(bi1, bi2, bi3);
        Assert.assertEquals(BigInteger.valueOf(4L), sum);
    }

    @Test
    public void testSumWithEmptyArray() {
        BigInteger sum = BigIntegerUtil.sum();
        Assert.assertNull(sum);
    }

    @Test
    public void testIdentical() throws Exception {
        Assert.assertTrue(BigIntegerUtil.identical(BigInteger.valueOf(1L), BigInteger.valueOf(1L)));
        Assert.assertTrue(BigIntegerUtil.identical(null, null));
        Assert.assertFalse(BigIntegerUtil.identical(null, BigInteger.valueOf(1L)));
        Assert.assertFalse(BigIntegerUtil.identical(BigInteger.valueOf(1L), null));
        Assert.assertFalse(BigIntegerUtil.identical(BigInteger.valueOf(2L), BigInteger.valueOf(1L)));
    }

}
