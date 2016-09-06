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
import java.util.HashMap;
import java.util.Map;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.usef.core.util.PtuUtil;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.FlexOrderSettlementDto;
import info.usef.core.workflow.dto.PtuSettlementDto;
import info.usef.core.workflow.dto.SettlementDto;
import info.usef.dso.workflow.settlement.initiate.RequestPenaltyDataParameter.IN;
import info.usef.dso.workflow.settlement.initiate.RequestPenaltyDataParameter.OUT;

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
public class DsoRequestPenaltyData implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoRequestPenaltyData.class);
    public static final int MINUTES_PER_HOUR = 60;
    public static final int DAYS_PER_MONTH = 31;
    public static final int HOURS_PER_DAY = 24;
    public static final int MEGA = 1000000;

    /**
     * {@inheritDoc}
     */
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.info("Started assigning penalties to settlements with arguments: {}", context);

        SettlementDto settlementDto = context.get(IN.SETTLEMENT_DTO.name(), SettlementDto.class);
        Integer ptuDuration = context.get(IN.PTU_DURATION.name(), Integer.class);

        // determine the first day of the month for settlement
        LocalDate firstDayOfMonth = settlementDto.getStartDate();

        Map<Integer, BigDecimal> apxPricesMap = retrieveApxPrices(firstDayOfMonth, 1,
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

    public Map<Integer, BigDecimal> retrieveApxPrices(LocalDate date, int startPtuIndex, int amountOfPtus) {
        LOGGER.info("Stubbing {} APX prices starting {} PTU {}", amountOfPtus, date, startPtuIndex);

        Map<Integer, BigDecimal> apxPrices = new HashMap<>();

        // if this method is used, the ptu index will be increased every ptu over multiple days
        for (int i = 0; i < amountOfPtus; i++) {
            apxPrices.put(i + 1, BigDecimal.ZERO);
        }

        return apxPrices;
    }
}
