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

package energy.usef.agr.controller;

import energy.usef.agr.service.business.PrognosisResponseBusinessService;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOrderStatus;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PrognosisResponse;
import energy.usef.core.event.RequestMoveToValidateEvent;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.MessageService;

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
 * Test class in charge of the unit tests related to the {@link PrognosisResponseController}.
 */
@RunWith(PowerMockRunner.class)
public class PrognosisResponseControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private PrognosisResponseBusinessService prognosisResponseBusinessService;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private Event<RequestMoveToValidateEvent> moveToValidateEventManager;

    private PrognosisResponseController controller;

    @Before
    public void init() {
        controller = new PrognosisResponseController();
        prognosisResponseBusinessService = new PrognosisResponseBusinessService();
        Whitebox.setInternalState(controller, messageService);
        Whitebox.setInternalState(controller, prognosisResponseBusinessService);
        Whitebox.setInternalState(controller, corePlanboardBusinessService);
        Whitebox.setInternalState(controller, "moveToValidateEventManager", moveToValidateEventManager);

        PowerMockito.when(messageService.getInitialMessageOfConversation(Matchers.any(String.class))).thenReturn(new Message());
    }

    @Test
    public void testActionOnAccepted() throws Exception {
        PrognosisResponse prognosisResponse = buildPrognosisResponse(DispositionAcceptedRejected.ACCEPTED);
        PlanboardMessage originalAPlan = new PlanboardMessage();
        originalAPlan.setDocumentStatus(DocumentStatus.SENT);
        originalAPlan.setPeriod(new LocalDate());
        originalAPlan.setConnectionGroup(new BrpConnectionGroup());
        originalAPlan.getConnectionGroup().setUsefIdentifier("brp.usef-example.com");

        Mockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(prognosisResponse.getPrognosisSequence(),
                DocumentType.A_PLAN, prognosisResponse.getMessageMetadata().getSenderDomain())).thenReturn(originalAPlan);

        controller.action(prognosisResponse, null);

        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).updatePrognosisStatus(
                prognosisResponse.getPrognosisSequence(),
                prognosisResponse.getMessageMetadata().getSenderDomain(), DocumentType.A_PLAN, DocumentStatus.ACCEPTED);
        Mockito.verify(moveToValidateEventManager, Mockito.times(1)).fire(Matchers.any(RequestMoveToValidateEvent.class));
        // Mockito.verify(createDPrognosisEventManager, Mockito.times(1)).fire(Matchers.any(CreateDPrognosisEvent.class));
    }

    @Test
    public void testActionOnRejected() throws Exception {
        PrognosisResponse prognosisResponse = buildPrognosisResponse(DispositionAcceptedRejected.REJECTED);
        PlanboardMessage originalAPlan = new PlanboardMessage();
        originalAPlan.setDocumentStatus(DocumentStatus.SENT);
        originalAPlan.setPeriod(new LocalDate());
        originalAPlan.setConnectionGroup(new BrpConnectionGroup());
        originalAPlan.getConnectionGroup().setUsefIdentifier("brp.usef-example.com");

        Mockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(prognosisResponse.getPrognosisSequence(),
                DocumentType.A_PLAN, prognosisResponse.getMessageMetadata().getSenderDomain())).thenReturn(originalAPlan);

        controller.action(prognosisResponse, null);

        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).updatePrognosisStatus(
                prognosisResponse.getPrognosisSequence(),
                prognosisResponse.getMessageMetadata().getSenderDomain(), DocumentType.A_PLAN, DocumentStatus.REJECTED);
    }

    /**
     * If PrognosisResponse contains a FlexOrderStatus section with isValidated=false, this should be logged.
     *
     * @throws BusinessException
     */
    @Test
    public void testActionOnNotValidatedFalse() throws BusinessException {
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);

        PrognosisResponse response = buildPrognosisResponse(DispositionAcceptedRejected.REJECTED);
        PlanboardMessage originalAPlan = new PlanboardMessage();
        originalAPlan.setDocumentStatus(DocumentStatus.SENT);
        originalAPlan.setPeriod(new LocalDate());
        originalAPlan.setConnectionGroup(new BrpConnectionGroup());
        originalAPlan.getConnectionGroup().setUsefIdentifier("brp.usef-example.com");

        Mockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(response.getPrognosisSequence(),
                DocumentType.A_PLAN, response.getMessageMetadata().getSenderDomain())).thenReturn(originalAPlan);

        FlexOrderStatus status = new FlexOrderStatus();
        status.setIsValidated(false);
        status.setSequence(101L);
        response.getFlexOrderStatus().add(status);
        controller.action(response, null);
        Mockito.verify(messageService, Mockito.times(1)).storeMessageError(message.capture(),
                Matchers.eq("Prognosis could not be validated. Flexoffer: 101"),
                Matchers.eq(0));
    }

    /**
     * If PrognosisResponse contains a FlexOrderStatus section without "isValidated" property, this should be logged.
     *
     * @throws BusinessException
     */
    @Test
    public void testActionOnNotValidatedNoIsValidated() throws BusinessException {
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);

        PrognosisResponse response = buildPrognosisResponse(DispositionAcceptedRejected.REJECTED);
        PlanboardMessage originalAPlan = new PlanboardMessage();
        originalAPlan.setDocumentStatus(DocumentStatus.SENT);
        originalAPlan.setPeriod(new LocalDate());
        originalAPlan.setConnectionGroup(new BrpConnectionGroup());
        originalAPlan.getConnectionGroup().setUsefIdentifier("brp.usef-example.com");

        Mockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(response.getPrognosisSequence(),
                DocumentType.A_PLAN, response.getMessageMetadata().getSenderDomain())).thenReturn(originalAPlan);

        FlexOrderStatus status = new FlexOrderStatus();
        status.setSequence(101L);
        response.getFlexOrderStatus().add(status);
        response.getFlexOrderStatus().add(status);
        controller.action(response, null);
        Mockito.verify(messageService, Mockito.times(1)).storeMessageError(message.capture(),
                Matchers.eq("Prognosis could not be validated. Flexoffer: 101, 101"),
                Matchers.eq(0));
    }

    /**
     * If PrognosisResponse contains a FlexOrderStatus section with isValidated=false, this should be logged.
     *
     * @throws BusinessException
     */
    @Test
    public void testActionOnNotValidatedTrue() throws BusinessException {
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);

        PrognosisResponse response = buildPrognosisResponse(DispositionAcceptedRejected.REJECTED);

        PlanboardMessage originalAPlan = new PlanboardMessage();
        originalAPlan.setDocumentStatus(DocumentStatus.SENT);
        originalAPlan.setPeriod(new LocalDate());
        originalAPlan.setConnectionGroup(new BrpConnectionGroup());
        originalAPlan.getConnectionGroup().setUsefIdentifier("brp.usef-example.com");

        Mockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(response.getPrognosisSequence(),
                DocumentType.A_PLAN, response.getMessageMetadata().getSenderDomain())).thenReturn(originalAPlan);

        FlexOrderStatus status = new FlexOrderStatus();
        status.setIsValidated(true);
        status.setSequence(101L);
        response.getFlexOrderStatus().add(status);
        controller.action(response, null);
        Mockito.verify(messageService, Mockito.times(0)).storeMessageError(message.capture(),
                Matchers.eq("Prognosis could not be validated. Flexoffer: 101"),
                Matchers.eq(0));
    }

    private PrognosisResponse buildPrognosisResponse(DispositionAcceptedRejected disposition) {
        PrognosisResponse response = new PrognosisResponse();
        response.setPrognosisSequence(1234);
        response.setResult(disposition);
        MessageMetadata metadata = new MessageMetadata();
        metadata.setConversationID("1011");
        metadata.setSenderDomain("sender.test");
        response.setMessageMetadata(metadata);
        return response;
    }

}
