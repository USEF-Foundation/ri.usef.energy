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

import javax.enterprise.event.Event;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    private static final String CONGESTION_POINT = "ea.32147890740214670";

    @Mock
    private MessageService messageService;

    @Mock
    private JMSHelperService jmsService;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Mock
    private Event<ColoringProcessEvent> coloringEventManager;

    @Mock
    private Config config;

    private FlexOfferController controller;

    @Before
    public void init() {
        controller = new FlexOfferController();
        Whitebox.setInternalState(controller, messageService);
        Whitebox.setInternalState(controller, jmsService);
        Whitebox.setInternalState(controller, corePlanboardBusinessService);
        Whitebox.setInternalState(controller, corePlanboardValidatorService);
        Whitebox.setInternalState(controller, coloringEventManager);
        Whitebox.setInternalState(controller, config);

        PowerMockito.when(config.getProperty(Matchers.eq(ConfigParam.HOST_DOMAIN))).thenReturn("usef-example.com");
    }

    @Test
    public void testFlexOffer() {
        try {
            Mockito.when(corePlanboardValidatorService
                    .validatePlanboardMessageExpirationDate(0L, DocumentType.FLEX_REQUEST, "something.com")).thenReturn(buildFlexRequest());

            // empty flex offer
            FlexOffer flexOffer = buildFlexOffer();
            controller.action(flexOffer, null);

            Mockito.verify(corePlanboardValidatorService, Mockito.times(1)).validateTimezone(Matchers.eq(flexOffer.getTimeZone()));
            Mockito.verify(corePlanboardValidatorService, Mockito.times(1)).validateCurrency(Matchers.eq(flexOffer.getCurrency()));
            Mockito.verify(corePlanboardValidatorService, Mockito.times(1)).validatePTUDuration(
                    Matchers.eq(flexOffer.getPTUDuration()));

            Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).storeFlexOffer(Mockito.anyString(),
                    Matchers.eq(flexOffer),
                    Matchers.eq(DocumentStatus.ACCEPTED), Matchers.eq(flexOffer.getMessageMetadata().getSenderDomain()));

            Mockito.verify(jmsService, Mockito.times(1)).sendMessageToOutQueue(Matchers.contains("Accepted"));

            Mockito.verify(coloringEventManager, Mockito.times(1)).fire(Matchers.any(ColoringProcessEvent.class));

        } catch (BusinessException e) {
            fail(e.getMessage());
        }
    }

    private FlexOffer buildFlexOffer() {
        FlexOffer request = new FlexOffer();
        request.setTimeZone("Europe/Amsterdam");
        request.setPTUDuration(Period.minutes(15));
        request.setCurrency("EUR");
        request.setMessageMetadata(MessageMetadataBuilder.buildDefault());
        request.getMessageMetadata().setSenderDomain("something.com");
        request.setCongestionPoint(CONGESTION_POINT);

        return request;
    }

    private PlanboardMessage buildFlexRequest() {
        PlanboardMessage flexRequest = new PlanboardMessage();
        flexRequest.setExpirationDate(new LocalDateTime().plusDays(1));

        return flexRequest;
    }

}
