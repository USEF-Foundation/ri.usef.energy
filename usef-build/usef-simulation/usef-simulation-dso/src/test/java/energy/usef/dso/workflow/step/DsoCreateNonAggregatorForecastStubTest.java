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

import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.plan.connection.forecast.DsoCreateNonAggregatorForecastParameter.IN;
import energy.usef.dso.workflow.plan.connection.forecast.DsoCreateNonAggregatorForecastParameter.OUT;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for the {@link DsoCreateNonAggregatorForecastStub} class.
 */
@RunWith(PowerMockRunner.class)
public class DsoCreateNonAggregatorForecastStubTest {

    private DsoCreateNonAggregatorForecastStub stub;

    @Mock
    private PbcFeederService pbcFeederService;

    @Before
    public void init() {
        stub = new DsoCreateNonAggregatorForecastStub();

        Whitebox.setInternalState(stub, "pbcFeederService", pbcFeederService);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvoke() throws Exception {
        Mockito.when(pbcFeederService.getPvLoadForecastPerPtu(Matchers.any(LocalDate.class), Matchers.anyInt(), Matchers.anyInt()))
                .thenReturn(buildPvLoadForecast());
        Mockito.when(pbcFeederService
                .getUncontrolledLoadPerPtu(Matchers.anyString(), Matchers.any(LocalDate.class), Matchers.anyInt(),
                        Matchers.anyInt())).thenReturn(buildUncontrolledLoadPerPtu());

        WorkflowContext output = stub.invoke(buildContext());

        List<Long> power = output.get(OUT.POWER.name(), List.class);
        List<Long> maxLoad = output.get(OUT.MAXLOAD.name(), List.class);

        Assert.assertEquals(96, power.size());
        Assert.assertEquals(96, maxLoad.size());

        IntStream.rangeClosed(1, 96).forEach(ptuIndex -> {
            Assert.assertEquals((long) ptuIndex * 20, (long) power.get(ptuIndex - 1));
            Assert.assertEquals((long) ptuIndex * 20, (long) maxLoad.get(ptuIndex - 1));
        });
    }

    private Map<Integer, BigDecimal> buildUncontrolledLoadPerPtu() {
        Map<Integer, BigDecimal> uncontrolledLoad = new HashMap<>();

        IntStream.rangeClosed(1, 96).forEach(ptuIndex -> uncontrolledLoad.put(ptuIndex, BigDecimal.valueOf(ptuIndex)));

        return uncontrolledLoad;
    }

    private Map<Integer, BigDecimal> buildPvLoadForecast() {
        Map<Integer, BigDecimal> pvLoadForecast = new HashMap<>();

        IntStream.rangeClosed(1, 96).forEach(ptuIndex -> pvLoadForecast.put(ptuIndex, BigDecimal.valueOf(ptuIndex)));

        return pvLoadForecast;
    }

    private WorkflowContext buildContext() {
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(IN.PTU_DATE.name(), DateTimeUtil.getCurrentDate().plusDays(1));
        context.setValue(IN.PTU_DURATION.name(), 15);
        context.setValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), "ean.1");
        context.setValue(IN.AGR_DOMAIN_LIST.name(), Arrays.asList("agr1.usef-example.com", "agr2.usef-example.com"));
        context.setValue(IN.AGR_CONNECTION_COUNT_LIST.name(), Arrays.asList(1l, 1l, 10l));

        return context;
    }
}
