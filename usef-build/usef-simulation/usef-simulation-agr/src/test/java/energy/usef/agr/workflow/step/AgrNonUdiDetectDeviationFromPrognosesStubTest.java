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
import energy.usef.agr.dto.ForecastPowerDataDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.workflow.operate.deviation.NonUdiDetectDeviationFromPrognosisStepParameter;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test to test the {@link AgrNonUdiDetectDeviationFromPrognosesStub}.
 */
@RunWith(PowerMockRunner.class)
public class AgrNonUdiDetectDeviationFromPrognosesStubTest {
    private static final int PTU_DURATION = 15; // minutes
    private static final LocalDate PERIOD = new LocalDate();

    private AgrNonUdiDetectDeviationFromPrognosesStub agrNonUdiDetectDeviationFromPrognosesStub;

    @Before
    public void init() {
        agrNonUdiDetectDeviationFromPrognosesStub = new AgrNonUdiDetectDeviationFromPrognosesStub();
    }

    @Test
    public void testInvoke() throws Exception {
        WorkflowContext workflowContext = new DefaultWorkflowContext();

        LocalDateTime timestamp = new LocalDateTime().withHourOfDay(0)
                .withMinuteOfHour(20).withSecondOfMinute(1).withMillisOfSecond(1);

        int currentPtuIndex = PtuUtil.getPtuIndex(timestamp, PTU_DURATION);
        int numberOfPtusPerDay = PtuUtil.getNumberOfPtusPerDay(timestamp.toLocalDate(), PTU_DURATION);

        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.PTU_DURATION.name(), PTU_DURATION);
        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.PERIOD.name(), timestamp.toLocalDate());
        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.CURRENT_PTU_INDEX.name(), currentPtuIndex);
        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.USEF_IDENTIFIER.name(), "brp1.usef-example.com");
        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.CONNECTION_PORTFOLIO_DTO.name(),
                buildConnectionPortfolioDtoList());
        workflowContext.setValue(NonUdiDetectDeviationFromPrognosisStepParameter.IN.LATEST_PROGNOSIS.name(),
                buildLatestPrognoses(numberOfPtusPerDay));

        workflowContext = agrNonUdiDetectDeviationFromPrognosesStub.invoke(workflowContext);
        @SuppressWarnings("unchecked")
        List<Integer> deviationIndexList = workflowContext
                .get(NonUdiDetectDeviationFromPrognosisStepParameter.OUT.DEVIATION_INDEX_LIST.name(), List.class);

        Assert.assertEquals(26, deviationIndexList.size());
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDtoList() {
        List<ConnectionPortfolioDto> connectionPortfolioDTOs = new ArrayList<>();

        IntStream.rangeClosed(1, 9).forEach(i -> {
            ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto("ean.0000" + i);
            IntStream.rangeClosed(1, 96).forEach(ptuIndex -> {
                PowerContainerDto powerContainerDto = new PowerContainerDto(PERIOD, ptuIndex);
                ForecastPowerDataDto forecast = new ForecastPowerDataDto();

                forecast.setAverageConsumption(BigInteger.valueOf(ptuIndex));
                forecast.setAverageProduction(BigInteger.valueOf(-38));
                forecast.setUncontrolledLoad(BigInteger.valueOf((i * 100) + ptuIndex));
                powerContainerDto.setForecast(forecast);
                connectionPortfolioDTO.getConnectionPowerPerPTU().put(ptuIndex, powerContainerDto);
            });
            connectionPortfolioDTOs.add(connectionPortfolioDTO);
        });

        return connectionPortfolioDTOs;
    }

    private PrognosisDto buildLatestPrognoses(int numberOfPtusPerDay) {
        PrognosisDto prognosisDto = new PrognosisDto();

        for (int i = 0; i <= numberOfPtusPerDay - 1; i++) {
            PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
            ptuPrognosisDto.setPower(BigInteger.valueOf(4500 + 15 * i));
            prognosisDto.getPtus().add(ptuPrognosisDto);
        }

        return prognosisDto;
    }
}
