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

package energy.usef.brp.workflow.plan.flexorder.acknowledge;

import energy.usef.brp.service.business.BrpPlanboardBusinessService;
import energy.usef.brp.workflow.plan.connection.forecast.PrepareFlexRequestsEvent;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.util.DateTimeUtil;

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

@RunWith(PowerMockRunner.class)
public class BrpFlexOrderAcknowledgementCoordinatorTest {

    private BrpFlexOrderAcknowledgementCoordinator coordinator;

    @Mock
    private BrpPlanboardBusinessService brpPlanboardBusinessService;
    @Mock
    private Event<PrepareFlexRequestsEvent> eventManager;

    @Before
    public void setUp() throws Exception {
        coordinator = new BrpFlexOrderAcknowledgementCoordinator();
        Whitebox.setInternalState(coordinator, brpPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, eventManager);
    }

    @Test
    public void testHandleEvent() throws Exception {
        // stubbing of brpPlanboardBusinessService
        PowerMockito.when(brpPlanboardBusinessService.updateFlexOrdersWithAcknowledgementStatus(Matchers.any(Long.class),
                Matchers.any(AcknowledgementStatus.class), Matchers.any(String.class))).thenReturn(buildPlanboardMessage());

        coordinator.handleEvent(buildFlexOrderAcknowledgementEvent());

        ArgumentCaptor<PrepareFlexRequestsEvent> eventCaptor = ArgumentCaptor.forClass(PrepareFlexRequestsEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(eventCaptor.capture());
        // verify that an event to trigger the flex request workflow has been fired for the right period.
        Assert.assertEquals(DateTimeUtil.getCurrentDate(), eventCaptor.getValue().getPeriod());
    }

    private FlexOrderAcknowledgementEvent buildFlexOrderAcknowledgementEvent() {
        return new FlexOrderAcknowledgementEvent(1L, AcknowledgementStatus.REJECTED, "agr.usef-example.com");
    }

    private PlanboardMessage buildPlanboardMessage() {
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setPeriod(DateTimeUtil.getCurrentDate());
        return planboardMessage;
    }
}
