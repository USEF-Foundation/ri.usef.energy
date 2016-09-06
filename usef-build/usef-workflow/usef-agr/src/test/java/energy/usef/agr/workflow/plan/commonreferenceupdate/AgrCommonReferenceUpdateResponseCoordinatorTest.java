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

package energy.usef.agr.workflow.plan.commonreferenceupdate;

import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdateResponse;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.MessageMetadata;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 */
@RunWith(PowerMockRunner.class)
public class AgrCommonReferenceUpdateResponseCoordinatorTest {

    private AgrCommonReferenceUpdateResponseCoordinator coordinator;

    @Mock
    private AgrPlanboardBusinessService businessService;

    @Before
    public void init() {
        coordinator = new AgrCommonReferenceUpdateResponseCoordinator();
        setInternalState(coordinator, "businessService", businessService);

        PowerMockito.doNothing().when(businessService).updateConnectionStatusForCRO(Mockito.anyString());
        PowerMockito.doNothing().when(businessService).cleanSynchronization();
    }

    @Test
    public void testHandleAcceptedEvent() throws Exception {
        CommonReferenceUpdateResponse message = new CommonReferenceUpdateResponse();
        message.setResult(DispositionAcceptedRejected.ACCEPTED);
        message.setMessageMetadata(new MessageMetadata());
        message.getMessageMetadata().setSenderDomain("usef.energy");

        CommonReferenceUpdateResponseEvent event = new CommonReferenceUpdateResponseEvent(message);
        coordinator.handleEvent(event);
        Mockito.verify(businessService, Mockito.times(1)).updateConnectionStatusForCRO(Mockito.anyString());
        Mockito.verify(businessService, Mockito.times(1)).cleanSynchronization();
    }

    @Test
    public void testHandleRejetedEvent() throws Exception {
        CommonReferenceUpdateResponse message = new CommonReferenceUpdateResponse();
        message.setResult(DispositionAcceptedRejected.REJECTED);
        CommonReferenceUpdateResponseEvent event = new CommonReferenceUpdateResponseEvent(message);
        coordinator.handleEvent(event);
        Mockito.verify(businessService, Mockito.times(0)).updateConnectionStatusForCRO(Mockito.anyString());
        Mockito.verify(businessService, Mockito.times(0)).cleanSynchronization();
    }
}
