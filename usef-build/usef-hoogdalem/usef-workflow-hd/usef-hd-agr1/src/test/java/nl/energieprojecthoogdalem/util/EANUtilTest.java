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
package nl.energieprojecthoogdalem.util;

import org.junit.Assert;
import org.junit.Test;

public class EANUtilTest {

    @Test
    public void testEANUtil() throws Exception {
        int homeNumber = 12;
        String homeString = "012";
        String ean;

        ean = EANUtil.toEAN(homeNumber);
        Assert.assertEquals(EANUtil.EAN_PREFIX + homeString, ean);

        ean = EANUtil.toEAN(homeString);
        Assert.assertEquals(EANUtil.EAN_PREFIX + homeString, ean);

        homeString = EANUtil.toHomeString(ean);
        Assert.assertEquals("012", homeString);

        homeNumber = EANUtil.toHomeInt(ean);
        Assert.assertEquals(12, homeNumber);
    }
}