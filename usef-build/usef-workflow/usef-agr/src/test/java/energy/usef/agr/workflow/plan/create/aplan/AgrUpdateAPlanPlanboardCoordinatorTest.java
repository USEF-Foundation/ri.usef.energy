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

package energy.usef.agr.workflow.plan.create.aplan;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import energy.usef.core.event.RequestMoveToValidateEvent;
import energy.usef.core.event.validation.EventValidationService;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.service.business.CorePlanboardBusinessService;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrUpdateAPlanPlanboardCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class AgrUpdateAPlanPlanboardCoordinatorTest {
    private static final LocalDate TEST_DATE = new LocalDate(2050, 11, 21);

    private AgrUpdateAPlanPlanboardCoordinator coordinator;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private Event<RequestMoveToValidateEvent> moveToValidateEventManager;

    @Mock
    private EventValidationService eventValidationService;

    @Before
    public void init() throws Exception {
        coordinator = new AgrUpdateAPlanPlanboardCoordinator();
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, moveToValidateEventManager);
        Whitebox.setInternalState(coordinator, eventValidationService);
    }

    @Test
    public void testHandleEvent() throws BusinessValidationException {
        coordinator.handleEvent(new FinalizeAPlansEvent(TEST_DATE));

        verify(corePlanboardBusinessService, times(1)).finalizeAPlans(TEST_DATE);
        verify(moveToValidateEventManager, times(1)).fire(Matchers.any(RequestMoveToValidateEvent.class));
    }
}
