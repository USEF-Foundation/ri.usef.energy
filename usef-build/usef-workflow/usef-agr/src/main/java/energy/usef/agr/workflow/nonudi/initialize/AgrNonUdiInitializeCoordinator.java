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

package energy.usef.agr.workflow.nonudi.initialize;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_INITIALIZE_NON_UDI_CLUSTERS;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.workflow.nonudi.initialize.AgrInitializeNonUdiClustersParameter.IN;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This coordinator will initialize the non-udi clusters, e.g. the PowerMatcher clusters.
 */
@Stateless
public class AgrNonUdiInitializeCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrNonUdiInitializeCoordinator.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private ConfigAgr configAgr;

    @Inject
    private EventValidationService eventValidationService;

    /**
     * This method will start the initialization of the clusters.
     */
    @Asynchronous
    public void initializeCluster(@Observes AgrNonUdiInitializeEvent event) throws BusinessValidationException {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        eventValidationService.validateEventPeriodTodayOrInFuture(event);

        // Check if aggregator really is a non-udi aggregator
        if (!configAgr.getBooleanProperty(ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR)) {
            LOGGER.warn("Aggregator that is not currently configured as a non-udi aggregator ({}) can not initialize cluster!",
                    ConfigAgrParam.AGR_IS_NON_UDI_AGGREGATOR);
            return;
        }

        // retrieve all connection groups for the requested period
        Map<ConnectionGroup, List<Connection>> activeConnectionGroupsWithConnections = corePlanboardBusinessService
                .findActiveConnectionGroupsWithConnections(event.getPeriod());

        Map<String, List<String>> brpConnectionListMap = new HashMap<>();
        Map<String, List<String>> cpConnectionListMap = new HashMap<>();
        for (Map.Entry<ConnectionGroup, List<Connection>> entry : activeConnectionGroupsWithConnections.entrySet()) {
            ConnectionGroup connectionGroup = entry.getKey();
            List<String> connectionList = entry.getValue().stream().map(Connection::getEntityAddress).collect(Collectors.toList());
            if (connectionGroup instanceof BrpConnectionGroup) {
                brpConnectionListMap.put(connectionGroup.getUsefIdentifier(), connectionList);
            } else if (connectionGroup instanceof CongestionPointConnectionGroup) {
                cpConnectionListMap.put(connectionGroup.getUsefIdentifier(), connectionList);
            }
        }

        // Invoking PBC
        DefaultWorkflowContext inputContext = new DefaultWorkflowContext();
        inputContext.setValue(IN.PERIOD.name(), event.getPeriod());
        inputContext.setValue(IN.BRP_CONNECTION_LIST_MAP.name(), brpConnectionListMap);
        inputContext.setValue(IN.CP_CONNECTION_LIST_MAP.name(), cpConnectionListMap);

        workflowStepExecuter.invoke(AGR_INITIALIZE_NON_UDI_CLUSTERS.name(), inputContext);

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }
}
