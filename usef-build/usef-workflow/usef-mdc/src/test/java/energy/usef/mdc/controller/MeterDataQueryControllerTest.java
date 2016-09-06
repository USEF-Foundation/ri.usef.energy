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

package energy.usef.mdc.controller;

import energy.usef.core.data.xml.bean.message.MeterDataQuery;
import energy.usef.mdc.workflow.meterdata.MeterDataQueryEvent;

import javax.enterprise.event.Event;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class MeterDataQueryControllerTest {

    @Mock
    private Event<MeterDataQueryEvent> eventManager;

    @Test
    public void testAction() throws Exception {
        MeterDataQueryController controller = new MeterDataQueryController();
        Whitebox.setInternalState(controller, eventManager);

        MeterDataQuery message = new MeterDataQuery();
        controller.action(message, null);

        ArgumentCaptor<MeterDataQueryEvent> captor = ArgumentCaptor.forClass(MeterDataQueryEvent.class);
        Mockito.verify(eventManager, Mockito.times(1)).fire(captor.capture());

        Assert.assertNotNull(captor.getValue().getMeterDataQuery());
        Assert.assertEquals(message, captor.getValue().getMeterDataQuery());
    }
}
