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
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import energy.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestStepParameter;

import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link DsoCreateFlexRequestStub} class.
 */
public class DsoCreateFlexRequestStubTest {
    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private static final LocalDate PTU_DATE = new LocalDate(2014, 11, 28);
    private static final long TEST_POWER = 1000;
    private static final long TEST_POWER_60 = 600;
    private static final long TEST_POWER_40 = 400;
    private DsoCreateFlexRequestStub dsoCreateFlexRequest;

    @Before
    public void init() {
        dsoCreateFlexRequest = new DsoCreateFlexRequestStub();
    }

    @Test
    public void testInvoke() {
        DefaultWorkflowContext context = buildWorkflowContext();
        dsoCreateFlexRequest.invoke(context);
        @SuppressWarnings("unchecked")
        List<FlexRequestDto> flexRequestDtos = (List<FlexRequestDto>) context.getValue(CreateFlexRequestStepParameter.OUT.FLEX_REQUESTS_DTO_LIST.name());
        Assert.assertNotNull("Did not expect a null array of FlexRequest.", flexRequestDtos);

        switch (flexRequestDtos.size()) {
        case 1:
            Assert.assertEquals(TEST_POWER, flexRequestDtos.get(0).getPtus().get(0).getPower().longValue());
            break;
        case 2:
            Assert.assertEquals(TEST_POWER_60, flexRequestDtos.get(0).getPtus().get(0).getPower().longValue());
            Assert.assertEquals(TEST_POWER_40, flexRequestDtos.get(1).getPtus().get(0).getPower().longValue());
            break;
        }
    }

    private DefaultWorkflowContext buildWorkflowContext() {
        DefaultWorkflowContext context = new DefaultWorkflowContext();
        context.setValue(CreateFlexRequestStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), CONGESTION_POINT_ENTITY_ADDRESS);
        context.setValue(CreateFlexRequestStepParameter.IN.PERIOD.name(), PTU_DATE);
        context.setValue(CreateFlexRequestStepParameter.IN.GRID_SAFETY_ANALYSIS_DTO.name(), buildGridSafetyAnalysisList());
        return context;
    }

    private GridSafetyAnalysisDto buildGridSafetyAnalysisList() {
        GridSafetyAnalysisDto gridSafetyAnalysisDto = new GridSafetyAnalysisDto();
        gridSafetyAnalysisDto.setEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
        gridSafetyAnalysisDto.setPtuDate(PTU_DATE);

        for (int i = 0; i < PtuUtil.getNumberOfPtusPerDay(new LocalDate(), 15); ++i) {
            PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto = new PtuGridSafetyAnalysisDto();
            ptuGridSafetyAnalysisDto.setDisposition(DispositionTypeDto.REQUESTED);
            ptuGridSafetyAnalysisDto.setPower(TEST_POWER);
            ptuGridSafetyAnalysisDto.setPtuIndex(i);
            gridSafetyAnalysisDto.getPtus().add(ptuGridSafetyAnalysisDto);
        }
        return gridSafetyAnalysisDto;
    }
}
