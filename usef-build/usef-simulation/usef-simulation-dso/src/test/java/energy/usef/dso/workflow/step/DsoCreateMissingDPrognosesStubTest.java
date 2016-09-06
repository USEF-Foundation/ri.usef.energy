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

import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.dso.workflow.validate.gridsafetyanalysis.CreateMissingDPrognosisParameter;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests to test the DsoCreateMissingPrognosisStub.
 */
public class DsoCreateMissingDPrognosesStubTest {
    private static final LocalDate ANALYSIS_DAY = new LocalDate(2014, 5, 20);
    private static final int PTU_DURATION = 15;
    private static final String AGGREGATOR_DOMAIN = "test.com";
    private static final int AGGREGATOR_CONNECTION_NUMBER = 3;

    private DsoCreateMissingDPrognosesStub dsoCreateMissingDPrognosisStub;

    @Before
    public void init() throws Exception {
        dsoCreateMissingDPrognosisStub = new DsoCreateMissingDPrognosesStub();
    }

    /**
     * Tests DsoCreateMissingPrognosisStub.invoke method.
     */
    @Test
    public void invoke() {
        WorkflowContext inContext = new DefaultWorkflowContext();
        prepateContext(inContext);
        WorkflowContext outContext = dsoCreateMissingDPrognosisStub.invoke(inContext);

        PrognosisDto prognosisDto = (PrognosisDto) outContext
                .getValue(CreateMissingDPrognosisParameter.OUT.D_PROGNOSIS.name());

        Assert.assertEquals(PtuUtil.getNumberOfPtusPerDay(ANALYSIS_DAY, PTU_DURATION), prognosisDto.getPtus().size());
    }

    private void prepateContext(WorkflowContext context) {
        context.setValue(CreateMissingDPrognosisParameter.IN.AGGREGATOR_DOMAIN.name(), AGGREGATOR_DOMAIN);
        context.setValue(CreateMissingDPrognosisParameter.IN.ANALYSIS_DAY.name(), ANALYSIS_DAY);
        context.setValue(CreateMissingDPrognosisParameter.IN.PTU_DURATION.name(), PTU_DURATION);
        context.setValue(CreateMissingDPrognosisParameter.IN.AGGREGATOR_CONNECTION_AMOUNT
                .name(), AGGREGATOR_CONNECTION_NUMBER);
    }

}
