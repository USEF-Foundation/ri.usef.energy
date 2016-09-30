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

package energy.usef.brp.workflow.settlement.send;

import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.core.data.xml.bean.message.DispositionAcceptedDisputed;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class in charge of the unit tests related to the {@link BrpSettlementMessageResponseCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class BrpSettlementMessageResponseCoordinatorTest {

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    private BrpSettlementMessageResponseCoordinator coordinator;

    /**
     * Initialize the session.
     */
    @Before
    public void init() throws Exception {
        coordinator = new BrpSettlementMessageResponseCoordinator();

        setInternalState(coordinator, "corePlanboardBusinessService", corePlanboardBusinessService);
    }

    @Test
    public void testProcessPtuSettlements() throws Exception {
        Mockito.when(corePlanboardBusinessService
                .findPlanboardMessagesWithOriginSequence(Matchers.eq(1l), Matchers.eq(DocumentType.FLEX_ORDER_SETTLEMENT),
                        Matchers.eq("agr1.usef-example.com"))).thenReturn(Arrays.asList(buildPlanboardMessage()));
        Mockito.when(corePlanboardBusinessService
                .findPlanboardMessagesWithOriginSequence(Matchers.eq(2l), Matchers.eq(DocumentType.FLEX_ORDER_SETTLEMENT),
                        Matchers.eq("agr1.usef-example.com"))).thenReturn(Arrays.asList(buildPlanboardMessage()));

        coordinator.processPtuSettlements(Arrays.asList(1l, 2l), DispositionAcceptedDisputed.ACCEPTED, "agr1.usef-example.com");

        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .findPlanboardMessagesWithOriginSequence(Matchers.eq(1l), Matchers.eq(DocumentType.FLEX_ORDER_SETTLEMENT),
                        Matchers.eq("agr1.usef-example.com"));
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .findPlanboardMessagesWithOriginSequence(Matchers.eq(2l), Matchers.eq(DocumentType.FLEX_ORDER_SETTLEMENT),
                        Matchers.eq("agr1.usef-example.com"));
        Mockito.verify(corePlanboardBusinessService, Mockito.times(2)).updatePlanboardMessage(Matchers.any(PlanboardMessage.class));

    }

    private PlanboardMessage buildPlanboardMessage() {
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentStatus(DocumentStatus.SENT);
        return planboardMessage;
    }
}
