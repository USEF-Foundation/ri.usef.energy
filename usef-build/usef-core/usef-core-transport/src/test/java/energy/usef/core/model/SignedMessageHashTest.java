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

package energy.usef.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SignedMessageHashTest {
    /**
     * Tests SignedMessageHash class.
     */
    @Test
    public void signedmessageHashTest() {
        SignedMessageHash hash = new SignedMessageHash();
        hash.setId(45L);

        assertEquals(0, hash.getHashedContent().length);

        hash.setHashedContent(new byte[0]);
        assertEquals(0, hash.getHashedContent().length);

        assertEquals(45, hash.getId().longValue());
        assertNotNull(hash);
    }

}
