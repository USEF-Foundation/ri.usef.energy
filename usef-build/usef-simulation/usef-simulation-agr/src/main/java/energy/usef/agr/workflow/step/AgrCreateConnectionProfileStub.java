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

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.dto.ElementDtuDataDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.PowerDataDto;
import energy.usef.agr.workflow.plan.connection.profile.CreateConnectionProfileStepParameter;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;

/**
 * This PBC is in charge of filling in the Profile values for each connection of the portfolio.
 */
public class AgrCreateConnectionProfileStub implements WorkflowStep {

    @Override
    @SuppressWarnings("unchecked")
    public WorkflowContext invoke(WorkflowContext context) {
        LocalDate period = context.get(CreateConnectionProfileStepParameter.IN.PERIOD.name(), LocalDate.class);
        Integer ptuDuration = context.get(CreateConnectionProfileStepParameter.IN.PTU_DURATION.name(), Integer.class);
        List<ConnectionPortfolioDto> connectionPortfolioDTOs = (List<ConnectionPortfolioDto>) context
                .get(CreateConnectionProfileStepParameter.IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class);
        Map<String, List<ElementDto>> elementsPerConnection = (Map<String, List<ElementDto>>) context
                .get(CreateConnectionProfileStepParameter.IN.ELEMENT_PER_CONNECTION_MAP.name(), Map.class);

        int ptusPerDay = PtuUtil.getNumberOfPtusPerDay(period, ptuDuration);

        for (ConnectionPortfolioDto connectionPortfolioDTO : connectionPortfolioDTOs) {
            if (elementsPerConnection.containsKey(connectionPortfolioDTO.getConnectionEntityAddress())) {
                List<ElementDto> elements = elementsPerConnection.get(connectionPortfolioDTO.getConnectionEntityAddress());
                mapElementsToPortfolio(elements, connectionPortfolioDTO, period, ptuDuration, ptusPerDay);
            }
        }

        context.setValue(CreateConnectionProfileStepParameter.OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), connectionPortfolioDTOs);
        return context;
    }

    private void mapElementsToPortfolio(List<ElementDto> elements, ConnectionPortfolioDto connectionPortfolioDTO, LocalDate period,
            int ptuDuration, int ptusPerDay) {
        Map<Integer, PowerContainerDto> connectionPowerPerPTU = connectionPortfolioDTO.getConnectionPowerPerPTU();
        for (ElementDto element : elements) {
            Map<Integer, ElementDtuDataDto> elementDtuMap = element.getElementDtuData().stream()
                    .collect(Collectors.toMap(ElementDtuDataDto::getDtuIndex, Function.identity()));

            int dtuDuration = element.getDtuDuration();
            int dtusPerPtu = ptuDuration / dtuDuration;

            for (int ptuIndex = 1; ptuIndex <= ptusPerDay; ptuIndex++) {
                if (!connectionPowerPerPTU.containsKey(ptuIndex)) {
                    connectionPowerPerPTU.put(ptuIndex, new PowerContainerDto(period, ptuIndex));
                }

                //collect data for this ptu.
                ElementDtuDataDto[] collectedElementDtuData = collectDtuData(elementDtuMap, dtusPerPtu, ptuIndex);

                addAverageToPowerContainer(collectedElementDtuData, connectionPowerPerPTU.get(ptuIndex));
            }
        }
    }

    private void addAverageToPowerContainer(ElementDtuDataDto[] collectedElementDtuData, PowerContainerDto powerContainerDto) {
        BigInteger[] uncontrolledLoad = new BigInteger[collectedElementDtuData.length];
        BigInteger[] averageConsumption = new BigInteger[collectedElementDtuData.length];
        BigInteger[] averageProduction = new BigInteger[collectedElementDtuData.length];
        BigInteger[] potentialFlexConsumption = new BigInteger[collectedElementDtuData.length];
        BigInteger[] potentialFlexProduction = new BigInteger[collectedElementDtuData.length];

        for (int i = 0; i < collectedElementDtuData.length; i++) {
            if (collectedElementDtuData[i] == null) {
                continue;
            }
            uncontrolledLoad[i] = collectedElementDtuData[i].getProfileUncontrolledLoad();
            averageConsumption[i] = collectedElementDtuData[i].getProfileAverageConsumption();
            averageProduction[i] = collectedElementDtuData[i].getProfileAverageProduction();
            potentialFlexConsumption[i] = collectedElementDtuData[i].getProfilePotentialFlexConsumption();
            potentialFlexProduction[i] = collectedElementDtuData[i].getProfilePotentialFlexProduction();
        }

        PowerDataDto profile = powerContainerDto.getProfile();
        profile.setUncontrolledLoad(sum(profile.getUncontrolledLoad(), average(uncontrolledLoad)));
        profile.setAverageConsumption(sum(profile.getAverageConsumption(), average(averageConsumption)));
        profile.setAverageProduction(sum(profile.getAverageProduction(), average(averageProduction)));
        profile.setPotentialFlexConsumption(sum(profile.getPotentialFlexConsumption(), average(potentialFlexConsumption)));
        profile.setPotentialFlexProduction(sum(profile.getPotentialFlexProduction(), average(potentialFlexProduction)));
    }

    private BigInteger sum(BigInteger bigInteger1, BigInteger bigInteger2) {
        BigInteger result = BigInteger.ZERO;
        if (bigInteger1 != null) {
            result = result.add(bigInteger1);
        }
        if (bigInteger2 != null) {
            result = result.add(bigInteger2);
        }
        return result;
    }

    private ElementDtuDataDto[] collectDtuData(Map<Integer, ElementDtuDataDto> elementDtuMap, int dtusPerPtu, int ptuIndex) {
        int startDtu = 1 + ((ptuIndex - 1) * dtusPerPtu);
        ElementDtuDataDto[] collectedElementDtuData = new ElementDtuDataDto[dtusPerPtu];
        for (int i = 0; i < dtusPerPtu; i++) {
            collectedElementDtuData[i] = elementDtuMap.get(startDtu + i);
        }
        return collectedElementDtuData;
    }

    private static BigInteger average(BigInteger... values) {
        int count = 0;
        BigInteger total = BigInteger.ZERO;
        for (BigInteger value : values) {
            if (value == null) {
                continue;
            }
            count++;
            total = total.add(value);
        }
        if (count > 0) {
            return total.divide(BigInteger.valueOf(count));
        }
        return null;
    }
}
