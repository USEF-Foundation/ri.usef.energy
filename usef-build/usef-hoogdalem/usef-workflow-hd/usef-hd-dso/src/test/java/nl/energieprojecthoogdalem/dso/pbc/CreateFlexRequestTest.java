/*
 * This software source code is provided by the USEF Foundation. The copyright
 * and all other intellectual property rights relating to all software source
 * code provided by the USEF Foundation (and changes and modifications as well
 * as on new versions of this software source code) belong exclusively to the
 * USEF Foundation and/or its suppliers or licensors. Total or partial
 * transfer of such a right is not allowed. The user of the software source
 * code made available by USEF Foundation acknowledges these rights and will
 * refrain from any form of infringement of these rights.
 *
 * The USEF Foundation provides this software source code "as is". In no event
 * shall the USEF Foundation and/or its suppliers or licensors have any
 * liability for any incidental, special, indirect or consequential damages;
 * loss of profits, revenue or data; business interruption or cost of cover or
 * damages arising out of or in connection with the software source code or
 * accompanying documentation.
 *
 * For the full license agreement see http://www.usef.info/license.
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
