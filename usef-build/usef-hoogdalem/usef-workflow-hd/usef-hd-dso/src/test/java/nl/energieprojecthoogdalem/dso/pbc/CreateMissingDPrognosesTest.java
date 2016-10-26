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

import info.usef.core.util.PtuUtil;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.dso.workflow.validate.gridsafetyanalysis.CreateMissingDPrognosisParameter;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

/**
 * Unit tests to test the {@link CreateMissingDPrognoses}.
 */
public class CreateMissingDPrognosesTest {
    private static final LocalDate ANALYSIS_DAY = new LocalDate(2014, 5, 20);
    private static final int PTU_DURATION = 15;
    private static final String AGGREGATOR_DOMAIN = "test.com";
    private static final int AGGREGATOR_CONNECTION_NUMBER = 3;

    private CreateMissingDPrognoses CreateMissingDPrognosis;

    @Before
    public void init() throws Exception {
        CreateMissingDPrognosis = new CreateMissingDPrognoses();
    }

    /**
     * Tests DsoCreateMissingPrognosisStub.invoke method.
     */
    @Test
    public void invoke() {
        WorkflowContext inContext = new DefaultWorkflowContext();
        prepateContext(inContext);
        WorkflowContext outContext = CreateMissingDPrognosis.invoke(inContext);

        PrognosisDto prognosisDto = (PrognosisDto) outContext
                .getValue(CreateMissingDPrognosisParameter.OUT.D_PROGNOSIS.name());

        prognosisDto.getPtus().forEach(ptuPrognosisDto ->

                Assert.assertEquals(BigInteger.ZERO, ptuPrognosisDto.getPower())
        );

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
