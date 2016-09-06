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

package energy.usef.dso.controller;

import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOrderResponse;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.Message;
import energy.usef.core.service.business.MessageService;
import energy.usef.dso.workflow.validate.acknowledgement.flexorder.FlexOrderAcknowledgementEvent;

import javax.enterprise.event.Event;

import org.junit.Assert;
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
 * Test class in charge of the unit tests related to the {@link FlexOrderResponseController}.
 */
@RunWith(PowerMockRunner.class)
public class FlexOrderResponseControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private Event<FlexOrderAcknowledgementEvent> eventManager;

    private FlexOrderResponseController controller;

    @Before
    public void init() {
        controller = new FlexOrderResponseController();
        Whitebox.setInternalState(controller, messageService);
        Whitebox.setInternalState(controller, eventManager);
        PowerMockito.when(messageService.getInitialMessageOfConversation(Matchers.any(String.class))).thenReturn(new Message());
    }

    @Test
    public void testActionOnAccepted() throws BusinessException {
        controller.action(buildFlexOrderResponse(DispositionAcceptedRejected.ACCEPTED), null);

        ArgumentCaptor<FlexOrderAcknowledgementEvent> eventCaptor = ArgumentCaptor
                .forClass(FlexOrderAcknowledgementEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());

        Assert.assertEquals(AcknowledgementStatus.ACCEPTED, eventCaptor.getValue().getAcknowledgementStatus());
    }

    @Test
    public void testActionOnRejected() throws BusinessException {
        controller.action(buildFlexOrderResponse(DispositionAcceptedRejected.REJECTED), null);

        ArgumentCaptor<FlexOrderAcknowledgementEvent> eventCaptor = ArgumentCaptor
                .forClass(FlexOrderAcknowledgementEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());

        Assert.assertEquals(AcknowledgementStatus.REJECTED, eventCaptor.getValue().getAcknowledgementStatus());
    }

    private FlexOrderResponse buildFlexOrderResponse(DispositionAcceptedRejected disposition) {
        FlexOrderResponse response = new FlexOrderResponse();
        response.setResult(disposition);
        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setSenderDomain("test.com");
        response.setMessageMetadata(messageMetadata);
        return response;
    }

}
