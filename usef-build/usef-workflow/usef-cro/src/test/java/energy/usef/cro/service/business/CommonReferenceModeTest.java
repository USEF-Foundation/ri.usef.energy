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

package energy.usef.cro.service.business;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test CommonReferenceMode class.
 */
public class CommonReferenceModeTest {

    /**
     * Test CommonReferenceMode class.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCommonReferenceMode() {
        CommonReferenceMode mode = CommonReferenceMode.fromValue("OPEN");
        mode = CommonReferenceMode.fromValue("CLOSE");
        assertEquals(CommonReferenceMode.CLOSED, mode);
        assertEquals("CLOSED", mode.value());
        CommonReferenceMode.fromValue("FOO");
    }
}
