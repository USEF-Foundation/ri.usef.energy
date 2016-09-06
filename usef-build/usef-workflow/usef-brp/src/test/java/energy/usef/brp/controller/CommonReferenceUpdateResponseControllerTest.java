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

package energy.usef.brp.controller;

import energy.usef.brp.workflow.plan.commonreferenceupdate.CommonReferenceUpdateResponseEvent;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdateResponse;
import energy.usef.core.exception.BusinessException;

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

@RunWith(PowerMockRunner.class)
public class CommonReferenceUpdateResponseControllerTest {

    private CommonReferenceUpdateResponseController commonReferenceUpdateResponeController;

    @Mock
    private Event<CommonReferenceUpdateResponseEvent> eventManager;

    @Before
    public void init() {
        commonReferenceUpdateResponeController = new CommonReferenceUpdateResponseController();
        Whitebox.setInternalState(commonReferenceUpdateResponeController, eventManager);

    }

    @Test
    public void testHandleMessage() {
        CommonReferenceUpdateResponse message = new CommonReferenceUpdateResponse();

        try {
            commonReferenceUpdateResponeController.action(message, null);
        } catch (BusinessException e) {
            Assert.fail(e.getMessage());
        }

        ArgumentCaptor<CommonReferenceUpdateResponseEvent> captor = ArgumentCaptor
                .forClass(CommonReferenceUpdateResponseEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(captor.capture());
        Assert.assertEquals(message, captor.getValue().getCommonReferenceUpdateResponse());
    }
}
