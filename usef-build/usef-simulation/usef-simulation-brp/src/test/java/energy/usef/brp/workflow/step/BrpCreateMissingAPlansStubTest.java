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

package energy.usef.brp.workflow.step;

import energy.usef.brp.workflow.plan.aplan.missing.BrpCreateMissingAPlansParamater;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests to test the {@link BrpCreateMissingAPlansStub}.
 */

public class BrpCreateMissingAPlansStubTest {

    private BrpCreateMissingAPlansStub brpCreateMissingAPlansStub;

    @Before
    public void init() throws Exception {
        brpCreateMissingAPlansStub = new BrpCreateMissingAPlansStub();
    }

    @Test
    public void testInvoke() throws Exception {

        WorkflowContext context = buildContext();

        WorkflowContext outContext = brpCreateMissingAPlansStub.invoke(context);

        PrognosisDto prognosisDto = outContext.get(BrpCreateMissingAPlansParamater.OUT.PROGNOSIS_DTO.name(), PrognosisDto.class);
        Assert.assertEquals("agr.usef-example.com", prognosisDto.getConnectionGroupEntityAddress());
        Assert.assertEquals(DateTimeUtil.getCurrentDate(), prognosisDto.getPeriod());
        for (int ptuIndex = 1; ptuIndex <= 12; ptuIndex++) {
            BigInteger power = prognosisDto.getPtus().get(ptuIndex - 1).getPower();
            Assert.assertTrue("Power should be between -500 * 10 and +500 * 10",
                    (power.compareTo(BigInteger.valueOf(-5000)) >= 0) && (power.compareTo(BigInteger.valueOf(5000)) <= 0));
        }
    }

    private WorkflowContext buildContext() {
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(BrpCreateMissingAPlansParamater.IN.AGGREGATOR_DOMAIN.name(), "agr.usef-example.com");
        context.setValue(BrpCreateMissingAPlansParamater.IN.CONNECTION_COUNT.name(), 10);
        context.setValue(BrpCreateMissingAPlansParamater.IN.PTU_DURATION.name(), 120);
        context.setValue(BrpCreateMissingAPlansParamater.IN.PERIOD.name(), DateTimeUtil.getCurrentDate());

        return context;
    }
}
