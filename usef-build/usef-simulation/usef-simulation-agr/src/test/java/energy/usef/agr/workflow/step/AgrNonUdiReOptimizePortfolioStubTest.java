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
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.AcknowledgementStatusDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

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

/**
 * Test class in charge of the unit tests related to the {@link AgrNonUdiReOptimizePortfolioStub} class.
 */
public class AgrNonUdiReOptimizePortfolioStubTest {
    private final static int NR_OF_PTUS = 12;

    private AgrNonUdiReOptimizePortfolioStub stub;

    @Before
    public void init() {
        stub = new AgrNonUdiReOptimizePortfolioStub();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvoke() throws Exception {
        WorkflowContext inContext = buildContext();

        List<PrognosisDto> prognosis = inContext.get(ReOptimizePortfolioStepParameter.IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), List.class);
        Map<Integer, BigInteger> prognosisPerPtu = prognosis.get(0).getPtus().stream()
                .collect(Collectors.toMap(ptuPrognosisDto -> ptuPrognosisDto.getPtuIndex().intValue(), PtuPrognosisDto::getPower));

        // calculate total power per ptu in forecast before re-optimize
        Map<Integer, BigInteger> summedPowerBeforeReOptimize = new HashMap<>();
        for (ConnectionPortfolioDto connectionDto : (List<ConnectionPortfolioDto>) inContext
                .getValue(ReOptimizePortfolioStepParameter.IN.CONNECTION_PORTFOLIO_IN.name())) {
            for (int ptuIndex = 1; ptuIndex <= NR_OF_PTUS; ptuIndex++) {
                if (summedPowerBeforeReOptimize.get(ptuIndex) == null) {
                    summedPowerBeforeReOptimize.put(ptuIndex, BigInteger.ZERO);
                }

                summedPowerBeforeReOptimize.put(ptuIndex, summedPowerBeforeReOptimize.get(ptuIndex)
                        .add(connectionDto.getConnectionPowerPerPTU().get(ptuIndex).getForecast().getAverageConsumption()));
            }
        }

        WorkflowContext result = stub.invoke(inContext);

        Assert.assertEquals(inContext.get(ReOptimizePortfolioStepParameter.IN.CONNECTION_PORTFOLIO_IN.name(), List.class).size(),
                result.get(ReOptimizePortfolioStepParameter.OUT_NON_UDI.CONNECTION_PORTFOLIO_OUT.name(), List.class).size());

        // sum all the flex power ordered
        Map<Integer, BigInteger> summedOrderedPower = new HashMap<>();
        for (FlexOrderDto flexOrderDto : (List<FlexOrderDto>) inContext.getValue(ReOptimizePortfolioStepParameter.IN.RECEIVED_FLEXORDER_LIST.name())) {
            for (PtuFlexOrderDto ptuFlexOrderDto : flexOrderDto.getPtus()) {
                int ptuIndex = ptuFlexOrderDto.getPtuIndex().intValue();

                if (summedOrderedPower.get(ptuIndex) == null) {
                    summedOrderedPower.put(ptuIndex, BigInteger.ZERO);
                }

                summedOrderedPower.put(ptuIndex, summedOrderedPower.get(ptuIndex).add(ptuFlexOrderDto.getPower()));
            }
        }

        // calculate total power per ptu in forecast after re-optimize
        Map<Integer, BigInteger> summedPowerAfterReOptimize = new HashMap<>();
        for (ConnectionPortfolioDto connectionDto : (List<ConnectionPortfolioDto>) result
                .getValue(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name())) {
            for (int ptuIndex = 1; ptuIndex <= NR_OF_PTUS; ptuIndex++) {
                if (summedPowerAfterReOptimize.get(ptuIndex) == null) {
                    summedPowerAfterReOptimize.put(ptuIndex, BigInteger.ZERO);
                }

                summedPowerAfterReOptimize.put(ptuIndex, summedPowerAfterReOptimize.get(ptuIndex)
                        .add(connectionDto.getConnectionPowerPerPTU().get(ptuIndex).getForecast().getAverageConsumption()));
            }
        }

        // Assert that the ordered power is divided amongst all the connections
        // summedPowerAfterReOptimize = Ordered + prognosis
        int targetLessThanZero = 0;
        for (int ptuIndex = 1; ptuIndex <= NR_OF_PTUS; ptuIndex++) {
            // Due to rounding issues, the result may slightly deviate (max deviation = 5 due to 5 connections used in this unit test).
            // Since we only reduce consumption, forecast is only changed for targets > 0
            BigInteger target = prognosisPerPtu.get(ptuIndex)
                    .add(summedOrderedPower.get(ptuIndex).subtract(summedPowerBeforeReOptimize.get(ptuIndex)));
            if (target.compareTo(BigInteger.ZERO) < 0) {
                targetLessThanZero++;
                int diff = summedPowerAfterReOptimize.get(ptuIndex)
                        .subtract(summedOrderedPower.get(ptuIndex).add(prognosisPerPtu.get(ptuIndex))).intValue();
                Assert.assertTrue("Forecast power consumption after re-optimize for ptu index " + ptuIndex + " is incorrect",
                        diff >= -5 && diff <= 5);
            } else {
                Assert.assertEquals("Forecast power consumption shouldn't be changed for target > 0 for ptu index " + ptuIndex,
                        summedPowerBeforeReOptimize.get(ptuIndex), summedPowerAfterReOptimize.get(ptuIndex));
            }
        }

        // Assert that there is at least one ptu targeted < 0
        Assert.assertTrue("There is no target < 0, this makes this unit test useless", targetLessThanZero > 0);
    }

    private WorkflowContext buildContext() {
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(ReOptimizePortfolioStepParameter.IN.PTU_DURATION.name(), (24 * 60) / NR_OF_PTUS);
        context.setValue(ReOptimizePortfolioStepParameter.IN.PTU_DATE.name(), DateTimeUtil.getCurrentDate().plusDays(1));
        context.setValue(ReOptimizePortfolioStepParameter.IN.CURRENT_PTU_INDEX.name(), 1);
        context.setValue(ReOptimizePortfolioStepParameter.IN.CONNECTION_PORTFOLIO_IN.name(), buildPortfolio());
        context.setValue(ReOptimizePortfolioStepParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(), buildMap());
        context.setValue(ReOptimizePortfolioStepParameter.IN.RECEIVED_FLEXORDER_LIST.name(), buildOrders());
        context.setValue(ReOptimizePortfolioStepParameter.IN.LATEST_A_PLAN_DTO_LIST.name(), buildAPlans());
        context.setValue(ReOptimizePortfolioStepParameter.IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), buildDPrognosis());

        return context;
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
        Integer[] orderedPower = { -195, -300, -400, -500, -600, -100, -200, -300, -400, -500, 100, 100 };

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

            IntStream.rangeClosed(1, NR_OF_PTUS).forEach(ptuIndex -> {
                PowerContainerDto powerContainerDto = new PowerContainerDto(DateTimeUtil.getCurrentDate().plusDays(1), ptuIndex);

                BigInteger consumption = BigInteger.valueOf((ptuIndex * 11 * finalConnectionCount) - 50);

                if (consumption.compareTo(BigInteger.ZERO) < 0) {
                    powerContainerDto.getForecast().setAverageConsumption(BigInteger.ZERO);
                    powerContainerDto.getForecast().setAverageProduction(BigInteger.ZERO);
                } else {
                    powerContainerDto.getForecast().setAverageConsumption(consumption);
                    powerContainerDto.getForecast().setAverageProduction(BigInteger.ZERO);
                }
                powerContainerDto.getForecast().setPotentialFlexConsumption(powerContainerDto.getForecast().
                        getAverageConsumption().negate());
                connectionPortfolioDTO.getConnectionPowerPerPTU().put(ptuIndex, powerContainerDto);
            });

            portfolioDTOs.add(connectionPortfolioDTO);
        }

        return portfolioDTOs;
    }

}
