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

import energy.usef.agr.model.ForecastPowerData;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.model.PowerData;
import energy.usef.agr.model.Udi;
import energy.usef.core.util.BigIntegerUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PowerContainer helper class.
 */
public class PowerContainerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PowerContainerUtil.class);

    /*
     * Hide implicit public constructor.
     */
    private PowerContainerUtil() {
    }

    /**
     * This helper method sums all udis correctly.
     *
     * @param udis
     * @param udiPowerContainers
     *@param ptuDuration
     * @param numberOfPtus   @return
     */
    public static Map<Integer, PowerContainer> sumUdisPerPtu(List<Udi> udis, Map<Udi, List<PowerContainer>> udiPowerContainers,
            Integer ptuDuration,
            Integer numberOfPtus) {
        Map<Integer, PowerContainer> summedPowerMap = new HashMap<>();
        for (Udi udi : udis) {
            if(!udiPowerContainers.containsKey(udi)) {
                LOGGER.warn("No powerContainers for active UDI: {}", udi.getEndpoint());
            }
            Map<Integer, PowerContainer> dtuMap = udiPowerContainers.get(udi).stream()
                    .collect(Collectors.toMap(PowerContainer::getTimeIndex, Function.identity()));

            int dtuSize = udi.getDtuSize();
            int dtusPerPtu = ptuDuration / dtuSize;

            for (int ptuIndex = 1; ptuIndex <= numberOfPtus; ptuIndex++) {
                int startDtu = 1 + ((ptuIndex - 1) * dtusPerPtu);

                //collect data for this ptu.
                PowerContainer[] collectedPowerContainers = new PowerContainer[dtusPerPtu];
                for (int i = 0; i < dtusPerPtu; i++) {
                    collectedPowerContainers[i] = dtuMap.get(startDtu + i);
                }
                PowerContainer averagedPowerContainer = average(ptuIndex, collectedPowerContainers);
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
     * Sums different PowerContainer and sets the time index to the same value as the time index of powerContainer1.
     *
     * @param powerContainer1
     * @param powerContainer2
     * @return
     */
    public static PowerContainer sum(PowerContainer powerContainer1, PowerContainer powerContainer2) {
        PowerContainer powerContainer = new PowerContainer();
        powerContainer.setTimeIndex(powerContainer1.getTimeIndex());
        powerContainer.setProfile(sum(powerContainer1.getProfile(), powerContainer2.getProfile()));
        powerContainer.setForecast(sum(powerContainer1.getForecast(), powerContainer2.getForecast()));
        powerContainer.setObserved(sum(powerContainer1.getObserved(), powerContainer2.getObserved()));
        return powerContainer;
    }

    private static <T extends PowerData> T sum(T powerData1, T powerData2) {
        if (powerData1 == null && powerData2 == null) {
            return null;
        }
        if (powerData1 == null) {
            return powerData2;
        }
        if (powerData2 == null) {
            return powerData1;
        }

        PowerData powerData = new PowerData();

        if (powerData1 instanceof ForecastPowerData && powerData2 instanceof ForecastPowerData) {
            ForecastPowerData forecastPowerData1 = (ForecastPowerData) powerData1;
            ForecastPowerData forecastPowerData2 = (ForecastPowerData) powerData2;

            ForecastPowerData forecastPowerData = new ForecastPowerData();
            forecastPowerData.setAllocatedFlexConsumption(
                    BigIntegerUtil.sum(forecastPowerData1.getAllocatedFlexConsumption(), forecastPowerData2.getAllocatedFlexConsumption()));
            forecastPowerData.setAllocatedFlexProduction(
                    BigIntegerUtil.sum(forecastPowerData1.getAllocatedFlexProduction(), forecastPowerData2.getAllocatedFlexProduction()));

            powerData = forecastPowerData;
        }

        powerData.setUncontrolledLoad(BigIntegerUtil.sum(powerData1.getUncontrolledLoad(), powerData2.getUncontrolledLoad()));
        powerData.setAverageConsumption(BigIntegerUtil.sum(powerData1.getAverageConsumption(), powerData2.getAverageConsumption()));
        powerData.setAverageProduction(BigIntegerUtil.sum(powerData1.getAverageProduction(), powerData2.getAverageProduction()));
        powerData.setPotentialFlexConsumption(
                BigIntegerUtil.sum(powerData1.getPotentialFlexConsumption(), powerData2.getPotentialFlexConsumption()));
        powerData.setPotentialFlexProduction(
                BigIntegerUtil.sum(powerData1.getPotentialFlexProduction(), powerData2.getPotentialFlexProduction()));

        return (T) powerData;
    }

    private static PowerContainer average(int ptuIndex, PowerContainer... containers) {
        List<PowerData> profiles = new ArrayList<>();
        List<PowerData> forecasts = new ArrayList<>();
        List<PowerData> observed = new ArrayList<>();
        for (PowerContainer container : containers) {
            if (container == null) {
                continue;
            }
            profiles.add(container.getProfile());
            forecasts.add(container.getForecast());
            observed.add(container.getObserved());
        }

        PowerContainer averagePowerContainer = new PowerContainer();
        averagePowerContainer.setTimeIndex(ptuIndex);
        averagePowerContainer.setProfile(average(profiles.toArray(new PowerData[profiles.size()])));
        averagePowerContainer.setForecast(average(forecasts.toArray(new ForecastPowerData[forecasts.size()])));
        averagePowerContainer.setObserved(average(observed.toArray(new PowerData[observed.size()])));
        return averagePowerContainer;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private static <T extends PowerData> T average(T... powerData) {
        BigInteger[] uncontrolledLoad = new BigInteger[powerData.length];
        BigInteger[] averageConsumption = new BigInteger[powerData.length];
        BigInteger[] averageProduction = new BigInteger[powerData.length];
        BigInteger[] potentialFlexConsumption = new BigInteger[powerData.length];
        BigInteger[] potentialFlexProduction = new BigInteger[powerData.length];
        BigInteger[] allocatedFlexConsumption = new BigInteger[powerData.length];
        BigInteger[] allocatedFlexProduction = new BigInteger[powerData.length];
        boolean actual = powerData instanceof ForecastPowerData[];
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
                allocatedFlexConsumption[i] = ((ForecastPowerData) powerData[i]).getAllocatedFlexConsumption();
                allocatedFlexProduction[i] = ((ForecastPowerData) powerData[i]).getAllocatedFlexProduction();
            }
        }
        PowerData averagedPowerData = new PowerData();
        if (actual) {
            ForecastPowerData actualAveragedPowerData = new ForecastPowerData();
            actualAveragedPowerData.setAllocatedFlexConsumption(BigIntegerUtil.average(allocatedFlexConsumption));
            actualAveragedPowerData.setAllocatedFlexProduction(BigIntegerUtil.average(allocatedFlexProduction));
            averagedPowerData = actualAveragedPowerData;
        }
        averagedPowerData.setUncontrolledLoad(BigIntegerUtil.average(uncontrolledLoad));
        averagedPowerData.setAverageConsumption(BigIntegerUtil.average(averageConsumption));
        averagedPowerData.setAverageProduction(BigIntegerUtil.average(averageProduction));
        averagedPowerData.setPotentialFlexConsumption(BigIntegerUtil.average(potentialFlexConsumption));
        averagedPowerData.setPotentialFlexProduction(BigIntegerUtil.average(potentialFlexProduction));
        return (T) averagedPowerData;
    }
}
