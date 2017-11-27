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
 
package nl.energieprojecthoogdalem.dso.pbc;

import info.usef.core.util.DateTimeUtil;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.dso.workflow.plan.connection.forecast.DsoCreateNonAggregatorForecastParameter.IN;
import info.usef.dso.workflow.plan.connection.forecast.DsoCreateNonAggregatorForecastParameter.OUT;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit test for the {@link CreateNonAggregatorForecast} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateNonAggregatorForecastTest {

    private CreateNonAggregatorForecast createNonAggregatorForecast;

    @Test
    @SuppressWarnings("unchecked")
    public void testInvoke() throws Exception
    {
        createNonAggregatorForecast = new CreateNonAggregatorForecast();

        WorkflowContext output = createNonAggregatorForecast.invoke(buildContext());

        List<Long> power = output.get(OUT.POWER.name(), List.class);
        List<Long> maxLoad = output.get(OUT.MAXLOAD.name(), List.class);

        Assert.assertEquals(0, power.size());
        Assert.assertEquals(0, maxLoad.size());
    }

    private WorkflowContext buildContext() {
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(IN.PTU_DATE.name(), DateTimeUtil.getCurrentDate().plusDays(1));
        context.setValue(IN.PTU_DURATION.name(), 15);
        context.setValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), "ean.1");
        context.setValue(IN.AGR_DOMAIN_LIST.name(), Arrays.asList("agr1.usef-example.com", "agr2.usef-example.com"));
        context.setValue(IN.AGR_CONNECTION_COUNT_LIST.name(), Arrays.asList(1L, 1L, 10L));

        return context;
    }
}
