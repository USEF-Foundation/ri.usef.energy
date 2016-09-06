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

package energy.usef.core.workflow.transformer;

import energy.usef.core.dto.PtuContainerDto;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.util.DateTimeUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class PtuContainerTransformerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PtuContainerTransformerTest.class);

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, PtuContainerTransformer.class.getDeclaredConstructors().length);
        Constructor<PtuContainerTransformer> constructor = PtuContainerTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransform() {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuDate(DateTimeUtil.parseDate("2015-04-17"));
        ptuContainer.setPtuIndex(17);

        PtuContainerDto ptuContainerDto = PtuContainerTransformer.transform(ptuContainer);

        LOGGER.info("{}", ptuContainerDto);
        Assert.assertNotNull(ptuContainerDto);
        Assert.assertEquals(DateTimeUtil.parseDate("2015-04-17"), ptuContainerDto.getPtuDate());
        Assert.assertEquals(17, ptuContainerDto.getPtuIndex());
    }

}
