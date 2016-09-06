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

package energy.usef.agr.util;

import static org.junit.Assert.assertEquals;

import energy.usef.agr.model.ForecastPowerData;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.model.PowerData;
import energy.usef.agr.model.Udi;
import energy.usef.agr.model.UdiPowerContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

public class PowerContainerUtilTest {
    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, PowerContainerUtil.class.getDeclaredConstructors().length);
        Constructor<PowerContainerUtil> constructor = PowerContainerUtil.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testSumUdisPerPtu() throws Exception {

        List<Udi> udiList = new ArrayList<>();
        Map<Udi, List<PowerContainer>> udiPowerContainers = new HashMap<>();

        buildUdiAndAddTo(udiList, udiPowerContainers, BigInteger.ONE);
        buildUdiAndAddTo(udiList, udiPowerContainers, BigInteger.TEN);


        //test with one
        Map<Integer, PowerContainer> powerContainerMap = PowerContainerUtil.sumUdisPerPtu(udiList, udiPowerContainers, 15, 5);

        assertEquals(5, powerContainerMap.size());

        //udis should be summed
        BigInteger testValue = BigInteger.valueOf(11);
        powerContainerMap.entrySet().forEach(entry -> {
            assertPowerData(testValue, entry.getValue().getProfile());
            assertPowerData(testValue, entry.getValue().getForecast());
            assertPowerData(testValue, entry.getValue().getObserved());
        });
    }

    private void buildUdiAndAddTo(List<Udi> udiList, Map<Udi, List<PowerContainer>> udiPowerContainers, BigInteger base) {
        Udi udi = new Udi();
        udi.setDtuSize(5);
        udiPowerContainers.put(udi, IntStream.range(1, 16).mapToObj(i -> {
            UdiPowerContainer pc = new UdiPowerContainer();
            pc.setTimeIndex(i);
            // 1 * base , 2*base , 0* base == averages to base
            BigInteger val = base.multiply(BigInteger.valueOf(i % 3));
            PowerData powerData = new PowerData();
            powerData.setAverageConsumption(val);
            powerData.setAverageProduction(val);
            powerData.setPotentialFlexConsumption(val);
            powerData.setPotentialFlexProduction(val);
            powerData.setUncontrolledLoad(val);
            ForecastPowerData forecastPowerData = new ForecastPowerData();
            forecastPowerData.setAverageConsumption(val);
            forecastPowerData.setAverageProduction(val);
            forecastPowerData.setPotentialFlexConsumption(val);
            forecastPowerData.setPotentialFlexProduction(val);
            forecastPowerData.setUncontrolledLoad(val);
            forecastPowerData.setAllocatedFlexConsumption(val);
            forecastPowerData.setAllocatedFlexProduction(val);
            pc.setProfile(powerData);
            pc.setForecast(forecastPowerData);
            pc.setObserved(powerData);
            return pc;
        }).collect(Collectors.toList()));
        udiList.add(udi);
    }

    private void assertPowerData(BigInteger testValue, PowerData powerData) {
        assertEquals(testValue, powerData.getAverageConsumption());
        assertEquals(testValue, powerData.getAverageProduction());
        assertEquals(testValue, powerData.getPotentialFlexConsumption());
        assertEquals(testValue, powerData.getPotentialFlexProduction());
        assertEquals(testValue, powerData.getUncontrolledLoad());
    }

}
