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

import energy.usef.core.data.xml.bean.message.FlexOfferRevocation;
import energy.usef.core.exception.BusinessException;
import energy.usef.dso.workflow.validate.revoke.flexoffer.FlexOfferRevocationEvent;

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
 * Test class in charge of the {@link FlexOfferRevocation} messages.
 */
@RunWith(PowerMockRunner.class)
public class FlexOfferRevocationControllerTest {
    private FlexOfferRevocationController controller;

    @Mock
    private Event<FlexOfferRevocationEvent> eventManager;

    @Before
    public void init() {
        controller = new FlexOfferRevocationController();
        Whitebox.setInternalState(controller, eventManager);
    }

    @Test
    public void testActionSucceeds() throws BusinessException {
        controller.action(buildMessage(), null);
        ArgumentCaptor<FlexOfferRevocationEvent> eventCaptor = ArgumentCaptor.forClass(FlexOfferRevocationEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());

        FlexOfferRevocationEvent capturedEvent = eventCaptor.getValue();
        Assert.assertNotNull("Did not expect a null event fired.", capturedEvent);
        Assert.assertNotNull("Did not expect a null reference to the FlexOfferRevocation message.",
                capturedEvent.getFlexOfferRevocation());
    }

    private FlexOfferRevocation buildMessage() {
        return new FlexOfferRevocation();
    }
}
