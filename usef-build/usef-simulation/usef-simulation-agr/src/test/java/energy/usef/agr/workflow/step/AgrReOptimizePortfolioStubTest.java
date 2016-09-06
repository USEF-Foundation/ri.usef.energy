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

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.dto.device.capability.ReduceCapabilityDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.dto.device.request.ConsumptionProductionTypeDto;
import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.util.PowerContainerDtoUtil;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter.IN;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.AcknowledgementStatusDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class in charge of the unit tests related to the {@link AgrReOptimizePortfolioStub} class.
 */
public class AgrReOptimizePortfolioStubTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrReOptimizePortfolioStubTest.class);
    private final static int NR_OF_PTUS = 12;

    private AgrReOptimizePortfolioStub stub;

    @Before
    public void init() {
        stub = new AgrReOptimizePortfolioStub();
    }

    @Test
    public void testInvokeWithEmptyPrognosis() throws Exception {
        testInvoke(false);
    }

    @Test
    public void testInvokeWithPrognosis() throws Exception {
        testInvoke(true);
    }

    @SuppressWarnings("unchecked")
    private void testInvoke(boolean prognosisAvailable) throws Exception {
        WorkflowContext inContext = buildContext(prognosisAvailable);

        List<PrognosisDto> prognosis = inContext.get(IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), List.class);

        Map<Integer, BigInteger> prognosisPerPtu;
        if (prognosisAvailable) {
            prognosisPerPtu = prognosis.get(0).getPtus().stream().collect(
                    Collectors.toMap(ptuPrognosisDto -> ptuPrognosisDto.getPtuIndex().intValue(), PtuPrognosisDto::getPower));
        } else {
            prognosisPerPtu = new HashMap<>();
            for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
                prognosisPerPtu.put(ptuIndex, BigInteger.ZERO);
            }
        }

        // calculate total power per ptu in forecast before re-optimize
        Map<Integer, BigInteger> summedPowerBeforeReOptimize = new HashMap<>();
        for (ConnectionPortfolioDto connectionDto : (List<ConnectionPortfolioDto>) inContext
                .getValue(IN.CONNECTION_PORTFOLIO_IN.name())) {
            for (int ptuIndex = 1; ptuIndex <= NR_OF_PTUS; ptuIndex++) {
                Map<Integer, PowerContainerDto> powerContainerPerPtu = PowerContainerDtoUtil
                        .sumUdisPerPtu(connectionDto.getUdis(), 120,
                                PtuUtil.getNumberOfPtusPerDay(DateTimeUtil.getCurrentDate().plusDays(1), 120));

                BigInteger forecastPower = powerContainerPerPtu.get(ptuIndex).getForecast().getAverageConsumption()
                        .subtract(powerContainerPerPtu.get(ptuIndex).getForecast().getAverageProduction());

                summedPowerBeforeReOptimize
                        .put(ptuIndex, summedPowerBeforeReOptimize.getOrDefault(ptuIndex, BigInteger.ZERO).add(forecastPower));
            }
        }

        WorkflowContext result = stub.invoke(inContext);

        if (prognosisAvailable) {
            assertionsWithPrognosis(prognosisPerPtu, inContext, summedPowerBeforeReOptimize, result);
        } else {
            assertionsWithEmptyPrognosis(result);
        }
    }

    private void assertionsWithEmptyPrognosis(WorkflowContext result) {
        Assert.assertEquals(0, result.get(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name(), List.class).size());
    }

    private void assertionsWithPrognosis(Map<Integer, BigInteger> prognosisPerPtu, WorkflowContext inContext,
            Map<Integer, BigInteger> summedPowerBeforeReOptimize, WorkflowContext result) {
        Assert.assertEquals(inContext.get(IN.CONNECTION_PORTFOLIO_IN.name(), List.class).size(),
                result.get(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name(), List.class).size());

        // sum all the flex power ordered
        Map<Integer, BigInteger> summedOrderedPower = new HashMap<>();
        for (FlexOrderDto flexOrderDto : (List<FlexOrderDto>) inContext.getValue(IN.RECEIVED_FLEXORDER_LIST.name())) {
            for (PtuFlexOrderDto ptuFlexOrderDto : flexOrderDto.getPtus()) {
                int ptuIndex = ptuFlexOrderDto.getPtuIndex().intValue();
                summedOrderedPower
                        .put(ptuIndex, summedOrderedPower.getOrDefault(ptuIndex, BigInteger.ZERO).add(ptuFlexOrderDto.getPower()));
            }
        }

        // calculate total power per ptu in forecast after re-optimize
        Map<Integer, BigInteger> summedPowerAfterReOptimize = new HashMap<>();
        for (ConnectionPortfolioDto connectionDto : (List<ConnectionPortfolioDto>) result
                .getValue(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name())) {
            for (int ptuIndex = 1; ptuIndex <= NR_OF_PTUS; ptuIndex++) {
                Map<Integer, PowerContainerDto> powerContainerPerPtu = PowerContainerDtoUtil
                        .sumUdisPerPtu(connectionDto.getUdis(), 120,
                                PtuUtil.getNumberOfPtusPerDay(DateTimeUtil.getCurrentDate().plusDays(1), 120));

                BigInteger forecastPower = powerContainerPerPtu.get(ptuIndex).getForecast().getAverageConsumption()
                        .subtract(powerContainerPerPtu.get(ptuIndex).getForecast().getAverageProduction());

                summedPowerAfterReOptimize
                        .put(ptuIndex, summedPowerAfterReOptimize.getOrDefault(ptuIndex, BigInteger.ZERO).add(forecastPower));
            }
        }

        // ptu index 1, 2 and 7 are capped, so all available potential flex will be used for these ptu's
        Integer[] expectedForecast = { -41, 97, 200, 300, 400, 740, 454, 700, 1235, 1400, 1565, 1730 };

        // Assert that the ordered power is divided amongst all the connections
        // summedPowerAfterReOptimize = Ordered + prognosis
        for (int ptuIndex = 1; ptuIndex <= NR_OF_PTUS; ptuIndex++) {
            // Due to rounding issues, the result may slightly deviate (max deviation = 5 due to 5 connections used in this unit
            // test).

            int diff = summedPowerAfterReOptimize.get(ptuIndex).subtract(BigInteger.valueOf(expectedForecast[ptuIndex - 1]))
                    .intValue();
            Assert.assertTrue("Forecast power consumption after re-optimize for ptu index " + ptuIndex + " is expected to be "
                            + expectedForecast[ptuIndex - 1] + " (is " + summedPowerAfterReOptimize.get(ptuIndex) + ")",
                        diff >= -5 && diff <= 5);
        }

        // check returned device messages
        List<DeviceMessageDto> deviceMessageDtos = result
                .get(ReOptimizePortfolioStepParameter.OUT.DEVICE_MESSAGES_OUT.name(), List.class);
        Assert.assertNotNull(deviceMessageDtos);

        Map<Integer, BigInteger> targetPerPtu = new HashMap<>();
        for (int ptuIndex = 1; ptuIndex <= NR_OF_PTUS; ptuIndex++) {
            BigInteger target = prognosisPerPtu.get(ptuIndex).add(summedOrderedPower.get(ptuIndex))
                    .subtract(summedPowerBeforeReOptimize.get(ptuIndex));
            target = new BigDecimal(target).multiply(BigDecimal.valueOf(1.05)).toBigInteger();
            targetPerPtu.put(ptuIndex, target);
        }

        // sum all the reduced power per ptu
        Map<Integer, BigInteger> summedReducedPowerPerPtu = new HashMap<>();
        deviceMessageDtos.forEach(deviceMessageDto -> deviceMessageDto.getReduceRequestDtos().forEach(reduceRequestDto -> {
            int ptuIndex = reduceRequestDto.getEndDTU().divide(BigInteger.valueOf(2)).intValue();
            summedReducedPowerPerPtu.put(ptuIndex,
                    summedReducedPowerPerPtu.getOrDefault(ptuIndex, BigInteger.ZERO).add(reduceRequestDto.getPower()));
            Assert.assertTrue("ReduceRequest should have type PRODUCTION for target > 0 and CONSUMPTION for target < 0",
                    (targetPerPtu.get(ptuIndex).compareTo(BigInteger.ZERO) < 0) ?
                            reduceRequestDto.getConsumptionProductionType() == ConsumptionProductionTypeDto.CONSUMPTION :
                            reduceRequestDto.getConsumptionProductionType() == ConsumptionProductionTypeDto.PRODUCTION);
        }));

        // make sure the device requests reduced enough power
        summedReducedPowerPerPtu.forEach((ptuIndex, reducedPower) -> {
            LOGGER.debug("ptu index {}: {} (target {})", ptuIndex, reducedPower, targetPerPtu.get(ptuIndex));
            Assert.assertTrue("Summed reduced powers in request (" + reducedPower + ") should be more or equal to the target (" +
                            targetPerPtu.get(ptuIndex) + "), or maximum 300",
                    reducedPower.abs().compareTo(targetPerPtu.get(ptuIndex)) >= 0 || reducedPower.abs()
                            .equals(BigInteger.valueOf(300)));
        });
    }

    private WorkflowContext buildContext(boolean prognosisAvailable) {
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(IN.PTU_DURATION.name(), (24 * 60) / NR_OF_PTUS);
        context.setValue(IN.PTU_DATE.name(), DateTimeUtil.getCurrentDate().plusDays(1));
        context.setValue(IN.CURRENT_PTU_INDEX.name(), 1);
        context.setValue(IN.CONNECTION_PORTFOLIO_IN.name(), buildPortfolio());
        context.setValue(IN.UDI_EVENTS.name(), buildUdiEvents());
        context.setValue(IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(), buildMap());
        context.setValue(IN.RECEIVED_FLEXORDER_LIST.name(), buildOrders());
        if (prognosisAvailable) {
            context.setValue(IN.LATEST_A_PLAN_DTO_LIST.name(), buildAPlans());
            context.setValue(IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), buildDPrognosis());
        } else {
            context.setValue(IN.LATEST_A_PLAN_DTO_LIST.name(), new ArrayList<>());
            context.setValue(IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), new ArrayList<>());
        }

        return context;
    }

    private List<UdiEventDto> buildUdiEvents() {
        List<UdiEventDto> udiEvents = new ArrayList<>();

        for (int count = 1; count <= 5; count++) {
            UdiEventDto udiEvent = new UdiEventDto();
            udiEvent.setUdiEndpoint("endpoint:" + count);
            udiEvent.setPeriod(DateTimeUtil.getCurrentDate().plusDays(1));

            // valid for the whole day
            udiEvent.setStartDtu(1);
            udiEvent.setEndDtu(96 * 2 + 1);

            // create a reduce capability of type CONSUMPTION
            ReduceCapabilityDto deviceCapability = new ReduceCapabilityDto();
            deviceCapability.setPowerStep(BigInteger.valueOf(10).negate());
            deviceCapability.setMinPower(deviceCapability.getPowerStep().multiply(BigInteger.valueOf(count * 2)));
            deviceCapability.setConsumptionProductionType(ConsumptionProductionTypeDto.CONSUMPTION);
            udiEvent.getDeviceCapabilities().add(deviceCapability);

            // create a reduce capability of type PRODUCTION
            deviceCapability = new ReduceCapabilityDto();
            deviceCapability.setPowerStep(BigInteger.valueOf(10).negate());
            deviceCapability.setMinPower(deviceCapability.getPowerStep().multiply(BigInteger.valueOf(count * 2)));
            deviceCapability.setConsumptionProductionType(ConsumptionProductionTypeDto.PRODUCTION);
            udiEvent.getDeviceCapabilities().add(deviceCapability);

            udiEvents.add(udiEvent);
        }

        return udiEvents;
    }

    private List<PrognosisDto> buildDPrognosis() {
        List<PrognosisDto> prognosis = new ArrayList<>();

        PrognosisDto prognosisDto = new PrognosisDto();
        prognosisDto.setConnectionGroupEntityAddress("EAN.CG.1");
        prognosisDto.setPeriod(DateTimeUtil.getCurrentDate().plusDays(1));

        IntStream.rangeClosed(1, NR_OF_PTUS).forEach(ptuIndex -> {
            PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
            ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
            ptuPrognosisDto.setPower(BigInteger.valueOf(ptuIndex * 200));
            prognosisDto.getPtus().add(ptuPrognosisDto);
        });

        prognosis.add(prognosisDto);

        return prognosis;
    }

    private List<PrognosisDto> buildAPlans() {
        return new ArrayList<>();
    }

    private List<FlexOrderDto> buildOrders() {
        List<FlexOrderDto> flexOrderDtos = new ArrayList<>();

        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setFlexOfferSequenceNumber(1l);
        flexOrderDto.setAcknowledgementStatus(AcknowledgementStatusDto.ACCEPTED);
        flexOrderDto.setConnectionGroupEntityAddress("EAN.CG.1");
        flexOrderDto.setParticipantDomain("dso.usef-example.com");
        flexOrderDto.setPeriod(DateTimeUtil.getCurrentDate().plusDays(1));
        flexOrderDto.getPtus().addAll(buildPtus());

        flexOrderDtos.add(flexOrderDto);

        return flexOrderDtos;
    }

    private List<PtuFlexOrderDto> buildPtus() {
        Integer[] orderedPower = { -195, -300, -400, -500, -600, -100, -1200, -900, -400, -500, 100, 100 };

        List<PtuFlexOrderDto> ptuFlexOrderDtos = new ArrayList<>();

        IntStream.rangeClosed(1, NR_OF_PTUS).forEach(ptuIndex -> {
            PtuFlexOrderDto ptuFlexOrderDto = new PtuFlexOrderDto();
            ptuFlexOrderDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
            ptuFlexOrderDto.setPower(BigInteger.valueOf(orderedPower[ptuIndex - 1]));

            ptuFlexOrderDtos.add(ptuFlexOrderDto);
        });

        return ptuFlexOrderDtos;
    }

    private Map<String, List<String>> buildMap() {
        Map<String, List<String>> connectionGroupsToConnectionMap = new HashMap<>();

        connectionGroupsToConnectionMap.put("EAN.CG.1", Arrays.asList("EAN.1", "EAN.2", "EAN.3", "EAN.4", "EAN.5"));

        return connectionGroupsToConnectionMap;
    }

    private List<ConnectionPortfolioDto> buildPortfolio() {
        List<ConnectionPortfolioDto> portfolioDTOs = new ArrayList<>();

        for (int connectionCount = 1; connectionCount <= 5; connectionCount++) {
            final int finalConnectionCount = connectionCount;

            ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto("EAN." + connectionCount);

            UdiPortfolioDto udiPortfolio1 = new UdiPortfolioDto("endpoint:" + finalConnectionCount, 60, "");

            IntStream.rangeClosed(1, 24).forEach(dtuIndex -> {
                PowerContainerDto powerContainerDto = new PowerContainerDto(DateTimeUtil.getCurrentDate().plusDays(1), dtuIndex);

                BigInteger consumption = BigInteger
                        .valueOf(((long) (Math.floor((dtuIndex + 1) / 2)) * 11 * finalConnectionCount) - 50);

                if (consumption.compareTo(BigInteger.ZERO) < 0) {
                    powerContainerDto.getForecast().setAverageConsumption(BigInteger.ZERO);
                    powerContainerDto.getForecast().setAverageProduction(consumption.abs());
                } else {
                    powerContainerDto.getForecast().setAverageConsumption(consumption);
                    powerContainerDto.getForecast().setAverageProduction(BigInteger.ZERO);
                }

                powerContainerDto.getForecast().setPotentialFlexConsumption(
                        powerContainerDto.getForecast().getAverageConsumption().divide(BigInteger.valueOf(2)).negate());
                powerContainerDto.getForecast().setPotentialFlexProduction(
                        powerContainerDto.getForecast().getAverageProduction().divide(BigInteger.valueOf(2)).negate());

                udiPortfolio1.getUdiPowerPerDTU().put(dtuIndex, powerContainerDto);
            });

            connectionPortfolioDTO.getUdis().add(udiPortfolio1);

            portfolioDTOs.add(connectionPortfolioDTO);
        }

        return portfolioDTOs;
    }
}
