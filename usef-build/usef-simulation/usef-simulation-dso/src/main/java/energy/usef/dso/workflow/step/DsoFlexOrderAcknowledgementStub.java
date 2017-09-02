package energy.usef.dso.workflow.step;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;

/**
 * Workflow step implementation for the Workflow 'DSO Flex Order Acknowledgement'. This implementation expects to find the following parameters
 * as input:
 * <ul>
 * <li>AGGREGATOR Aggregator which send the acknowledgement</li>
 * <li>ACKNOWLEDGEMENT_STATUS_DTO ({@link energy.usef.core.workflow.dto.AcknowledgementStatusDto}): the status</li>
 * <li>FLEX_OFFER_SEQUENCE_NUMBER ({@link Long}): the flex request </li>
 * * </ul>
 *
 * No output as this is only an informational message
 */
public class DsoFlexOrderAcknowledgementStub implements WorkflowStep {

    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        return context;
    }

}