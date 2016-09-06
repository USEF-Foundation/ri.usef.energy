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

package energy.usef.brp.controller;

import energy.usef.brp.workflow.plan.flexorder.acknowledge.FlexOrderAcknowledgementEvent;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.FlexOrderResponse;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.service.helper.MessageMetadataBuilder;

import javax.enterprise.event.Event;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link FlexOrderResponseController} class.
 */
@RunWith(PowerMockRunner.class)
public class FlexOrderResponseControllerTest {

    private FlexOrderResponseController controller;

    @Mock
    private Event<FlexOrderAcknowledgementEvent> flexOrderAcknowledgementEventManager;

    @Before
    public void init() {
        controller = new FlexOrderResponseController();
        Whitebox.setInternalState(controller, flexOrderAcknowledgementEventManager);
    }

    @Test
    public void testActionOnSuccess() throws BusinessException {
        controller.action(buildFlexOrderResponse(), null);
        ArgumentCaptor<FlexOrderAcknowledgementEvent> eventCaptor = ArgumentCaptor.forClass(FlexOrderAcknowledgementEvent.class);
        Mockito.verify(flexOrderAcknowledgementEventManager, Mockito.times(1)).fire(eventCaptor.capture());

        FlexOrderAcknowledgementEvent capturedEvent = eventCaptor.getValue();
        Assert.assertNotNull(capturedEvent);
        Assert.assertEquals(1L, capturedEvent.getFlexOrderSequence().longValue());
        Assert.assertEquals(AcknowledgementStatus.ACCEPTED, capturedEvent.getAcknowledgementStatus());
    }

    @Test
    public void testActionOnRejected() throws BusinessException {
        controller.action(buildRejectedFlexOrderResponse(), null);
        ArgumentCaptor<FlexOrderAcknowledgementEvent> eventCaptor = ArgumentCaptor.forClass(FlexOrderAcknowledgementEvent.class);
        Mockito.verify(flexOrderAcknowledgementEventManager, Mockito.times(1)).fire(eventCaptor.capture());

        FlexOrderAcknowledgementEvent capturedEvent = eventCaptor.getValue();
        Assert.assertNotNull(capturedEvent);
        Assert.assertEquals(2L, capturedEvent.getFlexOrderSequence().longValue());
        Assert.assertEquals(AcknowledgementStatus.REJECTED, capturedEvent.getAcknowledgementStatus());

    }

    private FlexOrderResponse buildFlexOrderResponse() {
        FlexOrderResponse response = new FlexOrderResponse();
        response.setResult(DispositionAcceptedRejected.ACCEPTED);
        response.setSequence(1l);
        response.setMessageMetadata(new MessageMetadataBuilder().conversationID().build());
        return response;
    }

    private FlexOrderResponse buildRejectedFlexOrderResponse() {
        FlexOrderResponse response = new FlexOrderResponse();
        response.setResult(DispositionAcceptedRejected.REJECTED);
        response.setSequence(2l);
        response.setMessageMetadata(new MessageMetadataBuilder().conversationID().build());
        return response;
    }

}
