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

package energy.usef.agr.workflow.nonudi.goals;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_NON_UDI_SET_ADS_GOALS;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.transformer.PrognosisTransformer;

import java.util.List;

import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This coordinator will set the ADS goals via the PowerMatcher.
 */
@Stateless
public class AgrNonUdiSetAdsGoalsCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrNonUdiSetAdsGoalsCoordinator.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private Config config;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * This method will start the initialization of the clusters.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void handleEvent(@Observes(during = TransactionPhase.AFTER_COMPLETION) AgrNonUdiSetAdsGoalsEvent event) throws BusinessValidationException {
        setGoals(event);
    }

    private void setGoals(AgrNonUdiSetAdsGoalsEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);
        // Check if aggregator really is a non-udi aggregator
        if (!configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            LOGGER.warn("Aggregator that is not currently configured as a non-udi aggregator ({}) can not set ADS goals!",
                    ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR);
            return;
        }

        if (StringUtils.isBlank(event.getUsefIdentifier())) {
            LOGGER.error("Missing required usefIdentifier in the AgrNonUdiSetAdsGoalsEvent.");
            return;
        }

        boolean usefIdentifierIsActive = corePlanboardBusinessService.findActiveConnectionGroupStates(event.getPeriod(), null)
                .stream()
                .map(connectionGroupState -> connectionGroupState.getConnectionGroup().getUsefIdentifier()).distinct()
                .anyMatch(usefIdentifier -> usefIdentifier.equals(event.getUsefIdentifier()));
        if (!usefIdentifierIsActive) {
            LOGGER.error("Trying to set ADS goals for an non-active usefIdentifier {}. This is not allowed.",
                    event.getUsefIdentifier());
            return;
        }

        List<PtuPrognosis> lastPrognoses = corePlanboardBusinessService
                .findLastPrognoses(event.getPeriod(), event.getUsefIdentifier());
        if (lastPrognoses == null || lastPrognoses.isEmpty()) {
            LOGGER.warn("Trying to set ADS goals while there are no prognoses yet. Exiting this workflow.");
            return;
        }

        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(AgrNonUdiSetAdsGoalsParameter.IN.PERIOD.name(), event.getPeriod());
        context.setValue(AgrNonUdiSetAdsGoalsParameter.IN.PTU_DURATION.name(),
                config.getIntegerProperty(ConfigParam.PTU_DURATION));
        context.setValue(AgrNonUdiSetAdsGoalsParameter.IN.PROGNOSIS_DTO.name(), PrognosisTransformer.mapToPrognosis(lastPrognoses));

        workflowStepExecuter.invoke(AGR_NON_UDI_SET_ADS_GOALS.name(), context);

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }
}
