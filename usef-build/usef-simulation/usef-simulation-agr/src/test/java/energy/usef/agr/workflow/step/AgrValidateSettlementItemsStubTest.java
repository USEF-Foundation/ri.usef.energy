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

import static org.junit.Assert.assertEquals;

import energy.usef.agr.workflow.settlement.receive.AgrReceiveSettlementMessageWorkflowParameter;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.DispositionAcceptedDisputedDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class in charge of the unit tests related to the {@link AgrControlActiveDemandSupplyStub} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrValidateSettlementItemsStubTest {

    private AgrValidateSettlementItemsStub agrValidateSettlementItemsStub;

    @Before
    public void init() {
        agrValidateSettlementItemsStub = new AgrValidateSettlementItemsStub();
    }

    @Test
    public void testInvokeSucceeds() {
        final LocalDate period = new LocalDate(2015, 10, 21);
        WorkflowContext context = buildWorkflowContext(period);

        context = agrValidateSettlementItemsStub.invoke(context);
        // verify success
        assertEquals(DispositionAcceptedDisputedDto.ACCEPTED, context.getValue(AgrReceiveSettlementMessageWorkflowParameter.OUT.FLEX_ORDER_SETTLEMENT_DISPOSITION.name()));

    }

    @Test
    public void testDifferentFlexOrderSequenceReturnsDisputedSettlement() {
        final LocalDate period = new LocalDate(2015, 10, 21);
        WorkflowContext context = buildWorkflowContext(period);
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.IN.ORDER_REFERENCE.name(), 2222L);
        context = agrValidateSettlementItemsStub.invoke(context);
        assertEquals(DispositionAcceptedDisputedDto.DISPUTED, context.getValue(AgrReceiveSettlementMessageWorkflowParameter.OUT.FLEX_ORDER_SETTLEMENT_DISPOSITION.name()));
    }

    @Test
    public void testDifferentPrognosisPowerReturnsDisputedSettlement() {
        final LocalDate period = new LocalDate(2015, 10, 21);
        WorkflowContext context = buildWorkflowContext(period);
        FlexOrderSettlementDto flexOrderSettlementDto = context
                .get(AgrReceiveSettlementMessageWorkflowParameter.IN.RECEIVED_FLEX_ORDER_SETTLEMENT.name(), FlexOrderSettlementDto.class);
        flexOrderSettlementDto.getPtuSettlementDtos().get(0).setPrognosisPower(BigInteger.valueOf(200L)); // 200 >> 100
        context = agrValidateSettlementItemsStub.invoke(context);
        assertEquals(DispositionAcceptedDisputedDto.DISPUTED, context.getValue(AgrReceiveSettlementMessageWorkflowParameter.OUT.FLEX_ORDER_SETTLEMENT_DISPOSITION.name()));
    }

    private WorkflowContext buildWorkflowContext(LocalDate period) {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.IN.ORDER_REFERENCE.name(), 1111L);
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.IN.RECEIVED_FLEX_ORDER_SETTLEMENT.name(), buildFlexOrderSettlement(period));
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.IN.PREPARED_FLEX_ORDER_SETTLEMENTS.name(), Collections.singletonList(buildFlexOrderSettlement(period)));
        return context;
    }

    private FlexOrderSettlementDto buildFlexOrderSettlement(LocalDate period) {
        FlexOrderSettlementDto flexOrderSettlementDto = new FlexOrderSettlementDto(period);
        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setPeriod(period);
        flexOrderDto.setConnectionGroupEntityAddress("ean.1111-1111");
        flexOrderDto.setSequenceNumber(1111L);
        flexOrderSettlementDto.setFlexOrder(flexOrderDto);
        flexOrderSettlementDto.getPtuSettlementDtos().add(buildPtuSettlementDto(BigInteger.ONE,
                BigInteger.valueOf(10),
                BigDecimal.valueOf(2.0),
                BigInteger.valueOf(100L),
                BigInteger.valueOf(100L)));
        return flexOrderSettlementDto;
    }

    private PtuSettlementDto buildPtuSettlementDto(BigInteger ptuIndex, BigInteger orderedFlexPower, BigDecimal price,
            BigInteger prognosisPower, BigInteger actualPower) {
        PtuSettlementDto ptuSettlementDto = new PtuSettlementDto();
        ptuSettlementDto.setPtuIndex(ptuIndex);
        ptuSettlementDto.setPrice(price);
        ptuSettlementDto.setPrognosisPower(prognosisPower);
        ptuSettlementDto.setOrderedFlexPower(orderedFlexPower);
        ptuSettlementDto.setActualPower(actualPower);
        return ptuSettlementDto;
    }

}
