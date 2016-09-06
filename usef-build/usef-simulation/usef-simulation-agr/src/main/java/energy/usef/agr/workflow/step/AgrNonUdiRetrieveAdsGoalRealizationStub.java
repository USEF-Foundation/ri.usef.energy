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

import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.workflow.nonudi.dto.CongestionManagementStatusDto;
import energy.usef.agr.workflow.nonudi.dto.ObjectiveAgentStatusDto;
import energy.usef.agr.workflow.nonudi.service.PowerMatcher;
import energy.usef.agr.workflow.nonudi.operate.AgrNonUdiRetrieveAdsGoalRealizationParameter;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.USEFRoleDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

/**
 * Stub implementation of the PBC 'Retrieve ADS goal realization' PBC for a non-UDI aggregator.
 * <p>
 * This PBC takes the following parameters as input:
 * <ul>
 * <li>PERIOD</li> : the period for which one wants to retrieve the ADS goal realization ({@link LocalDate}).
 * <li>PTU_DURATION</li> : the duration of PTU expressed in minutes ({@link Integer}).
 * <li>CURRENT_PORTFOLIO</li> : a list of {@link ConnectionGroupPortfolioDto} which is the current portfolio to
 * update.
 * </ul>
 * <p>
 * This PBC must output:
 * <ul>
 * <li>UPDATED_PORTFOLIO</li> : a list of {@link ConnectionGroupPortfolioDto} which contains the updated value for the different
 * connection groups given in input.
 * </ul>
 */
public class AgrNonUdiRetrieveAdsGoalRealizationStub implements WorkflowStep {
    @Inject
    private PowerMatcher powerMatcher;

    @Override
    @SuppressWarnings("unchecked")
    public WorkflowContext invoke(WorkflowContext context) {
        List<ConnectionGroupPortfolioDto> portfolioDTOs = context.get(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.CURRENT_PORTFOLIO.name(), List.class);
        LocalDate period = context.get(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.PERIOD.name(), LocalDate.class);
        Integer ptuDuration = context.get(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.PTU_DURATION.name(), Integer.class);

        List<ConnectionGroupPortfolioDto> updatedPortfolio = new ArrayList<>();
        for (ConnectionGroupPortfolioDto connectionGroupPortfolioDto : portfolioDTOs) {
            if (connectionGroupPortfolioDto.getUsefRole() == USEFRoleDto.BRP) {
                updatedPortfolio.add(handleBrpConnectionGroupPortfolio(connectionGroupPortfolioDto, period, ptuDuration));
            } else {
                updatedPortfolio.add(
                        handleCongestionPointConnectionGroupPortfolio(connectionGroupPortfolioDto, period, ptuDuration));
            }
        }
        context.setValue(AgrNonUdiRetrieveAdsGoalRealizationParameter.OUT.UPDATED_PORTFOLIO.name(), updatedPortfolio);
        return context;
    }

    private ConnectionGroupPortfolioDto handleCongestionPointConnectionGroupPortfolio(
            ConnectionGroupPortfolioDto connectionGroupPortfolioDto, LocalDate period, Integer ptuDuration) {
        Optional<CongestionManagementStatusDto> congestionManagementStatusDtoOptional = powerMatcher
                .retrieveCongestionPointAdsGoalRealization(connectionGroupPortfolioDto.getUsefIdentifier());
        congestionManagementStatusDtoOptional.ifPresent(congestionManagementStatusDto -> {
            Integer ptuIndex = PtuUtil.getPtuIndex(new LocalDateTime(), ptuDuration);
            updateObservedValue(connectionGroupPortfolioDto, period, ptuIndex,
                    congestionManagementStatusDto.getCurrentAllocation().toBigInteger());

        });
        return connectionGroupPortfolioDto;
    }

    private ConnectionGroupPortfolioDto handleBrpConnectionGroupPortfolio(ConnectionGroupPortfolioDto connectionGroupPortfolioDto,
            LocalDate period, Integer ptuDuration) {
        Optional<ObjectiveAgentStatusDto> objectiveAgentStatusDtoOptional = powerMatcher.retrieveBrpAdsGoalRealization(
                connectionGroupPortfolioDto.getUsefIdentifier());
        objectiveAgentStatusDtoOptional.ifPresent(objectiveAgentStatusDto -> {
            Integer ptuIndex = PtuUtil.getPtuIndex(new LocalDateTime(), ptuDuration);
            updateObservedValue(connectionGroupPortfolioDto, period, ptuIndex,
                    objectiveAgentStatusDto.getCurrentAllocation().toBigInteger());
        });
        return connectionGroupPortfolioDto;
    }

    private void updateObservedValue(ConnectionGroupPortfolioDto connectionGroupPortfolioDto, LocalDate period, Integer ptuIndex,
            BigInteger observedPower) {
        if (connectionGroupPortfolioDto.getConnectionGroupPowerPerPTU().get(ptuIndex) == null) {
            connectionGroupPortfolioDto.getConnectionGroupPowerPerPTU().put(ptuIndex, new PowerContainerDto(period, ptuIndex));
        }

        connectionGroupPortfolioDto.getConnectionGroupPowerPerPTU().get(ptuIndex).getObserved()
                .setUncontrolledLoad(BigInteger.ZERO);
        if (observedPower.compareTo(BigInteger.ZERO) >= 0) {
            connectionGroupPortfolioDto.getConnectionGroupPowerPerPTU().get(ptuIndex).getObserved()
                    .setAverageConsumption(observedPower);
            connectionGroupPortfolioDto.getConnectionGroupPowerPerPTU().get(ptuIndex).getObserved()
                    .setAverageProduction(BigInteger.ZERO);
        } else {
            connectionGroupPortfolioDto.getConnectionGroupPowerPerPTU().get(ptuIndex).getObserved()
                    .setAverageProduction(observedPower);
            connectionGroupPortfolioDto.getConnectionGroupPowerPerPTU().get(ptuIndex).getObserved()
                    .setAverageConsumption(BigInteger.ZERO);
        }
    }

}
