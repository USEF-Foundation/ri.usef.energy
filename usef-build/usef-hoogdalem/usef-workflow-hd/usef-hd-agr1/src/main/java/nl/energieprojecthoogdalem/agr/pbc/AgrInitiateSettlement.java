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

import static java.math.BigInteger.ZERO;
import static java.util.stream.Collectors.*;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.workflow.AgrWorkflowStep;
import info.usef.agr.workflow.settlement.initiate.AgrInitiateSettlementParameter;
import info.usef.core.util.PtuUtil;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.FlexOfferDto;
import info.usef.core.workflow.dto.FlexOrderDto;
import info.usef.core.workflow.dto.FlexOrderSettlementDto;
import info.usef.core.workflow.dto.FlexRequestDto;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.core.workflow.dto.PtuFlexOrderDto;
import info.usef.core.workflow.dto.PtuSettlementDto;
import info.usef.core.workflow.dto.SettlementDto;
import info.usef.core.workflow.settlement.CoreInitiateSettlementParameter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PBC implementation for the initiation of settlement for an Aggregator. <p> This PBC receives in input: <ul> <li>START_DATE:
 * {@link LocalDate} start date of the settlement (inclusive);</li> <li>END_DATE: {@link LocalDate} end date of the settlement
 * (inclusive);</li> <li>PTU_DURATION: {@link Integer} the duration of a PTU in minutes;</li> <li>PROGNOSIS_DTO_LIST: {@link List}
 * of relevant {@link PrognosisDto};</li> <li>FLEX_REQUEST_DTO_LIST: {@link List} of relevant {@link FlexRequestDto};</li>
 * <li>FLEX_OFFER_DTO_LIST: {@link List} of relevant {@link FlexOfferDto};</li> <li>FLEX_ORDER_DTO_LIST: {@link List} of relevant
 * {@link FlexOrderDto};</li> <li>CONNECTION_PORTFOLIO_DTO_LIST: {@link Map} of {@link List} of {@link ConnectionPortfolioDto} per
 * period, the portoflio for the settlement period.</li> </ul> <p></p> The PBC must output: <ul> <li>SETTLEMENT_DTO: {@link
 * SettlementDto} the settlement for all the flex orders.</li> </ul>
 */
public class AgrInitiateSettlement implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrInitiateSettlement.class);

    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.debug("Started PBC {}", AgrWorkflowStep.AGR_INITIATE_SETTLEMENT.name());
        Integer ptuDuration = context.get(CoreInitiateSettlementParameter.IN.PTU_DURATION.name(), Integer.class);
        // portfolio values
        Map<String, List<String>> connectionsPerConnectionGroup = context.get(AgrInitiateSettlementParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(),
                Map.class);
        Map<LocalDate, List<ConnectionPortfolioDto>> connectionPortfolioPerPeriod = context.get(
                AgrInitiateSettlementParameter.IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), Map.class);
        // map flex orders per congestion point, participant and period
        Map<String, Map<String, Map<LocalDate, List<FlexOrderDto>>>> flexOrderDtosMap = mapFlexOrderDtos(context);
        Map<String, Map<String, Map<LocalDate, List<PrognosisDto>>>> prognosisDtosMap = mapPrognosisDtos(context);
        SettlementDto settlementDto = new SettlementDto(
                context.get(CoreInitiateSettlementParameter.IN.START_DATE.name(), LocalDate.class),
                context.get(CoreInitiateSettlementParameter.IN.END_DATE.name(), LocalDate.class)
        );
        for (String congestionPoint : flexOrderDtosMap.keySet()) {
            Map<String, Map<LocalDate, List<FlexOrderDto>>> perParticipant = flexOrderDtosMap.get(congestionPoint);
            for (String participant : perParticipant.keySet()) {
                Map<LocalDate, List<FlexOrderDto>> perPeriod = perParticipant.get(participant);
                for (LocalDate period : perPeriod.keySet()) {
                    LOGGER.debug("Processing flex orders for congestion point [{}] for participant [{}] on [{}]", congestionPoint,
                            participant, period);
                    PrognosisDto latestPrognosis = fetchLatestPrognosis(prognosisDtosMap, congestionPoint, participant, period);
                    settlementDto.getFlexOrderSettlementDtos().addAll(settleFlexOrders(
                            latestPrognosis,
                            perPeriod.get(period),
                            ptuDuration,
                            period,
                            buildPortfolioForConnectionGroup(connectionPortfolioPerPeriod.get(period),
                                    connectionsPerConnectionGroup, congestionPoint)));
                }
            }
        }
        context.setValue(CoreInitiateSettlementParameter.OUT.SETTLEMENT_DTO.name(), settlementDto);
        return context;
    }

    private List<FlexOrderSettlementDto> settleFlexOrders(PrognosisDto latestPrognosisDto, List<FlexOrderDto> flexOrderDtos,
            Integer ptuDuration, LocalDate period, List<ConnectionPortfolioDto> connectionPortfolio) {
        // ordering with bigger sequence first
        flexOrderDtos.sort((order1, order2) -> order1.getSequenceNumber() > order2.getSequenceNumber() ? -1 : 1);
        // initialize FlexOrderSettlementDtos
        Map<FlexOrderDto, FlexOrderSettlementDto> flexOrderSettlementMap = new HashMap<>();
        for (FlexOrderDto flexOrderDto : flexOrderDtos) {
            FlexOrderSettlementDto flexOrderSettlementDto = new FlexOrderSettlementDto(flexOrderDto.getPeriod());
            flexOrderSettlementDto.setFlexOrder(flexOrderDto);
            flexOrderSettlementMap.put(flexOrderDto, flexOrderSettlementDto);
        }
        // populate PtuSettlementDtos
        for (int i = 0; i < PtuUtil.getNumberOfPtusPerDay(period, ptuDuration); ++i) {

            BigInteger actualPower = fetchActualPower(connectionPortfolio, i + 1, ptuDuration);
            BigInteger prognosisPower = latestPrognosisDto.getPtus().get(i).getPower();
            BigInteger totalDeficiency = actualPower.subtract(prognosisPower);

            for (FlexOrderDto flexOrderDto : flexOrderDtos) {
                PtuFlexOrderDto ptuFlexOrderDto = flexOrderDto.getPtus().get(i);
                PtuSettlementDto ptuSettlementDto = initializePtuSettlementDto(ptuFlexOrderDto, actualPower,
                        prognosisPower);
                BigInteger flexOrderDeficiency;
                BigInteger flexOrderPower = ptuFlexOrderDto.getPower();
                if (flexOrderPower.compareTo(ZERO) != -1 && totalDeficiency.compareTo(ZERO) == -1) {
                    flexOrderDeficiency = totalDeficiency.max(flexOrderPower.negate());
                } else if (flexOrderPower.compareTo(ZERO) == -1 && totalDeficiency.compareTo(ZERO) != -1) {
                    flexOrderDeficiency = totalDeficiency.min(flexOrderPower.abs());
                } else {
                    flexOrderDeficiency = ZERO;
                }
                BigInteger deliveredFlexPower = flexOrderPower.add(flexOrderDeficiency);
                ptuSettlementDto.setDeliveredFlexPower(deliveredFlexPower);
                ptuSettlementDto.setPowerDeficiency(flexOrderDeficiency);
                totalDeficiency = totalDeficiency.subtract(flexOrderDeficiency);
                flexOrderSettlementMap.get(flexOrderDto).getPtuSettlementDtos().add(ptuSettlementDto);
            }
        }
        return new ArrayList<>(flexOrderSettlementMap.values());
    }

    private PrognosisDto fetchLatestPrognosis(Map<String, Map<String, Map<LocalDate, List<PrognosisDto>>>> prognosisDtosMap,
            String congestionPoint, String participant, LocalDate period) {
        // each prognosis in the PBC is supposed to be non-rejected.
        List<PrognosisDto> prognosisDtosPerDay = prognosisDtosMap.getOrDefault(congestionPoint, new HashMap<>())
                .getOrDefault(participant, new HashMap<>())
                .getOrDefault(period, new ArrayList<>());
        return prognosisDtosPerDay.stream()
                .max((prognosis1, prognosis2) -> prognosis1.getSequenceNumber() > prognosis2.getSequenceNumber() ? 1 : -1)
                .orElse(null);
    }

    private BigInteger fetchActualPower(List<ConnectionPortfolioDto> connectionPortfolio, int index, int ptuDuration) {
          //UDI
            BigInteger connectionLoad = connectionPortfolio.stream()
                    .map(connectionPortfolioDTO -> fetchConnectionMostAccuratePower(connectionPortfolioDTO, index))
                    .reduce(BigInteger::add).orElse(ZERO);
            BigInteger adsLoad = connectionPortfolio.stream()
                    .flatMap(connectionPortfolioDTO -> connectionPortfolioDTO.getUdis().stream())
                    .map(udiPortfolioDTO -> computeAverageLoadForUdi(udiPortfolioDTO, index, ptuDuration))
                    .reduce(BigInteger::add).orElse(ZERO);

            return adsLoad.add(connectionLoad);
    }

    private BigInteger fetchConnectionMostAccuratePower(ConnectionPortfolioDto connectionPortfolioDto,
            int timeIndex) {
        if (connectionPortfolioDto == null) {
            return ZERO;
        }
        PowerContainerDto powerContainer = connectionPortfolioDto.getConnectionPowerPerPTU().get(timeIndex);
        BigInteger uncontrolledLoad = powerContainer.getMostAccurateUncontrolledLoad();
        BigInteger consumption = powerContainer.getMostAccurateAverageConsumption();
        BigInteger production = powerContainer.getMostAccurateAverageProduction();
        return BigInteger.ZERO
                .add(uncontrolledLoad == null ? ZERO : uncontrolledLoad)
                .add(consumption == null ? ZERO : consumption)
                .subtract(production == null ? ZERO : production);
    }

    private BigInteger computeAverageLoadForUdi(UdiPortfolioDto udiDto, Integer ptuIndex, Integer ptuDuration) {
        Integer dtusPerPtu = ptuDuration / udiDto.getDtuSize();
        // build average power over the entire ptu
        Double result = udiDto.getUdiPowerPerDTU().entrySet()
                .stream()
                .filter(entry -> ((entry.getKey() - 1) / dtusPerPtu + 1) == ptuIndex)
                .map(entry -> entry.getValue()
                        .getMostAccurateAverageConsumption()
                        .subtract(entry.getValue().getMostAccurateAverageProduction()))
                .mapToInt(BigInteger::intValue)
                .average().orElse(0D);
        return BigInteger.valueOf(Math.round(result));
    }

    private PtuSettlementDto initializePtuSettlementDto(PtuFlexOrderDto ptuFlexOrderDto, BigInteger actualPower,
            BigInteger prognosisPower) {
        PtuSettlementDto ptuSettlementDto = new PtuSettlementDto();
        ptuSettlementDto.setPtuIndex(ptuFlexOrderDto.getPtuIndex());
        ptuSettlementDto.setActualPower(actualPower);
        ptuSettlementDto.setDeliveredFlexPower(ZERO);
        ptuSettlementDto.setOrderedFlexPower(ptuFlexOrderDto.getPower());
        ptuSettlementDto.setPowerDeficiency(ZERO);
        ptuSettlementDto.setPrognosisPower(prognosisPower);
        ptuSettlementDto.setPrice(null);
        ptuSettlementDto.setPenalty(null);
        ptuSettlementDto.setNetSettlement(null);
        return ptuSettlementDto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Map<LocalDate, List<FlexOrderDto>>>> mapFlexOrderDtos(WorkflowContext context) {
        List<FlexOrderDto> flexOrderDtos = context.get(CoreInitiateSettlementParameter.IN.FLEX_ORDER_DTO_LIST.name(), List.class);
        return flexOrderDtos.stream().collect(
                groupingBy(FlexOrderDto::getConnectionGroupEntityAddress,
                        groupingBy(FlexOrderDto::getParticipantDomain,
                                groupingBy(FlexOrderDto::getPeriod, toList()))));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Map<LocalDate, List<PrognosisDto>>>> mapPrognosisDtos(WorkflowContext context) {
        List<PrognosisDto> prognosisDtos = context.get(CoreInitiateSettlementParameter.IN.PROGNOSIS_DTO_LIST.name(), List.class);
        return prognosisDtos.stream().collect(
                groupingBy(PrognosisDto::getConnectionGroupEntityAddress,
                        groupingBy(PrognosisDto::getParticipantDomain,
                                groupingBy(PrognosisDto::getPeriod, toList()))));
    }

    @SuppressWarnings("unchecked")
    private List<ConnectionPortfolioDto> buildPortfolioForConnectionGroup(List<ConnectionPortfolioDto> connectionPortfolio,
            Map<String, List<String>> connectionsPerConnectionGroup, String connectionGroup) {
        List<String> relevantConnections = connectionsPerConnectionGroup.get(connectionGroup);
        // map the ConnectionDto of the portfolio per connection group
        return connectionPortfolio.stream()
                .filter(connectionPortfolioDTO -> relevantConnections.contains(connectionPortfolioDTO.getConnectionEntityAddress()))
                .collect(Collectors.toList());
    }
}
