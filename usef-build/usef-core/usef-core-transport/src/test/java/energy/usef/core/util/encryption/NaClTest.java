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

package energy.usef.core.util.encryption;

import energy.usef.core.util.VersionUtil;
import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;

/**
 *
 */
public class NaClTest extends TestCase {

    private static final String VERSION = "1.0.11";

    @Before
    public void init() throws Exception {
    }

    @Test
    public void testVersion () {
        assertTrue(VersionUtil.compareVersions(VERSION, NaCl.sodium().sodium_version_string()) <= 0);
    }
}
