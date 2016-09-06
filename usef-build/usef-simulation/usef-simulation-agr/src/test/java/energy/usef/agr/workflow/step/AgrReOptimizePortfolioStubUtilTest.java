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

package energy.usef.agr.workflow.step;

import static energy.usef.core.util.DateTimeUtil.getCurrentDate;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.core.workflow.dto.AcknowledgementStatusDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PrognosisTypeDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * JUnit test for the {@link AgrReOptimizePortfolioStubUtil} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class AgrReOptimizePortfolioStubUtilTest {

    @Test
    public void testMapConnectionPortfolioPerConnectionGroup() throws Exception {
        Map<String, List<ConnectionPortfolioDto>> connectionPortfolioPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .mapConnectionPortfolioPerConnectionGroup(
                buildConnectionPortfolio(), buildConnectionGroupToConnections());

        Assert.assertEquals(2, connectionPortfolioPerConnectionGroup.size());
        Assert.assertEquals(5, connectionPortfolioPerConnectionGroup.get("brp").size());
        Assert.assertEquals(5, connectionPortfolioPerConnectionGroup.get("dso").size());
    }

    @Test
    public void testMapPrognosisPowerPerPtuPerConnectionGroup() throws Exception {
        Map<String, Map<Integer, BigInteger>> prognosisPowerPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .mapPrognosisPowerPerPtuPerConnectionGroup(
                buildAPlans(), buildDPrognosis());

        Assert.assertEquals(2, prognosisPowerPerPtuPerConnectionGroup.size());
        Assert.assertEquals(12, prognosisPowerPerPtuPerConnectionGroup.get("brp").size());
        Assert.assertEquals(12, prognosisPowerPerPtuPerConnectionGroup.get("dso").size());
        for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
            Assert.assertEquals(BigInteger.valueOf(ptuIndex * 10), prognosisPowerPerPtuPerConnectionGroup.get("brp").get(ptuIndex));
            Assert.assertEquals(BigInteger.valueOf(ptuIndex * 10), prognosisPowerPerPtuPerConnectionGroup.get("dso").get(ptuIndex));
        }
    }

    @Test
    public void testSumOrderedPowerPerPtuPerConnectionGroup() throws Exception {
        Map<String, Map<Integer, BigInteger>> orderedPowerPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .sumOrderedPowerPerPtuPerConnectionGroup(
                buildFlexOrders());

        Assert.assertEquals(2, orderedPowerPerPtuPerConnectionGroup.size());
        Assert.assertEquals(12, orderedPowerPerPtuPerConnectionGroup.get("brp").size());
        Assert.assertEquals(12, orderedPowerPerPtuPerConnectionGroup.get("dso").size());
        for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
            Assert.assertEquals(BigInteger.valueOf(ptuIndex * 100 * 3),
                    orderedPowerPerPtuPerConnectionGroup.get("brp").get(ptuIndex));
            Assert.assertEquals(BigInteger.valueOf(ptuIndex * 100 * 3),
                    orderedPowerPerPtuPerConnectionGroup.get("dso").get(ptuIndex));
        }
    }

    @Test
    public void testSumForecastPowerPerPtuPerConnectionGroup() throws Exception {
        Map<String, Map<Integer, BigInteger>> forecastPowerPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .sumForecastPowerPerPtuPerConnectionGroup(
                AgrReOptimizePortfolioStubUtil
                        .mapConnectionPortfolioPerConnectionGroup(buildConnectionPortfolio(), buildConnectionGroupToConnections()),
                getCurrentDate(), 120);

        Assert.assertEquals(2, forecastPowerPerPtuPerConnectionGroup.size());
        Assert.assertEquals(12, forecastPowerPerPtuPerConnectionGroup.get("brp").size());
        Assert.assertEquals(12, forecastPowerPerPtuPerConnectionGroup.get("dso").size());
        for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
            Assert.assertEquals(BigInteger.valueOf(150), forecastPowerPerPtuPerConnectionGroup.get("brp").get(ptuIndex));
            Assert.assertEquals(BigInteger.valueOf(150), forecastPowerPerPtuPerConnectionGroup.get("dso").get(ptuIndex));
        }

    }

    @Test
    public void testFetchTargetPowerPerPtuPerConnectionGroup() throws Exception {
        Map<String, Map<Integer, BigInteger>> targetPowerPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .fetchTargetPowerPerPtuPerConnectionGroup(
                AgrReOptimizePortfolioStubUtil.mapPrognosisPowerPerPtuPerConnectionGroup(buildAPlans(), buildDPrognosis()),
                AgrReOptimizePortfolioStubUtil.sumOrderedPowerPerPtuPerConnectionGroup(buildFlexOrders()), AgrReOptimizePortfolioStubUtil
                                .sumForecastPowerPerPtuPerConnectionGroup(
                        AgrReOptimizePortfolioStubUtil.mapConnectionPortfolioPerConnectionGroup(buildConnectionPortfolio(), buildConnectionGroupToConnections()),
                        getCurrentDate(), 120));

        Assert.assertEquals(2, targetPowerPerPtuPerConnectionGroup.size());
        Assert.assertEquals(12, targetPowerPerPtuPerConnectionGroup.get("brp").size());
        Assert.assertEquals(12, targetPowerPerPtuPerConnectionGroup.get("dso").size());
        for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
            // assert that the target power per ptu == prognosis + ordered - forecast
            Assert.assertEquals(BigInteger.valueOf((ptuIndex * 100 * 3) + (10 * ptuIndex) - 150),
                    targetPowerPerPtuPerConnectionGroup.get("brp").get(ptuIndex));
            Assert.assertEquals(BigInteger.valueOf((ptuIndex * 100 * 3) + (10 * ptuIndex) - 150),
                    targetPowerPerPtuPerConnectionGroup.get("dso").get(ptuIndex));
        }
    }

    @Test
    public void testSumPotentialFlexPerPtuPerConnectionGroup() throws Exception {
        Map<String, Map<Integer, BigInteger>> potentialFlexPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .sumPotentialFlexConsumptionPerPtuPerConnectionGroup(
                AgrReOptimizePortfolioStubUtil
                        .mapConnectionPortfolioPerConnectionGroup(buildConnectionPortfolio(), buildConnectionGroupToConnections()),
                getCurrentDate(), 120);

        Assert.assertEquals(2, potentialFlexPerPtuPerConnectionGroup.size());
        Assert.assertEquals(12, potentialFlexPerPtuPerConnectionGroup.get("brp").size());
        Assert.assertEquals(12, potentialFlexPerPtuPerConnectionGroup.get("dso").size());
        for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
            // assert that the potential flex consumption power per ptu == 5 * 2 * 10 * ptuIndex
            Assert.assertEquals(BigInteger.valueOf(ptuIndex * -100),
                    potentialFlexPerPtuPerConnectionGroup.get("brp").get(ptuIndex));
            Assert.assertEquals(BigInteger.valueOf(ptuIndex * -100),
                    potentialFlexPerPtuPerConnectionGroup.get("dso").get(ptuIndex));
        }
    }

    @Test
    public void testFetchFlexFactorPerPtuPerConnectionGroup() throws Exception {
        Map<String, Map<Integer, BigInteger>> targetPowerPerPtuPerConnectionGroup = buildTargetPowerPerPtuPerConnectionGroup();
        Map<String, Map<Integer, BigInteger>> potentialFlexConsumptionPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .sumPotentialFlexConsumptionPerPtuPerConnectionGroup(
                AgrReOptimizePortfolioStubUtil
                        .mapConnectionPortfolioPerConnectionGroup(buildConnectionPortfolio(), buildConnectionGroupToConnections()),
                getCurrentDate(), 120);
        Map<String, Map<Integer, BigInteger>> potentialFlexProductionPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .sumPotentialFlexProductionPerPtuPerConnectionGroup(
                AgrReOptimizePortfolioStubUtil
                        .mapConnectionPortfolioPerConnectionGroup(buildConnectionPortfolio(), buildConnectionGroupToConnections()),
                getCurrentDate(), 120);

        Map<String, Map<Integer, BigDecimal>> factorPerPtuPerConnectionGroup = AgrReOptimizePortfolioStubUtil
                .fetchFlexFactorPerPtuPerConnectionGroup(
                targetPowerPerPtuPerConnectionGroup, potentialFlexConsumptionPerPtuPerConnectionGroup,
                potentialFlexProductionPerPtuPerConnectionGroup);

        Assert.assertEquals(2, factorPerPtuPerConnectionGroup.size());
        Assert.assertEquals(12, factorPerPtuPerConnectionGroup.get("brp").size());
        Assert.assertEquals(12, factorPerPtuPerConnectionGroup.get("dso").size());
        for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
            // assert that the factor == target / flex (0 when factor < 0 and 1 when factor > 1)
            BigDecimal target = new BigDecimal(targetPowerPerPtuPerConnectionGroup.get("brp").get(ptuIndex));
            BigDecimal flex = new BigDecimal(potentialFlexConsumptionPerPtuPerConnectionGroup.get("brp").get(ptuIndex));
            BigDecimal factor = target.divide(flex, 5, RoundingMode.HALF_UP);
            if (factor.compareTo(BigDecimal.ONE) > 0) {
                factor = BigDecimal.valueOf(1);
            } else if (factor.compareTo(BigDecimal.ZERO) < 0) {
                factor = BigDecimal.ZERO;
            }
            Assert.assertEquals(factor.setScale(5), factorPerPtuPerConnectionGroup.get("brp").get(ptuIndex).setScale(5));
        }
    }

    private Map<String, Map<Integer, BigInteger>> buildTargetPowerPerPtuPerConnectionGroup() {
        Map<String, Map<Integer, BigInteger>> targetPowerPerPtuPerConnectionGroup = new HashMap<>();

        Map<Integer, BigInteger> targetPowerPerPtu = new HashMap<>();
        for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
            targetPowerPerPtu.put(ptuIndex, BigInteger.valueOf(-600 + ptuIndex * 100));
        }

        targetPowerPerPtuPerConnectionGroup.put("brp", targetPowerPerPtu);
        targetPowerPerPtuPerConnectionGroup.put("dso", targetPowerPerPtu);

        return targetPowerPerPtuPerConnectionGroup;
    }

    private Map<String, List<String>> buildConnectionGroupToConnections() {
        Map<String, List<String>> toReturn = new HashMap<>();

        toReturn.put("brp", Arrays.asList("ean.1", "ean.2", "ean.3", "ean.4", "ean.5"));
        toReturn.put("dso", Arrays.asList("ean.6", "ean.7", "ean.8", "ean.9", "ean.10"));

        return toReturn;
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolio() {
        List<ConnectionPortfolioDto> connectionPortfolioDTOList = new ArrayList<>();

        for (int connectionId = 1; connectionId <= 10; connectionId++) {
            ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto("ean." + connectionId);

            for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
                PowerContainerDto powerContainerDto = new PowerContainerDto(getCurrentDate(), ptuIndex);
                powerContainerDto.getForecast().setAverageConsumption(BigInteger.TEN);

                connectionPortfolioDTO.getConnectionPowerPerPTU().put(ptuIndex, powerContainerDto);
            }

            for (int udiId = 1; udiId <= 2; udiId++) {
                UdiPortfolioDto udi = new UdiPortfolioDto("udi." + connectionId + "." + udiId, 120, "profile");
                for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
                    PowerContainerDto powerContainerDto = new PowerContainerDto(getCurrentDate(), ptuIndex);
                    powerContainerDto.getForecast().setAverageConsumption(BigInteger.valueOf(10));
                    powerContainerDto.getForecast().setPotentialFlexConsumption(BigInteger.valueOf(-10 * ptuIndex));

                    udi.getUdiPowerPerDTU().put(ptuIndex, powerContainerDto);
                }
                connectionPortfolioDTO.getUdis().add(udi);
            }

            connectionPortfolioDTOList.add(connectionPortfolioDTO);
        }

        return connectionPortfolioDTOList;
    }

    private List<PrognosisDto> buildDPrognosis() {
        List<PrognosisDto> prognosisDtoList = new ArrayList<>();

        PrognosisDto prognosisDto = new PrognosisDto();
        prognosisDto.setConnectionGroupEntityAddress("dso");
        prognosisDto.setType(PrognosisTypeDto.D_PROGNOSIS);
        prognosisDto.setParticipantDomain("dso.usef-example.com");

        for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
            PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
            ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
            ptuPrognosisDto.setPower(BigInteger.valueOf(ptuIndex * 10));
            prognosisDto.getPtus().add(ptuPrognosisDto);
        }

        prognosisDtoList.add(prognosisDto);

        return prognosisDtoList;
    }

    private List<PrognosisDto> buildAPlans() {
        List<PrognosisDto> prognosisDtoList = new ArrayList<>();

        PrognosisDto prognosisDto = new PrognosisDto();
        prognosisDto.setConnectionGroupEntityAddress("brp");
        prognosisDto.setType(PrognosisTypeDto.A_PLAN);
        prognosisDto.setParticipantDomain("brp.usef-example.com");

        for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
            PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
            ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
            ptuPrognosisDto.setPower(BigInteger.valueOf(ptuIndex * 10));
            prognosisDto.getPtus().add(ptuPrognosisDto);
        }

        prognosisDtoList.add(prognosisDto);

        return prognosisDtoList;
    }

    private List<FlexOrderDto> buildFlexOrders() {
        List<FlexOrderDto> flexOrderDtoList = new ArrayList<>();

        for (int flexOrderId = 1; flexOrderId <= 6; flexOrderId++) {
            FlexOrderDto flexOrderDto = new FlexOrderDto();
            flexOrderDto.setAcknowledgementStatus(AcknowledgementStatusDto.ACCEPTED);
            flexOrderDto.setPeriod(getCurrentDate());
            flexOrderDto.setFlexOfferSequenceNumber((long) flexOrderId);
            flexOrderDto.setSequenceNumber((long) flexOrderId);
            if (flexOrderId % 2 == 0) {
                flexOrderDto.setParticipantDomain("brp.usef-example.com");
                flexOrderDto.setConnectionGroupEntityAddress("brp");
            } else {
                flexOrderDto.setParticipantDomain("dso.usef-example.com");
                flexOrderDto.setConnectionGroupEntityAddress("dso");
            }
            for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
                PtuFlexOrderDto ptuFlexOrderDto = new PtuFlexOrderDto();
                ptuFlexOrderDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
                ptuFlexOrderDto.setPower(BigInteger.valueOf(ptuIndex * 100));
                flexOrderDto.getPtus().add(ptuFlexOrderDto);
            }

            flexOrderDtoList.add(flexOrderDto);
        }

        return flexOrderDtoList;
    }

}
