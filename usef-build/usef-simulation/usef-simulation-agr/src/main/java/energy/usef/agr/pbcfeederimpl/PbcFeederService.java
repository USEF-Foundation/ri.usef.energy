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

package energy.usef.agr.pbcfeederimpl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.dto.ElementDtuDataDto;
import energy.usef.agr.dto.ElementTypeDto;
import energy.usef.agr.dto.ForecastPowerDataDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.PowerDataDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.pbcfeeder.PbcFeederClient;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;

/**
 * This class is the entry point for all data that is being 'fed' to the Aggregator PBCs.
 */
public class PbcFeederService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbcFeederService.class);
    private static final int MIN_PERCONNECTION_PERC = 80;
    private static final int MAX_PERCONNECTION_PERC = 120;
    private static final int ONE_HUNDRED = 100;
    private static final int MAX_PERPTU_PERC = 110;
    private static final int MIN_PERPTU_PERC = 90;

    private static final int MOD_PROBOF_5PERC = 20;
    private static final int MAGIC_DEVICE_MIN = 500;
    private static final int MAGIC_DEVICE_MAX = 2000;

    private static final String ELEMENT_ADS_PROFILE_PREFIX = "ADS";
    private static final String ELEMENT_MANAGED_DEVICE_ADS_SUFFIX = ELEMENT_ADS_PROFILE_PREFIX + "1";

    private static final String ELEMENT_PV_PROFILE_PREFIX = "PV";
    private static final String ELEMENT_MANAGED_DEVICE_PV_SUFFIX = ELEMENT_PV_PROFILE_PREFIX + "1";

    private static final String ELEMENT_SYNTHETIC_DATA_PROFILE_PREFIX = "UCL";
    private static final String ELEMENT_SYNTHETIC_DATA_SUFFIX = ELEMENT_SYNTHETIC_DATA_PROFILE_PREFIX + "1";

    @Inject
    private PbcFeederClient pbcFeederClient;

    /**
     * This method sets the uncontrolled load on the ConnectionDto.
     *
     * @param date
     * @param startPtuIndex
     * @param amountOfPtus
     * @param connections   : A {@link List} of {@link ConnectionPortfolioDto}s.
     * @return
     */
    public List<ConnectionPortfolioDto> updateConnectionUncontrolledLoadForecast(LocalDate date, int startPtuIndex,
            int amountOfPtus,
            List<ConnectionPortfolioDto> connections) {
        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(date, startPtuIndex, amountOfPtus);

        for (ConnectionPortfolioDto connectionDto : connections) {
            pbcStubDataDtoList.stream()
                    .map(pbcStubData -> {
                        PowerContainerDto powerContainer = new PowerContainerDto(date,
                                pbcStubData.getPtuContainer().getPtuIndex());
                        double rawUncontrolledLoad = pbcStubData.getCongestionPointAvg();
                        double uncontrolledLoad = pseudoRandomizeRawUncontrolledLoad(rawUncontrolledLoad, connectionDto,
                                pbcStubData);
                        powerContainer.getForecast().setUncontrolledLoad(BigInteger.valueOf(Math.round(uncontrolledLoad)));
                        return powerContainer;
                    })
                    .forEach(powerContainerDto -> connectionDto.getConnectionPowerPerPTU()
                            .put(powerContainerDto.getTimeIndex(), powerContainerDto));
        }
        return connections;
    }

    private double pseudoRandomizeRawUncontrolledLoad(double rawUncontrolledLoad, ConnectionPortfolioDto connectionDto,
            PbcStubDataDto pbcStubData) {

        // * Each connection has a scaling factor for the Baseline that is valid for all PTUs. This factor will be between 80% and
        // 120%.
        Random connectionBasedRandom = new Random(connectionDto.getConnectionEntityAddress().hashCode());
        int multiplierPerConnection =
                connectionBasedRandom.nextInt((MAX_PERCONNECTION_PERC - MIN_PERCONNECTION_PERC) + 1) + MIN_PERCONNECTION_PERC;
        double connectionRandomizedUncontrolledLoad = (rawUncontrolledLoad * multiplierPerConnection) / ONE_HUNDRED;

        // * Random factor on top of the baseline to be generated every PTU between -10% and 10% of the scaling factor
        Random ptuBasedRandom = new Random(pbcStubData.getIndex());
        int multiplierPerPtu = ptuBasedRandom.nextInt(MAX_PERPTU_PERC - MIN_PERPTU_PERC) + MIN_PERPTU_PERC;
        double ptuCalculatedUncontrolledLoad = (connectionRandomizedUncontrolledLoad * multiplierPerPtu) / ONE_HUNDRED;

        // * A random peak factor with a probability of 5% of a load random between 500W and 2000W.
        double uncontrolledLoad;
        if (pbcStubData.getIndex() % MOD_PROBOF_5PERC == 0) {
            Random peakRandom = new Random(pbcStubData.getIndex());
            uncontrolledLoad =
                    ptuCalculatedUncontrolledLoad + peakRandom.nextInt(MAGIC_DEVICE_MAX - MAGIC_DEVICE_MIN) - MAGIC_DEVICE_MIN;
            LOGGER.trace("Random peak factor occurred [{}]", uncontrolledLoad - ptuCalculatedUncontrolledLoad);
        } else {
            uncontrolledLoad = ptuCalculatedUncontrolledLoad;
        }
        LOGGER.trace("Randomized uncontrolled load [{}] (original [{}]) for connection [{}]", uncontrolledLoad,
                rawUncontrolledLoad,
                connectionDto.getConnectionEntityAddress());
        return uncontrolledLoad;
    }

    /**
     * This method creates and adds a UDI to every connection. The UDI is a PV, with the PVForecast values from the
     * stubinputdatasheet.
     *
     * @param date
     * @param connections
     * @param ptuDuration
     * @param dtuDuration
     * @return connectionDtos with UDI for Photo-voltaic forecast.
     */
    public List<ConnectionPortfolioDto> retrieveUDIListWithPvLoadForecast(LocalDate date,
            List<ConnectionPortfolioDto> connections, int ptuDuration, int dtuDuration) {
        int ptusPerDay = PtuUtil.getNumberOfPtusPerDay(date, ptuDuration);
        int startPtuIndex = 1;

        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(date, startPtuIndex, ptusPerDay);

        for (ConnectionPortfolioDto connectionDto : connections) {
            // build UDI for PV
            UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto(
                    "[" + connectionDto.getConnectionEntityAddress().hashCode() + "]:9541", dtuDuration, ELEMENT_PV_PROFILE_PREFIX);

            int dtusPerPtu = ptuDuration / dtuDuration;

            pbcStubDataDtoList.stream().forEach(pbcStubData -> {
                int ptuIndex = pbcStubData.getPtuContainer().getPtuIndex();
                int startDtu = (ptuIndex - 1) * dtusPerPtu + 1; // inclusive
                int endDtu = startDtu + dtusPerPtu; // exclusive
                for (int dtuIndex = startDtu; dtuIndex < endDtu; dtuIndex++) {
                    PowerContainerDto powerContainer = new PowerContainerDto(date, dtuIndex);
                    updatePowerValue(powerContainer.getForecast(), pbcStubData.getPvLoadForecast());
                    udiPortfolioDto.getUdiPowerPerDTU().put(dtuIndex, powerContainer);
                }
            });
            connectionDto.getUdis().add(udiPortfolioDto);
        }
        return connections;
    }

    /**
     * This method creates and adds two UDI's to every connection. The UDI is a PV, with the PVForecast values from the
     * stubinputdatasheet.
     *
     * @param date
     * @param connections
     * @param ptuDuration
     * @return connectionDtos with UDI's for Photo-voltaic forecast.
     */
    public List<ConnectionPortfolioDto> retrieveUDIListWithPvLoadAveragePower(LocalDate date,
            List<ConnectionPortfolioDto> connections, int ptuDuration) {
        //minus one because we need the previous one as well.
        int startPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(), ptuDuration) - 1;
        int ptusPerDay = PtuUtil.getNumberOfPtusPerDay(date, ptuDuration);
        LocalDate startDate = date;
        if (startPtuIndex <= 0) {
            startDate = startDate.minusDays(1);
            startPtuIndex = PtuUtil.getNumberOfPtusPerDay(startDate, ptuDuration);
        }

        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(date, 1, ptusPerDay);
        final int finalStartPtuIndex = startPtuIndex;
        for (ConnectionPortfolioDto connectionDto : connections) {
            //
            connectionDto.getUdis().stream().filter(udiDto -> udiDto.getEndpoint() != null).forEach(
                    udiDto -> fillDtusForUdi(udiDto, pbcStubDataDtoList, ptuDuration, udiDto.getDtuSize(), finalStartPtuIndex));
        }
        return connections;
    }

    /**
     * Fills a connection portfolio with forecasts based on the pbc spreadsheet data and ignoring the udi's.
     *
     * @param forecastDay the period ({@link LocalDate})
     * @param connections the connection portfolio
     * @param ptuDuration the number of minutes for one ptu
     * @return {@link List} of {@link ConnectionPortfolioDto}'s.
     */
    public List<ConnectionPortfolioDto> retrieveNonUDIListWithForecast(LocalDate forecastDay,
            List<ConnectionPortfolioDto> connections,
            int ptuDuration) {
        int ptusPerDay = PtuUtil.getNumberOfPtusPerDay(forecastDay, ptuDuration);
        int startPtuIndex = 1;

        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(forecastDay, startPtuIndex, ptusPerDay);

        for (ConnectionPortfolioDto connectionDto : connections) {

            BigInteger adsLoadConstant = fetchADSLoadConstant();

            pbcStubDataDtoList.stream().forEach(pbcStubData -> {
                int ptuIndex = pbcStubData.getPtuContainer().getPtuIndex();
                PowerContainerDto powerContainerDto = connectionDto.getConnectionPowerPerPTU().get(ptuIndex);
                Double value = pbcStubData.getPvLoadForecast();
                ForecastPowerDataDto forecast = powerContainerDto.getForecast();
                BigInteger power = BigInteger.valueOf((int) Math.round(value));
                power.add(adsLoadConstant);

                updatePowerValue(forecast, power);
                forecast.setPotentialFlexConsumption(forecast.getAverageConsumption().negate());
                forecast.setPotentialFlexProduction(forecast.getAverageProduction().negate());
            });
        }
        return connections;
    }

    /**
     * Retrieves the APX prices from the data input sheet for a given period and a given duration.
     *
     * @param date          {@link LocalDate} period.
     * @param startPtuIndex {@link Integer} starting ptu index of the duration (must be greater than 0).
     * @param amountOfPtus  {@link Integer} amount of ptus of the duration.
     * @return a {@link Map} of ptu index as key and APX price as value.
     */
    public Map<Integer, BigDecimal> retrieveApxPrices(LocalDate date, int startPtuIndex, int amountOfPtus) {
        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(date, startPtuIndex, amountOfPtus);

        Function<PbcStubDataDto, Integer> getPtuIndex = data -> data.getPtuContainer().getPtuIndex();
        Function<PbcStubDataDto, BigDecimal> getApx = data -> new BigDecimal(data.getApx());
        return pbcStubDataDtoList.stream().collect(Collectors.toMap(getPtuIndex::apply, getApx::apply));
    }

    private void fillDtusForUdi(UdiPortfolioDto pvUdiDto, List<PbcStubDataDto> pbcStubDataDtoList, int ptuDuration, int dtuDuration,
            int startPtuIndex) {
        LOGGER.trace("Filling DTUs actual power for the UDI [{}] (size of pbc stub data = {}).", pvUdiDto.getEndpoint(),
                pbcStubDataDtoList.size());
        pbcStubDataDtoList.stream().forEach(
                pbcStubData -> setObservedAndForecast(pbcStubData, ptuDuration, dtuDuration, pvUdiDto, startPtuIndex));
    }

    private void setObservedAndForecast(PbcStubDataDto pbcStubData, int ptuDuration, int dtuDuration,
            UdiPortfolioDto udiPortfolioDto, int currentPtuIndex) {

        LocalDate currentDate = DateTimeUtil.getCurrentDate();

        int dtusPerPtu = ptuDuration / dtuDuration;
        int ptuIndex = pbcStubData.getPtuContainer().getPtuIndex();
        int startDtu = ((ptuIndex - 1) * dtusPerPtu) + 1; // inclusive
        int endDtu = startDtu + dtusPerPtu - 1; // inclusive

        for (int dtuIndex = startDtu; dtuIndex <= endDtu; dtuIndex++) {
            PowerContainerDto powerContainerDto = udiPortfolioDto.getUdiPowerPerDTU().get(dtuIndex);
            if (powerContainerDto == null || powerContainerDto.getPeriod().toDateTimeAtStartOfDay().toDate()
                    .compareTo(pbcStubData.getPtuContainer().getPtuDate()) != 0 && powerContainerDto.getTimeIndex() != dtuIndex) {
                continue;
            }

            // only set observed if the ptuIndex and dtu period is equal to the previous ptu date and ptu index
            // ideally you only set observed valued for udi's with reporting capabilities
            if (currentPtuIndex == ptuIndex && currentDate.compareTo(powerContainerDto.getPeriod()) == 0) {
                updatePowerValue(powerContainerDto.getObserved(), pbcStubData.getPvLoadActual());
            } else {
                // only set forecast for future ptu indexes
                if (ptuIndex > currentPtuIndex || powerContainerDto.getPeriod().compareTo(currentDate) > 0) {
                    updatePowerValue(powerContainerDto.getForecast(), pbcStubData.getPvLoadForecast());
                }
            }
        }
    }

    /**
     * This method fills the 'ADS' devices with a value between 500W
     * and 2000W. This value does not vary over the given PTUs. The 'PV' devices are filled based on PBCFeeder data.
     *
     * @param period
     * @param ptusPerDay
     * @param connections @return
     */
    public List<ConnectionPortfolioDto> updateUdiLoadForecast(LocalDate period, int ptusPerDay,
            List<ConnectionPortfolioDto> connections) {

        List<PbcStubDataDto> pbcStubDataList = pbcFeederClient.getPbcStubDataList(period, 1, ptusPerDay);
        //pv is production so should be negative for the methods below.
        Map<Integer, BigInteger> pvLoadForecastPerPtu = negateMap(fetchPVLoadForecast(pbcStubDataList));

        for (ConnectionPortfolioDto connectionDto : connections) {
            connectionDto.getUdis().forEach(udiDto -> {

                if (udiDto.getProfile().startsWith(ELEMENT_PV_PROFILE_PREFIX)) {
                    fillForecastOfUdi(period, pvLoadForecastPerPtu, udiDto);
                } else if (udiDto.getProfile().startsWith(ELEMENT_ADS_PROFILE_PREFIX)) {
                    fillForecastOfUdi(period, createADSLoadMap(period, udiDto.getDtuSize()), udiDto);
                }
            });

        }
        return connections;
    }

    private void fillForecastOfUdi(LocalDate period, Map<Integer, BigInteger> powerMap, UdiPortfolioDto udiDto) {
        int dtusPerDay = PtuUtil.getNumberOfPtusPerDay(period, udiDto.getDtuSize());

        for (int dtuIndex = 1; dtuIndex <= dtusPerDay; dtuIndex++) {
            PowerContainerDto powerContainerDto = new PowerContainerDto(period, dtuIndex);
            ForecastPowerDataDto forecast = powerContainerDto.getForecast();
            updatePowerValue(forecast,  powerMap.get(dtuIndex));
            forecast.setPotentialFlexConsumption(forecast.getAverageConsumption().negate());
            forecast.setPotentialFlexProduction(forecast.getAverageProduction().negate());
            udiDto.getUdiPowerPerDTU().put(dtuIndex, powerContainerDto);
        }
    }

    /**
     * Creates and fills elements for a list of {@link ConnectionPortfolioDto}'s.
     * <p>
     * 3 Element's are created: 1 ADS(MANAGED_DEVICE_, 1 PV(MANAGED_DEVICE) and one UCL(SYNTHETIC_DATA).
     *
     * @param connectionPortfolioDtoList
     * @param period
     * @param ptusPerDay
     * @param ptuSize
     * @return
     */
    public List<ElementDto> fillElementsFromPBCFeeder(List<ConnectionPortfolioDto> connectionPortfolioDtoList, LocalDate period,
            Integer ptusPerDay, Integer ptuSize) {

        List<ElementDto> elementDtoList = new ArrayList<>();

        // fetch PBC data for the period
        List<PbcStubDataDto> pbcStubDataList = pbcFeederClient.getPbcStubDataList(period, 1, ptusPerDay);

        // map the PBC data into uncontrolled load and PV load forecast
        Map<Integer, BigInteger> uncontrolledLoadPerPtu = fetchUncontrolledLoad(pbcStubDataList);
        //pv is production so should be negative for the methods below.
        Map<Integer, BigInteger> pvLoadForecastPerPtu = negateMap(fetchPVLoadForecast(pbcStubDataList));

        // for each connection create 3 elements (PV1 and ADS1 and UCL1)
        connectionPortfolioDtoList.stream().forEach(connectionPortfolioDTO -> {

            elementDtoList.add(createManagedDeviceForADS(period, ptusPerDay, ptuSize, connectionPortfolioDTO));

            elementDtoList.add(createManagedDeviceForPV(ptusPerDay, ptuSize, pvLoadForecastPerPtu, connectionPortfolioDTO));

            elementDtoList.add(createSyntheticData(ptusPerDay, ptuSize, uncontrolledLoadPerPtu, connectionPortfolioDTO));
        });

        return elementDtoList;
    }

    private ElementDto createSyntheticData(Integer ptusPerDay, Integer ptuSize, Map<Integer, BigInteger> uncontrolledLoadPerPtu,
            ConnectionPortfolioDto connectionPortfolioDTO) {
        ElementDto syntheticDataElement = buildElement(ELEMENT_SYNTHETIC_DATA_SUFFIX, ptuSize,
                ElementTypeDto.SYNTHETIC_DATA, ELEMENT_SYNTHETIC_DATA_PROFILE_PREFIX,
                connectionPortfolioDTO.getConnectionEntityAddress());
        // add uncontrolled load to the synthetic data element (dtu size = ptu size)
        for (int dtuIndex = 1; dtuIndex <= ptusPerDay; dtuIndex++) {
            ElementDtuDataDto elementDtuDataDto = new ElementDtuDataDto();
            elementDtuDataDto.setDtuIndex(dtuIndex);
            elementDtuDataDto.setProfileUncontrolledLoad(uncontrolledLoadPerPtu.get(elementDtuDataDto.getDtuIndex()));
            syntheticDataElement.getElementDtuData().add(elementDtuDataDto);
        }
        return syntheticDataElement;
    }

    private ElementDto createManagedDeviceForPV(Integer ptusPerDay, Integer ptuSize, Map<Integer, BigInteger> pvLoadForecastPerPtu,
            ConnectionPortfolioDto connectionPortfolioDTO) {
        ElementDto managedDevicePVElement = buildElement(ELEMENT_MANAGED_DEVICE_PV_SUFFIX, ptuSize,
                ElementTypeDto.MANAGED_DEVICE, ELEMENT_PV_PROFILE_PREFIX, connectionPortfolioDTO.getConnectionEntityAddress());
        addElementData(ptusPerDay, pvLoadForecastPerPtu, managedDevicePVElement);
        return managedDevicePVElement;
    }

    private ElementDto createManagedDeviceForADS(LocalDate period, Integer ptusPerDay, Integer ptuSize,
            ConnectionPortfolioDto connectionPortfolioDTO) {
        Map<Integer, BigInteger> adsLoadPerPtu = createADSLoadMap(period, ptuSize);

        ElementDto managedDeviceADSElement = buildElement(ELEMENT_MANAGED_DEVICE_ADS_SUFFIX, ptuSize,
                ElementTypeDto.MANAGED_DEVICE, ELEMENT_ADS_PROFILE_PREFIX, connectionPortfolioDTO.getConnectionEntityAddress());
        addElementData(ptusPerDay, adsLoadPerPtu, managedDeviceADSElement);
        return managedDeviceADSElement;
    }

    private ElementDto buildElement(String id, Integer dtuSize, ElementTypeDto elementType, String profile,
            String connectionEntityAddress) {
        ElementDto elementDto = new ElementDto();
        elementDto.setId(connectionEntityAddress + "." + id);
        elementDto.setElementType(elementType);
        elementDto.setProfile(profile);
        elementDto.setConnectionEntityAddress(connectionEntityAddress);
        elementDto.setDtuDuration(dtuSize);

        return elementDto;
    }

    private void addElementData(Integer ptusPerDay, Map<Integer, BigInteger> loadPerDtu, ElementDto elementDto) {
        for (int dtuIndex = 1; dtuIndex <= ptusPerDay; dtuIndex++) {
            addElementDtuData(elementDto, dtuIndex, loadPerDtu.get(dtuIndex));
        }
    }

    private void addElementDtuData(ElementDto elementDto, Integer ptuIndex, BigInteger load) {
        ElementDtuDataDto elementDtuDataDto = new ElementDtuDataDto();
        elementDtuDataDto.setDtuIndex(ptuIndex);

        // set average load production / consumption
        if (load.compareTo(BigInteger.ZERO) < 0) {
            elementDtuDataDto.setProfileAverageProduction(load.abs());
            elementDtuDataDto.setProfileAverageConsumption(BigInteger.ZERO);
        } else {
            elementDtuDataDto.setProfileAverageProduction(BigInteger.ZERO);
            elementDtuDataDto.setProfileAverageConsumption(load);
        }

        elementDtuDataDto.setProfilePotentialFlexProduction(elementDtuDataDto.getProfileAverageProduction().negate());
        elementDtuDataDto.setProfilePotentialFlexConsumption(elementDtuDataDto.getProfileAverageConsumption().negate());

        elementDto.getElementDtuData().add(elementDtuDataDto);
    }

    private Map<Integer, BigInteger> fetchUncontrolledLoad(List<PbcStubDataDto> pbcStubDataDtos) {
        return pbcStubDataDtos.stream()
                .collect(Collectors.toMap(pbcStubDataDto -> pbcStubDataDto.getPtuContainer().getPtuIndex(),
                        pbcStubDataDto -> BigInteger.valueOf(Math.round(pbcStubDataDto.getCongestionPointAvg()))));
    }

    private Map<Integer, BigInteger> fetchPVLoadForecast(List<PbcStubDataDto> pbcStubDataDtos) {
        return pbcStubDataDtos.stream()
                .collect(Collectors.toMap(pbcStubDataDto -> pbcStubDataDto.getPtuContainer().getPtuIndex(),
                        pbcStubDataDto -> BigInteger.valueOf(Math.round(pbcStubDataDto.getPvLoadForecast()))));
    }

    private BigInteger fetchADSLoadConstant() {
        Random random = new Random();
        BigInteger load = BigInteger.valueOf(random.nextInt(MAGIC_DEVICE_MAX - MAGIC_DEVICE_MIN) + MAGIC_DEVICE_MIN);
        return random.nextBoolean() ? load : load.negate();
    }

    private Map<Integer, BigInteger> createADSLoadMap(LocalDate period, Integer timeSize) {
        Integer timeSlotsPerDay = PtuUtil.getNumberOfPtusPerDay(period, timeSize);
        final BigInteger adsLoadConstant = fetchADSLoadConstant();
        return IntStream.rangeClosed(1, timeSlotsPerDay).mapToObj(Integer::valueOf)
                .collect(Collectors.toMap(Function.identity(), i -> adsLoadConstant));
    }

    private Map<Integer, BigInteger> negateMap(Map<Integer, BigInteger> powerMap) {
        return powerMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().negate()));
    }

    private void updatePowerValue(PowerDataDto powerDataDto, Double value) {
        if (value != null) {
            BigInteger power = BigInteger.valueOf((int) Math.round(value));
            updatePowerValue(powerDataDto, power);
        }
    }

    private void updatePowerValue(PowerDataDto powerDataDto, BigInteger power) {
        if (power != null) {
            if (power.compareTo(BigInteger.ZERO) >= 0) {
                powerDataDto.setAverageConsumption(power);
                powerDataDto.setAverageProduction(BigInteger.ZERO);
            } else {
                powerDataDto.setAverageConsumption(BigInteger.ZERO);
                powerDataDto.setAverageProduction(power.abs());
            }
        }
    }

}
