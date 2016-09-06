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

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

/**
 *
 */
public class BigDecimalUtilTest extends TestCase {
    @Test
    public void testIdentical() throws Exception {
        Assert.assertTrue(BigDecimalUtil.identical(BigDecimal.valueOf(1L), BigDecimal.valueOf(1L)));
        Assert.assertTrue(BigDecimalUtil.identical(null, null));
        Assert.assertFalse(BigDecimalUtil.identical(null, BigDecimal.valueOf(1L)));
        Assert.assertFalse(BigDecimalUtil.identical(BigDecimal.valueOf(1L), null));
        Assert.assertFalse(BigDecimalUtil.identical(BigDecimal.valueOf(2L), BigDecimal.valueOf(1L)));
    }

}
