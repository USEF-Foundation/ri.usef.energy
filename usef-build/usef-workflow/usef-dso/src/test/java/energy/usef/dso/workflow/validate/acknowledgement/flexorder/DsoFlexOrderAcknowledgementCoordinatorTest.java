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

import static energy.usef.core.model.AcknowledgementStatus.ACCEPTED;
import static energy.usef.dso.workflow.validate.acknowledgement.flexorder.FlexOrderAcknowledgementStepParameter.IN.ACKNOWLEDGEMENT_STATUS_DTO;
import static energy.usef.dso.workflow.validate.acknowledgement.flexorder.FlexOrderAcknowledgementStepParameter.IN.FLEX_ORDER_SEQUENCE_NUMBER;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.util.ReflectionUtil;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestEvent;
import java.math.BigInteger;
import java.util.stream.IntStream;
import javax.enterprise.event.Event;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

/**
 * Test class in charge of the unit tests related to the {@link DsoFlexOrderAcknowledgementCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class DsoFlexOrderAcknowledgementCoordinatorTest {
    private static final String WORKFLOW_ENDED_LOG = "Workflow is ended";
    private static final String NO_FLEX_ORDER_FOUND_LOG = "No flex order to update was found";
    private static final Long SEQUENCE = 1234L;
    private static final AcknowledgementStatus ACKNOWLEDGEMENT_STATUS = AcknowledgementStatus.REJECTED;
    private static final String AGGREGATOR_DOMAIN = "test.com";

    private DsoFlexOrderAcknowledgementCoordinator dsoFlexOrderAcknowledgementCoordinator;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private Logger LOGGER;

    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Mock
    private Event<CreateFlexRequestEvent> eventManager;

    @Before
    public void init() throws Exception {
        dsoFlexOrderAcknowledgementCoordinator = new DsoFlexOrderAcknowledgementCoordinator();

        ReflectionUtil.setFinalStatic(DsoFlexOrderAcknowledgementCoordinator.class.getDeclaredField("LOGGER"), LOGGER);
        Whitebox.setInternalState(dsoFlexOrderAcknowledgementCoordinator, dsoPlanboardBusinessService);
        Whitebox.setInternalState(dsoFlexOrderAcknowledgementCoordinator, eventManager);
        Whitebox.setInternalState(dsoFlexOrderAcknowledgementCoordinator, workflowStepExecuter);
    }

    /**
     * Tests DsoFlexRequestAcknowledgementCoordinator.sendOperate method.
     */
    @Test
    public void testInvokeWorkflow() {
        ArgumentCaptor<WorkflowContext> contextCapturer = ArgumentCaptor.forClass(WorkflowContext.class);
        ArgumentCaptor<CreateFlexRequestEvent> eventCaptor = ArgumentCaptor.forClass(CreateFlexRequestEvent.class);

        FlexOrderDto dto = createFlexRequestDto();
        Mockito.when(
                dsoPlanboardBusinessService.updateFlexOrdersWithAcknowledgementStatus(SEQUENCE, ACKNOWLEDGEMENT_STATUS,
                        AGGREGATOR_DOMAIN))
                .thenReturn(dto);

        dsoFlexOrderAcknowledgementCoordinator
                .handleEvent(new FlexOrderAcknowledgementEvent(SEQUENCE, ACKNOWLEDGEMENT_STATUS,
                AGGREGATOR_DOMAIN));

        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());
        Assert.assertEquals(dto.getConnectionGroupEntityAddress(), eventCaptor.getValue().getCongestionPointEntityAddress());

        Mockito.verify(workflowStepExecuter, Mockito.timeout(1000).times(1)).invoke(Mockito.eq(DsoWorkflowStep.DSO_FLEX_ORDER_ACKNOWLEDGEMENT.name()),
                contextCapturer.capture());

        Assert.assertNotNull(contextCapturer.getValue().getValue(ACKNOWLEDGEMENT_STATUS_DTO.name()));
        Assert.assertThat(contextCapturer.getValue().getValue(FLEX_ORDER_SEQUENCE_NUMBER.name()), is(1234L));
    }

    /**
     * Tests DsoFlexRequestAcknowledgementCoordinator.sendOperate method.
     */
    @Test
    public void testInvokeWorkflowWithAccepted() {
        ArgumentCaptor<WorkflowContext> contextCapturer = ArgumentCaptor.forClass(WorkflowContext.class);
        ArgumentCaptor<CreateFlexRequestEvent> eventCaptor = ArgumentCaptor
                .forClass(CreateFlexRequestEvent.class);

        FlexOrderDto dto = createFlexRequestDto();
        Mockito.when(
                dsoPlanboardBusinessService.updateFlexOrdersWithAcknowledgementStatus(SEQUENCE, ACCEPTED,
                        AGGREGATOR_DOMAIN))
                .thenReturn(dto);

        dsoFlexOrderAcknowledgementCoordinator.handleEvent(new FlexOrderAcknowledgementEvent(SEQUENCE, ACCEPTED,
                AGGREGATOR_DOMAIN));

        Mockito.verifyZeroInteractions(eventManager);
        Mockito.verify(workflowStepExecuter, Mockito.timeout(1000).times(1)).invoke(Mockito.eq(DsoWorkflowStep.DSO_FLEX_ORDER_ACKNOWLEDGEMENT.name()),
                contextCapturer.capture());

        Assert.assertNotNull(contextCapturer.getValue().getValue(ACKNOWLEDGEMENT_STATUS_DTO.name()));
        Assert.assertThat(contextCapturer.getValue().getValue(FLEX_ORDER_SEQUENCE_NUMBER.name()), is(1234L));
    }

    /**
     * Tests DsoFlexRequestAcknowledgementCoordinator.sendOperate method with wrong result from plan board update.
     */
    @Test
    public void testInvokeWorkflowWithNullEntityAddressResult() {
        Mockito.when(
                dsoPlanboardBusinessService.updateFlexOrdersWithAcknowledgementStatus(SEQUENCE, ACKNOWLEDGEMENT_STATUS,
                        AGGREGATOR_DOMAIN))
                .thenReturn(null);

        dsoFlexOrderAcknowledgementCoordinator
                .handleEvent(new FlexOrderAcknowledgementEvent(SEQUENCE, ACKNOWLEDGEMENT_STATUS,
                AGGREGATOR_DOMAIN));

        Mockito.verify(eventManager, Mockito.times(0)).fire(Matchers.any(CreateFlexRequestEvent.class));
        verify(LOGGER, times(1)).warn(
                contains(NO_FLEX_ORDER_FOUND_LOG));
        Mockito.verifyZeroInteractions(workflowStepExecuter);
    }


    private FlexOrderDto createFlexRequestDto() {
        FlexOrderDto dto = new FlexOrderDto();

        dto.setSequenceNumber(1234L);
        dto.setConnectionGroupEntityAddress("abc.com");
        dto.setPeriod(new LocalDate());
        IntStream.range(1, 4).mapToObj(BigInteger::valueOf).forEach(i -> {
            PtuFlexOrderDto ptuFlexOrderDto = new PtuFlexOrderDto();
            ptuFlexOrderDto.setPtuIndex(i);
            dto.getPtus().add(ptuFlexOrderDto);
        });
        return dto;
    }

}
