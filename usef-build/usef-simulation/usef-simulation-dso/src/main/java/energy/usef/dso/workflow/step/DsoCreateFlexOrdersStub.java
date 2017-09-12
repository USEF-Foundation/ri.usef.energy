package energy.usef.dso.workflow.step;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import java.util.List;

/**
 * Workflow step implementation for the Workflow 'DSO Place Flex Orders'. This implementation expects to find the following parameters
 * as input:
 * <ul>
 * <li>FLEX_ORDER_DTO_LIST ({@link List <FlexOrderDto>}): Flex order DTO list.</li>
 * </ul>
 *
 */
public class DsoCreateFlexOrdersStub implements WorkflowStep {

    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        return context;
    }
}
