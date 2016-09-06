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

package energy.usef.brp.workflow.step;

import energy.usef.brp.workflow.settlement.initiate.BrpInitiateSettlementParameter;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.ConnectionMeterDataDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.MeterDataDto;
import energy.usef.core.workflow.dto.MeterDataSetDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;
import energy.usef.core.workflow.dto.PtuMeterDataDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link BrpInitiateSettlementStub}.
 */
public class BrpInitiateSettlementStubTest {

    private static final LocalDate START_DATE = new LocalDate(2015, 10, 1);
    private static final LocalDate END_DATE = new LocalDate(2015, 10, 30);
    private static final String CONGESTION_POINT = "agr1.usef-example.com";
    private static final int PTU_DURATION = 120;
    private static final int PTUS_PER_DAY = 12;
    private static final String AGR_DOMAIN = "agr1.usef-example.com";
    private BrpInitiateSettlementStub brpInitiateSettlementStub;

    @Before
    public void setUp() throws Exception {
        brpInitiateSettlementStub = new BrpInitiateSettlementStub();
    }

    @Test
    public void testInvokeWithSmartMeterData() throws Exception {
        WorkflowContext outContext = brpInitiateSettlementStub.invoke(buildWorkflowContextWithMeterData());
        SettlementDto settlementDto = outContext
                .get(CoreInitiateSettlementParameter.OUT.SETTLEMENT_DTO.name(), SettlementDto.class);
        Assert.assertNotNull(settlementDto);
        Assert.assertEquals(1, settlementDto.getFlexOrderSettlementDtos().size());
        FlexOrderSettlementDto flexOrderSettlementDto = settlementDto.getFlexOrderSettlementDtos().get(0);
        flexOrderSettlementDto.getPtuSettlementDtos().stream().forEach(ptuSettlementDto -> {
            Assert.assertEquals(BigInteger.valueOf(1500), ptuSettlementDto.getActualPower());
            Assert.assertEquals(BigInteger.valueOf(1000), ptuSettlementDto.getPrognosisPower());
            Assert.assertEquals(BigInteger.valueOf(-1000), ptuSettlementDto.getOrderedFlexPower());
            Assert.assertEquals(BigInteger.valueOf(500), ptuSettlementDto.getPowerDeficiency());
            Assert.assertEquals(BigInteger.valueOf(-500), ptuSettlementDto.getDeliveredFlexPower());
        });
    }

    private WorkflowContext buildWorkflowContextWithMeterData() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(CoreInitiateSettlementParameter.IN.PTU_DURATION.name(), PTU_DURATION);
        context.setValue(CoreInitiateSettlementParameter.IN.START_DATE.name(), START_DATE);
        context.setValue(CoreInitiateSettlementParameter.IN.END_DATE.name(), END_DATE);
        context.setValue(CoreInitiateSettlementParameter.IN.FLEX_ORDER_DTO_LIST.name(), buildFlexOrderDtos());
        context.setValue(CoreInitiateSettlementParameter.IN.PROGNOSIS_DTO_LIST.name(), buildPrognosisDtos());
        context.setValue(BrpInitiateSettlementParameter.IN.SMART_METER_DATA.name(), buildSmartMeterData());
        return context;
    }

    private List<FlexOrderDto> buildFlexOrderDtos() {
        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setSequenceNumber(4L);
        flexOrderDto.setConnectionGroupEntityAddress(CONGESTION_POINT);
        flexOrderDto.setParticipantDomain(AGR_DOMAIN);
        flexOrderDto.setPeriod(START_DATE);
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> new PtuFlexOrderDto(BigInteger.valueOf(index), BigInteger.valueOf(-1000L), BigDecimal.TEN))
                .forEach(ptu -> flexOrderDto.getPtus().add(ptu));
        return Collections.singletonList(flexOrderDto);
    }

    private List<PrognosisDto> buildPrognosisDtos() {
        PrognosisDto prognosisDto = new PrognosisDto();
        prognosisDto.setPeriod(START_DATE);
        prognosisDto.setConnectionGroupEntityAddress(CONGESTION_POINT);
        prognosisDto.setParticipantDomain(AGR_DOMAIN);
        prognosisDto.setSequenceNumber(1L);
        IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
            PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
            ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(index));
            ptuPrognosisDto.setPower(BigInteger.valueOf(1000));
            return ptuPrognosisDto;
        }).forEach(ptu -> prognosisDto.getPtus().add(ptu));
        return Collections.singletonList(prognosisDto);
    }

    private List<MeterDataSetDto> buildSmartMeterData() {
        MeterDataSetDto meterDataSetDto = new MeterDataSetDto(CONGESTION_POINT);
        MeterDataDto meterDataDto = new MeterDataDto();
        meterDataDto.setPeriod(START_DATE);
        ConnectionMeterDataDto connectionMeterDataDto = new ConnectionMeterDataDto();
        connectionMeterDataDto.setAgrDomain(AGR_DOMAIN);
        connectionMeterDataDto.setEntityCount(BigInteger.valueOf(2));
        IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
            PtuMeterDataDto ptuMeterDataDto = new PtuMeterDataDto(index);
            ptuMeterDataDto.setPower(BigInteger.valueOf(1500));
            return ptuMeterDataDto;
        }).forEach(ptu -> connectionMeterDataDto.getPtuMeterDataDtos().add(ptu));
        meterDataDto.getConnectionMeterDataDtos().add(connectionMeterDataDto);
        meterDataSetDto.getMeterDataDtos().add(meterDataDto);
        return Collections.singletonList(meterDataSetDto);
    }
}
