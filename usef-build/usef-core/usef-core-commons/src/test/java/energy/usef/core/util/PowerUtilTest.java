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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class PowerUtilTest {

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, PowerUtil.class.getDeclaredConstructors().length);
        Constructor<PowerUtil> constructor = PowerUtil.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testPowerToEnergy() {
        Assert.assertEquals(BigInteger.valueOf(25), PowerUtil.powerToEnergy(BigInteger.valueOf(100), 15));
        Assert.assertEquals(BigInteger.valueOf(100), PowerUtil.powerToEnergy(BigInteger.valueOf(100), 60));
    }

    @Test
    public void testpowerPriceToMWHPrice() {
        Assert.assertEquals(BigDecimal.valueOf(1157.35749), PowerUtil.wattPricePerPTUToMWhPrice(BigInteger.valueOf(1000),
                new BigDecimal(8.1015023984029385720394820347230984230942).divide(BigDecimal.valueOf(7.0)), 60));
        Assert.assertTrue(BigDecimal.valueOf(4000)
                .compareTo(PowerUtil.wattPricePerPTUToMWhPrice(BigInteger.valueOf(1000), new BigDecimal(1), 15))
                == 0);
        Assert.assertTrue(BigDecimal.valueOf(-4000)
                .compareTo(PowerUtil.wattPricePerPTUToMWhPrice(BigInteger.valueOf(-1000), new BigDecimal(-1), 15)) == 0);
    }

    @Test
    public void testMegaWattHourPriceToWattPricePerPtu() {
        BigDecimal wattPrice = PowerUtil.megaWattHourPriceToWattPricePerPtu(BigDecimal.valueOf(4000000), 15);
        Assert.assertEquals(new BigDecimal("1.00000"), wattPrice);
    }
}
