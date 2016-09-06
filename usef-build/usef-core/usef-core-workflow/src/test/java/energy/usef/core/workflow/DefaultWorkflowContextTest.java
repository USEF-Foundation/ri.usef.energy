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

package energy.usef.core.workflow;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link DefaultWorkflowContext} implementation of {@link WorkflowContext}.
 */
public class DefaultWorkflowContextTest {

    private WorkflowContext workflowContext;

    @Before
    public void setUp() throws Exception {
        workflowContext = new DefaultWorkflowContext();
    }

    @Test
    public void testGetAndSetValue() throws Exception {
        workflowContext.setValue("VALUE", 1L);
        Assert.assertNull(workflowContext.getValue("OTHERVALUE"));
        Assert.assertEquals(1L, workflowContext.getValue("VALUE"));
        Assert.assertEquals(1L, workflowContext.get("VALUE", Long.class).longValue());
    }

    @Test
    public void testRemove() throws Exception {
        workflowContext.setValue("KEY", 1L);
        Assert.assertEquals(1L, workflowContext.getValue("KEY"));
        workflowContext.remove("KEY");
        Assert.assertNull(workflowContext.getValue("KEY"));
    }

    @Test
    public void testRemoveNotPresentKey() {
        workflowContext.setValue("VALUE", 1L);
        try {
            workflowContext.remove("KEY");
        } catch (Exception e) {
            Assert.fail("Exception caught while expecting nothing.");
        }

    }

    @Test
    public void testToString() throws Exception {
        workflowContext.setValue("COLLECTION", Collections.singletonList(1L));
        workflowContext.setValue("KEY", 1L);
        Assert.assertEquals("\n\t # COLLECTION=1 elements \n\t # KEY=1", workflowContext.toString());
    }
}
