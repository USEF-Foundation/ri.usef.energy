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

import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.workflow.dto.USEFRoleDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

/**
 *
 */
public class USEFRoleTransformerTest extends TestCase {

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, USEFRoleTransformer.class.getDeclaredConstructors().length);
        Constructor<USEFRoleTransformer> constructor = USEFRoleTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransform() {
        Assert.assertEquals(USEFRoleDto.AGR, USEFRoleTransformer.transform(USEFRole.AGR));
        Assert.assertEquals(USEFRoleDto.BRP, USEFRoleTransformer.transform(USEFRole.BRP));
        Assert.assertEquals(USEFRoleDto.CRO, USEFRoleTransformer.transform(USEFRole.CRO));
        Assert.assertEquals(USEFRoleDto.DSO, USEFRoleTransformer.transform(USEFRole.DSO));
        Assert.assertEquals(USEFRoleDto.MDC, USEFRoleTransformer.transform(USEFRole.MDC));
    }

    @Test
    public void testTransformToXml() {
        Assert.assertEquals(USEFRole.AGR, USEFRoleTransformer.transformToXml(USEFRoleDto.AGR));
        Assert.assertEquals(USEFRole.BRP, USEFRoleTransformer.transformToXml(USEFRoleDto.BRP));
        Assert.assertEquals(USEFRole.CRO, USEFRoleTransformer.transformToXml(USEFRoleDto.CRO));
        Assert.assertEquals(USEFRole.DSO, USEFRoleTransformer.transformToXml(USEFRoleDto.DSO));
        Assert.assertEquals(USEFRole.MDC, USEFRoleTransformer.transformToXml(USEFRoleDto.MDC));
    }
}
