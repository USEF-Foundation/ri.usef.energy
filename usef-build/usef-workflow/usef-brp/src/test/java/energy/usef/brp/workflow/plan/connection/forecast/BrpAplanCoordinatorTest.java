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

package energy.usef.brp.workflow.plan.connection.forecast;

import static energy.usef.core.model.PrognosisType.A_PLAN;
import static org.junit.Assert.assertEquals;

import energy.usef.brp.service.business.BrpPlanboardBusinessService;
import energy.usef.brp.workflow.plan.flexrequest.create.CreateFlexRequestEvent;
import energy.usef.brp.workflow.BrpWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link BrpAplanCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class BrpAplanCoordinatorTest {
    private static final long APLAN_SEQUENCE_1 = 20140216130000456l;
    private static final long APLAN_SEQUENCE_2 = 20140216130000457l;
    private static final String PERIOD = "2014-02-16";
    private static final String AGGREGATOR_DOMAIN_1 = "agr.usef-example.com";
    private static final String AGGREGATOR_DOMAIN_2 = "agr2.usef-example.com";
    private static final Integer PTU_DURATION = 15;

    private BrpAplanCoordinator coordinator;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private BrpPlanboardBusinessService brpPlanboardBusinessService;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private Config config;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private Event<PrepareFlexRequestsEvent> prepareFlexRequestsEventManager;
    @Mock
    private Event<CreateFlexRequestEvent> createFlexRequestEventManager;
    @Mock
    private EventValidationService eventValidationService;

    @Before
    public void init() {
        coordinator = new BrpAplanCoordinator();
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, brpPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, eventValidationService);
        Whitebox.setInternalState(coordinator, "prepareFlexRequestsEventManager", prepareFlexRequestsEventManager);
        Whitebox.setInternalState(coordinator, "createFlexRequestEventManager", createFlexRequestEventManager);
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(PTU_DURATION);
    }

    @Test
    public void testReceivedAPlanEvent() throws BusinessValidationException {
        PowerMockito.when(corePlanboardBusinessService
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.A_PLAN)))
                .thenReturn(buildPlanboardAPlans());
        PowerMockito.when(corePlanboardBusinessService
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.A_PLAN),
                        Matchers.eq(DocumentStatus.RECEIVED)))
                .thenReturn(buildPlanboardAPlans());
        PowerMockito.when(workflowStepExecuter.invoke(Matchers.eq(BrpWorkflowStep.BRP_RECEIVED_APLAN.name()), Mockito.any())).then(
                call -> buildReceivedAPlanWorkflowContext((WorkflowContext) call.getArguments()[1]));
        PowerMockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Matchers.eq(APLAN_SEQUENCE_1),
                Matchers.eq(DocumentType.A_PLAN), Matchers.eq(AGGREGATOR_DOMAIN_1))).then(invocation -> {
            PlanboardMessage planboardMessage = new PlanboardMessage();
            Message message = new Message();
            message.setConversationId(UUID.randomUUID().toString());
            planboardMessage.setMessage(message);
            planboardMessage.setDocumentStatus(DocumentStatus.RECEIVED);
            return planboardMessage;
        });
        PowerMockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Matchers.eq(APLAN_SEQUENCE_2),
                Matchers.eq(DocumentType.A_PLAN), Matchers.eq(AGGREGATOR_DOMAIN_2))).then(invocation -> {
            PlanboardMessage planboardMessage = new PlanboardMessage();
            Message message = new Message();
            message.setConversationId(UUID.randomUUID().toString());
            planboardMessage.setMessage(message);
            planboardMessage.setDocumentStatus(DocumentStatus.RECEIVED);
            return planboardMessage;
        });

        coordinator.receivedAPlanEvent(new ReceivedAPlanEvent(DateTimeUtil.getCurrentDate()));

        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(Matchers.any(String.class));
        Mockito.verify(prepareFlexRequestsEventManager, Mockito.times(1)).fire(Matchers.any(PrepareFlexRequestsEvent.class));
    }

    @Test
    public void testPrepareFlexRequestsEvent() throws BusinessValidationException {
        PowerMockito.when(corePlanboardBusinessService
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.A_PLAN),
                        Matchers.eq(DocumentStatus.PROCESSED))).thenReturn(buildPlanboardAPlans());
        PowerMockito.when(workflowStepExecuter.invoke(Matchers.eq(BrpWorkflowStep.BRP_PREPARE_FLEX_REQUESTS.name()),
                Mockito.any())).then(
                call -> buildCreateFlexRequestEventWorkflowContext((WorkflowContext) call.getArguments()[1]));
        PowerMockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Matchers.eq(APLAN_SEQUENCE_1),
                Matchers.eq(DocumentType.A_PLAN), Matchers.eq(AGGREGATOR_DOMAIN_1))).then(invocation -> {
            PlanboardMessage planboardMessage = new PlanboardMessage();
            Message message = new Message();
            message.setConversationId(UUID.randomUUID().toString());
            planboardMessage.setMessage(message);
            return planboardMessage;
        });
        PowerMockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Matchers.eq(APLAN_SEQUENCE_2),
                Matchers.eq(DocumentType.A_PLAN), Matchers.eq(AGGREGATOR_DOMAIN_2))).then(invocation -> {
            PlanboardMessage planboardMessage = new PlanboardMessage();
            Message message = new Message();
            message.setConversationId(UUID.randomUUID().toString());
            planboardMessage.setMessage(message);
            return planboardMessage;
        });

        coordinator.prepareFlexRequestsEvent(new PrepareFlexRequestsEvent(DateTimeUtil.getCurrentDate()));

        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(Matchers.any(String.class));
        ArgumentCaptor<CreateFlexRequestEvent> captor = ArgumentCaptor.forClass(CreateFlexRequestEvent.class);
        Mockito.verify(createFlexRequestEventManager, Mockito.times(1)).fire(captor.capture());
        assertEquals(1, captor.getValue().getFlexRequestDtos().size());
    }

    private List<PtuPrognosis> buildPlanboardAPlans() {
        ConnectionGroup agrConnectionGroup = new AgrConnectionGroup();
        agrConnectionGroup.setUsefIdentifier(AGGREGATOR_DOMAIN_1);

        // let's assume 6 ptus per day
        List<PtuPrognosis> aplans = new ArrayList<>();
        for (int i = 1; i <= 6; ++i) {
            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setPtuIndex(i);
            ptuContainer.setPtuDate(DateTimeUtil.parseDate(PERIOD));
            PtuPrognosis aplan = new PtuPrognosis();
            aplan.setConnectionGroup(agrConnectionGroup);
            aplan.setId((long) i * -1);
            aplan.setParticipantDomain(AGGREGATOR_DOMAIN_1);
            aplan.setPower(BigInteger.valueOf(1000 * i));
            aplan.setPtuContainer(ptuContainer);
            aplan.setSequence(APLAN_SEQUENCE_1);
            aplan.setType(A_PLAN);
            aplans.add(aplan);
        }

        ConnectionGroup agrConnectionGroup2 = new AgrConnectionGroup();
        agrConnectionGroup2.setUsefIdentifier(AGGREGATOR_DOMAIN_2);

        for (int i = 1; i <= 6; ++i) {
            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setPtuIndex(i);
            ptuContainer.setPtuDate(DateTimeUtil.parseDate(PERIOD));
            PtuPrognosis aplan = new PtuPrognosis();
            aplan.setConnectionGroup(agrConnectionGroup2);
            aplan.setId((long) i * -1);
            aplan.setParticipantDomain(AGGREGATOR_DOMAIN_2);
            aplan.setPower(BigInteger.valueOf(1000 * i));
            aplan.setPtuContainer(ptuContainer);
            aplan.setSequence(APLAN_SEQUENCE_2);
            aplan.setType(A_PLAN);
            aplans.add(aplan);
        }

        return aplans;
    }

    private WorkflowContext buildReceivedAPlanWorkflowContext(WorkflowContext context) {
        List<PrognosisDto> acceptedAPlans = new ArrayList<>();
        PrognosisDto acceptedAPlan = new PrognosisDto();
        acceptedAPlan.setSequenceNumber(APLAN_SEQUENCE_1);
        acceptedAPlan.setParticipantDomain(AGGREGATOR_DOMAIN_1);
        acceptedAPlans.add(acceptedAPlan);
        context.setValue(ReceivedAPlanWorkflowParameter.OUT.ACCEPTED_A_PLAN_DTO_LIST.name(), acceptedAPlans);

        List<PrognosisDto> processedAPlans = new ArrayList<>();
        PrognosisDto processedAPlan = new PrognosisDto();
        processedAPlan.setSequenceNumber(APLAN_SEQUENCE_2);
        processedAPlan.setParticipantDomain(AGGREGATOR_DOMAIN_2);
        processedAPlans.add(processedAPlan);
        context.setValue(ReceivedAPlanWorkflowParameter.OUT.PROCESSED_A_PLAN_DTO_LIST.name(), processedAPlans);
        return context;

    }

    private WorkflowContext buildCreateFlexRequestEventWorkflowContext(WorkflowContext context) {

        List<FlexRequestDto> flexRequestDtos = new ArrayList<>();
        FlexRequestDto flexRequestDto = new FlexRequestDto();
        flexRequestDto.setParticipantDomain(AGGREGATOR_DOMAIN_2);
        flexRequestDto.setPrognosisSequenceNumber(APLAN_SEQUENCE_2);
        flexRequestDtos.add(flexRequestDto);
        context.setValue(PrepareFlexRequestWorkflowParameter.OUT.FLEX_REQUEST_DTO_LIST.name(), flexRequestDtos);

        List<PrognosisDto> acceptedAPlans = new ArrayList<>();
        PrognosisDto acceptedAPlan = new PrognosisDto();
        acceptedAPlan.setSequenceNumber(APLAN_SEQUENCE_1);
        acceptedAPlan.setParticipantDomain(AGGREGATOR_DOMAIN_1);
        acceptedAPlans.add(acceptedAPlan);
        context.setValue(PrepareFlexRequestWorkflowParameter.OUT.ACCEPTED_A_PLAN_DTO_LIST.name(),
                acceptedAPlans);

        return context;

    }
}
