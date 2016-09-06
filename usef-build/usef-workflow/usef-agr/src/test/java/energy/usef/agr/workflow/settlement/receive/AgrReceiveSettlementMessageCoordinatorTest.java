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

package energy.usef.agr.workflow.settlement.receive;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import energy.usef.agr.workflow.AgrWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.DispositionAcceptedDisputedDto;
import energy.usef.core.workflow.settlement.CoreSettlementBusinessService;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.Collections;
import java.util.Optional;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class in charge of the unit tests related to the {@link AgrReceiveSettlementMessageCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrReceiveSettlementMessageCoordinatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrReceiveSettlementMessageCoordinatorTest.class);

    private AgrReceiveSettlementMessageCoordinator coordinator;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private CoreSettlementBusinessService coreSettlementBusinessService;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private Config config;
    @Mock
    private JMSHelperService jmsHelperService;

    @Before
    public void init() {
        coordinator = new AgrReceiveSettlementMessageCoordinator();
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, coreSettlementBusinessService);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("agr.usef-example.com");
    }

    @Test
    public void test() {
        final LocalDate startDate = new LocalDate(2015, 1, 1);
        final LocalDate endDate = new LocalDate(2015, 1, 31);
        CheckSettlementEvent event = new CheckSettlementEvent();
        event.setPeriodInMonth(new LocalDate(2015, 1, 15));
        when(corePlanboardBusinessService.findPlanboardMessages(DocumentType.FLEX_ORDER_SETTLEMENT, startDate, endDate,
                DocumentStatus.RECEIVED)).thenReturn(Collections.singletonList(buildFlexOrderSettlementMessage()));
        when(workflowStepExecuter
                .invoke(eq(AgrWorkflowStep.AGR_VALIDATE_SETTLEMENT_ITEMS.name()), any(WorkflowContext.class))).then(
                call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(AgrReceiveSettlementMessageWorkflowParameter.OUT.FLEX_ORDER_SETTLEMENT_DISPOSITION.name(),
                            DispositionAcceptedDisputedDto.ACCEPTED);
                    return context;
                });
        coordinator.handleCheckSettlementEvent(event);
        verify(coreSettlementBusinessService, times(1)).findFlexOrderSettlementsForPeriod(
                eq(startDate),
                eq(endDate),
                eq(Optional.<String>empty()),
                eq(Optional.of("dso.usef-example.com")));
        verify(workflowStepExecuter, times(1)).invoke(
                eq(AgrWorkflowStep.AGR_VALIDATE_SETTLEMENT_ITEMS.name()),
                any(WorkflowContext.class));
        verify(jmsHelperService, times(1)).sendMessageToOutQueue(any(String.class));

    }

    private PlanboardMessage buildFlexOrderSettlementMessage() {
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setPeriod(new LocalDate(2015, 1, 1));
        planboardMessage.setDocumentType(DocumentType.FLEX_ORDER_SETTLEMENT);
        planboardMessage.setDocumentStatus(DocumentStatus.RECEIVED);
        planboardMessage.setParticipantDomain("dso.usef-example.com");
        planboardMessage.setMessage(buildMessage());
        return planboardMessage;
    }

    private Message buildMessage() {
        StringBuilder xml = new StringBuilder();
        xml.append("<SettlementMessage PTU-Duration=\"120\" >");
        xml.append("<MessageMetadata SenderDomain=\"dso.usef-example.com\" SenderRole=\"DSO\" />");
        xml.append("<FlexOrderSettlement OrderReference=\"1\" CongestionPoint=\"ean.000000000001\">");
        xml.append("<PTUSettlement Start=\"1\" Duration=\"12\" PrognosisPower=\"1000\" OrderedFlexPower=\"1000\" ");
        xml.append("  ActualPower=\"1500\" DeliveredFlexPower=\"500\" NetSettlement=\"10.99\" />");
        xml.append("</FlexOrderSettlement></SettlementMessage>");
        Message message = new Message();
        message.setXml(xml.toString());
        return message;
    }

}
