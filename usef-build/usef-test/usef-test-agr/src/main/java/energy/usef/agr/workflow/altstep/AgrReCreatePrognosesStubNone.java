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

package energy.usef.agr.workflow.altstep;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesWorkflowParameter;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.PrognosisDto;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stub implementation of the PBC which is in charge of deciding to re-create or not A-Plans and or D-Prognoses after the
 * re-optimization of the portfolio. This version does not recreate any prognoses
 * <p>
 * The PBC receives the following parameters as input:
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
public class AgrReCreatePrognosesStubNone implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrReCreatePrognosesStubNone.class);
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
                connectionGroupsToConnections, currentPortfolio, period, ptuSize);

        List<Long> sequencesOfDPrognosesToBeRecreated = new ArrayList<>();
        List<Long> sequencesOfAPlansToBeRecreated = new ArrayList<>();

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

    private Map<String, Map<Integer, BigInteger>> totalLoadPerConnectionGroupPerPtu(
            Map<String, List<String>> connectionGroupsToConnections, List<ConnectionPortfolioDto> connectionPortfolioDtos,
            LocalDate period, Integer ptuSize) {
        Map<String, List<ConnectionPortfolioDto>> portfolioPerConnectionGroup = buildConnectionGroupToPortfolioMap(
                connectionPortfolioDtos, connectionGroupsToConnections);
        Map<String, Map<Integer, BigInteger>> uncontrolledLoadPerCongestionPoint = uncontrolledLoadPerConnectionGroupPerPtu(
                portfolioPerConnectionGroup);
        Map<String, Map<Integer, BigInteger>> controlledLoadPerCongestionPoint = loadPerConnectionGroupPerPtu(
                portfolioPerConnectionGroup, ptuSize);
        Map<String, Map<Integer, BigInteger>> totalLoadPerCongestionPoint = new HashMap<>();
        // for each entry in the uncontrolled load per congestion point per ptu, add the corresponding controlled load.
        for (String usefIdentifier : uncontrolledLoadPerCongestionPoint.keySet()) {
            Map<Integer, BigInteger> totalLoad = new HashMap<>();
            totalLoadPerCongestionPoint.put(usefIdentifier, totalLoad);
            for (Map.Entry<Integer, BigInteger> uncontrolledLoadPerPtu : uncontrolledLoadPerCongestionPoint.get(usefIdentifier)
                    .entrySet()) {
                totalLoad.put(uncontrolledLoadPerPtu.getKey(), uncontrolledLoadPerPtu.getValue()
                        .add(controlledLoadPerCongestionPoint.get(usefIdentifier).get(uncontrolledLoadPerPtu.getKey())));
            }

        }

        // return the now modified uncontrolled load
        return totalLoadPerCongestionPoint;
    }

    private Map<String, Map<Integer, BigInteger>> uncontrolledLoadPerConnectionGroupPerPtu(
            Map<String, List<ConnectionPortfolioDto>> portfolioPerConnectionGroup) {
        // for each connection in the portfolio, map the forecast power per ConnectionGroup per ptu (for the specified period)
        Function<Map.Entry<Integer, PowerContainerDto>, Integer> getIndexOf = Map.Entry::getKey;
        Function<Map.Entry<Integer, PowerContainerDto>, BigInteger> getForecastOfPowerContainer = entry -> entry.getValue()
                .getForecast().getUncontrolledLoad();

        return portfolioPerConnectionGroup.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().stream()
                        .flatMap(connectionPortfolioDTO -> connectionPortfolioDTO.getConnectionPowerPerPTU().entrySet().stream())
                        .collect(Collectors.groupingBy(getIndexOf::apply, Collectors.mapping(getForecastOfPowerContainer::apply,
                                Collectors.reducing(BigInteger.ZERO, BigInteger::add))))));
    }

    private Map<String, Map<Integer, BigInteger>> loadPerConnectionGroupPerPtu(
            Map<String, List<ConnectionPortfolioDto>> portfolioPerConnectionGroup, Integer ptuSize) {
        // sum the forecast power for all the DTUs of all UDIs for all connections, grouped by ConnectionGroup and PTU
        Map<String, Map<Integer, BigInteger>> loadPerPtuPerConnectionGroup = new HashMap<>();

        portfolioPerConnectionGroup.forEach((connectionGroup, connectionGroupPortfolioDTOs) -> {
            Map<Integer, BigInteger> loadPerPtu = new HashMap<>();
            loadPerPtuPerConnectionGroup.put(connectionGroup, loadPerPtu);
            for (ConnectionPortfolioDto connectionDto : connectionGroupPortfolioDTOs) {
                // calculate the power per ptu for all udi's
                for (UdiPortfolioDto udiDto : connectionDto.getUdis()) {
                    Integer dtusPerPtu = ptuSize / udiDto.getDtuSize();
                    udiDto.getUdiPowerPerDTU().entrySet().forEach(entry -> {
                        Integer ptuIndex = ((entry.getValue().getTimeIndex() - 1) / dtusPerPtu) + 1;
                        if (loadPerPtu.get(ptuIndex) == null) {
                            loadPerPtu.put(ptuIndex, BigInteger.ZERO);
                        }
                        loadPerPtu.put(ptuIndex, loadPerPtu.get(ptuIndex).add(entry.getValue().getForecast().calculatePower()));
                    });
                }

                // add the power per ptu for the connection
                connectionDto.getConnectionPowerPerPTU().forEach((ptuIndex, powerContainerDto) -> {
                    BigInteger connectionForecastPower = connectionDto.getConnectionPowerPerPTU().get(ptuIndex).getForecast()
                            .calculatePower();
                    if (loadPerPtu.get(ptuIndex) == null) {
                        loadPerPtu.put(ptuIndex, connectionForecastPower);
                    } else {
                        loadPerPtu.get(ptuIndex).add(connectionForecastPower);
                    }
                });
            }

        });

        return loadPerPtuPerConnectionGroup;
    }

    private Map<String, List<ConnectionPortfolioDto>> buildConnectionGroupToPortfolioMap(
            List<ConnectionPortfolioDto> connectionPortfolio, Map<String, List<String>> connectionGroupToConnections) {
        Map<String, ConnectionPortfolioDto> connectionDtosByAddress = connectionPortfolio.stream()
                .collect(Collectors.toMap(ConnectionPortfolioDto::getConnectionEntityAddress, Function.identity()));
        return connectionGroupToConnections.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue().stream().map(connectionDtosByAddress::get).collect(Collectors.toList())));
    }
}
