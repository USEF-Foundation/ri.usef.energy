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

package energy.usef.brp.model;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class CommonReferenceOperatorTest {

    /***
     * This tests the equals and hashcode methods of the entity Aggregator.
     */
    @Test
    public void testEquals() {
        CommonReferenceOperator participant1 = newCommonReferenceOperator("domain1");
        CommonReferenceOperator participant2 = newCommonReferenceOperator("domain2");
        CommonReferenceOperator participant3 = newCommonReferenceOperator("domain1");

        Assert.assertTrue(participant1.equals(participant1));
        Assert.assertTrue(!participant1.equals(null));
        Assert.assertTrue(!participant1.equals(participant2));
        Assert.assertTrue(participant1.equals(participant3));
    }

    private CommonReferenceOperator newCommonReferenceOperator(String domain) {
        CommonReferenceOperator object = new CommonReferenceOperator();
        object.setDomain(domain);
        return object;
    }
}
