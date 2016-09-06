/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.agr.pbc;

import static org.junit.Assert.assertEquals;

import info.usef.agr.workflow.settlement.receive.AgrReceiveSettlementMessageWorkflowParameter;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.DispositionAcceptedDisputedDto;
import info.usef.core.workflow.dto.FlexOrderDto;
import info.usef.core.workflow.dto.FlexOrderSettlementDto;
import info.usef.core.workflow.dto.PtuSettlementDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class in charge of the unit tests related to the {@link AgrValidateSettlementItems} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrValidateSettlementItemsTest {

    private AgrValidateSettlementItems agrValidateSettlementItems;

    @Before
    public void init() {
        agrValidateSettlementItems = new AgrValidateSettlementItems();
    }

    @Test
    public void testInvokeSucceeds() {
        final LocalDate period = new LocalDate(2015, 10, 21);
        WorkflowContext context = buildWorkflowContext(period);

        context = agrValidateSettlementItems.invoke(context);
        // verify success
        assertEquals(DispositionAcceptedDisputedDto.ACCEPTED, context.getValue(AgrReceiveSettlementMessageWorkflowParameter.OUT.FLEX_ORDER_SETTLEMENT_DISPOSITION.name()));

    }

    @Test
    public void testDifferentFlexOrderSequenceReturnsDisputedSettlement() {
        final LocalDate period = new LocalDate(2015, 10, 21);
        WorkflowContext context = buildWorkflowContext(period);
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.IN.ORDER_REFERENCE.name(), 2222L);
        context = agrValidateSettlementItems.invoke(context);
        assertEquals(DispositionAcceptedDisputedDto.DISPUTED, context.getValue(AgrReceiveSettlementMessageWorkflowParameter.OUT.FLEX_ORDER_SETTLEMENT_DISPOSITION.name()));
    }

    @Test
    public void testDifferentPrognosisPowerReturnsDisputedSettlement() {
        final LocalDate period = new LocalDate(2015, 10, 21);
        WorkflowContext context = buildWorkflowContext(period);
        FlexOrderSettlementDto flexOrderSettlementDto = context
                .get(AgrReceiveSettlementMessageWorkflowParameter.IN.RECEIVED_FLEX_ORDER_SETTLEMENT.name(), FlexOrderSettlementDto.class);
        flexOrderSettlementDto.getPtuSettlementDtos().get(0).setPrognosisPower(BigInteger.valueOf(200L)); // 200 >> 100
        context = agrValidateSettlementItems.invoke(context);

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
