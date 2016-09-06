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

import energy.usef.core.data.xml.bean.message.DispositionAvailableRequested;
import energy.usef.core.workflow.dto.DispositionTypeDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

/**
 *
 */
public class DispositionTransformerTest extends TestCase {

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        assertEquals("There must be only one constructor", 1, DispositionTransformer.class.getDeclaredConstructors().length);
        Constructor<DispositionTransformer> constructor = DispositionTransformer.class.getDeclaredConstructor();
        assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransformDispositionAvailableRequested() throws Exception {
        Assert.assertEquals(null, DispositionTransformer.transform((energy.usef.core.model.DispositionAvailableRequested) null));
        Assert.assertEquals(DispositionTypeDto.AVAILABLE,
                DispositionTransformer.transform(energy.usef.core.model.DispositionAvailableRequested.AVAILABLE));
        Assert.assertEquals(DispositionTypeDto.REQUESTED,
                DispositionTransformer.transform(energy.usef.core.model.DispositionAvailableRequested.REQUESTED));
    }

    @Test
    public void testTransformDispositionTypeDto() throws Exception {
        Assert.assertEquals(null, DispositionTransformer.transform((DispositionTypeDto) null));
        Assert.assertEquals(energy.usef.core.model.DispositionAvailableRequested.AVAILABLE,
                DispositionTransformer.transform(DispositionTypeDto.AVAILABLE));
        Assert.assertEquals(energy.usef.core.model.DispositionAvailableRequested.REQUESTED,
                DispositionTransformer.transform(DispositionTypeDto.REQUESTED));
    }

    @Test
    public void testTransformToXml() throws Exception {
        Assert.assertEquals(null, DispositionTransformer.transformToXml((DispositionTypeDto) null));
        Assert.assertEquals(DispositionAvailableRequested.AVAILABLE,
                DispositionTransformer.transformToXml(DispositionTypeDto.AVAILABLE));
        Assert.assertEquals(DispositionAvailableRequested.REQUESTED,
                DispositionTransformer.transformToXml(DispositionTypeDto.REQUESTED));
    }
}
