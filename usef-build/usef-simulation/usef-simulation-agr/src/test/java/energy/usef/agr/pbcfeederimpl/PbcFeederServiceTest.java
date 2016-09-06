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

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.dto.ElementTypeDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.pbcfeeder.PbcFeederClient;
import energy.usef.pbcfeeder.dto.PbcPtuContainerDto;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class PbcFeederServiceTest {

    @Mock
    PbcFeederClient pbcFeederClient;
    private PbcFeederService pbcFeederService;

    @Before
    public void setUp() throws Exception {
        pbcFeederService = new PbcFeederService();
        Whitebox.setInternalState(pbcFeederService, "pbcFeederClient", pbcFeederClient);
    }

    @Test
    public void testGetUncontrolledLoadCongestionPoint() {
        // given
        LocalDate date = new LocalDate("2015-04-22");
        Mockito.when(pbcFeederClient.getPbcStubDataList(date, 1, 96)).then(
                invocation -> IntStream.rangeClosed(1, 96).mapToObj(index -> {
                    PbcStubDataDto pbcStubDataDto = new PbcStubDataDto();
                    PbcPtuContainerDto ptuContainerDto = new PbcPtuContainerDto(date.toDateMidnight().toDate(), index);
                    pbcStubDataDto.setCongestionPointAvg(100.00);
                    pbcStubDataDto.setPtuContainer(ptuContainerDto);
                    pbcStubDataDto.setIndex(index * 2);
                    return pbcStubDataDto;
                }).collect(Collectors.toList()));

        List<ConnectionPortfolioDto> connectionDtos = IntStream.rangeClosed(1, 3)
                .mapToObj(index -> new ConnectionPortfolioDto("ean.12345"))
                .collect(Collectors.toList());

        // when
        connectionDtos = pbcFeederService
                .updateConnectionUncontrolledLoadForecast(date, 1, 96, connectionDtos);

        // then
        Assert.assertEquals(96, connectionDtos.get(0).getConnectionPowerPerPTU().size());

    }

    @Test
    public void testGetUDIListWithPvLoadForecast() {
        // given
        LocalDate date = new LocalDate("2015-04-22");
        Mockito.when(pbcFeederClient.getPbcStubDataList(date, 1, 96)).then(
                invocation -> IntStream.rangeClosed(1, 96).mapToObj(idx -> {
                    PbcStubDataDto pbcStubDataDto = new PbcStubDataDto();
                    PbcPtuContainerDto ptuContainerDto = new PbcPtuContainerDto(date.toDateMidnight().toDate(), idx);
                    pbcStubDataDto.setPvLoadForecast(100.00);
                    pbcStubDataDto.setPtuContainer(ptuContainerDto);
                    return pbcStubDataDto;
                }).collect(Collectors.toList()));

        List<ConnectionPortfolioDto> connectionDtos = IntStream.rangeClosed(1, 3)
                .mapToObj(idx -> new ConnectionPortfolioDto("ean.12345"))
                .collect(Collectors.toList());

        // actual invocation
        List<ConnectionPortfolioDto> connectionDtoList = pbcFeederService.retrieveUDIListWithPvLoadForecast(date, connectionDtos,
                15, 15);

        // then
        Assert.assertNotNull(connectionDtoList.get(0).getUdis().get(0));
        Assert.assertEquals(96, connectionDtoList.get(0).getUdis().get(0).getUdiPowerPerDTU().size());
    }

    @Test
    public void testGetUDIListWithPvLoadActualAveragePower() {
        final int NR_OF_DAYS = 2;

        // given
        LocalDate date = new LocalDate();

        // retrieve pbc data for 2 days
        Mockito.when(pbcFeederClient.getPbcStubDataList(Matchers.any(LocalDate.class), Matchers.anyInt(),
                Matchers.anyInt())).thenReturn(buildPbcStubDataList(date, NR_OF_DAYS));

        Mockito.when(pbcFeederClient.getPbcStubDataList(Matchers.any(LocalDate.class), Matchers.anyInt(), Matchers.anyInt())).then(
                invocation -> IntStream.rangeClosed(1, 96).mapToObj(idx -> {
                    PbcStubDataDto pbcStubDataDto = new PbcStubDataDto();
                    PbcPtuContainerDto ptuContainerDto = new PbcPtuContainerDto(date.toDateMidnight().toDate(), idx);
                    pbcStubDataDto.setPvLoadActual(100.00 * idx);
                    pbcStubDataDto.setPvLoadForecast(10.00 * idx);
                    pbcStubDataDto.setPtuContainer(ptuContainerDto);
                    return pbcStubDataDto;
                }).collect(Collectors.toList()));

        List<ConnectionPortfolioDto> connectionDtos = new ArrayList<>();

        for (int idx = 1; idx <= 3; idx++) {
            ConnectionPortfolioDto connectionDto = new ConnectionPortfolioDto("ean.12345" + idx);
            connectionDto.getUdis().addAll(generateUdiDtos(date, NR_OF_DAYS));

            connectionDtos.add(connectionDto);
        }

        // when
        List<ConnectionPortfolioDto> connectionDtoList = pbcFeederService
                .retrieveUDIListWithPvLoadAveragePower(date, connectionDtos, 15);

        // then
        Assert.assertNotNull(connectionDtoList.get(0).getUdis().get(0));
        Assert.assertEquals(3, connectionDtoList.size());
        Assert.assertEquals(3 * NR_OF_DAYS, connectionDtoList.get(0).getUdis().size());
        Assert.assertEquals(96, connectionDtoList.get(0).getUdis().get(0).getUdiPowerPerDTU().size());

        int currentPtuIndex = determineCurrentPTUIndex(15);
        Assert.assertEquals(BigInteger.valueOf(100 * (currentPtuIndex - 1)),
                connectionDtoList.get(0).getUdis().get(0).getUdiPowerPerDTU().get(currentPtuIndex - 1).getObserved()
                        .calculatePower());
        Assert.assertEquals(BigInteger.valueOf(10 * currentPtuIndex),
                connectionDtoList.get(0).getUdis().get(0).getUdiPowerPerDTU().get(currentPtuIndex).getForecast().calculatePower());

        // make sure there is exactly one actual average power per udi
        for (ConnectionPortfolioDto connectionDto : connectionDtoList) {
            for (UdiPortfolioDto udiDto : connectionDto.getUdis()) {
                int nrOfActuals = 0;
                for (PowerContainerDto udiPowerContainerDto : udiDto.getUdiPowerPerDTU().values()) {
                    if ((udiPowerContainerDto.getObserved().getAverageConsumption() != null &&
                            udiPowerContainerDto.getObserved().getAverageConsumption().compareTo(BigInteger.ZERO) != 0)
                            || (udiPowerContainerDto.getObserved().getAverageProduction() != null &&
                            udiPowerContainerDto.getObserved().getAverageProduction().compareTo(BigInteger.ZERO) != 0)) {
                        nrOfActuals++;
                    }
                }
                // only actuals are expected for current day
                if (udiDto.getUdiPowerPerDTU().get(1).getPeriod().compareTo(date) == 0) {
                    Assert.assertEquals(1, nrOfActuals);
                } else {
                    Assert.assertEquals(0, nrOfActuals);
                }
            }
        }

        // make sure that there are no forecasts for previous ptu's
        for (ConnectionPortfolioDto connectionDto : connectionDtoList) {
            for (UdiPortfolioDto udiDto : connectionDto.getUdis()) {
                for (PowerContainerDto udiPowerContainerDto : udiDto.getUdiPowerPerDTU().values()) {
                    if (udiPowerContainerDto.getPeriod().compareTo(date) <= 0 && udiPowerContainerDto.getTimeIndex() < currentPtuIndex
                            && ((udiPowerContainerDto.getForecast().getAverageConsumption() != null &&
                            udiPowerContainerDto.getForecast().getAverageConsumption().compareTo(BigInteger.ZERO) != 0)
                            || (udiPowerContainerDto.getForecast().getAverageProduction() != null &&
                            udiPowerContainerDto.getForecast().getAverageProduction().compareTo(BigInteger.ZERO) != 0))) {
                        Assert.fail("Forecast average power for a ptu (" + udiPowerContainerDto.getTimeIndex()
                                + ") in the past should not be possible at this moment!");
                    }
                }
            }
        }
    }

    @Test
    public void testRetrieveNonUDIListWithForecast() {
        // given
        LocalDate date = new LocalDate();
        Mockito.when(pbcFeederClient.getPbcStubDataList(Matchers.any(LocalDate.class), Matchers.anyInt(), Matchers.anyInt())).then(
                invocation -> IntStream.rangeClosed(1, 96).mapToObj(idx -> {
                    PbcStubDataDto pbcStubDataDto = new PbcStubDataDto();
                    PbcPtuContainerDto ptuContainerDto = new PbcPtuContainerDto(date.toDateMidnight().toDate(), idx);
                    pbcStubDataDto.setPvLoadActual(100.00 * idx);
                    pbcStubDataDto.setPvLoadForecast(10.00 * idx);
                    pbcStubDataDto.setPtuContainer(ptuContainerDto);
                    return pbcStubDataDto;
                }).collect(Collectors.toList()));

        List<ConnectionPortfolioDto> connectionDtos = new ArrayList<>();

        for (int idx = 1; idx <= 3; idx++) {
            ConnectionPortfolioDto connectionDto = new ConnectionPortfolioDto("ean.12345" + idx);
            IntStream.rangeClosed(1, 96).forEach(index -> {
                connectionDto.getConnectionPowerPerPTU().put(index, new PowerContainerDto(date, index));
            });
            connectionDtos.add(connectionDto);
        }

        // when
        List<ConnectionPortfolioDto> connectionDtoList = pbcFeederService.retrieveNonUDIListWithForecast(date, connectionDtos, 15);

        // then
        Assert.assertNotNull(connectionDtoList.get(0));
        Assert.assertEquals(3, connectionDtoList.size());
        Assert.assertEquals(96, connectionDtoList.get(0).getConnectionPowerPerPTU().size());

        int currentPtuIndex = determineCurrentPTUIndex(15);
        Assert.assertEquals(BigInteger.valueOf(10 * currentPtuIndex),
                connectionDtoList.get(0).getConnectionPowerPerPTU().get(currentPtuIndex).getForecast().calculatePower());

    }

    @Test
    public void testGetAdsLoadForecast() {
        // given
        LocalDate date = new LocalDate("2015-04-22");
        Mockito.when(pbcFeederClient.getPbcStubDataList(date, 1, 96)).then(
                invocation -> IntStream.rangeClosed(1, 96).mapToObj(idx -> {
                    PbcStubDataDto pbcStubDataDto = new PbcStubDataDto();
                    PbcPtuContainerDto ptuContainerDto = new PbcPtuContainerDto(date.toDateMidnight().toDate(), idx);
                    pbcStubDataDto.setPvLoadForecast(100.00);
                    pbcStubDataDto.setPtuContainer(ptuContainerDto);
                    return pbcStubDataDto;
                }).collect(Collectors.toList()));

        List<ConnectionPortfolioDto> connectionDtos = IntStream.rangeClosed(1, 3)
                .mapToObj(idx ->  {
                    ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto("ean.12345");
                    connectionPortfolioDTO.getUdis().add(new UdiPortfolioDto("aaa-"+idx, 5 , "ADS_1234"));
                    return connectionPortfolioDTO;
                })
                .collect(Collectors.toList());

        // when
        List<ConnectionPortfolioDto> connectionDtoList = pbcFeederService.updateUdiLoadForecast(date, 96, connectionDtos);

        // then
        Assert.assertNotNull(connectionDtoList.get(0).getUdis().get(0));
        Assert.assertEquals(288, connectionDtoList.get(0).getUdis().get(0).getUdiPowerPerDTU().size());
    }

    @Test
    public void testRetrieveApxPrices() {
        // stubbing of the PBCFeederClient
        Mockito.when(pbcFeederClient.getPbcStubDataList(Matchers.any(LocalDate.class),
                Matchers.anyInt(),
                Matchers.anyInt())).then(invocation -> {
            List<PbcStubDataDto> data = new ArrayList<>();
            final Date period = ((LocalDate) invocation.getArguments()[0]).toDateMidnight().toDate();
            final int startPtu = (int) invocation.getArguments()[1];
            IntStream.range(startPtu, startPtu + (int) invocation.getArguments()[2]).mapToObj(index -> {
                PbcStubDataDto ptuData = new PbcStubDataDto();
                ptuData.setPtuContainer(new PbcPtuContainerDto(period, index));
                ptuData.setApx(index + 10D);
                return ptuData;
            }).forEach(data::add);
            return data;
        });

        // actual invocation
        Map<Integer, BigDecimal> apxPrices = pbcFeederService.retrieveApxPrices(DateTimeUtil.parseDate("2015-06-17"), 1, 12);

        // assertions
        Assert.assertNotNull(apxPrices);
        Assert.assertEquals(12, apxPrices.size());
        apxPrices.entrySet().stream().forEach(entry -> Assert
                .assertEquals(BigDecimal.valueOf(10D + entry.getKey()).setScale(0, RoundingMode.HALF_UP), entry.getValue()));

    }

    @Test
    public void testFillElementsFromPBCFeeder() {
        // variables and mocking
        List<ConnectionPortfolioDto> connectionPortfolio = Arrays
                .asList(new ConnectionPortfolioDto("ean.0000000001"), new ConnectionPortfolioDto("ean.0000000002"));
        final LocalDate period = new LocalDate(2015, 8, 20);
        final Integer ptusPerDay = 12;
        Mockito.when(pbcFeederClient.getPbcStubDataList(Matchers.any(LocalDate.class), Matchers.eq(1), Matchers.any(Integer.class)))
                .then(call -> IntStream.rangeClosed(1, (Integer) call.getArguments()[2]).mapToObj(index -> {
                    PbcStubDataDto pbcStubDataDto = new PbcStubDataDto();
                    pbcStubDataDto.setPtuContainer(new PbcPtuContainerDto(period.toDateMidnight().toDate(), index));
                    pbcStubDataDto.setCongestionPointAvg(1000D);
                    pbcStubDataDto.setPvLoadForecast(-500D);
                    return pbcStubDataDto;
                }).collect(Collectors.toList()));

        // invocation
        List<ElementDto> elementList = pbcFeederService.fillElementsFromPBCFeeder(connectionPortfolio, period, ptusPerDay, 120);

        Assert.assertNotNull(elementList);

        // 2 connections, 3 elements per connection, so make sure there are 6 elements returned
        Assert.assertEquals(6, elementList.size());

        // make sure that there are exactly 12 elementDtuData objects created per MANAGED_DEVICE element
        elementList.stream().filter(elementDto -> ElementTypeDto.MANAGED_DEVICE.equals(elementDto.getElementType())).forEach(elementDto -> {
            Assert.assertEquals(12, elementDto.getElementDtuData().size());
        });

        List<ElementDto> connnectionElementList1 = elementList.stream()
                .filter(elementDto -> elementDto.getConnectionEntityAddress().equals("ean.0000000001"))
                .collect(Collectors.toList());
        List<ElementDto> connnectionElementList2 = elementList.stream()
                .filter(elementDto -> elementDto.getConnectionEntityAddress().equals("ean.0000000002"))
                .collect(Collectors.toList());

        // 3 elements per connection
        Assert.assertEquals(3, connnectionElementList1.size());
        Assert.assertEquals(3, connnectionElementList2.size());

        // exactly 1 element with id PV1 and ADS1 per connection
        Assert.assertEquals(1,
                connnectionElementList1.stream().filter(elementDto -> elementDto.getId().equals("ean.0000000001.PV1")).count());
        Assert.assertEquals(1,
                connnectionElementList1.stream().filter(elementDto -> elementDto.getId().equals("ean.0000000001.ADS1")).count());
        Assert.assertEquals(1,
                connnectionElementList1.stream().filter(elementDto -> elementDto.getId().equals("ean.0000000001.UCL1")).count());
        Assert.assertEquals(1,
                connnectionElementList2.stream().filter(elementDto -> elementDto.getId().equals("ean.0000000002.PV1")).count());
        Assert.assertEquals(1,
                connnectionElementList2.stream().filter(elementDto -> elementDto.getId().equals("ean.0000000002.ADS1")).count());
        Assert.assertEquals(1,
                connnectionElementList2.stream().filter(elementDto -> elementDto.getId().equals("ean.0000000002.UCL1")).count());

        // check the uncontrolled load in the synthetic data elements
        connnectionElementList1.stream().filter(elementDto -> elementDto.getId().contains(".UCL1")).forEach(elementDto -> {
            elementDto.getElementDtuData().forEach(elementDtuDataDto -> {
                Assert.assertNotNull(elementDtuDataDto.getProfileUncontrolledLoad());
                Assert.assertEquals(BigInteger.valueOf(1000), elementDtuDataDto.getProfileUncontrolledLoad());
            });
        });
    }

    private List<PbcStubDataDto> buildPbcStubDataList(LocalDate date, int nrOfDays) {
        List<PbcStubDataDto> pbcData = new ArrayList<>();

        for (int days = 0; days < nrOfDays; days++) {
            final int addDays = days;
            pbcData.addAll(IntStream.rangeClosed(1, 96).mapToObj(idx -> {
                PbcStubDataDto pbcStubDataDto = new PbcStubDataDto();
                PbcPtuContainerDto ptuContainerDto = new PbcPtuContainerDto(date.plusDays(addDays).toDateMidnight().toDate(), idx);
                pbcStubDataDto.setPvLoadActual(100.00 * idx);
                pbcStubDataDto.setPvLoadForecast(10.00 * idx);
                pbcStubDataDto.setPtuContainer(ptuContainerDto);
                return pbcStubDataDto;
            }).collect(Collectors.toList()));
        }

        return pbcData;
    }

    private int determineCurrentPTUIndex(int ptuDuration) {
        LocalDateTime currentDateTime = DateTimeUtil.getCurrentDateTime();
        return 1 + (currentDateTime.get(DateTimeFieldType.secondOfDay()) / (ptuDuration * 60));
    }

    /**
     * Generate a list of UdiDto's with 3 Udi's, each with 96 dtuDto's (dtu size 15) for a specified number of days.
     */
    private List<UdiPortfolioDto> generateUdiDtos(LocalDate date, int nrOfDays) {
        List<UdiPortfolioDto> udiDtoList = new ArrayList<>();

        for (int days = 0; days < nrOfDays; days++) {
            for (int index = 1; index <= 3; index++) {
                UdiPortfolioDto udiDto = new UdiPortfolioDto("[abc]:9541", 15, "BEMS");
                for (int index2 = 1; index2 <= 96; index2++) {
                    PowerContainerDto powerContainerDto = new PowerContainerDto(date.plusDays(days), index2);
                    udiDto.getUdiPowerPerDTU().put(index2, powerContainerDto);
                }
                udiDtoList.add(udiDto);
            }
        }
        return udiDtoList;
    }
}
