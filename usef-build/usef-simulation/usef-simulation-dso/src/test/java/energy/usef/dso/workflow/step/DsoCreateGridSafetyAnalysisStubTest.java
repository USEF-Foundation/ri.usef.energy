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
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PrognosisTypeDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;
import energy.usef.dso.pbcfeederimpl.PbcFeederService;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.NonAggregatorForecastDto;
import energy.usef.dso.workflow.dto.PtuNonAggregatorForecastDto;
import energy.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter;
import energy.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter.IN;
import energy.usef.pbcfeeder.dto.PbcPowerLimitsDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit tests to test the DsoCreateGridSafetyAnalysis step.
 */
@RunWith(PowerMockRunner.class)
public class DsoCreateGridSafetyAnalysisStubTest {
    private DsoCreateGridSafetyAnalysisStub dsoCreateGridSafetyAnalysis;

    @Mock
    private PbcFeederService pbcFeederService;

    @Before
    public void init() throws Exception {
        dsoCreateGridSafetyAnalysis = new DsoCreateGridSafetyAnalysisStub();
        Whitebox.setInternalState(dsoCreateGridSafetyAnalysis, pbcFeederService);
    }

    /**
     * Tests DsoCreateGridSafetyAnalysis.invoke method.
     */
    @Test
    public void invoke() {
        PowerMockito.when(pbcFeederService.getCongestionPointPowerLimits(Matchers.any(String.class)))
                .thenReturn(buildPowerLimitsDto(-1000L, 1000L));
        WorkflowContext context = buildWorkflowContext();
        dsoCreateGridSafetyAnalysis.invoke(context);

        GridSafetyAnalysisDto gridSafetyAnalysisDto = (GridSafetyAnalysisDto) context
                .getValue(CreateGridSafetyAnalysisStepParameter.OUT.GRID_SAFETY_ANALYSIS.name());

        Assert.assertNotNull(gridSafetyAnalysisDto);
        Assert.assertEquals(4, gridSafetyAnalysisDto.getPtus().size());
        Assert.assertEquals(new Integer(1), gridSafetyAnalysisDto.getPtus().get(0).getPtuIndex());
        Assert.assertEquals(DispositionTypeDto.AVAILABLE, gridSafetyAnalysisDto.getPtus().get(0).getDisposition());
        Assert.assertEquals(DispositionTypeDto.AVAILABLE, gridSafetyAnalysisDto.getPtus().get(1).getDisposition());
        Assert.assertEquals(DispositionTypeDto.REQUESTED, gridSafetyAnalysisDto.getPtus().get(2).getDisposition());
        Assert.assertEquals(DispositionTypeDto.REQUESTED, gridSafetyAnalysisDto.getPtus().get(3).getDisposition());
        Assert.assertEquals(1, gridSafetyAnalysisDto.getPtus().get(0).getPower().intValue());
        Assert.assertEquals(0, gridSafetyAnalysisDto.getPtus().get(1).getPower().intValue());
        Assert.assertEquals(1, gridSafetyAnalysisDto.getPtus().get(2).getPower().intValue());
        Assert.assertEquals(2, gridSafetyAnalysisDto.getPtus().get(3).getPower().intValue());

    }

    private WorkflowContext buildWorkflowContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        final String entityAddress = "abc.com";
        final LocalDate period = DateTimeUtil.parseDate("2015-08-12");

        NonAggregatorForecastDto nonAggregatorForecastDto = new NonAggregatorForecastDto();
        nonAggregatorForecastDto.setEntityAddress(entityAddress);
        nonAggregatorForecastDto.setPtuDate(period);

        for (int i = 0; i < 4; i++) {
            PtuNonAggregatorForecastDto ptuNonAggregatorForecastDto = new PtuNonAggregatorForecastDto();
            ptuNonAggregatorForecastDto.setPtuIndex(i + 1);
            ptuNonAggregatorForecastDto.setMaxLoad((long) (899 + i));
            ptuNonAggregatorForecastDto.setPower((long) (899 + i));
            nonAggregatorForecastDto.getPtus().add(ptuNonAggregatorForecastDto);
        }
        context.setValue(IN.NON_AGGREGATOR_FORECAST.name(), nonAggregatorForecastDto);

        List<PrognosisDto> prognosisDtos = IntStream.rangeClosed(1, 4).mapToObj(index -> {
            PrognosisDto prognosisDto = new PrognosisDto();
            IntStream.rangeClosed(1, 4).mapToObj(ptuIndex -> {
                PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
                ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
                ptuPrognosisDto.setPower(BigInteger.valueOf(25L));
                return ptuPrognosisDto;
            }).forEach(ptu -> prognosisDto.getPtus().add(ptu));
            prognosisDto.setType(PrognosisTypeDto.D_PROGNOSIS);
            prognosisDto.setConnectionGroupEntityAddress(entityAddress);
            prognosisDto.setPeriod(period);
            return prognosisDto;
        }).collect(Collectors.toList());

        context.setValue(IN.D_PROGNOSIS_LIST.name(), prognosisDtos);
        context.setValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), entityAddress);
        context.setValue(IN.PERIOD.name(), period);
        return context;
    }

    private PbcPowerLimitsDto buildPowerLimitsDto(Long lower, Long upper) {
        return new PbcPowerLimitsDto(BigDecimal.valueOf(lower), BigDecimal.valueOf(upper));
    }

}
