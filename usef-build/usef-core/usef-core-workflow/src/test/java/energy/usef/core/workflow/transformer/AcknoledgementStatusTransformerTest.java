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

import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.workflow.dto.AcknowledgementStatusDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link AcknoledgementStatusTransformer} class.
 */
public class AcknoledgementStatusTransformerTest {

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, AcknoledgementStatusTransformer.class.getDeclaredConstructors().length);
        Constructor<AcknoledgementStatusTransformer> constructor = AcknoledgementStatusTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransformIsSuccessful() throws Exception {
        AcknowledgementStatusDto dto = AcknoledgementStatusTransformer.transform(AcknowledgementStatus.ACCEPTED);
        Assert.assertNotNull(dto);
        Assert.assertEquals(AcknowledgementStatusDto.ACCEPTED, dto);
    }

    @Test
    public void testTransformReturnsNull() throws Exception {
        AcknowledgementStatusDto dto = AcknoledgementStatusTransformer.transform(null);
        Assert.assertNull(dto);
    }

    @Test
    public void testTransformReturnsNoResponseStatus() throws Exception {
        AcknowledgementStatusDto dto = AcknoledgementStatusTransformer.transform(AcknowledgementStatus.NO_RESPONSE);
        Assert.assertNotNull(dto);
        Assert.assertEquals(AcknowledgementStatusDto.NO_RESPONSE, dto);
    }

    @Test
    public void testTransformStatusRejected() throws Exception {
        AcknowledgementStatusDto dto = AcknoledgementStatusTransformer.transform(AcknowledgementStatus.REJECTED);
        Assert.assertNotNull(dto);
        Assert.assertEquals(AcknowledgementStatusDto.REJECTED, dto);
    }

    @Test
    public void testTransformStatusSent() throws Exception {
        AcknowledgementStatusDto dto = AcknoledgementStatusTransformer.transform(AcknowledgementStatus.SENT);
        Assert.assertNotNull(dto);
        Assert.assertEquals(AcknowledgementStatusDto.SENT, dto);
    }

}
