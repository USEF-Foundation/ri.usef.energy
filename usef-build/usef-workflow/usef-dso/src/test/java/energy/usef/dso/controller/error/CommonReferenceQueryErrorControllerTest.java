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

package energy.usef.dso.controller.error;

import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.dso.workflow.plan.connection.forecast.CreateConnectionForecastEvent;

import javax.enterprise.event.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Text class for CommonReferenceQueryErrorController.
 */
@RunWith(PowerMockRunner.class)
public class CommonReferenceQueryErrorControllerTest {
    @Mock
    private Event<CreateConnectionForecastEvent> eventManager;

    private CommonReferenceQueryErrorController controller = new CommonReferenceQueryErrorController();

    @Before
    public void init() {
        Whitebox.setInternalState(controller,
                eventManager);
    }

    /**
     * Tests CommonReferenceQueryErrorController.execute method.
     */
    @Test
    public void testExecute() {
        CommonReferenceQuery message = new CommonReferenceQuery();
        controller.execute(message);
        Mockito.verify(eventManager, Mockito.times(1)).fire(Matchers.any(CreateConnectionForecastEvent.class));
    }

}
