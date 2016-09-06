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

package energy.usef.dso.workflow.step;

import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.settlement.initiate.RequestPenaltyDataParameter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class for {@link DsoRequestPenaltyDataStub}
 */
@RunWith(PowerMockRunner.class)
public class DsoRequestPenaltyDataStubTest {

    private static final int PTUS_PER_DAY = 96;
    @Mock
    private PbcFeederService pbcFeederService;

    private DsoRequestPenaltyDataStub dsoRequestPenaltyDataStub;

    @Before
    public void init() {
        dsoRequestPenaltyDataStub = new DsoRequestPenaltyDataStub();

        Whitebox.setInternalState(dsoRequestPenaltyDataStub, pbcFeederService);

        // Mocked PBC Feeder data will contain a APX Price of 10 * PTU's passed since the first of the month. Eg, Apx price for 1st
        // ptu for 2nd of the month will be 97 * 10.
        Mockito.when(pbcFeederService.retrieveApxPrices(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
                .then(call -> IntStream.rangeClosed(1, PTUS_PER_DAY * 31).mapToObj(Integer::valueOf)
                        .collect(Collectors.toMap(Function.identity(), i -> new BigDecimal("" + (i * 10)))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithNoDeficiency() throws Exception {
        WorkflowContext context = dsoRequestPenaltyDataStub.invoke(buildWorkflowContext(new BigInteger(String.valueOf(0))));
        SettlementDto settlementDto = context.get(RequestPenaltyDataParameter.OUT.UPDATED_SETTLEMENT_DTO.name(),
                SettlementDto.class);
        for (FlexOrderSettlementDto flexOrderSettlementDto : settlementDto.getFlexOrderSettlementDtos()) {
            for (PtuSettlementDto ptuSettlementDto : flexOrderSettlementDto.getPtuSettlementDtos()) {
                Assert.assertEquals(BigDecimal.valueOf(0), ptuSettlementDto.getPenalty());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithDeficiency() throws Exception {
        WorkflowContext context = dsoRequestPenaltyDataStub.invoke(buildWorkflowContext(new BigInteger(String.valueOf(100))));
        SettlementDto settlementDto = context
                .get(RequestPenaltyDataParameter.OUT.UPDATED_SETTLEMENT_DTO.name(), SettlementDto.class);
        for (FlexOrderSettlementDto flexOrderSettlementDto : settlementDto.getFlexOrderSettlementDtos()) {
            for (PtuSettlementDto ptuSettlementDto : flexOrderSettlementDto.getPtuSettlementDtos()) {
                int numberOfPtusPassed = numberOfPtusBetween(DateTimeUtil.getCurrentDate().withDayOfMonth(1),
                        flexOrderSettlementDto.getPeriod(), 15, ptuSettlementDto.getPtuIndex().intValue());

                // penalty = (apxPrice / (1000000 * (60 / <ptuDuration>))) * <powerDeficiency>
                Assert.assertEquals(BigDecimal.valueOf(numberOfPtusPassed * 10).divide(BigDecimal.valueOf(1000000 * 4))
                        .multiply(new BigDecimal(ptuSettlementDto.getPowerDeficiency())), ptuSettlementDto.getPenalty());
            }
        }
    }

    private WorkflowContext buildWorkflowContext(BigInteger deficiency) {
        DefaultWorkflowContext context = new DefaultWorkflowContext();
        context.setValue(RequestPenaltyDataParameter.IN.SETTLEMENT_DTO.name(), buildSettlementDto(deficiency));
        context.setValue(RequestPenaltyDataParameter.IN.PTU_DURATION.name(), 15);
        return context;
    }

    private SettlementDto buildSettlementDto(BigInteger deficiency) {
        SettlementDto settlementDto = new SettlementDto(DateTimeUtil.getCurrentDate().withDayOfMonth(1),
                DateTimeUtil.getCurrentDate().withDayOfMonth(1).plusMonths(1).minusDays(1));
        settlementDto.getFlexOrderSettlementDtos().add(buildFlexOrderSettlementDto(new LocalDate(), deficiency));
        return settlementDto;
    }

    private FlexOrderSettlementDto buildFlexOrderSettlementDto(LocalDate period, BigInteger deficiency) {
        FlexOrderSettlementDto flexOrderSettlementDto = new FlexOrderSettlementDto(period);
        IntStream.rangeClosed(1, 96).mapToObj(index -> buildPtuSettlementDto(index, deficiency))
                .forEach(ptuSettlementDto -> flexOrderSettlementDto.getPtuSettlementDtos().add(ptuSettlementDto));
        return flexOrderSettlementDto;
    }

    private PtuSettlementDto buildPtuSettlementDto(Integer index, BigInteger deficiency) {
        PtuSettlementDto ptuSettlementDto = new PtuSettlementDto();
        ptuSettlementDto.setPowerDeficiency(deficiency);
        ptuSettlementDto.setPtuIndex(BigInteger.valueOf(index));
        return ptuSettlementDto;
    }

    private Integer numberOfPtusBetween(LocalDate startDate, LocalDate endDate, int ptuDuration, int ptuIndex) {
        Double ptusPassed = Math.floor(
                Minutes.minutesBetween(startDate.toLocalDateTime(LocalTime.MIDNIGHT), endDate.toLocalDateTime(LocalTime.MIDNIGHT))
                        .getMinutes() / ptuDuration) + ptuIndex;
        return ptusPassed.intValue();
    }

}
