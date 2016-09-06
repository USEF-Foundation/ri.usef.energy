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

package energy.usef.dso.controller;

import energy.usef.core.controller.BaseIncomingResponseMessageController;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.model.Message;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.model.PtuState;
import energy.usef.core.model.RegimeType;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes common reference update response.
 */
@Stateless
public class CommonReferenceQueryResponseController extends BaseIncomingResponseMessageController<CommonReferenceQueryResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonReferenceQueryResponseController.class);

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;
    @Inject
    private ConfigDso configDso;
    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * {@inheritDoc}
     */
    public void action(CommonReferenceQueryResponse message, Message savedMessage) throws BusinessException {
        LOGGER.debug("CommonReferenceQueryResponse received");
        if (DispositionSuccessFailure.SUCCESS.equals(message.getResult())) {
            Integer initializationDelay = 1;
            Integer initializationDuration = configDso.getIntegerProperty(ConfigDsoParam.DSO_INITIALIZE_PLANBOARD_DAYS_INTERVAL);
            LocalDate initializationDate = findFirstMessageOfConversation(message).getCreationTime()
                    .toLocalDate()
                    .plusDays(initializationDelay);

            // Store connections
            corePlanboardBusinessService.storeCommonReferenceQueryResponse(message, CommonReferenceEntityType.CONGESTION_POINT,
                    initializationDate, initializationDuration);

            initializePtuContainers(initializationDate, initializationDuration);

            message.getCongestionPoint()
                    .forEach(xmlCongestionPoint -> dsoPlanboardBusinessService.updateAggregatorsOnCongestionPointConnectionGroup(
                            xmlCongestionPoint, initializationDate, initializationDuration));
        }
    }

    /**
     * Initialize the PTU containers.
     *
     * @param initializationDate
     * @param initializationDuration
     */
    private void initializePtuContainers(LocalDate initializationDate, Integer initializationDuration) {
        // initialize the PTU containers
        for (int i = 0; i < initializationDuration; ++i) {
            List<PtuContainer> ptuContainers = corePlanboardBusinessService
                    .findOrCreatePtuContainersForPeriod(initializationDate.plusDays(i));
            List<ConnectionGroupState> activeConnectionGroupStates = corePlanboardBusinessService
                    .findActiveConnectionGroupStates(initializationDate.plusDays(i), CongestionPointConnectionGroup.class);
            activeConnectionGroupStates.stream().map(ConnectionGroupState::getConnectionGroup).distinct().forEach(
                    connectionGroup -> ptuContainers.stream()
                            .forEach(ptuContainer -> createPtuState(ptuContainer, connectionGroup)));
        }
    }

    private void createPtuState(PtuContainer ptuContainer, ConnectionGroup connectionGroup) {
        PtuState ptuState = new PtuState();
        ptuState.setPtuContainer(ptuContainer);
        ptuState.setConnectionGroup(connectionGroup);
        ptuState.setRegime(RegimeType.GREEN);
        ptuState.setSequence(sequenceGeneratorService.next());
        ptuState.setState(PtuContainerState.PlanValidate);
        corePlanboardBusinessService.storePtuState(ptuState);
    }

}
