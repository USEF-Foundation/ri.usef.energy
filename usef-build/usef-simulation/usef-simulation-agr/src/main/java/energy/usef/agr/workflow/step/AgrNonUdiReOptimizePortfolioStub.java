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

import static energy.usef.agr.workflow.step.AgrReOptimizePortfolioStubUtil.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ForecastPowerDataDto;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter.IN;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter.OUT_NON_UDI;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PrognosisDto;

/**
 * A simple implementation of a workflow step to simulation the behavior of an Aggregator Re-optimize portfolio for non-udi
 * aggregators. This implementation only reduce the consumption of the connections. It will not do anything with production of power
 * and it will not increase the consumption of the connections.
 * <p>
 * The calculation done in this PBC is: 1. Calculate the target power per ptu with target = prognosis + ordered - summed forecast
 * consumption of all connections 2. Calculate the factor per ptu with factor = target / summed potential flex consumption for all
 * connections 3. Calculate the new forecast consumption per ptu with forecast = forecast + potential flex * factor
 * <p>
 * The PBC receives the following parameters as input : <ul> <li>PTU_DURATION : PTU duration.</li> <li>CURRENT_PTU_INDEX : Current
 * PTU index.</li> <li>PTU_DATE : Period of re-optimization.</li> <li>CONNECTION_PORTFOLIO_IN : List of connection group portfolios
 * {@link ConnectionPortfolioDto}.</li> <li>CONNECTION_GROUPS_TO_CONNECTIONS_MAP : map giving the relationship between each
 * connection group and its connections.</li> <li>RECEIVED_FLEXORDER_LIST : aggregate info and collection of {@link
 * FlexOrderDto}</li> <li>LATEST_A_PLAN_DTO_LIST : contains list of most recent {@link PrognosisDto} (A-plans)</li>
 * <li>LATEST_D_PROGNOSIS_DTO_LIST : contains list of most recent {@link PrognosisDto} (D-Prognoses)</li>
 * <li>RELEVANT_PROGNOSIS_LIST : contains list of prognosis relevant to FlexOrder.</li> </ul>
 * <p>
 * The PBC must output the modified connection portfolio and devise messages: <ul> <li>CONNECTION_PORTFOLIO_OUT : re-optimized
 * connection portfolio {@link ConnectionGroupPortfolioDto}.</li> </ul>
 */
public class AgrNonUdiReOptimizePortfolioStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrNonUdiReOptimizePortfolioStub.class);

    /**
     * Invoke step to generate a random between min and max nr of messages and put hem on the WorkflowContext.
     *
     * @param context incoming workflow context
     * @return WorkflowContext containing a new list of deviceMessage
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {

        // Getting input parameters
        int ptuDuration = (int) context.getValue(IN.PTU_DURATION.name());
        int currentPtuIndex = (int) context.getValue(IN.CURRENT_PTU_INDEX.name());
        LocalDate period = (LocalDate) context.getValue(IN.PTU_DATE.name());
        List<ConnectionPortfolioDto> connectionPortfolio = context.get(IN.CONNECTION_PORTFOLIO_IN.name(), List.class);
        Map<String, List<String>> connectionGroupsToConnectionMap = context.get(IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(),
                HashMap.class);
        List<FlexOrderDto> flexOrders = context.get(IN.RECEIVED_FLEXORDER_LIST.name(), List.class);
        List<PrognosisDto> dPrognosis = context.get(IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), List.class);
        List<PrognosisDto> aPlans = context.get(IN.LATEST_A_PLAN_DTO_LIST.name(), List.class);

        LOGGER.info("Aggregator Re-optimize portfolio Stub started with {} connections in the portfolio.",
                connectionPortfolio.size());

        // validate the input
        if (!validateInput(context)) {
            return returnDefaultContext(context);
        }

        LocalDate currentDate = DateTimeUtil.getCurrentDate();

        // Do some mapping, summing and calculation for quick access later in the process
        Map<String, List<ConnectionPortfolioDto>> connectionPortfolioPerConnectionGroup = mapConnectionPortfolioPerConnectionGroup(
                connectionPortfolio, connectionGroupsToConnectionMap);
        Map<String, Map<Integer, BigInteger>> prognosisPowerPerPtuPerConnectionGroup = mapPrognosisPowerPerPtuPerConnectionGroup(
                aPlans, dPrognosis);
        Map<String, Map<Integer, BigInteger>> orderedPowerPerPtuPerConnectionGroup = sumOrderedPowerPerPtuPerConnectionGroup(
                flexOrders);
        Map<String, Map<Integer, BigInteger>> forecastPowerPerPtuPerConnectionGroup = sumForecastPowerPerPtuPerConnectionGroup(
                connectionPortfolioPerConnectionGroup, period, ptuDuration);
        Map<String, Map<Integer, BigInteger>> targetPowerPerPtuPerConnectionGroup = fetchTargetPowerPerPtuPerConnectionGroup(
                prognosisPowerPerPtuPerConnectionGroup, orderedPowerPerPtuPerConnectionGroup,
                forecastPowerPerPtuPerConnectionGroup);
        Map<String, Map<Integer, BigInteger>> sumPotentialFlexConsumptionPerPtuPerConnectionGroup = sumPotentialFlexConsumptionPerPtuPerConnectionGroup(
                connectionPortfolioPerConnectionGroup, period, ptuDuration);
        Map<String, Map<Integer, BigInteger>> sumPotentialFlexProductionPerPtuPerConnectionGroup = sumPotentialFlexProductionPerPtuPerConnectionGroup(
                connectionPortfolioPerConnectionGroup, period, ptuDuration);
        Map<String, Map<Integer, BigDecimal>> flexFactorPerPtuPerConnectionGroup = fetchFlexFactorPerPtuPerConnectionGroup(
                targetPowerPerPtuPerConnectionGroup, sumPotentialFlexConsumptionPerPtuPerConnectionGroup,
                sumPotentialFlexProductionPerPtuPerConnectionGroup);

        // Calculate the new portfolio consumption forecast (consumption + potentialFlex * factor)
        calculatePortfolioForecast(connectionPortfolioPerConnectionGroup, flexFactorPerPtuPerConnectionGroup, period, currentDate,
                currentPtuIndex);

        context.setValue(OUT_NON_UDI.CONNECTION_PORTFOLIO_OUT.name(), connectionPortfolio);
        return context;
    }

    // Calculate the new portfolio consumption forecasts (forecast = forecast + summedPotentialFlex * factor)
    private void calculatePortfolioForecast(Map<String, List<ConnectionPortfolioDto>> connectionPortfolioPerConnectionGroup,
            Map<String, Map<Integer, BigDecimal>> potentialFlexFactorPerPtuPerConnectionGroup, LocalDate ptuDate,
            LocalDate currentDate, final int currentPtuIndex) {
        boolean isAfterToday = ptuDate.compareTo(currentDate) > 0;
        connectionPortfolioPerConnectionGroup.forEach((connectionGroupId, connectionPortfolioList) ->
                connectionPortfolioList.stream()
                        .flatMap(connectionPortfolioDTO -> connectionPortfolioDTO.getConnectionPowerPerPTU().entrySet().stream())
                        .filter(entry -> isAfterToday || entry.getKey() >= currentPtuIndex)
                        .forEach(entry -> {
                            Integer ptuIndex = entry.getKey();
                            ForecastPowerDataDto forecastPowerData = entry.getValue().getForecast();

                            processxFlexIntoForecast(potentialFlexFactorPerPtuPerConnectionGroup, connectionGroupId, ptuIndex,
                                    forecastPowerData);

                        }));
    }

    private void processxFlexIntoForecast(Map<String, Map<Integer, BigDecimal>> potentialFlexFactorPerPtuPerConnectionGroup,
            String connectionGroupId, Integer ptuIndex, ForecastPowerDataDto forecastPowerData) {

        BigDecimal factor = potentialFlexFactorPerPtuPerConnectionGroup.get(connectionGroupId).get(ptuIndex);

        if (factor.compareTo(BigDecimal.ZERO) == 0) {
            // nothing to do
            return;
        }

        if (factor.compareTo(BigDecimal.ZERO) > 0) {
            // factor > 0, reduce consumption (increase of production is not (yet) supported by this stub)
            // new consumption power = forecast consumption + flex consumption * factor
            BigDecimal flexConsumption = new BigDecimal(forecastPowerData.getPotentialFlexConsumption());
            BigInteger factoredFlex = flexConsumption.multiply(factor).toBigInteger();

            if (factoredFlex.compareTo(BigInteger.ZERO) > 0) {
                return;
            }

            BigInteger newForecast = forecastPowerData.getAverageConsumption().add(factoredFlex);
            // consumption cannot be less than 0
            if (newForecast.compareTo(BigInteger.ZERO) < 0) {
                newForecast = BigInteger.ZERO;
            }
            forecastPowerData.setAverageConsumption(newForecast);
        } else {
            // factor < 0, reduce production (increase of consumption is not (yet) supported by this stub)
            // new production power = forecast production + flex production * factor
            BigDecimal flexProduction = new BigDecimal(forecastPowerData.getPotentialFlexProduction());
            BigInteger factoredFlex = flexProduction.multiply(factor).toBigInteger();

            if (factoredFlex.compareTo(BigInteger.ZERO) < 0) {
                return;
            }

            BigInteger newForecast = forecastPowerData.getAverageProduction().subtract(factoredFlex);
            // production cannot be less than 0
            if (newForecast.compareTo(BigInteger.ZERO) < 0) {
                newForecast = BigInteger.ZERO;
            }
            forecastPowerData.setAverageProduction(newForecast);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean validateInput(WorkflowContext context) {
        boolean validInput = true;

        LocalDate ptuDate = context.get(IN.PTU_DATE.name(), LocalDate.class);
        List<ConnectionPortfolioDto> connectionPortfolio = context.get(IN.CONNECTION_PORTFOLIO_IN.name(), List.class);
        List<FlexOrderDto> flexOrders = context.get(IN.RECEIVED_FLEXORDER_LIST.name(), List.class);
        List<PrognosisDto> dPrognosis = context.get(IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), List.class);
        List<PrognosisDto> aPlans = context.get(IN.LATEST_A_PLAN_DTO_LIST.name(), List.class);

        if (connectionPortfolio.isEmpty()) {
            validInput = false;
            LOGGER.info("No connections in portfolio, unable to re-optimize portfolio.");
        }

        LocalDate currentDate = DateTimeUtil.getCurrentDate();
        if (ptuDate.isBefore(currentDate)) {
            validInput = false;
            LOGGER.error("Unable to re-optimize portfolio for periods in the past: {}", ptuDate);
        }

        if (dPrognosis.isEmpty() && aPlans.isEmpty()) {
            validInput = false;
            LOGGER.error("D-Prognosis and/or A-Plans expected, but none were found in the input.");
        }
        return validInput;
    }

    private WorkflowContext returnDefaultContext(WorkflowContext context) {
        context.setValue(OUT_NON_UDI.CONNECTION_PORTFOLIO_OUT.name(), new ArrayList<ConnectionPortfolioDto>());
        LOGGER.info("Ended AgrReOptimizePortfolioStub workflow with empty return values");
        return context;
    }

}
