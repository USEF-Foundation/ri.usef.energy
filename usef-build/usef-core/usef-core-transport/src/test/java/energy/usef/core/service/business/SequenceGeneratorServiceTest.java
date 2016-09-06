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

package energy.usef.core.service.business;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the
 */
public class SequenceGeneratorServiceTest {

    private SequenceGeneratorService sequenceGeneratorService;

    @Before
    public void setUp() throws Exception {
        sequenceGeneratorService = new SequenceGeneratorService();
    }

    @Test
    public void testNext() throws Exception {
        long sequence1 = sequenceGeneratorService.next();
        long sequence2 = sequenceGeneratorService.next();
        Assert.assertNotEquals(sequence1, sequence2);
        Assert.assertEquals(sequence2, sequence1+1);
    }
}
