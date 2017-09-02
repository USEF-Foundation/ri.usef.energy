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

package energy.usef.dso.workflow.validate.acknowledgement.flexorder;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_FLEX_REQUEST_ACKNOWLEDGEMENT;

import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.AcknowledgementStatusDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.validate.acknowledgement.flexorder.FlexOrderAcknowledgementStepParameter.IN;
import energy.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestEvent;

import java.math.BigInteger;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flexibility Order Acknowledgement workflow coordinator.
 */
@Stateless
public class DsoFlexOrderAcknowledgementCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DsoFlexOrderAcknowledgementCoordinator.class);

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Inject
    private Event<CreateFlexRequestEvent> eventManager;
    @Inject
    private WorkflowStepExecuter workflow;

    /**
     * {@inheritDoc}
     */
    @Asynchronous
    public void handleEvent(@Observes FlexOrderAcknowledgementEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        // Updating plan-board
        LOGGER.debug("Updating plan-board");
        FlexOrderDto flexOrderDto = dsoPlanboardBusinessService.updateFlexOrdersWithAcknowledgementStatus(event.getSequence(),
                event.getAcknowledgementStatus(), event.getAggregatorDomain());

        if (flexOrderDto == null) {
            LOGGER.warn("No flex order to update was found");
        } else if (AcknowledgementStatus.ACCEPTED != event.getAcknowledgementStatus()) {
            invokePBC(event, flexOrderDto.getFlexOfferSequenceNumber());
            // Flexibility request is rejected or no response received, creating a new request
            CreateFlexRequestEvent createFlexRequestEvent = new CreateFlexRequestEvent(flexOrderDto.getConnectionGroupEntityAddress(),
                    flexOrderDto.getPeriod(),
                    flexOrderDto.getPtus().stream()
                            .map(PtuFlexOrderDto::getPtuIndex)
                            .mapToInt(BigInteger::intValue)
                            .mapToObj(Integer::valueOf)
                            .toArray(Integer[]::new));
            eventManager.fire(createFlexRequestEvent);
        } else {
            invokePBC(event, flexOrderDto.getFlexOfferSequenceNumber());
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void invokePBC(FlexOrderAcknowledgementEvent event, Long flexOfferSequenceNumber) {
        AcknowledgementStatusDto status = AcknowledgementStatusDto.valueOf(event.getAcknowledgementStatus().name());
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(IN.ACKNOWLEDGEMENT_STATUS_DTO.name(), status);
        inContext.setValue(IN.FLEX_OFFER_SEQUENCE_NUMBER.name(), flexOfferSequenceNumber);
        inContext.setValue(IN.AGGREGATOR.name(), event.getAggregatorDomain());

        workflow.invoke(DSO_FLEX_REQUEST_ACKNOWLEDGEMENT.name(), inContext);
    }


}
