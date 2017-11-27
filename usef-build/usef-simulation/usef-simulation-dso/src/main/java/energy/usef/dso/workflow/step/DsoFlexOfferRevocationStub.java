package energy.usef.dso.workflow.step;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;

/**
 * Workflow step implementation for the Workflow 'DSO Flex Offer revocation'. This implementation expects to find the following parameters
 * as input:
 * <ul>
 * <li>AGGREGATOR Aggregator which send the acknowledgement</li>
 * <li>FLEX_OFFER_DTO list of {@link energy.usef.core.workflow.dto.FlexOfferDto}</li>
 * </ul>
 *
 * No output expected as this is only an informational message
 */
public class DsoFlexOfferRevocationStub implements WorkflowStep {

    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        return context;
    }

}