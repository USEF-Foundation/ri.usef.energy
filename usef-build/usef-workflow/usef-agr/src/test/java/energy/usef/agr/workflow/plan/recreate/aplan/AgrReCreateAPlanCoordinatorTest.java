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

package energy.usef.agr.workflow.plan.recreate.aplan;

import energy.usef.agr.workflow.plan.create.aplan.CreateAPlanEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;

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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrReCreateAPlanCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrReCreateAPlanCoordinatorTest {

    private AgrReCreateAPlanCoordinator coordinator;

    @Mock
    private Event<CreateAPlanEvent> createAPlanEventManager;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private EventValidationService eventValidationService;

    @Before
    public void setUp() throws Exception {
        coordinator = new AgrReCreateAPlanCoordinator();
        Whitebox.setInternalState(coordinator, createAPlanEventManager);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, eventValidationService);
    }

    @Test
    public void testHandleEventWithMultipleUsefIdentifiers() throws Exception {
        // stubbing of the PlanboardMessageRepository
        PowerMockito.when(corePlanboardBusinessService
                .findPlanboardMessages(Matchers.eq(DocumentType.A_PLAN), Matchers.any(LocalDate.class),
                        Matchers.eq(DocumentStatus.TO_BE_RECREATED)))
                .thenReturn(buildPlanboardMessages(false));

        // call the coordinator
        coordinator.handleEvent(buildRecreateAPlanEvent(DateTimeUtil.getCurrentDate()));

        // verify that an event has been sent for each PlanboardMessage
        Mockito.verify(createAPlanEventManager, Mockito.times(2)).fire(Matchers.any(CreateAPlanEvent.class));
    }

    @Test
    public void testHandleEventWithSpecifiedIdentifier() throws Exception {
        // stubbing of the PlanboardMessageRepository
        PowerMockito.when(corePlanboardBusinessService
                .findPlanboardMessages(Matchers.eq(DocumentType.A_PLAN), Matchers.any(LocalDate.class),
                        Matchers.eq(DocumentStatus.TO_BE_RECREATED)))
                .thenReturn(buildPlanboardMessages(true));

        // call the coordinator
        coordinator.handleEvent(buildRecreateAPlanEvent(DateTimeUtil.getCurrentDate()));

        // verify that an event has been sent for each PlanboardMessage
        ArgumentCaptor<CreateAPlanEvent> eventCaptor = ArgumentCaptor.forClass(CreateAPlanEvent.class);
        Mockito.verify(createAPlanEventManager, Mockito.times(1)).fire(eventCaptor.capture());
        CreateAPlanEvent capturedEvent = eventCaptor.getValue();
        Assert.assertNotNull(capturedEvent);
        Assert.assertEquals("brp.usef-example.com", capturedEvent.getUsefIdentifier());
        Assert.assertEquals(DateTimeUtil.getCurrentDate(), capturedEvent.getPeriod());
    }

    private ReCreateAPlanEvent buildRecreateAPlanEvent(LocalDate period) {
        return new ReCreateAPlanEvent(period);
    }

    private List<PlanboardMessage> buildPlanboardMessages(boolean differentConnectionGroups) {
        List<PlanboardMessage> planboardMessages = new ArrayList<>();

        for (int i = 0; i < 2; ++i) {
            ConnectionGroup connectionGroup = new BrpConnectionGroup();
            connectionGroup.setUsefIdentifier("brp" + (differentConnectionGroups ? "" : i + 1) + ".usef-example.com");
            PlanboardMessage planboardMessage = new PlanboardMessage();
            planboardMessage.setDocumentStatus(DocumentStatus.TO_BE_RECREATED);
            planboardMessage.setDocumentType(DocumentType.A_PLAN);
            planboardMessage.setConnectionGroup(connectionGroup);
            planboardMessages.add(planboardMessage);
        }
        return planboardMessages;
    }
}
