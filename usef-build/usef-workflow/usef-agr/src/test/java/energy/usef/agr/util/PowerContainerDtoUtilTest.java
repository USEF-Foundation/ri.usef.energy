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

import energy.usef.agr.dto.ForecastPowerDataDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.PowerDataDto;
import energy.usef.agr.dto.UdiPortfolioDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test to test the {@link PowerContainerDto}.
 */
public class PowerContainerDtoUtilTest {
    private final static int PTU_INDEX = 1;

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, PowerContainerDtoUtil.class.getDeclaredConstructors().length);
        Constructor<PowerContainerDtoUtil> constructor = PowerContainerDtoUtil.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testSumPerPtu() throws Exception {
        List<PowerContainerDto> powerContainerDtoList = new ArrayList<>();
        powerContainerDtoList.add(buildPowerContainerDto(BigInteger.valueOf(10)));
        powerContainerDtoList.add(buildPowerContainerDto(BigInteger.valueOf(11)));

        PowerContainerDto summedPowerContainerDto = PowerContainerDtoUtil.sum(powerContainerDtoList);

        Assert.assertEquals(powerContainerDtoList.get(0).getProfile().getAverageConsumption()
                        .add(powerContainerDtoList.get(1).getProfile().getAverageConsumption()),
                summedPowerContainerDto.getProfile().getAverageConsumption());
        Assert.assertEquals(powerContainerDtoList.get(0).getProfile().getAverageProduction()
                        .add(powerContainerDtoList.get(1).getProfile().getAverageProduction()),
                summedPowerContainerDto.getProfile().getAverageProduction());
        Assert.assertEquals(powerContainerDtoList.get(0).getProfile().getUncontrolledLoad()
                        .add(powerContainerDtoList.get(1).getProfile().getUncontrolledLoad()),
                summedPowerContainerDto.getProfile().getUncontrolledLoad());
        Assert.assertEquals(powerContainerDtoList.get(0).getProfile().getPotentialFlexConsumption()
                        .add(powerContainerDtoList.get(1).getProfile().getPotentialFlexConsumption()),
                summedPowerContainerDto.getProfile().getPotentialFlexConsumption());
        Assert.assertEquals(powerContainerDtoList.get(0).getProfile().getPotentialFlexProduction()
                        .add(powerContainerDtoList.get(1).getProfile().getPotentialFlexProduction()),
                summedPowerContainerDto.getProfile().getPotentialFlexProduction());

        Assert.assertEquals(powerContainerDtoList.get(0).getForecast().getAverageConsumption()
                        .add(powerContainerDtoList.get(1).getForecast().getAverageConsumption()),
                summedPowerContainerDto.getForecast().getAverageConsumption());
        Assert.assertEquals(powerContainerDtoList.get(0).getForecast().getAverageProduction()
                        .add(powerContainerDtoList.get(1).getForecast().getAverageProduction()),
                summedPowerContainerDto.getForecast().getAverageProduction());
        Assert.assertEquals(powerContainerDtoList.get(0).getForecast().getUncontrolledLoad()
                        .add(powerContainerDtoList.get(1).getForecast().getUncontrolledLoad()),
                summedPowerContainerDto.getForecast().getUncontrolledLoad());
        Assert.assertEquals(powerContainerDtoList.get(0).getForecast().getPotentialFlexConsumption()
                        .add(powerContainerDtoList.get(1).getForecast().getPotentialFlexConsumption()),
                summedPowerContainerDto.getForecast().getPotentialFlexConsumption());
        Assert.assertEquals(powerContainerDtoList.get(0).getForecast().getPotentialFlexProduction()
                        .add(powerContainerDtoList.get(1).getForecast().getPotentialFlexProduction()),
                summedPowerContainerDto.getForecast().getPotentialFlexProduction());

        Assert.assertEquals(powerContainerDtoList.get(0).getObserved().getAverageConsumption()
                        .add(powerContainerDtoList.get(1).getObserved().getAverageConsumption()),
                summedPowerContainerDto.getObserved().getAverageConsumption());
        Assert.assertEquals(powerContainerDtoList.get(0).getObserved().getAverageProduction()
                        .add(powerContainerDtoList.get(1).getObserved().getAverageProduction()),
                summedPowerContainerDto.getObserved().getAverageProduction());
        Assert.assertEquals(powerContainerDtoList.get(0).getObserved().getUncontrolledLoad()
                        .add(powerContainerDtoList.get(1).getObserved().getUncontrolledLoad()),
                summedPowerContainerDto.getObserved().getUncontrolledLoad());
        Assert.assertEquals(powerContainerDtoList.get(0).getObserved().getPotentialFlexConsumption()
                        .add(powerContainerDtoList.get(1).getObserved().getPotentialFlexConsumption()),
                summedPowerContainerDto.getObserved().getPotentialFlexConsumption());
        Assert.assertEquals(powerContainerDtoList.get(0).getObserved().getPotentialFlexProduction()
                        .add(powerContainerDtoList.get(1).getObserved().getPotentialFlexProduction()),
                summedPowerContainerDto.getObserved().getPotentialFlexProduction());

    }

    @Test
    public void testSum() throws Exception {
        PowerContainerDto powerContainerDto1 = new PowerContainerDto(new LocalDate(), 1);
        PowerContainerDto powerContainerDto2 = new PowerContainerDto(new LocalDate(), 1);

        powerContainerDto1.setProfile(buildPowerDataDto(BigInteger.valueOf(2)));
        powerContainerDto1.setForecast(buildForecastPowerDataDto(BigInteger.valueOf(3)));
        powerContainerDto1.setObserved(buildPowerDataDto(BigInteger.valueOf(5)));

        powerContainerDto2.setProfile(buildPowerDataDto(BigInteger.valueOf(11)));
        powerContainerDto2.setForecast(buildForecastPowerDataDto(BigInteger.valueOf(13)));
        powerContainerDto2.setObserved(buildPowerDataDto(BigInteger.valueOf(17)));

        PowerContainerDto summedPowerContainer = PowerContainerDtoUtil.sum(powerContainerDto1, powerContainerDto2);

        Assert.assertEquals(BigInteger.valueOf((2 + 11)), summedPowerContainer.getProfile().getUncontrolledLoad());
        Assert.assertEquals(BigInteger.valueOf((2 + 11) * 2), summedPowerContainer.getProfile().getAverageConsumption());
        Assert.assertEquals(BigInteger.valueOf((2 + 11) * 3), summedPowerContainer.getProfile().getAverageProduction());
        Assert.assertEquals(BigInteger.valueOf((2 + 11) * 4), summedPowerContainer.getProfile().getPotentialFlexConsumption());
        Assert.assertEquals(BigInteger.valueOf((2 + 11) * 5), summedPowerContainer.getProfile().getPotentialFlexProduction());

        Assert.assertEquals(BigInteger.valueOf((3 + 13)), summedPowerContainer.getForecast().getUncontrolledLoad());
        Assert.assertEquals(BigInteger.valueOf((3 + 13) * 2), summedPowerContainer.getForecast().getAverageConsumption());
        Assert.assertEquals(BigInteger.valueOf((3 + 13) * 3), summedPowerContainer.getForecast().getAverageProduction());
        Assert.assertEquals(BigInteger.valueOf((3 + 13) * 4), summedPowerContainer.getForecast().getPotentialFlexConsumption());
        Assert.assertEquals(BigInteger.valueOf((3 + 13) * 5), summedPowerContainer.getForecast().getPotentialFlexProduction());

        Assert.assertEquals(BigInteger.valueOf((5 + 17)), summedPowerContainer.getObserved().getUncontrolledLoad());
        Assert.assertEquals(BigInteger.valueOf((5 + 17) * 2), summedPowerContainer.getObserved().getAverageConsumption());
        Assert.assertEquals(BigInteger.valueOf((5 + 17) * 3), summedPowerContainer.getObserved().getAverageProduction());
        Assert.assertEquals(BigInteger.valueOf((5 + 17) * 4), summedPowerContainer.getObserved().getPotentialFlexConsumption());
        Assert.assertEquals(BigInteger.valueOf((5 + 17) * 5), summedPowerContainer.getObserved().getPotentialFlexProduction());
    }

    @Test
    public void testSumWithNulls() {
        PowerContainerDto sum = PowerContainerDtoUtil.sum(null);
        Assert.assertNull(sum);
    }

    @Test
    public void testSumUdisPerPtu() {
        LocalDate period = new LocalDate("2015-04-17");

        UdiPortfolioDto udi1 = buildUdiPortfolioDto(period, 1, 15);
        UdiPortfolioDto udi2 = buildUdiPortfolioDto(period, 2, 15);
        List<UdiPortfolioDto> udis = Arrays.asList(udi1, udi2);

        Map<Integer, PowerContainerDto> powerContainerDtoMap = PowerContainerDtoUtil.sumUdisPerPtu(udis, 15, 96);

        IntStream.rangeClosed(1, 96).forEach(ptuIndex -> {
            Assert.assertEquals(BigInteger.valueOf(ptuIndex * 2),
                    powerContainerDtoMap.get(ptuIndex).getForecast().getUncontrolledLoad());
            Assert.assertEquals(BigInteger.valueOf(ptuIndex * 2),
                    powerContainerDtoMap.get(ptuIndex).getForecast().getAverageConsumption());
            Assert.assertEquals(BigInteger.valueOf(ptuIndex * 2),
                    powerContainerDtoMap.get(ptuIndex).getForecast().getAverageProduction());
        });
    }

    @Test
    public void testAverageOfUdiPortfolio() {
        LocalDate period = new LocalDate();
        UdiPortfolioDto udiPortfolioDto = buildUdiPortfolioDto(period, 1, 5); // 3 DTUs per PTU
        Map<Integer, PowerContainerDto> average = PowerContainerDtoUtil.average(udiPortfolioDto, period, 15);
        Assert.assertEquals(96, average.size());
        // assert profile values
        Assert.assertNull(average.get(PTU_INDEX).getProfile().getUncontrolledLoad());
        Assert.assertNull(average.get(PTU_INDEX).getProfile().getAverageConsumption());
        Assert.assertNull(average.get(PTU_INDEX).getProfile().getAverageProduction());
        Assert.assertNull(average.get(PTU_INDEX).getProfile().getPotentialFlexConsumption());
        Assert.assertNull(average.get(PTU_INDEX).getProfile().getPotentialFlexProduction());
        // assert forecast value
        Assert.assertEquals(2,average.get(PTU_INDEX).getForecast().getUncontrolledLoad().intValue());
        Assert.assertEquals(2,average.get(PTU_INDEX).getForecast().getAverageConsumption().intValue());
        Assert.assertEquals(2,average.get(PTU_INDEX).getForecast().getAverageProduction().intValue());
        // assert observed value
        Assert.assertNull(average.get(PTU_INDEX).getObserved().getUncontrolledLoad());
        Assert.assertNull(average.get(PTU_INDEX).getObserved().getAverageConsumption());
        Assert.assertNull(average.get(PTU_INDEX).getObserved().getAverageProduction());
        Assert.assertNull(average.get(PTU_INDEX).getObserved().getPotentialFlexConsumption());
        Assert.assertNull(average.get(PTU_INDEX).getObserved().getPotentialFlexProduction());

    }

    private UdiPortfolioDto buildUdiPortfolioDto(LocalDate period, int nr, int dtuSize) {
        UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto("endpoint:" + nr, dtuSize, "profile");

        int dtusPerPtu = 15 / dtuSize;

        IntStream.rangeClosed(1, 96 * dtusPerPtu).forEach(ptuIndex -> {
            PowerContainerDto powerContainer = new PowerContainerDto(period, ptuIndex);

            powerContainer.getForecast().setUncontrolledLoad(BigInteger.valueOf(ptuIndex));
            powerContainer.getForecast().setAverageConsumption(BigInteger.valueOf(ptuIndex));
            powerContainer.getForecast().setAverageProduction(BigInteger.valueOf(ptuIndex));

            udiPortfolioDto.getUdiPowerPerDTU().put(ptuIndex, powerContainer);
        });

        return udiPortfolioDto;
    }

    @Test
    public void testAveragePowerContainers() {
        PowerContainerDto powerContainer1 = buildPowerContainerDto(BigInteger.valueOf(10));
        PowerContainerDto powerContainer2 = buildPowerContainerDto(BigInteger.valueOf(8));
        PowerContainerDto powerContainer3 = buildPowerContainerDto(BigInteger.valueOf(6));
        PowerContainerDto average = PowerContainerDtoUtil.average(1, powerContainer1, powerContainer2, powerContainer3);
        Assert.assertEquals(1, average.getTimeIndex().intValue());
        Assert.assertEquals(new LocalDate(), average.getPeriod());
        // assert profile values
        Assert.assertEquals(8, average.getProfile().getUncontrolledLoad().intValue());
        Assert.assertEquals(16, average.getProfile().getAverageConsumption().intValue());
        Assert.assertEquals(24, average.getProfile().getAverageProduction().intValue());
        Assert.assertEquals(32, average.getProfile().getPotentialFlexConsumption().intValue());
        Assert.assertEquals(40, average.getProfile().getPotentialFlexProduction().intValue());
        // assert forecast values
        Assert.assertEquals(16, average.getForecast().getUncontrolledLoad().intValue());
        Assert.assertEquals(32, average.getForecast().getAverageConsumption().intValue());
        Assert.assertEquals(48, average.getForecast().getAverageProduction().intValue());
        Assert.assertEquals(64, average.getForecast().getPotentialFlexConsumption().intValue());
        Assert.assertEquals(80, average.getForecast().getPotentialFlexProduction().intValue());
        // assert observerd values
        Assert.assertEquals(24, average.getObserved().getUncontrolledLoad().intValue());
        Assert.assertEquals(48, average.getObserved().getAverageConsumption().intValue());
        Assert.assertEquals(72, average.getObserved().getAverageProduction().intValue());
        Assert.assertEquals(96, average.getObserved().getPotentialFlexConsumption().intValue());
        Assert.assertEquals(120, average.getObserved().getPotentialFlexProduction().intValue());
    }

    @Test
    public void testAveragePowerData() {
        ForecastPowerDataDto powerData1 = buildForecastPowerDataDto(BigInteger.valueOf(10));
        ForecastPowerDataDto powerData2 = buildForecastPowerDataDto(BigInteger.valueOf(8));
        ForecastPowerDataDto powerData3 = buildForecastPowerDataDto(BigInteger.valueOf(6));
        ForecastPowerDataDto average = PowerContainerDtoUtil.average(powerData1, powerData2, powerData3);
        Assert.assertNotNull(average);
        Assert.assertEquals(8, average.getUncontrolledLoad().intValue()); // (10 + 8 + 6)/3
        Assert.assertEquals(16, average.getAverageConsumption().intValue());
        Assert.assertEquals(24, average.getAverageProduction().intValue());
        Assert.assertEquals(32, average.getPotentialFlexConsumption().intValue());
        Assert.assertEquals(40, average.getPotentialFlexProduction().intValue());
        Assert.assertEquals(48, average.getAllocatedFlexConsumption().intValue());
        Assert.assertEquals(56, average.getAllocatedFlexProduction().intValue());
    }

    private PowerContainerDto buildPowerContainerDto(BigInteger value) {
        PowerContainerDto powerContainerDto = new PowerContainerDto(new LocalDate(), PTU_INDEX);

        powerContainerDto.setProfile(buildPowerDataDto(value.multiply(BigInteger.valueOf(1))));
        powerContainerDto.setForecast(buildForecastPowerDataDto(value.multiply(BigInteger.valueOf(2))));
        powerContainerDto.setObserved(buildPowerDataDto(value.multiply(BigInteger.valueOf(3))));

        return powerContainerDto;
    }

    private PowerDataDto buildPowerDataDto(BigInteger value) {
        PowerDataDto powerDataDto = new PowerDataDto();

        powerDataDto.setUncontrolledLoad(value.multiply(BigInteger.valueOf(1)));
        powerDataDto.setAverageConsumption(value.multiply(BigInteger.valueOf(2)));
        powerDataDto.setAverageProduction(value.multiply(BigInteger.valueOf(3)));
        powerDataDto.setPotentialFlexConsumption(value.multiply(BigInteger.valueOf(4)));
        powerDataDto.setPotentialFlexProduction(value.multiply(BigInteger.valueOf(5)));

        return powerDataDto;
    }

    private ForecastPowerDataDto buildForecastPowerDataDto(BigInteger value) {
        ForecastPowerDataDto powerDataDto = new ForecastPowerDataDto();

        powerDataDto.setUncontrolledLoad(value.multiply(BigInteger.valueOf(1)));
        powerDataDto.setAverageConsumption(value.multiply(BigInteger.valueOf(2)));
        powerDataDto.setAverageProduction(value.multiply(BigInteger.valueOf(3)));
        powerDataDto.setPotentialFlexConsumption(value.multiply(BigInteger.valueOf(4)));
        powerDataDto.setPotentialFlexProduction(value.multiply(BigInteger.valueOf(5)));
        powerDataDto.setAllocatedFlexConsumption(value.multiply(BigInteger.valueOf(6)));
        powerDataDto.setAllocatedFlexProduction(value.multiply(BigInteger.valueOf(7)));

        return powerDataDto;
    }
}
