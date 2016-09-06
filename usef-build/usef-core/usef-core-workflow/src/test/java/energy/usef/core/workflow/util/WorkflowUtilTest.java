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

package energy.usef.core.workflow.util;

import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.exception.WorkflowException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for {@link WorkflowUtil}
 */
public class WorkflowUtilTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowUtilTest.class);

    private enum KEYNAME {
        KEY_NAME
    }

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, WorkflowUtil.class.getDeclaredConstructors().length);
        Constructor<WorkflowUtil> constructor = WorkflowUtil.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testContextValidationWithCorrectProperties() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(KEYNAME.KEY_NAME.name(), "Dummy Value");
        WorkflowUtil.validateContext("Test", context, KEYNAME.values());
    }

    @Test
    public void testContextValidationWithEmptyContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        try {
            WorkflowUtil.validateContext("Test", context, KEYNAME.values());
            Assert.fail("Empty context should throw exception.");
        } catch (WorkflowException we) {
            LOGGER.info(we.getMessage());
        }
    }

}
