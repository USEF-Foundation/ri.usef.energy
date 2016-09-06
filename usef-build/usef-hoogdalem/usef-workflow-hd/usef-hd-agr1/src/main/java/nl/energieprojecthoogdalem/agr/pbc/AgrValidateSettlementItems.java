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

import info.usef.agr.workflow.settlement.receive.AgrReceiveSettlementMessageWorkflowParameter;
import info.usef.core.exception.TechnicalException;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.DispositionAcceptedDisputedDto;
import info.usef.core.workflow.dto.FlexOrderSettlementDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a the PBC handling received {@link FlexOrderSettlementDto}.
 */
public class AgrValidateSettlementItems implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrValidateSettlementItems.class);

    // A delta value of 1.0 prevents disputes.
    private static final BigDecimal DELTA = BigDecimal.valueOf(0.2);

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public WorkflowContext invoke(WorkflowContext context) {
        // 1. Basic check
        FlexOrderSettlementDto actualPreparedFlexSettlementOrder = checkReceivedSettlementIsPrepared(context);
        if (actualPreparedFlexSettlementOrder == null) {
            context.setValue(AgrReceiveSettlementMessageWorkflowParameter.OUT.FLEX_ORDER_SETTLEMENT_DISPOSITION.name(), DispositionAcceptedDisputedDto.DISPUTED);
            return context;
        }

        // 2. Check values for the flex
        FlexOrderSettlementDto receivedSettlementDto = (FlexOrderSettlementDto) context.getValue(
                AgrReceiveSettlementMessageWorkflowParameter.IN.RECEIVED_FLEX_ORDER_SETTLEMENT.name());
        Long orderSequence = receivedSettlementDto.getFlexOrder().getSequenceNumber();
        if (receivedSettlementDto.getPtuSettlementDtos().size() !=
                actualPreparedFlexSettlementOrder.getPtuSettlementDtos().size()) {
            throw new TechnicalException(
                    "The amount of PTUs in the received settlement order is different than the amount of PTUs in the prepared "
                            + "settlement.");
        }

        DispositionAcceptedDisputedDto resultingDisposition = DispositionAcceptedDisputedDto.ACCEPTED;
        for (int i = 0; i < receivedSettlementDto.getPtuSettlementDtos().size()
                && resultingDisposition == DispositionAcceptedDisputedDto.ACCEPTED; ++i) {
            resultingDisposition = checkSettlementDtoForDisputeReasons(receivedSettlementDto, orderSequence,
                    actualPreparedFlexSettlementOrder, resultingDisposition, i);

        }
        context.setValue(AgrReceiveSettlementMessageWorkflowParameter.OUT.FLEX_ORDER_SETTLEMENT_DISPOSITION.name(), resultingDisposition);
        return context;
    }

    private DispositionAcceptedDisputedDto checkSettlementDtoForDisputeReasons(FlexOrderSettlementDto receivedSettlementDto,
            Long orderSequence, FlexOrderSettlementDto preparedFlexOrderSettlement,
            DispositionAcceptedDisputedDto disposition, int i) {

        DispositionAcceptedDisputedDto resultingDisposition = disposition;

        if (detectBigIntegerDelta(preparedFlexOrderSettlement.getPtuSettlementDtos().get(i).getActualPower(),
                receivedSettlementDto.getPtuSettlementDtos().get(i).getActualPower(), DELTA)) {
            resultingDisposition = DispositionAcceptedDisputedDto.DISPUTED;
            LOGGER.warn("PBC says DISPUTE for order {} on PTU {}/{}: actual power is too different. AGR:{}, Counter-party: {}",
                    orderSequence, receivedSettlementDto.getPeriod(), i,
                    preparedFlexOrderSettlement.getPtuSettlementDtos().get(i).getActualPower(),
                    receivedSettlementDto.getPtuSettlementDtos().get(i).getActualPower());
        }

        if (detectBigIntegerDelta(preparedFlexOrderSettlement.getPtuSettlementDtos().get(i).getOrderedFlexPower(),
                receivedSettlementDto.getPtuSettlementDtos().get(i).getOrderedFlexPower(), DELTA)) {
            LOGGER.warn("PBC says DISPUTE for order {} on PTU {}/{}: flex power is too different. AGR:{}, Counter-party: {}",
                    orderSequence, receivedSettlementDto.getPeriod(), i,
                    preparedFlexOrderSettlement.getPtuSettlementDtos().get(i).getOrderedFlexPower(),
                    receivedSettlementDto.getPtuSettlementDtos().get(i).getOrderedFlexPower());
            resultingDisposition = DispositionAcceptedDisputedDto.DISPUTED;
        }

        if (detectBigIntegerDelta(preparedFlexOrderSettlement.getPtuSettlementDtos().get(i).getPrognosisPower(),
                receivedSettlementDto.getPtuSettlementDtos().get(i).getPrognosisPower(), DELTA)) {
            LOGGER.warn(
                    "PBC says DISPUTE for order {} on PTU {}/{}: prognosis power is too different. AGR:{}, Counter-party: {}",
                    orderSequence, receivedSettlementDto.getPeriod(), i,
                    preparedFlexOrderSettlement.getPtuSettlementDtos().get(i).getPrognosisPower(),
                    receivedSettlementDto.getPtuSettlementDtos().get(i).getPrognosisPower());
            resultingDisposition = DispositionAcceptedDisputedDto.DISPUTED;
        }

        if (detectBigDecimalDelta(preparedFlexOrderSettlement.getPtuSettlementDtos().get(i).getPrice(),
                receivedSettlementDto.getPtuSettlementDtos().get(i).getPrice(), DELTA)) {
            LOGGER.warn("PBC says DISPUTE for order {} on PTU {}/{}: price is too different. AGR:{}, Counter-party: {}",
                    orderSequence, receivedSettlementDto.getPeriod(), i,
                    preparedFlexOrderSettlement.getPtuSettlementDtos().get(i).getPrice(),
                    receivedSettlementDto.getPtuSettlementDtos().get(i).getPrice());
            resultingDisposition = DispositionAcceptedDisputedDto.DISPUTED;
        }
        return resultingDisposition;
    }

    @SuppressWarnings("unchecked")
    private FlexOrderSettlementDto checkReceivedSettlementIsPrepared(WorkflowContext context) {
        // loop over the prepared settlement to find at least one which matches the received flex order settlement.
        for (FlexOrderSettlementDto flexOrderSettlementDto : (List<FlexOrderSettlementDto>) context.getValue(
                AgrReceiveSettlementMessageWorkflowParameter.IN.PREPARED_FLEX_ORDER_SETTLEMENTS.name())) {
            if (flexOrderSettlementDto.getFlexOrder().getSequenceNumber()
                    .equals(context.get(AgrReceiveSettlementMessageWorkflowParameter.IN.ORDER_REFERENCE.name(), Long.class))) {
                LOGGER.info("PTU settlement were prepared for order {} (Counter-party: {})",
                        flexOrderSettlementDto.getFlexOrder().getSequenceNumber(),
                        flexOrderSettlementDto.getFlexOrder().getParticipantDomain());
                return flexOrderSettlementDto;
            }
        }
        LOGGER.warn("PTU settlement were NOT prepared for order {} (Counter-party: {})",
                context.getValue(AgrReceiveSettlementMessageWorkflowParameter.IN.ORDER_REFERENCE.name()),
                context.getValue(AgrReceiveSettlementMessageWorkflowParameter.IN.COUNTER_PARTY_ROLE.name()));
        return null;
    }

    private boolean detectBigDecimalDelta(BigDecimal number1, BigDecimal number2, BigDecimal maximumDelta) {
        BigDecimal value = maximumDelta.multiply(number1).abs();
        return number1.subtract(number2).abs().compareTo(value) >= 1;
    }

    private boolean detectBigIntegerDelta(BigInteger number1, BigInteger number2, BigDecimal maximumDelta) {
        return detectBigDecimalDelta(new BigDecimal(number1), new BigDecimal(number2), maximumDelta);
    }
}
