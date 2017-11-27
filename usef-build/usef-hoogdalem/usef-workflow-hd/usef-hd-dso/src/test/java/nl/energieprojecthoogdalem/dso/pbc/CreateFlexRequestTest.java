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

import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.DispositionTypeDto;
import info.usef.core.workflow.dto.FlexRequestDto;
import info.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import info.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import info.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestStepParameter;
import info.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestStepParameter.OUT;

import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link CreateFlexRequest} class.
 */
public class CreateFlexRequestTest
{
    private CreateFlexRequest dsoCreateFlexRequest;

    private static int PTU_COUNT = 96;
    private static final long TEST_POWER = 1000;
    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private static final LocalDate PTU_DATE = new LocalDate(2014, 11, 28);

    @Test
    public void testInvoke()
    {
        dsoCreateFlexRequest = new CreateFlexRequest();

        DefaultWorkflowContext context = buildWorkflowContext();
        WorkflowContext result = dsoCreateFlexRequest.invoke(context);

        @SuppressWarnings("unchecked")
        List<FlexRequestDto> flexRequestDtos = (List<FlexRequestDto>) result.getValue(OUT.FLEX_REQUESTS_DTO_LIST.name());

        Assert.assertNotNull("Did not expect a null array of FlexRequest.", flexRequestDtos);


        flexRequestDtos.forEach(flexRequestDto ->
        {
            for (int i = 0; i < PTU_COUNT; i++)
            {
                //System.err.println("idx " + i + " type " + flexRequestDto.getPtus().get(i).getDisposition() + " pwr " + flexRequestDto.getPtus().get(i).getPower().longValue());
                if(i < 48)
                {
                    Assert.assertEquals(DispositionTypeDto.AVAILABLE, flexRequestDto.getPtus().get(i).getDisposition());
                    Assert.assertEquals(TEST_POWER , flexRequestDto.getPtus().get(i).getPower().longValue());
                }
                else
                {
                    Assert.assertEquals(DispositionTypeDto.REQUESTED, flexRequestDto.getPtus().get(i).getDisposition());
                    Assert.assertEquals(TEST_POWER, flexRequestDto.getPtus().get(i).getPower().longValue());
                }
            }
        });
    }

    private DefaultWorkflowContext buildWorkflowContext() {
        DefaultWorkflowContext context = new DefaultWorkflowContext();
        context.setValue(CreateFlexRequestStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), CONGESTION_POINT_ENTITY_ADDRESS);
        context.setValue(CreateFlexRequestStepParameter.IN.PERIOD.name(), PTU_DATE);
        context.setValue(CreateFlexRequestStepParameter.IN.GRID_SAFETY_ANALYSIS_DTO.name(), buildGridSafetyAnalysisList());
        return context;
    }

    private GridSafetyAnalysisDto buildGridSafetyAnalysisList()
    {
        GridSafetyAnalysisDto gridSafetyAnalysisDto = new GridSafetyAnalysisDto();
        gridSafetyAnalysisDto.setEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
        gridSafetyAnalysisDto.setPtuDate(PTU_DATE);

        for (int i = 0; i < PTU_COUNT; i++)
        {
            PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto = new PtuGridSafetyAnalysisDto();
            ptuGridSafetyAnalysisDto.setPtuIndex(i);
            ptuGridSafetyAnalysisDto.setPower(TEST_POWER);

            if(i < 48)
                ptuGridSafetyAnalysisDto.setDisposition(DispositionTypeDto.AVAILABLE);
            else
                ptuGridSafetyAnalysisDto.setDisposition(DispositionTypeDto.REQUESTED);

            gridSafetyAnalysisDto.getPtus().add(ptuGridSafetyAnalysisDto);
        }
        return gridSafetyAnalysisDto;
    }
}
