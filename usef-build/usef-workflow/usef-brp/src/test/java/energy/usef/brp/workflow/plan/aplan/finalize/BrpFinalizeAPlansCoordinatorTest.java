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

package energy.usef.brp.workflow.plan.aplan.finalize;

import energy.usef.brp.service.business.BrpPlanboardBusinessService;

import energy.usef.core.event.validation.EventValidationService;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link BrpFinalizeAPlansCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class BrpFinalizeAPlansCoordinatorTest {

    @Mock
    private BrpPlanboardBusinessService brpPlanboardBusinessService;

    @Mock
    private EventValidationService eventValidationService;

    private BrpFinalizeAPlansCoordinator coordinator;

    @Before
    public void init() throws Exception {
        coordinator = new BrpFinalizeAPlansCoordinator();
        Whitebox.setInternalState(coordinator, brpPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, eventValidationService);
    }

    @Test
    public void testHandleEvent() throws Exception {
        coordinator.handleEvent(new FinalizeAPlansEvent(new LocalDate()));

        Mockito.verify(brpPlanboardBusinessService, Mockito.times(1)).finalizePendingAPlans(Matchers.any(LocalDate.class));
    }
}
