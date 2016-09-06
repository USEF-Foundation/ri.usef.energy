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

package energy.usef.dso.controller.error;

import energy.usef.core.controller.error.OutgoingErrorMessageController;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.factory.ControllerClassFactoryBuilder;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.dso.workflow.validate.acknowledgement.flexorder.FlexOrderAcknowledgementEvent;

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
 * Text class for FlexOrderErrorController.
 */
@RunWith(PowerMockRunner.class)
public class FlexOrderErrorControllerTest {
    @Mock
    private Event<FlexOrderAcknowledgementEvent> eventManager;

    private FlexOrderErrorController controller;

    @Before
    public void init() {
        controller = new FlexOrderErrorController();
        Whitebox.setInternalState(controller, eventManager);
    }

    /**
     * Tests FlexOrderErrorController.execute method.
     */
    @Test
    public void testExecute() {
        FlexOrder message = new FlexOrder();
        message.setSequence(12345678);
        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setSenderDomain("test.com");
        message.setMessageMetadata(messageMetadata);
        controller.execute(message);

        ArgumentCaptor<FlexOrderAcknowledgementEvent> eventCaptor = ArgumentCaptor
                .forClass(FlexOrderAcknowledgementEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());

        Assert.assertEquals(AcknowledgementStatus.NO_RESPONSE, eventCaptor.getValue().getAcknowledgementStatus());
    }

    /**
     * Tests that FlexOrderErrorController is correctly loaded by the OutgoingErrorMessageControllerFactory.
     */
    @Test
    public void testGetOutgoingErrorMessageControllerFactory() {
        Class<? extends Message> xmlClass = FlexOrder.class;
        Class<? extends OutgoingErrorMessageController<? extends Message>> controllerClass = ControllerClassFactoryBuilder
                .getBuilder().getOutgoingErrorMessageControllerFactory()
                .getControllerClass(xmlClass);
        Assert.assertEquals(controllerClass, FlexOrderErrorController.class);
    }

}
