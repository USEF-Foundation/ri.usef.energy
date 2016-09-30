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

package energy.usef.dso.pbcfeederimpl;

import energy.usef.pbcfeeder.PbcFeederClient;
import energy.usef.pbcfeeder.dto.PbcPowerLimitsDto;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the entry point for all data that is being 'fed' to the DSO PBCs.
 */
public class PbcFeederService {

    private static final int CP_ONE = 1;
    private static final int CP_TWO = 2;
    private static final int CP_THREE = 3;

    private static final int MAX_CP = 3;
    private static final int MIN_CP = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(PbcFeederService.class);

    @Inject
    private PbcFeederClient pbcFeederClient;

    /**
     * Return a Map with ptuIndex mapped to uncontrolled load per ptu.
     *
     * @param congestionPoint {@link String} the usef identifier of the congestion point.
     * @param date            {@link LocalDate} period for which the uncontrolled load is requested.
     * @param startPtuIndex   {@link int} the index of the ptu starting the requested period.
     * @param amountOfPtus    {@link int} amount of ptus totally requested.
     * @return {@link Map} &lt;ptuIndex ({@link Integer}), unControlledLoad ({@link BigDecimal})&gt;.
     */
    public Map<Integer, BigDecimal> getUncontrolledLoadPerPtu(String congestionPoint, LocalDate date, int startPtuIndex,
            int amountOfPtus) {
        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(date, startPtuIndex, amountOfPtus);
        int congestionPointId = createCongestionPointIdBasedOnHash(congestionPoint);

        return pbcStubDataDtoList.stream().collect(Collectors.toMap(pbcStubData -> pbcStubData.getPtuContainer().getPtuIndex(),
                pbcStubData -> BigDecimal.valueOf(getRawUncontrolledLoadForCongestionPoint(congestionPointId, pbcStubData))));
    }

    /**
     * Return a Map with ptuIndex mapped to pvLoadForecast per ptu.
     *
     * @param date          {@link LocalDate} period for which the pvLoadForecast is requested.
     * @param startPtuIndex {@link int} the index of the ptu starting the requested period.
     * @param amountOfPtus  {@link int} amount of ptus totally requested.
     * @return {@link Map} &lt;ptuIndex ({@link Integer}), unControlledLoad ({@link BigDecimal})&gt;.
     */
    public Map<Integer, BigDecimal> getPvLoadForecastPerPtu(LocalDate date, int startPtuIndex, int amountOfPtus) {
        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(date, startPtuIndex, amountOfPtus);

        return pbcStubDataDtoList.stream().collect(Collectors.toMap(pbcStubData -> pbcStubData.getPtuContainer().getPtuIndex(),
                pbcStubData -> BigDecimal.valueOf(pbcStubData.getPvLoadForecast())));
    }

    /**
     * Retrieves the uncontrolled load for a given congestion point on specified period for a given duration.
     *
     * @param congestionPointEntityAddress {@link String} usef identifier of the congestion point.
     * @param date                         {@link LocalDate} period for which uncontrolled load is desired.
     * @param startPtuIndex                index of the ptu starting the duration (1, 2, ...)
     * @param amountOfPtus                 amount of ptus of the duration (> 0)
     * @return the uncontrolled load.
     */
    public Integer getUncontrolledLoad(String congestionPointEntityAddress, LocalDate date, int startPtuIndex, int amountOfPtus) {
        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(date, startPtuIndex, amountOfPtus);
        int congestionPointId = createCongestionPointIdBasedOnHash(congestionPointEntityAddress);
        double uncontrolledLoad = pbcStubDataDtoList.stream()
                .mapToDouble(pbcStubData -> getRawUncontrolledLoadForCongestionPoint(congestionPointId, pbcStubData)).sum();
        return (int) Math.round(uncontrolledLoad);
    }

    /**
     * Gets the power limits for the given congestion point.
     *
     * @param congestionPointEntityAddress {@link String} entity address of the congestion point.
     * @return a {@link PbcPowerLimitsDto} wrapping the lower and upper limits for a congestion point.
     */
    public PbcPowerLimitsDto getCongestionPointPowerLimits(String congestionPointEntityAddress) {
        Integer congestionPointId = createCongestionPointIdBasedOnHash(congestionPointEntityAddress);
        List<BigDecimal> powerLimits = pbcFeederClient.getCongestionPointPowerLimits(congestionPointId);
        LOGGER.debug("Found power limits for congestion point [{}] (id={}): {}", congestionPointEntityAddress, congestionPointId,
                powerLimits.stream().map(BigDecimal::toString).collect(Collectors.joining(", ")));
        return new PbcPowerLimitsDto(powerLimits.get(0), powerLimits.get(1));
    }

    private double getRawUncontrolledLoadForCongestionPoint(int congestionPointId, PbcStubDataDto pbcStubData) {
        double value;
        switch (congestionPointId) {
        case CP_ONE:
            value = pbcStubData.getCongestionPointOne();
            break;
        case CP_TWO:
            value = pbcStubData.getCongestionPointTwo();
            break;
        case CP_THREE:
            value = pbcStubData.getCongestionPointThree();
            break;
        default:
            value = pbcStubData.getCongestionPointAvg();
            break;
        }
        return value;
    }

    /**
     * Transform congestionPoint String into CongestionPoint nr consistently pseudo-randomly.
     *
     * @param congestionPoint
     * @return
     */
    private int createCongestionPointIdBasedOnHash(String congestionPoint) {
        Random random = new Random(congestionPoint.hashCode());
        return random.nextInt((MAX_CP - MIN_CP) + 1) + MIN_CP;
    }

    /**
     * This method sets the uncontrolled load on the SynchronisationConnectionDto. This method can return data for multiple dates, so the ptu
     * index in the map is not a ptu index for one single day!
     *
     * @param date
     * @param startPtuIndex
     * @param amountOfPtus
     * @return
     */
    public Map<Integer, BigDecimal> retrieveApxPrices(LocalDate date, int startPtuIndex, int amountOfPtus) {
        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(date, startPtuIndex, amountOfPtus);

        Map<Integer, BigDecimal> apxPrices = new HashMap<>();

        // if this method is used, the ptu index will be increased every ptu over multiple days
        for (int i = 0; i < pbcStubDataDtoList.size(); i++) {
            apxPrices.put(i + 1, BigDecimal.valueOf(pbcStubDataDtoList.get(i).getApx()));
        }

        return apxPrices;
    }
}
