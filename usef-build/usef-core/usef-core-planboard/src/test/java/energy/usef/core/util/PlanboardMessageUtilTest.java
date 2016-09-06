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

package energy.usef.core.util;

import energy.usef.core.model.PlanboardMessage;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;

public class PlanboardMessageUtilTest {

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, PlanboardMessageUtil.class.getDeclaredConstructors().length);
        Constructor<PlanboardMessageUtil> constructor = PlanboardMessageUtil.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testSortPlanboardMessageListDescByCreationTime() throws Exception {

        LocalDateTime localDateTime = DateTimeUtil.getCurrentDateTime();

        List<PlanboardMessage> planboardMessageList = IntStream.rangeClosed(0, 20).mapToObj(index -> {
            PlanboardMessage pbMessage = new PlanboardMessage();
            pbMessage.setCreationDateTime(localDateTime.minusHours(index));
            return pbMessage;
        }).collect(Collectors.toList());

        PlanboardMessageUtil.sortPlanboardMessageListDescByCreationTime(planboardMessageList);

        Assert.assertEquals(planboardMessageList.get(0).getCreationDateTime(), localDateTime);
        Assert.assertEquals(planboardMessageList.get(1).getCreationDateTime(), localDateTime.minusHours(1));
        Assert.assertEquals(planboardMessageList.get(2).getCreationDateTime(), localDateTime.minusHours(2));
        Assert.assertEquals(planboardMessageList.get(3).getCreationDateTime(), localDateTime.minusHours(3));
        Assert.assertEquals(planboardMessageList.get(4).getCreationDateTime(), localDateTime.minusHours(4));
        Assert.assertEquals(planboardMessageList.get(20).getCreationDateTime(), localDateTime.minusHours(20));
    }
}
