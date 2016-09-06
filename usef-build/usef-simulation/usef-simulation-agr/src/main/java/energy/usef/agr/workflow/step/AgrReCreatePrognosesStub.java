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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesWorkflowParameter;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

/**
 * Stub implementation of the PBC which is in charge of deciding to re-create or not A-Plans and or D-Prognoses after the
 * re-optimization of the portfolio.
 * <p>
 * The PBC receives the following parameters as input to make the decision:
 * <ul>
 * <li>LATEST_D_PROGNOSES_DTO_LIST : the list of latest {@link Prognosis} of type 'D-Progosis'.
 * </li>
 * <li>LATEST_A_PLANS_DTO_LIST : the list of latest {@link Prognosis} of type 'A-Plan'.</li>
 * <li>CURRENT_PORTFOLIO : the current (and hence latest) portfolio with connection information.</li>
 * <li>CONNECTION_GROUPS_TO_CONNECTIONS_MAP : a map with a list of connection entity addresses per connection group.</li>
 * </ul>
 * <p>
 * The PBC must output two flags indicating whether A-Plans and/or D-Prognoses must be re-created. These parameters have to be
 * present and have to be named:
 * <ul>
 * <li>REQUIRES_NEW_APLANS_FLAG : boolean value indicating the re-creation of A-Plans</li>
 * <li>REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST : {@link java.util.List} of {@link Long} that are the sequence numbers of the
 * D-Prognoses which will be re-created.</li>
 * </ul>
 */
public class AgrReCreatePrognosesStub implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrReCreatePrognosesStub.class);
    private static final BigDecimal D_PROGNOSIS_RECREATION_THRESHOLD = new BigDecimal("0.05");
    private static final BigInteger A_PLAN_RECREATION_THRESHOLD = new BigInteger("1000");

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.debug("Invoking PBC 'AGRReCreatePrognoses'.");

        List<PrognosisDto> dPrognoses = (List<PrognosisDto>) context
                .getValue(ReCreatePrognosesWorkflowParameter.IN.LATEST_D_PROGNOSES_DTO_LIST.name());
        List<PrognosisDto> aPlans = (List<PrognosisDto>) context
                .getValue(ReCreatePrognosesWorkflowParameter.IN.LATEST_A_PLANS_DTO_LIST.name());
        List<ConnectionPortfolioDto> currentPortfolio = context
                .get(ReCreatePrognosesWorkflowParameter.IN.CURRENT_PORTFOLIO.name(), List.class);
        Map<String, List<String>> connectionGroupsToConnections = context
                .get(ReCreatePrognosesWorkflowParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(), HashMap.class);
        LocalDate period = (LocalDate) context.getValue(ReCreatePrognosesWorkflowParameter.IN.PERIOD.name());
        Integer ptuSize = context.get(ReCreatePrognosesWorkflowParameter.IN.PTU_DURATION.name(), Integer.class);
        LOGGER.debug("Received [{}] d-prognoses, [{}] a-plans and the current portfolio.", dPrognoses.size(), aPlans.size());

        Map<String, Map<Integer, BigInteger>> loadPerPtuPerConnectionGroup = totalLoadPerConnectionGroupPerPtu(
                connectionGroupsToConnections, currentPortfolio, ptuSize);

        List<Long> sequencesOfDPrognosesToBeRecreated = compareDPrognosesAgainstLoad(dPrognoses, loadPerPtuPerConnectionGroup);
        List<Long> sequencesOfAPlansToBeRecreated = compareAPlansAgainstLoad(aPlans, loadPerPtuPerConnectionGroup);

        context.setValue(ReCreatePrognosesWorkflowParameter.OUT.REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST.name(),
                sequencesOfDPrognosesToBeRecreated);
        context.setValue(ReCreatePrognosesWorkflowParameter.OUT.REQUIRES_NEW_A_PLAN_SEQUENCES_LIST.name(),
                sequencesOfAPlansToBeRecreated);
        LOGGER.debug("Output values:");
        LOGGER.debug("Re-create A-Plans: [{}]",
                sequencesOfAPlansToBeRecreated.stream().map(String::valueOf).collect(Collectors.joining(";")));
        LOGGER.debug("Re-Create D-Prognoses: [{}].",
                sequencesOfDPrognosesToBeRecreated.stream().map(String::valueOf).collect(Collectors.joining(";")));
        return context;
    }

    private List<Long> compareDPrognosesAgainstLoad(List<PrognosisDto> dPrognoses,
            Map<String, Map<Integer, BigInteger>> loadPerPtuPerConnectionGroup) {
        List<Long> toBeRecreatedSequences = new ArrayList<>();
        // add the sequence if any PTU deviates more than 5% than the load
        dPrognoses.stream().forEach(prognosisDto -> prognosisDto.getPtus().stream()
                .filter(ptu -> !isDPrognosisPtuWithinDeviationThreshold(ptu,
                        loadPerPtuPerConnectionGroup.get(prognosisDto.getConnectionGroupEntityAddress())
                                .get(ptu.getPtuIndex().intValue())))
                .findAny().ifPresent(ptu -> {
                    LOGGER.debug("Power of PTU [{}/{}] of D-Prognosis [{}] for participant [{}] was too different from the "
                                    + "forecasted load in the portfolio.", prognosisDto.getPeriod(), ptu.getPtuIndex(),
                            prognosisDto.getSequenceNumber(), prognosisDto.getParticipantDomain());
                    toBeRecreatedSequences.add(prognosisDto.getSequenceNumber());
                }));
        return toBeRecreatedSequences;
    }

    private List<Long> compareAPlansAgainstLoad(List<PrognosisDto> aPlans,
            Map<String, Map<Integer, BigInteger>> loadPerPtuPerConnectionGroup) {
        List<Long> toBeRecreatedSequences = new ArrayList<>();
        aPlans.stream().forEach(aplan -> aplan.getPtus().stream().filter(ptu -> !isAPlanPtuWithinDeviationThreshold(ptu,
                loadPerPtuPerConnectionGroup.get(aplan.getConnectionGroupEntityAddress()).get(ptu.getPtuIndex().intValue())))
                .findAny().ifPresent(ptu -> {
                    LOGGER.debug("Power of PTU [{}/{}] of A-Plan [{}] for participant [{}] was too different from the "
                                    + "forecasted load in the portfolio.", aplan.getPeriod(), ptu.getPtuIndex(),
                            aplan.getSequenceNumber(), aplan.getParticipantDomain());
                    toBeRecreatedSequences.add(aplan.getSequenceNumber());
                }));
        return toBeRecreatedSequences;
    }

    private Map<String, Map<Integer, BigInteger>> totalLoadPerConnectionGroupPerPtu(
            Map<String, List<String>> connectionGroupsToConnections, List<ConnectionPortfolioDto> connectionPortfolioDtos,
            Integer ptuSize) {
        Map<String, List<ConnectionPortfolioDto>> portfolioPerConnectionGroup = buildConnectionGroupToPortfolioMap(
                connectionPortfolioDtos, connectionGroupsToConnections);
        return loadPerConnectionGroupPerPtu(portfolioPerConnectionGroup, ptuSize);
    }

    private Map<String, Map<Integer, BigInteger>> loadPerConnectionGroupPerPtu(
            Map<String, List<ConnectionPortfolioDto>> portfolioPerConnectionGroup, Integer ptuSize) {
        // sum the forecast power for all the DTUs of all UDIs for all connections, grouped by ConnectionGroup and PTU
        Map<String, Map<Integer, BigInteger>> loadPerPtuPerConnectionGroup = new HashMap<>();

        portfolioPerConnectionGroup.forEach((connectionGroup, connectionPortfolioDtos) -> {
            Map<Integer, BigInteger> loadPerPtu = new HashMap<>();
            loadPerPtuPerConnectionGroup.put(connectionGroup, loadPerPtu);
            for (ConnectionPortfolioDto connectionDto : connectionPortfolioDtos) {
                addUdiForecastPerPtu(ptuSize, loadPerPtu, connectionDto);
                addConnectForecastPerPtu(loadPerPtu, connectionDto);
            }

        });

        return loadPerPtuPerConnectionGroup;
    }

    private void addConnectForecastPerPtu(Map<Integer, BigInteger> loadPerPtu, ConnectionPortfolioDto connectionDto) {
        connectionDto.getConnectionPowerPerPTU().forEach((ptuIndex, powerContainerDto) -> {
            BigInteger connectionForecastPower = connectionDto.getConnectionPowerPerPTU().get(ptuIndex).getForecast()
                    .calculatePower();
            initializePtuIndexPower(loadPerPtu, ptuIndex);
            loadPerPtu.put(ptuIndex, loadPerPtu.get(ptuIndex).add(connectionForecastPower));
        });
    }

    private void addUdiForecastPerPtu(Integer ptuSize, Map<Integer, BigInteger> loadPerPtu, ConnectionPortfolioDto connectionDto) {
        for (UdiPortfolioDto udiDto : connectionDto.getUdis()) {
            Integer dtusPerPtu = ptuSize / udiDto.getDtuSize();
            udiDto.getUdiPowerPerDTU().entrySet().forEach(entry -> {
                Integer ptuIndex = ((entry.getValue().getTimeIndex() - 1) / dtusPerPtu) + 1;
                initializePtuIndexPower(loadPerPtu, ptuIndex);
                loadPerPtu.put(ptuIndex, loadPerPtu.get(ptuIndex).add(entry.getValue().getForecast().calculatePower()));
            });
        }
    }

    private void initializePtuIndexPower(Map<Integer, BigInteger> loadPerPtu, Integer ptuIndex) {
        if (loadPerPtu.get(ptuIndex) == null) {
            loadPerPtu.put(ptuIndex, BigInteger.ZERO);
        }
    }

    private boolean isDPrognosisPtuWithinDeviationThreshold(PtuPrognosisDto ptu, BigInteger load) {
        BigDecimal power = new BigDecimal(ptu.getPower());
        BigDecimal decimalLoad = new BigDecimal(load);
        if(load.compareTo(BigInteger.ZERO) == 0) {
            //if power is also zero, within threshold.
            return ptu.getPower().compareTo(BigInteger.ZERO) == 0;
        }
        LOGGER.trace("Comparing PtuPrognosisDto power [{}] against forecasted load [{}] (max. difference percentage = [{}]%)",
                power, decimalLoad, D_PROGNOSIS_RECREATION_THRESHOLD.multiply(BigDecimal.valueOf(100)));
        return power.subtract(decimalLoad)
                .divide(decimalLoad, MathContext.DECIMAL64)
                .abs()
                .setScale(2, RoundingMode.HALF_UP)
                .compareTo(D_PROGNOSIS_RECREATION_THRESHOLD) != 1;
    }

    private boolean isAPlanPtuWithinDeviationThreshold(PtuPrognosisDto ptu, BigInteger load) {
        LOGGER.trace("Comparing PtuPrognosisDto power [{}] against forecasted load [{}] (max. difference power = [{}]W)",
                ptu.getPower(), load, A_PLAN_RECREATION_THRESHOLD);
        return ptu.getPower().subtract(load).abs().compareTo(A_PLAN_RECREATION_THRESHOLD) != 1;
    }

    private Map<String, List<ConnectionPortfolioDto>> buildConnectionGroupToPortfolioMap(
            List<ConnectionPortfolioDto> connectionPortfolio, Map<String, List<String>> connectionGroupToConnections) {
        Map<String, ConnectionPortfolioDto> connectionDtosByAddress = connectionPortfolio.stream()
                .collect(Collectors.toMap(ConnectionPortfolioDto::getConnectionEntityAddress, Function.identity()));
        return connectionGroupToConnections.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue().stream().map(connectionDtosByAddress::get).collect(Collectors.toList())));
    }
}
