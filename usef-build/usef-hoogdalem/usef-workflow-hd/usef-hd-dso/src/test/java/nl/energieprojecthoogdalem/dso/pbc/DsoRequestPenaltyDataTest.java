/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.dso.pbc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import info.usef.core.util.DateTimeUtil;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.FlexOrderSettlementDto;
import info.usef.core.workflow.dto.PtuSettlementDto;
import info.usef.core.workflow.dto.SettlementDto;
import info.usef.dso.workflow.settlement.initiate.RequestPenaltyDataParameter;

/**
 * Test class for {@link DsoRequestPenaltyData}
 */
@RunWith(PowerMockRunner.class)
public class DsoRequestPenaltyDataTest {

    private DsoRequestPenaltyData dsoRequestPenaltyData;

    @Before
    public void init() {
        dsoRequestPenaltyData = new DsoRequestPenaltyData();

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithNoDeficiency() throws Exception {
        WorkflowContext context = dsoRequestPenaltyData.invoke(buildWorkflowContext(new BigInteger(String.valueOf(0))));
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
        WorkflowContext context = dsoRequestPenaltyData.invoke(buildWorkflowContext(new BigInteger(String.valueOf(100))));
        SettlementDto settlementDto = context
                .get(RequestPenaltyDataParameter.OUT.UPDATED_SETTLEMENT_DTO.name(), SettlementDto.class);
        for (FlexOrderSettlementDto flexOrderSettlementDto : settlementDto.getFlexOrderSettlementDtos()) {
            for (PtuSettlementDto ptuSettlementDto : flexOrderSettlementDto.getPtuSettlementDtos()) {
                int numberOfPtusPassed = numberOfPtusBetween(DateTimeUtil.getCurrentDate().withDayOfMonth(1),
                        flexOrderSettlementDto.getPeriod(), 15, ptuSettlementDto.getPtuIndex().intValue());

                // penalty = (apxPrice / (1000000 * (60 / <ptuDuration>))) * <powerDeficiency>
                Assert.assertEquals(BigDecimal.ZERO, ptuSettlementDto.getPenalty());
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
