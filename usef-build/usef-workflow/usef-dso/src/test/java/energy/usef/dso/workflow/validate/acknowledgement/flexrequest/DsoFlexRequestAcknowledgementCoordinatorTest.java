package energy.usef.dso.workflow.validate.acknowledgement.flexrequest;

import static energy.usef.dso.workflow.validate.acknowledgement.flexrequest.FlexRequestAcknowledgementStepParameter.IN.ACKNOWLEDGEMENT_STATUS_DTO;
import static energy.usef.dso.workflow.validate.acknowledgement.flexrequest.FlexRequestAcknowledgementStepParameter.IN.SEQUENCE_NUMBER;

import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.workflow.DsoWorkflowStep;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class DsoFlexRequestAcknowledgementCoordinatorTest {

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    private DsoFlexRequestAcknowledgementCoordinator coordinator;

    @Before
    public void init() {
        coordinator = new DsoFlexRequestAcknowledgementCoordinator();
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
    }

    @Test
    public void handleEvent() {
        ArgumentCaptor<WorkflowContext> contextCapturer = ArgumentCaptor.forClass(WorkflowContext.class);
        FlexRequestAcknowledgementEvent event = new FlexRequestAcknowledgementEvent(1L, AcknowledgementStatus.ACCEPTED,
                "aggr1.com");
        coordinator.handleEvent(event);

        Mockito.verify(workflowStepExecuter, Mockito.timeout(1000).times(1)).invoke(Mockito.eq(DsoWorkflowStep.DSO_FLEX_REQUEST_ACKNOWLEDGEMENT.name()),
                contextCapturer.capture());

        Assert.assertNotNull(contextCapturer.getValue().getValue(
                ACKNOWLEDGEMENT_STATUS_DTO.name()));
        Assert.assertNotNull(contextCapturer.getValue().getValue(
                SEQUENCE_NUMBER.name()));
    }

}