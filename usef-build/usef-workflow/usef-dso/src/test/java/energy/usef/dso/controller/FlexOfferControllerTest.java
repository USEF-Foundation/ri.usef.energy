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

import static org.junit.Assert.fail;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOffer;
import energy.usef.core.data.xml.bean.message.FlexOfferRevocation;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.dso.workflow.coloring.ColoringProcessEvent;

import energy.usef.dso.workflow.validate.create.flexoffer.DsoFlexOfferCoordinator;
import energy.usef.dso.workflow.validate.create.flexoffer.FlexOfferReceivedEvent;
import energy.usef.dso.workflow.validate.revoke.flexoffer.FlexOfferRevocationEvent;
import javax.enterprise.event.Event;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;
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
 * Test class in charge of the unit tests related to the {@link FlexRequestResponseController}.
 */
@RunWith(PowerMockRunner.class)
public class
FlexOfferControllerTest {

    private FlexOfferController controller;

    @Mock
    private Event<FlexOfferReceivedEvent> eventManager;

    @Before
    public void init() {
        controller = new FlexOfferController();
        Whitebox.setInternalState(controller, eventManager);
    }

    @Test
    public void testActionSucceeds() throws BusinessException {
        controller.action(buildMessage(), null);
        ArgumentCaptor<FlexOfferReceivedEvent> eventCaptor = ArgumentCaptor.forClass(FlexOfferReceivedEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());

        FlexOfferReceivedEvent capturedEvent = eventCaptor.getValue();
        Assert.assertNotNull("Did not expect a null event fired.", capturedEvent);
        Assert.assertNotNull("Did not expect a null reference to the FlexOffer message.",
                capturedEvent.getFlexOffer());
    }

    private FlexOffer buildMessage() {
        return new FlexOffer();
    }
}
