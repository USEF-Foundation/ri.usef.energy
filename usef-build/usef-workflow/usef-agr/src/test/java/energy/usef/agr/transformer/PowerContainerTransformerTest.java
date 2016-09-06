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

package energy.usef.agr.transformer;

import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.PowerDataDto;
import energy.usef.agr.model.ConnectionPowerContainer;
import energy.usef.agr.model.ForecastPowerData;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.model.PowerData;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

/**
 *
 */
public class PowerContainerTransformerTest extends TestCase {
    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, PowerContainerTransformer.class.getDeclaredConstructors().length);
        Constructor<PowerContainerTransformer> constructor = PowerContainerTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransform() throws Exception {

        PowerContainer powerContainer = new ConnectionPowerContainer();
        powerContainer.setProfile(new PowerData());
        powerContainer.getProfile().setAverageConsumption(BigInteger.valueOf(1));
        powerContainer.getProfile().setAverageProduction(BigInteger.valueOf(2));
        powerContainer.getProfile().setPotentialFlexConsumption(BigInteger.valueOf(3));
        powerContainer.getProfile().setPotentialFlexProduction(BigInteger.valueOf(4));
        powerContainer.getProfile().setUncontrolledLoad(BigInteger.valueOf(5));
        powerContainer.setForecast(new ForecastPowerData());
        powerContainer.getForecast().setAverageConsumption(BigInteger.valueOf(6));
        powerContainer.getForecast().setAverageProduction(BigInteger.valueOf(7));
        powerContainer.getForecast().setPotentialFlexConsumption(BigInteger.valueOf(8));
        powerContainer.getForecast().setPotentialFlexProduction(BigInteger.valueOf(9));
        powerContainer.getForecast().setUncontrolledLoad(BigInteger.valueOf(10));
        powerContainer.getForecast().setAllocatedFlexConsumption(BigInteger.valueOf(10));
        powerContainer.getForecast().setAllocatedFlexProduction(BigInteger.valueOf(10));
        powerContainer.setObserved(new PowerData());
        powerContainer.getObserved().setAverageConsumption(BigInteger.valueOf(11));
        powerContainer.getObserved().setAverageProduction(BigInteger.valueOf(12));
        powerContainer.getObserved().setPotentialFlexConsumption(BigInteger.valueOf(13));
        powerContainer.getObserved().setPotentialFlexProduction(BigInteger.valueOf(14));
        powerContainer.getObserved().setUncontrolledLoad(BigInteger.valueOf(15));

        PowerContainerDto result = PowerContainerTransformer.transform(powerContainer);

        assertEquals(BigInteger.valueOf(1), result.getProfile().getAverageConsumption());
        assertEquals(BigInteger.valueOf(2), result.getProfile().getAverageProduction());
        assertEquals(BigInteger.valueOf(3), result.getProfile().getPotentialFlexConsumption());
        assertEquals(BigInteger.valueOf(4), result.getProfile().getPotentialFlexProduction());
        assertEquals(BigInteger.valueOf(5), result.getProfile().getUncontrolledLoad());
        assertEquals(BigInteger.valueOf(6), result.getForecast().getAverageConsumption());
        assertEquals(BigInteger.valueOf(7), result.getForecast().getAverageProduction());
        assertEquals(BigInteger.valueOf(8), result.getForecast().getPotentialFlexConsumption());
        assertEquals(BigInteger.valueOf(9), result.getForecast().getPotentialFlexProduction());
        assertEquals(BigInteger.valueOf(10), result.getForecast().getAllocatedFlexConsumption());
        assertEquals(BigInteger.valueOf(10), result.getForecast().getAllocatedFlexProduction());
        assertEquals(BigInteger.valueOf(10), result.getForecast().getUncontrolledLoad());
        assertEquals(BigInteger.valueOf(11), result.getObserved().getAverageConsumption());
        assertEquals(BigInteger.valueOf(12), result.getObserved().getAverageProduction());
        assertEquals(BigInteger.valueOf(13), result.getObserved().getPotentialFlexConsumption());
        assertEquals(BigInteger.valueOf(14), result.getObserved().getPotentialFlexProduction());
        assertEquals(BigInteger.valueOf(15), result.getObserved().getUncontrolledLoad());
    }

    @Test
    public void testUpdateValues() throws Exception {
        PowerData toPowerContainerModel = new PowerData();
        PowerDataDto fromPowerContainerDTO = new PowerDataDto();
        fromPowerContainerDTO.setAverageConsumption(BigInteger.valueOf(1));
        fromPowerContainerDTO.setAverageProduction(BigInteger.valueOf(2));
        fromPowerContainerDTO.setPotentialFlexConsumption(BigInteger.valueOf(3));
        fromPowerContainerDTO.setPotentialFlexProduction(BigInteger.valueOf(4));
        fromPowerContainerDTO.setUncontrolledLoad(BigInteger.valueOf(5));

        PowerContainerTransformer.updateValues(fromPowerContainerDTO, toPowerContainerModel);

        assertEquals(BigInteger.valueOf(1), toPowerContainerModel.getAverageConsumption());
        assertEquals(BigInteger.valueOf(2), toPowerContainerModel.getAverageProduction());
        assertEquals(BigInteger.valueOf(3), toPowerContainerModel.getPotentialFlexConsumption());
        assertEquals(BigInteger.valueOf(4), toPowerContainerModel.getPotentialFlexProduction());
        assertEquals(BigInteger.valueOf(5), toPowerContainerModel.getUncontrolledLoad());
    }
}
