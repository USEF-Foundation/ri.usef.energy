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
import energy.usef.core.util.BigIntegerUtil;
import energy.usef.core.util.PtuUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

/**
 * Util class for calculations on {@link PowerContainerDto} objects.
 */
public class PowerContainerDtoUtil {

    private PowerContainerDtoUtil() {
    }

    /**
     * Returns a {@link PowerContainerDto} containing the sum of all {@link PowerContainerDto}'s in input powerContainerDtos.
     *
     * @param powerContainerDtos
     * @return
     */
    public static PowerContainerDto sum(List<PowerContainerDto> powerContainerDtos) {
        if (powerContainerDtos == null || powerContainerDtos.isEmpty()) {
            return null;
        }

        LocalDate period = powerContainerDtos.get(0).getPeriod();
        int ptuIndex = powerContainerDtos.get(0).getTimeIndex();

        PowerContainerDto summedPowerContainer = new PowerContainerDto(period, ptuIndex);
        for (PowerContainerDto powerContainerDto : powerContainerDtos) {
            if (powerContainerDto.getPeriod().equals(period) && powerContainerDto.getTimeIndex().equals(ptuIndex)) {
                summedPowerContainer = sum(summedPowerContainer, powerContainerDto);
            }
        }

        return summedPowerContainer;
    }

    /**
     * This helper method sums all udis correctly.
     *
     * @param udis
     * @param ptuDuration
     * @param numberOfPtus
     * @return a map with ptu index mapped to powerContainerDtos
     */
    public static Map<Integer, PowerContainerDto> sumUdisPerPtu(List<UdiPortfolioDto> udis, Integer ptuDuration,
            Integer numberOfPtus) {
        Map<Integer, PowerContainerDto> summedPowerMap = new HashMap<>();
        for (UdiPortfolioDto udiPortfolioDto : udis) {
            Map<Integer, PowerContainerDto> dtuMap = udiPortfolioDto.getUdiPowerPerDTU();

            int dtuSize = udiPortfolioDto.getDtuSize();
            int dtusPerPtu = ptuDuration / dtuSize;

            for (int ptuIndex = 1; ptuIndex <= numberOfPtus; ptuIndex++) {
                int startDtu = 1 + ((ptuIndex - 1) * dtusPerPtu);

                //collect data for this ptu.
                PowerContainerDto[] collectedPowerContainers = new PowerContainerDto[dtusPerPtu];
                for (int i = 0; i < dtusPerPtu; i++) {
                    collectedPowerContainers[i] = dtuMap.get(startDtu + i);
                }

                PowerContainerDto averagedPowerContainer = average(ptuIndex, collectedPowerContainers);
                //sum with other udis
                if (summedPowerMap.containsKey(ptuIndex)) {
                    summedPowerMap.put(ptuIndex, sum(averagedPowerContainer, summedPowerMap.get(ptuIndex)));
                } else {
                    summedPowerMap.put(ptuIndex, averagedPowerContainer);
                }
            }
        }
        return summedPowerMap;
    }

    /**
     * Sums to powerContainers and puts the summed result into the first argument.
     *
     * @param powerContainer1
     * @param powerContainer2
     * @return {@link PowerContainerDto} containing the summed power containers
     */
    public static PowerContainerDto sum(PowerContainerDto powerContainer1, PowerContainerDto powerContainer2) {
        powerContainer1.setProfile(sum(powerContainer1.getProfile(), powerContainer2.getProfile()));
        powerContainer1.setForecast(sum(powerContainer1.getForecast(), powerContainer2.getForecast()));
        powerContainer1.setObserved(sum(powerContainer1.getObserved(), powerContainer2.getObserved()));

        return powerContainer1;
    }

    private static <T extends PowerDataDto> T sum(T powerData1, T powerData2) {
        if (powerData1 == null) {
            return powerData2;
        }
        if (powerData2 == null) {
            return powerData1;
        }

        PowerDataDto powerData = new PowerDataDto();

        if (powerData1 instanceof ForecastPowerDataDto && powerData2 instanceof ForecastPowerDataDto) {
            ForecastPowerDataDto forecastPowerDataDto1 = (ForecastPowerDataDto) powerData1;
            ForecastPowerDataDto forecastPowerDataDto2 = (ForecastPowerDataDto) powerData2;

            ForecastPowerDataDto forecastPowerDataDto = new ForecastPowerDataDto();
            forecastPowerDataDto.setAllocatedFlexConsumption(
                    BigIntegerUtil.sum(forecastPowerDataDto1.getAllocatedFlexConsumption(), forecastPowerDataDto2.getAllocatedFlexConsumption()));
            forecastPowerDataDto.setAllocatedFlexProduction(
                    BigIntegerUtil.sum(forecastPowerDataDto1.getAllocatedFlexProduction(), forecastPowerDataDto2.getAllocatedFlexProduction()));

            powerData = forecastPowerDataDto;
        }

        powerData.setUncontrolledLoad(BigIntegerUtil.sum(powerData1.getUncontrolledLoad(), powerData2.getUncontrolledLoad()));
        powerData.setAverageConsumption(BigIntegerUtil.sum(powerData1.getAverageConsumption(), powerData2.getAverageConsumption()));
        powerData.setAverageProduction(BigIntegerUtil.sum(powerData1.getAverageProduction(), powerData2.getAverageProduction()));
        powerData.setPotentialFlexConsumption(
                BigIntegerUtil.sum(powerData1.getPotentialFlexConsumption(), powerData2.getPotentialFlexConsumption()));
        powerData.setPotentialFlexProduction(BigIntegerUtil.sum(powerData1.getPotentialFlexProduction(), powerData2.getPotentialFlexProduction()));

        return (T) powerData;
    }

    /**
     * Computes the average power per ptu for each PowerContainerDto of a Udi.
     *
     * @param udiPortfolioDto {@link UdiPortfolioDto} the UDI.
     * @param period          {@link LocalDate} period for which the average is computed (needed to know the number of ptus per day).
     * @param ptuDuration     {@link Integer} duration of a ptu (needed to know the number of ptus per day).
     * @return a {@link Map} of {@link PowerContainerDto} per ptu index.
     */
    public static Map<Integer, PowerContainerDto> average(UdiPortfolioDto udiPortfolioDto, LocalDate period, Integer ptuDuration) {
        if (udiPortfolioDto == null || ptuDuration == null || udiPortfolioDto.getUdiPowerPerDTU().isEmpty()) {
            return null;
        }
        Integer dtusPerPtu = ptuDuration / udiPortfolioDto.getDtuSize();
        Integer ptusPerDay = PtuUtil.getNumberOfPtusPerDay(period, ptuDuration);
        Map<Integer, PowerContainerDto> powerContainerDtoPerPtu = new HashMap<>();
        for (int ptuIndex = 1; ptuIndex <= ptusPerDay; ++ptuIndex) {
            int startDtu = 1 + ((ptuIndex - 1) * dtusPerPtu);
            PowerContainerDto[] collectedPowerContainerDtos = new PowerContainerDto[dtusPerPtu];
            for (int i = 0; i < dtusPerPtu; ++i) {
                collectedPowerContainerDtos[i] = udiPortfolioDto.getUdiPowerPerDTU().get(startDtu + i);
            }
            powerContainerDtoPerPtu.put(ptuIndex, average(ptuIndex, collectedPowerContainerDtos));
        }
        return powerContainerDtoPerPtu;
    }

    public static PowerContainerDto average(int ptuIndex, PowerContainerDto... containers) {
        List<PowerDataDto> profiles = new ArrayList<>();
        List<PowerDataDto> forecasts = new ArrayList<>();
        List<PowerDataDto> observed = new ArrayList<>();
        for (PowerContainerDto container : containers) {
            if (container == null) {
                continue;
            }
            profiles.add(container.getProfile());
            forecasts.add(container.getForecast());
            observed.add(container.getObserved());
        }

        PowerContainerDto averagePowerContainer = new PowerContainerDto(containers[0].getPeriod(), ptuIndex);
        averagePowerContainer.setProfile(average(profiles.toArray(new PowerDataDto[profiles.size()])));
        averagePowerContainer.setForecast(average(forecasts.toArray(new ForecastPowerDataDto[forecasts.size()])));
        averagePowerContainer.setObserved(average(observed.toArray(new PowerDataDto[observed.size()])));

        return averagePowerContainer;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends PowerDataDto> T average(T... powerData) {
        BigInteger[] uncontrolledLoad = new BigInteger[powerData.length];
        BigInteger[] averageConsumption = new BigInteger[powerData.length];
        BigInteger[] averageProduction = new BigInteger[powerData.length];
        BigInteger[] potentialFlexConsumption = new BigInteger[powerData.length];
        BigInteger[] potentialFlexProduction = new BigInteger[powerData.length];
        BigInteger[] allocatedFlexConsumption = new BigInteger[powerData.length];
        BigInteger[] allocatedFlexProduction = new BigInteger[powerData.length];
        boolean actual = powerData instanceof ForecastPowerDataDto[];
        for (int i = 0; i < powerData.length; i++) {
            if (powerData[i] == null) {
                continue;
            }
            uncontrolledLoad[i] = powerData[i].getUncontrolledLoad();
            averageConsumption[i] = powerData[i].getAverageConsumption();
            averageProduction[i] = powerData[i].getAverageProduction();
            potentialFlexConsumption[i] = powerData[i].getPotentialFlexConsumption();
            potentialFlexProduction[i] = powerData[i].getPotentialFlexProduction();
            if (actual) {
                allocatedFlexConsumption[i] = ((ForecastPowerDataDto) powerData[i]).getAllocatedFlexConsumption();
                allocatedFlexProduction[i] = ((ForecastPowerDataDto) powerData[i]).getAllocatedFlexProduction();
            }
        }

        PowerDataDto averagedPowerData = new PowerDataDto();
        if (actual) {
            ForecastPowerDataDto averagedForecastPowerData = new ForecastPowerDataDto();
            averagedForecastPowerData.setAllocatedFlexConsumption(BigIntegerUtil.average(allocatedFlexConsumption));
            averagedForecastPowerData.setAllocatedFlexProduction(BigIntegerUtil.average(allocatedFlexProduction));
            averagedPowerData = averagedForecastPowerData;
        }

        averagedPowerData.setUncontrolledLoad(BigIntegerUtil.average(uncontrolledLoad));
        averagedPowerData.setAverageConsumption(BigIntegerUtil.average(averageConsumption));
        averagedPowerData.setAverageProduction(BigIntegerUtil.average(averageProduction));
        averagedPowerData.setPotentialFlexConsumption(BigIntegerUtil.average(potentialFlexConsumption));
        averagedPowerData.setPotentialFlexProduction(BigIntegerUtil.average(potentialFlexProduction));

        return (T) averagedPowerData;
    }

}
