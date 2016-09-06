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

import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.settlement.initiate.RequestPenaltyDataParameter.IN;
import energy.usef.dso.workflow.settlement.initiate.RequestPenaltyDataParameter.OUT;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow step implementation for the Request penalty data of the 'Dso Initiate Settlement' workflow. This implementation expects
 * to find the following parameters as input:
 * <ul>
 * <li>SETTLEMENT_DTO: complete Settlement for a given period ({@link SettlementDto})</li>
 * <li>AGGREGATOR_DOMAIN: Aggregator domain({@link String})</li>
 * <li>PTU_DURATION: Number of minutes per PTU ({@link Integer})</li>
 * </ul>
 * This implementation must return the following parameters as input:
 * <ul>
 * <li>UPDATED_SETTLEMENT_DTO: complete Settlement for a given period, enriched with penalty data.</li>
 * </ul>
 */
public class DsoRequestPenaltyDataStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoRequestPenaltyDataStub.class);
    public static final int MINUTES_PER_HOUR = 60;
    public static final int DAYS_PER_MONTH = 31;
    public static final int HOURS_PER_DAY = 24;
    public static final int MEGA = 1000000;

    @Inject
    private PbcFeederService pbcFeederService;

    /**
     * {@inheritDoc}
     */
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("Started assigning penalties to settlements with arguments: {}", context);

        SettlementDto settlementDto = context.get(IN.SETTLEMENT_DTO.name(), SettlementDto.class);
        Integer ptuDuration = context.get(IN.PTU_DURATION.name(), Integer.class);

        // determine the first day of the month for settlement
        LocalDate firstDayOfMonth = settlementDto.getStartDate();

        // retrieve apx prices from PBC feeder for at least 31 days
        Map<Integer, BigDecimal> apxPricesMap = pbcFeederService.retrieveApxPrices(firstDayOfMonth, 1,
                (HOURS_PER_DAY * MINUTES_PER_HOUR / ptuDuration) * DAYS_PER_MONTH);

        for (FlexOrderSettlementDto flexOrderSettlementDto : settlementDto.getFlexOrderSettlementDtos()) {
            for (PtuSettlementDto ptuSettlementDto : flexOrderSettlementDto.getPtuSettlementDtos()) {
                if (!isPenalty(ptuSettlementDto)) {
                    ptuSettlementDto.setPenalty(BigDecimal.ZERO);
                    continue;
                }

                // calculate the index in the PBC feeder data map (winter- and summer time proof)
                Integer startIndex = PtuUtil
                        .numberOfPtusBetween(firstDayOfMonth, flexOrderSettlementDto.getPeriod(), 1,
                                ptuSettlementDto.getPtuIndex().intValue(), ptuDuration);

                // penalty is based on apx price from PBC feeder, price per MWh
                BigDecimal apxPrice = apxPricesMap.get(startIndex + 1);
                BigDecimal penalty = BigDecimal.ZERO;

                // if there is no penalty available, something is wrong with the pbc feeder
                if (apxPrice == null) {
                    LOGGER.error("Unable to fetch apx price from pbc feeder for ptuSettlementDto (date: {}, index "
                            + "(from start of month): {}", flexOrderSettlementDto.getPeriod(), startIndex);
                } else {
                    penalty = calculatePenalty(apxPrice, ptuDuration, ptuSettlementDto.getPowerDeficiency());
                }

                ptuSettlementDto.setPenalty(penalty);
            }
        }

        // store the updated ptu settlement list in the context
        context.setValue(OUT.UPDATED_SETTLEMENT_DTO.name(), settlementDto);

        LOGGER.info("Ended assigning penalties to settlements");
        return context;
    }

    private BigDecimal calculatePenalty(BigDecimal apxPrice, int ptuDuration, BigInteger powerDeficiency) {
        // penalty = (apxPrice / (1000000 * (60 / <ptuDuration>))) * <powerDeficiency>
        // APX price is in MWh, convert to Watts per PTU.
        return apxPrice.divide(BigDecimal.valueOf(MEGA * (MINUTES_PER_HOUR / ptuDuration)))
                .multiply(new BigDecimal(powerDeficiency)).abs();
    }

    /**
     * Returns true if there is a power deficiency for the ptuSettlement.
     *
     * @param ptuSettlementDto
     * @return
     */
    private boolean isPenalty(PtuSettlementDto ptuSettlementDto) {
        if (ptuSettlementDto.getPowerDeficiency().equals(BigInteger.ZERO)) {
            return false;
        }
        // Flex Power Deficiency != 0 => Penalty!
        return true;
    }

}
