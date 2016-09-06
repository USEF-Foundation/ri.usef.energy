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

import static java.math.BigInteger.ZERO;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import energy.usef.core.exception.TechnicalException;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.ConnectionMeterDataDto;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.MeterDataDto;
import energy.usef.core.workflow.dto.MeterDataSetDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;
import energy.usef.core.workflow.dto.PtuMeterDataDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;
import energy.usef.core.workflow.dto.SettlementDto;
import energy.usef.core.workflow.settlement.CoreInitiateSettlementParameter;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.dto.GridMonitoringDto;
import energy.usef.dso.workflow.settlement.initiate.DsoInitiateSettlementParameter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub implementation of the Dso Initiate Settlement workflow.
 * <p>
 * This PBC receives in input:
 * <ul>
 * <li>START_DATE: {@link LocalDate} start date of the settlement (inclusive);</li>
 * <li>END_DATE: {@link LocalDate} end date of the settlement (inclusive);</li>
 * <li>PTU_DURATION: {@link Integer} the duration of a PTU in minutes;</li>
 * <li>PROGNOSIS_DTO_LIST: {@link List} of relevant {@link PrognosisDto};</li>
 * <li>FLEX_REQUEST_DTO_LIST: {@link List} of relevant {@link FlexRequestDto};</li>
 * <li>FLEX_OFFER_DTO_LIST: {@link List} of relevant {@link FlexOfferDto};</li>
 * <li>FLEX_ORDER_DTO_LIST: {@link List} of relevant {@link FlexOrderDto};</li>
 * <li>SMART_METER_DATA: {@link List} of {@link MeterDataSetDto}, the smart meter data for the periods;</li>
 * <li>GRID_MONITORING_DATA: {@link List} of {@link GridMonitoringDto} the grid monitoring data (if the smart meter data is not
 * present).</li>
 * </ul>
 * <p></p>
 * The PBC must output:
 * <ul>
 * <li>SETTLEMENT_DTO: {@link SettlementDto} the settlement for all the flex orders.</li>
 * </ul>
 */
public class DsoInitiateSettlementStub implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoInitiateSettlementStub.class);
    private static final Map<LocalDate, GridMonitoringDto> EMPTY_MAP = new HashMap<>();

    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.debug("Started PBC {}", DsoWorkflowStep.DSO_INITIATE_SETTLEMENT.name());
        Integer ptuDuration = context.get(CoreInitiateSettlementParameter.IN.PTU_DURATION.name(), Integer.class);
        Map<String, Map<String, Map<LocalDate, List<PtuMeterDataDto>>>> ptuMeterDataMap = buildMeterDataMap(context);
        Map<String, Map<LocalDate, GridMonitoringDto>> gridMonitoringDataMap = buildGridMonitoringMap(context);
        // map flex orders per congestion point, period and participant
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
                    settlementDto.getFlexOrderSettlementDtos().addAll(processFlexOrders(
                            latestPrognosis,
                            perPeriod.get(period),
                            ptuDuration,
                            period,
                            ptuMeterDataMap.get(congestionPoint),
                            gridMonitoringDataMap.getOrDefault(congestionPoint, EMPTY_MAP).get(period)));
                }
            }
        }
        context.setValue(CoreInitiateSettlementParameter.OUT.SETTLEMENT_DTO.name(), settlementDto);
        return context;
    }

    private List<FlexOrderSettlementDto> processFlexOrders(PrognosisDto latestPrognosisDto, List<FlexOrderDto> flexOrderDtos,
            Integer ptuDuration, LocalDate period, Map<String, Map<LocalDate, List<PtuMeterDataDto>>> ptuMeterData,
            GridMonitoringDto gridMonitoringDto) {
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
            final int index = i;
            BigInteger prognosisPower = latestPrognosisDto.getPtus().get(i).getPower();
            BigInteger actualPower = fetchActualPower(ptuMeterData, gridMonitoringDto, latestPrognosisDto.getParticipantDomain(),
                    period, index + 1);
            BigInteger totalDeficiency = actualPower.subtract(prognosisPower);
            for (FlexOrderDto flexOrderDto : flexOrderDtos) {
                PtuFlexOrderDto ptuFlexOrderDto = flexOrderDto.getPtus().get(index);
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

    private BigInteger fetchActualPower(Map<String, Map<LocalDate, List<PtuMeterDataDto>>> ptuMeterData,
            GridMonitoringDto gridMonitoringDto, String participantDomain, LocalDate period, int index) {
        if (ptuMeterData != null) {
            PtuMeterDataDto smartMeterValue = ptuMeterData.getOrDefault(participantDomain, new HashMap<>()).get(period).stream()
                    .filter(ptu -> index == ptu.getPtuIndex())
                    .findFirst().orElse(null);
            return smartMeterValue.getPower();
        }
        Integer connectionCount = gridMonitoringDto.getConnectionCountPerAggregator().get(participantDomain);
        Integer totalConnectionCount = gridMonitoringDto.getConnectionCountPerAggregator().values().stream()
                .reduce(0, (i1, i2) -> i1 + i2);
        if (connectionCount == null || connectionCount == 0) {
            throw new TechnicalException(
                    "Aggregator has zero connection on the congestion point even though it has flex orders.");
        }
        return gridMonitoringDto.getPtuGridMonitoringDtos().get(index - 1)
                .getActualPower()
                .multiply(BigInteger.valueOf(connectionCount))
                .divide(BigInteger.valueOf(totalConnectionCount));
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
    private Map<String, Map<LocalDate, GridMonitoringDto>> buildGridMonitoringMap(WorkflowContext context) {
        List<GridMonitoringDto> gridMonitoringDtos = context
                .get(DsoInitiateSettlementParameter.IN.GRID_MONITORING_DATA.name(), List.class);
        if (gridMonitoringDtos == null) {
            return new HashMap<>();
        }
        return gridMonitoringDtos.stream().collect(groupingBy(GridMonitoringDto::getCongestionPointEntityAddress,
                toMap(GridMonitoringDto::getPeriod, Function.identity())));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Map<LocalDate, List<PtuMeterDataDto>>>> buildMeterDataMap(WorkflowContext context) {
        List<MeterDataSetDto> meterDataSets = context.get(DsoInitiateSettlementParameter.IN.SMART_METER_DATA.name(), List.class);
        if (meterDataSets == null) {
            return new HashMap<>();
        }
        Map<String, Map<String, Map<LocalDate, List<PtuMeterDataDto>>>> result = new HashMap<>();
        for (MeterDataSetDto meterDataSet : meterDataSets) {
            result.put(meterDataSet.getEntityAddress(), buildMeterDataPerCongestionMap(meterDataSet));
        }
        return result;
    }

    private Map<String, Map<LocalDate, List<PtuMeterDataDto>>> buildMeterDataPerCongestionMap(MeterDataSetDto meterDataSet) {
        Map<String, Map<LocalDate, List<PtuMeterDataDto>>> perCongestionPoint = new HashMap<>();
        for (MeterDataDto meterData : meterDataSet.getMeterDataDtos()) {
            for (ConnectionMeterDataDto connectionMeterData : meterData.getConnectionMeterDataDtos()) {
                String participantDomain = connectionMeterData.getAgrDomain();
                LocalDate period = meterData.getPeriod();
                if (!perCongestionPoint.containsKey(participantDomain)) {
                    perCongestionPoint.put(participantDomain, new HashMap<>());
                }
                if (!perCongestionPoint.get(participantDomain).containsKey(period)) {
                    perCongestionPoint.get(participantDomain).put(period, new ArrayList<>());
                }
                perCongestionPoint.get(participantDomain).get(period).addAll(connectionMeterData.getPtuMeterDataDtos());
            }
        }
        return perCongestionPoint;
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
}
