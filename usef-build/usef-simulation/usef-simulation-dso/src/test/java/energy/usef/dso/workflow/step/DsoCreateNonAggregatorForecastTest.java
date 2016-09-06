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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.plan.connection.forecast.DsoCreateNonAggregatorForecastParameter.IN;
import energy.usef.dso.workflow.plan.connection.forecast.WorkflowParameter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit tests to test the forcast step.
 */
@RunWith(PowerMockRunner.class)
public class DsoCreateNonAggregatorForecastTest {

    /* Those parameters are available to the workflow step. */
    private static final String AGR_DOMAIN_ARRAY = WorkflowParameter.AGR_DOMAIN_LIST;
    private static final String AGR_CONNECTION_COUNT_ARRAY = WorkflowParameter.AGR_CONNECTION_COUNT_LIST;
    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "CONGESTION_POINT_ENTITY_ADDRESS";
    private static final String PTU_DATE = "PERIOD";
    private static final String PTU_DURATION = "PTU_DURATION";

    /* Those are expected to be produced by the workflow step. */
    private static final String POWER = "POWER";
    private static final String MAXLOAD = "MAXLOAD";

    @Mock
    PbcFeederService pbcFeederService;

    private DsoCreateNonAggregatorForecastStub workflowStep;
    private static final int PTUS_PER_DAY = 96;

    @Before
    public void init() throws Exception {
        workflowStep = new DsoCreateNonAggregatorForecastStub();

        Whitebox.setInternalState(workflowStep, pbcFeederService);
        Mockito.when(pbcFeederService.getUncontrolledLoadPerPtu(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
                .then(call -> IntStream.rangeClosed(1, PTUS_PER_DAY)
                        .mapToObj(Integer::valueOf)
                        .collect(Collectors.toMap(Function.identity(), i -> new BigDecimal("" + (i * 10)))));

        Mockito.when(pbcFeederService.getPvLoadForecastPerPtu(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
                .then(call -> IntStream.rangeClosed(1, PTUS_PER_DAY)
                        .mapToObj(Integer::valueOf)
                        .collect(Collectors.toMap(Function.identity(), i -> new BigDecimal("" + (i * 20)))));
    }

    @Test
    public void testDsoNonAggregatorForecastStub() {
        DefaultWorkflowContext context = new DefaultWorkflowContext();

        List<String> agrDomain = IntStream.rangeClosed(1, 2).mapToObj(index -> "domain" + index).collect(Collectors.toList());
        List<Long> agrCount = new ArrayList<>();
        agrCount.add(2l);
        agrCount.add(4l);
        agrCount.add(2l); // non-aggregator count

        context.setValue(IN.PTU_DURATION.name(), 15); // 15 minutes
        context.setValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), "something");
        context.setValue(IN.PTU_DATE.name(), DateTimeUtil.getCurrentDate());
        context.setValue(IN.AGR_DOMAIN_LIST.name(), agrDomain);
        context.setValue(IN.AGR_CONNECTION_COUNT_LIST.name(), agrCount);

        workflowStep.invoke(context);

        @SuppressWarnings("unchecked")
        List<Long> power = (List<Long>) context.getValue(POWER);
        @SuppressWarnings("unchecked")
        List<Long> maxload = (List<Long>) context.getValue(MAXLOAD);
        assertNotNull(maxload.get(0));
        Assert.assertEquals(60L, power.get(0).longValue());
        Assert.assertEquals(5760L, power.get(95).longValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDsoNonAggregatorForecastStubWithThirdAggregator() {
        DefaultWorkflowContext context = new DefaultWorkflowContext();

        List<String> agrDomain = IntStream.rangeClosed(1, 2).mapToObj(index -> "domain" + index).collect(Collectors.toList());
        agrDomain.add("domain3");

        List<Long> agrCount = new ArrayList<>();
        agrCount.add(2l);
        agrCount.add(4l);
        agrCount.add(2l);
        agrCount.add(1l); // non-aggregator count

        context.setValue(IN.PTU_DURATION.name(), 15); // 15 minutes
        context.setValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), "something");
        context.setValue(IN.PTU_DATE.name(), DateTimeUtil.getCurrentDate());
        context.setValue(IN.AGR_DOMAIN_LIST.name(), agrDomain);
        context.setValue(IN.AGR_CONNECTION_COUNT_LIST.name(), agrCount);

        List<Long> power = (List<Long>) context.getValue(POWER);
        List<Long> maxload = (List<Long>) context.getValue(MAXLOAD);

        workflowStep.invoke(context);

        power = (List<Long>) context.getValue(POWER);
        assertEquals(96, power.size());
        maxload = (List<Long>) context.getValue(MAXLOAD);
        assertEquals(96, maxload.size());
        assertNotNull(maxload.get(0));
        assertNotNull(power.get(0));
    }
}
