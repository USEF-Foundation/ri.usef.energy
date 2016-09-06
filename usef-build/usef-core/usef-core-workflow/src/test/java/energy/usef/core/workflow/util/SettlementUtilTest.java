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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by MasterMe on 23-7-2015.
 */
public class SettlementUtilTest {

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, SettlementUtil.class.getDeclaredConstructors().length);
        Constructor<SettlementUtil> constructor = SettlementUtil.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }


    /**
     * Exact unit test, with same data as xls in user story: ALUS-559.
     *
     * @throws Exception
     */
    @Test
    public void testCalculateDeliverdFlexPower() throws Exception {
        assertEquals(BigInteger.valueOf(100), SettlementUtil.calculateDeliveredFlexPower(BigInteger.valueOf(100),
                BigInteger.valueOf(1000), BigInteger.valueOf(1200)));

        assertEquals(BigInteger.valueOf(99), SettlementUtil.calculateDeliveredFlexPower(BigInteger.valueOf(-100),
                BigInteger.valueOf(1000), BigInteger.valueOf(901)));

        assertEquals(BigInteger.valueOf(0), SettlementUtil.calculateDeliveredFlexPower(BigInteger.valueOf(100),
                BigInteger.valueOf(1000), BigInteger.valueOf(901)));

        assertEquals(BigInteger.valueOf(99), SettlementUtil.calculateDeliveredFlexPower(BigInteger.valueOf(-100),
                BigInteger.valueOf(1000), BigInteger.valueOf(901)));

        assertEquals(BigInteger.valueOf(50), SettlementUtil.calculateDeliveredFlexPower(BigInteger.valueOf(100),
                BigInteger.valueOf(-1000), BigInteger.valueOf(-950)));

        assertEquals(BigInteger.valueOf(50), SettlementUtil.calculateDeliveredFlexPower(BigInteger.valueOf(-100),
                BigInteger.valueOf(-1000), BigInteger.valueOf(-1050)));

        assertEquals(BigInteger.valueOf(50), SettlementUtil.calculateDeliveredFlexPower(BigInteger.valueOf(100),
                BigInteger.valueOf(-1000), BigInteger.valueOf(-950)));

        assertEquals(BigInteger.valueOf(0), SettlementUtil.calculateDeliveredFlexPower(BigInteger.valueOf(-100),
                BigInteger.valueOf(-1000), BigInteger.valueOf(-900)));

    }
}
