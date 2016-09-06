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

package energy.usef.dso.workflow.step;

import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.dso.workflow.settlement.collect.MeterDataQueryEventsParameter;

import java.util.Collections;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link DsoMeterDataQueryEventsStub} class.
 */
public class DsoMeterDataQueryEventsStubTest {
    private static final String CONNECTION_ENTITY_ADDRESS = "ean.123456789012345678";

    private DsoMeterDataQueryEventsStub dsoMeterDataQueryEventsStub;

    @Before
    public void init() {
        dsoMeterDataQueryEventsStub = new DsoMeterDataQueryEventsStub();
    }

    @Test
    public void testInvoke() {
        WorkflowContext context = dsoMeterDataQueryEventsStub.invoke(buildWorkflowContext());

        @SuppressWarnings("unchecked")
        List<ConnectionMeterEventDto> connectionMeterEventDtoList = (List<ConnectionMeterEventDto>) context
                .getValue(MeterDataQueryEventsParameter.OUT.CONNECTION_METER_EVENT_DTO_LIST.name());

        Assert.assertTrue(connectionMeterEventDtoList.size() >= 1);
        Assert.assertEquals(CONNECTION_ENTITY_ADDRESS, connectionMeterEventDtoList.get(0).getEntityAddress());
    }

    private DefaultWorkflowContext buildWorkflowContext() {
        DefaultWorkflowContext context = new DefaultWorkflowContext();
        context.setValue(MeterDataQueryEventsParameter.IN.CONNECTION_LIST.name(),
                Collections.singletonList(CONNECTION_ENTITY_ADDRESS));
        context.setValue(MeterDataQueryEventsParameter.IN.PERIOD.name(), new LocalDate(2014, 11, 28));
        return context;
    }

}
