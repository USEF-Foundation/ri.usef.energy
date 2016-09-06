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

package energy.usef.agr.workflow.nonudi.operate;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.USEFRoleDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;

import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class in charge of the workflow 'Retrieve ADS Goal Realization' for a non-UDI aggregator. The process is triggered
 * by a time-based event.
 */
@Singleton
public class AgrNonUdiRetrieveAdsGoalRealizationCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrNonUdiRetrieveAdsGoalRealizationCoordinator.class);

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private Config config;

    @Inject
    private ConfigAgr configAgr;

    @Asynchronous
    public void handleRetrieveAdsGoalRealizationEvent(@Observes AgrNonUdiRetrieveAdsGoalRealizationEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        // variables of the workflow

        if (!configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            LOGGER.warn("Aggregator that is not as a non-udi aggregator ({}), cannot retrieve ADS Goal Realization!",
                    ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR);
            return;
        }
        final LocalDate period = DateTimeUtil.getCurrentDate();

        List<ConnectionGroupPortfolioDto> connectionGroupPortfolioDtos = buildConnectionGroupPortfolioDto(period);

        if (connectionGroupPortfolioDtos.isEmpty()) {
            LOGGER.debug("Connection portfolio on Connection Group level is empty. Workflow will interrupt.");
            return;
        }
        // invocation of the PBC
        List<ConnectionGroupPortfolioDto> updatedPortfolio = invokePBC(period, connectionGroupPortfolioDtos);
        // update the portfolio with the updated values from the PBC.
        agrPortfolioBusinessService.updateConnectionGroupPowerContainers(updatedPortfolio, period);
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private List<ConnectionGroupPortfolioDto> invokePBC(LocalDate period,
            List<ConnectionGroupPortfolioDto> connectionGroupPortfolioDtos) {
        // build the context for the PBC
        WorkflowContext inputContext = new DefaultWorkflowContext();
        inputContext.setValue(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.PERIOD.name(), period);
        inputContext.setValue(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
        inputContext.setValue(AgrNonUdiRetrieveAdsGoalRealizationParameter.IN.CURRENT_PORTFOLIO.name(), connectionGroupPortfolioDtos);
        // invocation of the PBC
        WorkflowContext outputContext = workflowStepExecuter
                .invoke(AgrWorkflowStep.AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION.name(), inputContext);
        WorkflowUtil.validateContext(AgrWorkflowStep.AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION.name(), outputContext, AgrNonUdiRetrieveAdsGoalRealizationParameter.OUT
                .values());
        return outputContext.get(AgrNonUdiRetrieveAdsGoalRealizationParameter.OUT.UPDATED_PORTFOLIO.name(), List.class);
    }

    private List<ConnectionGroupPortfolioDto> buildConnectionGroupPortfolioDto(LocalDate period) {
        return corePlanboardBusinessService.findActiveConnectionGroupStates(period, null).stream()
                .map(ConnectionGroupState::getConnectionGroup)
                .distinct()
                .map(connectionGroup -> {
                    if (connectionGroup instanceof BrpConnectionGroup) {
                        return new ConnectionGroupPortfolioDto(connectionGroup.getUsefIdentifier(), USEFRoleDto.BRP);
                    } else {
                        return new ConnectionGroupPortfolioDto(connectionGroup.getUsefIdentifier(), USEFRoleDto.DSO);
                    }
                })
                .collect(Collectors.toList());
    }

}
