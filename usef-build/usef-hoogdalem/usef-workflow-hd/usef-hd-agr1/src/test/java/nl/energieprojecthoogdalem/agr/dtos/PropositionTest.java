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
package nl.energieprojecthoogdalem.agr.dtos;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class PropositionTest {

    private Proposition all, none, pv, bat;

    @Before
    public void setUp() throws Exception {
        all = new Proposition("y","y");
        bat = new Proposition("n","y");
        pv = new Proposition("y","n");
        none = new Proposition("n","n");
    }

    @Test
    public void testHas() throws Exception {
        checkProposition(all, true ,true);
        checkProposition(bat, false ,true);
        checkProposition(pv, true ,false);
        checkProposition(none, false ,false);
    }

    private void checkProposition(Proposition proposition, boolean hasPv, boolean hasBat)
    {
        Assert.assertEquals(hasPv, proposition.hasPv());
        Assert.assertEquals(hasBat, proposition.hasBattery());
    }

}