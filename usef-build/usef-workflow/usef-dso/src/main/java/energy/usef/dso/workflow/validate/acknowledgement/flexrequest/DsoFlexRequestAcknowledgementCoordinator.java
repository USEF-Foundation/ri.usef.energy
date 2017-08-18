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

package energy.usef.dso.workflow.validate.acknowledgement.flexrequest;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.dso.workflow.DsoWorkflowStep.DSO_FLEX_REQUEST_ACKNOWLEDGEMENT;

import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.AcknowledgementStatusDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.validate.acknowledgement.flexrequest.FlexRequestAcknowledgementStepParameter.IN;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flexibility request Acknowledgement workflow coordinator.
 */
@Stateless
public class DsoFlexRequestAcknowledgementCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoFlexRequestAcknowledgementCoordinator.class);
    @Inject
    private WorkflowStepExecuter workflow;

    /**
     * {@inheritDoc}
     */
    @Asynchronous
    public void handleEvent(@Observes FlexRequestAcknowledgementEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        AcknowledgementStatusDto status = AcknowledgementStatusDto.valueOf(event.getAcknowledgementStatus().name());
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(IN.ACKNOWLEDGEMENT_STATUS_DTO.name(), status);
        inContext.setValue(IN.SEQUENCE_NUMBER.name(), event.getSequence());
        inContext.setValue(IN.AGGREGATOR.name(), event.getAggregatorDomain());

        workflow.invoke(DSO_FLEX_REQUEST_ACKNOWLEDGEMENT.name(), inContext);

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }
}
