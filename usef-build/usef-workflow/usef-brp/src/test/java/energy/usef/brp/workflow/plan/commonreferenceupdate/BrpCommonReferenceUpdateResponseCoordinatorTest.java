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

package energy.usef.brp.workflow.plan.commonreferenceupdate;

import energy.usef.brp.service.business.BrpBusinessService;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdateResponse;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.MessageMetadata;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class BrpCommonReferenceUpdateResponseCoordinatorTest {

    private BrpCommonReferenceUpdateResponseCoordinator coordinator;

    @Mock
    private BrpBusinessService businessService;

    @Before
    public void init() {
        coordinator = new BrpCommonReferenceUpdateResponseCoordinator();
        Whitebox.setInternalState(coordinator, businessService);
    }

    @Test
    public void testHandleEvent() {
        CommonReferenceUpdateResponse message = new CommonReferenceUpdateResponse();
        message.setMessageMetadata(new MessageMetadata());
        message.getMessageMetadata().setSenderDomain("cro.domain.nl");
        coordinator.handleEvent(new CommonReferenceUpdateResponseEvent(message));

        // set result to accepted and try again
        message.setResult(DispositionAcceptedRejected.ACCEPTED);

        coordinator.handleEvent(new CommonReferenceUpdateResponseEvent(message));

        Mockito.verify(businessService, Mockito.times(1)).updateConnectionStatusForCRO("cro.domain.nl");
        Mockito.verify(businessService, Mockito.times(1)).cleanSynchronization();
    }
}
