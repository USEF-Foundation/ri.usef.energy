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

import energy.usef.agr.workflow.settlement.receive.AgrReceiveSettlementMessageWorkflowParameter;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.DispositionAcceptedDisputedDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub implementation of a the PBC handling received {@link FlexOrderSettlementDto}.
 */
public class AgrValidateSettlementItemsStub implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrValidateSettlementItemsStub.class);

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
