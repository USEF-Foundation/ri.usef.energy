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
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import energy.usef.dso.workflow.operate.PlaceOperateFlexOrdersStepParameter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class in charge of the unit tests related to the {@link DsoPlaceOperateFlexOrdersStub} class.
 */
@RunWith(PowerMockRunner.class)
public class DsoPlaceOperateFlexOrdersStubTest {
    private DsoPlaceOperateFlexOrdersStub dsoPlaceOperateFlexOrdersStub;

    @Before
    public void init() throws Exception {
        dsoPlaceOperateFlexOrdersStub = new DsoPlaceOperateFlexOrdersStub();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvoke() {
        WorkflowContext inContext = buildWorkflowContext(30, 100, false);
        WorkflowContext outContext = dsoPlaceOperateFlexOrdersStub.invoke(inContext);
        List<FlexOfferDto> acceptedFlexOfferDtos = (List<FlexOfferDto>) outContext
                .getValue(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name());
        Assert.assertTrue(acceptedFlexOfferDtos.size() == 4);

        inContext = buildWorkflowContext(60, 100, false);
        outContext = dsoPlaceOperateFlexOrdersStub.invoke(inContext);
        acceptedFlexOfferDtos = (List<FlexOfferDto>) outContext
                .getValue(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name());
        Assert.assertTrue(acceptedFlexOfferDtos.size() == 2);

        inContext = buildWorkflowContext(160, 100, false);
        outContext = dsoPlaceOperateFlexOrdersStub.invoke(inContext);
        acceptedFlexOfferDtos = (List<FlexOfferDto>) outContext
                .getValue(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name());
        Assert.assertTrue(acceptedFlexOfferDtos.size() == 1);

        inContext = buildWorkflowContext(5, 100, false);
        outContext = dsoPlaceOperateFlexOrdersStub.invoke(inContext);
        acceptedFlexOfferDtos = (List<FlexOfferDto>) outContext
                .getValue(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name());
        Assert.assertTrue(acceptedFlexOfferDtos.size() == 5);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithEmptyPtuData() {
        WorkflowContext inContext = buildWorkflowContext(30, 100, true);
        WorkflowContext outContext = dsoPlaceOperateFlexOrdersStub.invoke(inContext);
        List<FlexOfferDto> acceptedFlexOfferDtos = (List<FlexOfferDto>) outContext
                .getValue(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name());
        Assert.assertTrue(acceptedFlexOfferDtos.size() == 0);
    }

    private WorkflowContext buildWorkflowContext(long flexOfferPower, long gridSafetyAnalysisPower, boolean emptyPtuData) {
        DefaultWorkflowContext context = new DefaultWorkflowContext();
        context.setValue(PlaceOperateFlexOrdersStepParameter.IN.FLEX_OFFER_DTO_LIST.name(),
                generateFlexOfferDtos(flexOfferPower, emptyPtuData));
        context.setValue(PlaceOperateFlexOrdersStepParameter.IN.GRID_SAFETY_ANALYSIS_DTO.name(),
                generateGridSafetyAnalysisDto(gridSafetyAnalysisPower));
        return context;
    }

    private List<FlexOfferDto> generateFlexOfferDtos(long power, boolean emptyPtuData) {
        List<FlexOfferDto> flexOfferDtos = new ArrayList<>();
        for (int j = 0; j < 5; j++) {
            FlexOfferDto flexOfferDto = new FlexOfferDto();
            flexOfferDto.setSequenceNumber((long) j);
            flexOfferDtos.add(flexOfferDto);
            if (!emptyPtuData) {
                for (int i = 0; i < 4; i++) {
                    PtuFlexOfferDto ptuFlexOfferDto = new PtuFlexOfferDto();
                    ptuFlexOfferDto.setPtuIndex(BigInteger.valueOf(i + 1));
                    ptuFlexOfferDto.setPower(BigInteger.valueOf(power));
                    ptuFlexOfferDto.setPrice(BigDecimal.valueOf(4));

                    flexOfferDto.getPtus().add(ptuFlexOfferDto);
                }
            }

        }
        return flexOfferDtos;
    }

    private GridSafetyAnalysisDto generateGridSafetyAnalysisDto(long power) {
        GridSafetyAnalysisDto gridSafetyAnalysisDto = new GridSafetyAnalysisDto();
        for (int i = 0; i < 4; i++) {
            if (i % 2 == 0) {
                continue;
            }
            PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto = new PtuGridSafetyAnalysisDto();
            ptuGridSafetyAnalysisDto.setPtuIndex(i + 1);
            ptuGridSafetyAnalysisDto.setPower(power);
            gridSafetyAnalysisDto.getPtus().add(ptuGridSafetyAnalysisDto);
        }
        return gridSafetyAnalysisDto;
    }

}
