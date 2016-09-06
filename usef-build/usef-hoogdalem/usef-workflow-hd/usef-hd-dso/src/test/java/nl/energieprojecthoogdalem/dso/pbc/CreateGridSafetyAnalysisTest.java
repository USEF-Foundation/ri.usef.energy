/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.dso.pbc;

import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.DispositionTypeDto;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.core.workflow.dto.PrognosisTypeDto;
import info.usef.core.workflow.dto.PtuPrognosisDto;
import info.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import info.usef.dso.workflow.dto.NonAggregatorForecastDto;
import info.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import info.usef.dso.workflow.dto.PtuNonAggregatorForecastDto;
import info.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter;
import info.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter.IN;

import java.math.BigInteger;
import static java.math.BigInteger.ZERO;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import info.usef.pbcfeeder.dto.PbcPowerLimitsDto;
import nl.energieprojecthoogdalem.dso.limits.LimitConfiguration;
import org.joda.time.LocalDate;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit tests to test the {@link CreateGridSafetyAnalysis }.
 */
@RunWith(PowerMockRunner.class)
public class CreateGridSafetyAnalysisTest
{

    private CreateGridSafetyAnalysis dsoCreateGridSafetyAnalysis;

    private static final int PTU_COUNT = 96
                            ,PROGNOSIS_SIZE = 4;
    
    private static final String EA = "example.com";
    private static final BigInteger SAFE_VALUE = BigInteger.valueOf(3500)
                                    , CONSUMPTION_CONGESTION_VALUE = BigInteger.valueOf(LimitConfiguration.DEFAULT_UPPER.intValue() +4000)
                                    , PRODUCTION_CONGESTION_VALUE = BigInteger.valueOf(LimitConfiguration.DEFAULT_LOWER.intValue() -4000)
        ;
    private static final LocalDate PERIOD = new LocalDate(2015, 8, 12);

    @Mock
    LimitConfiguration limitConfiguration;

    @Before
    public void init() throws Exception
    {
        PbcPowerLimitsDto limits = new PbcPowerLimitsDto(LimitConfiguration.DEFAULT_LOWER, LimitConfiguration.DEFAULT_UPPER);
        PowerMockito.doReturn(limits).when(limitConfiguration).getLimits(Matchers.any(LocalDate.class));

        dsoCreateGridSafetyAnalysis = new CreateGridSafetyAnalysis();
        Whitebox.setInternalState(dsoCreateGridSafetyAnalysis, "limitConfiguration", limitConfiguration);
    }

    /**
     * Tests DsoCreateGridSafetyAnalysis.invoke method.
     */
    @Test
    public void invoke()
    {
        WorkflowContext context = buildWorkflowContext();
        dsoCreateGridSafetyAnalysis.invoke(context);

        GridSafetyAnalysisDto gridSafetyAnalysisDto = (GridSafetyAnalysisDto) context
                .getValue(CreateGridSafetyAnalysisStepParameter.OUT.GRID_SAFETY_ANALYSIS.name());

        assertNotNull(gridSafetyAnalysisDto);
        assertEquals(PTU_COUNT, gridSafetyAnalysisDto.getPtus().size());
        
        for(int i = 0; i < PTU_COUNT; i++)
        {
            int ptuIdx = i +1;
            PtuGridSafetyAnalysisDto ptu = gridSafetyAnalysisDto.getPtus().get(i);

            assertEquals(new Integer(ptuIdx), ptu.getPtuIndex());

            if(ptuIdx > 19 && ptuIdx < 31)
            {
                assertEquals(DispositionTypeDto.REQUESTED, ptu.getDisposition());
                Long requested = LimitConfiguration.DEFAULT_UPPER.toBigInteger().subtract(CONSUMPTION_CONGESTION_VALUE).longValue();
                assertEquals(requested, ptu.getPower());
            }
            else if(ptuIdx > 49 && ptuIdx < 61)
            {
                assertEquals(DispositionTypeDto.REQUESTED, ptu.getDisposition());
                Long requested = LimitConfiguration.DEFAULT_LOWER.toBigInteger().subtract(PRODUCTION_CONGESTION_VALUE).longValue();
                assertEquals(requested, ptu.getPower());
            }
            else if(ptuIdx > 34 && ptuIdx < 46)
            {
                assertEquals(DispositionTypeDto.AVAILABLE, ptu.getDisposition());
                assertEquals(Long.valueOf(LimitConfiguration.DEFAULT_LOWER.toBigInteger().subtract(SAFE_VALUE.negate().multiply(BigInteger.valueOf(PROGNOSIS_SIZE))).longValue()), ptu.getPower());
            }
            else if(ptuIdx == 1)
                assertEquals(Long.valueOf(0L), ptu.getPower());
            else
            {
                assertEquals(DispositionTypeDto.AVAILABLE, ptu.getDisposition());
                assertEquals(Long.valueOf(LimitConfiguration.DEFAULT_UPPER.toBigInteger().subtract(SAFE_VALUE.multiply(BigInteger.valueOf(PROGNOSIS_SIZE))).longValue()), ptu.getPower());
            }
            //System.out.println("validated idx " + ptuIdx);
        }
    }

    private WorkflowContext buildWorkflowContext()
    {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.PERIOD.name(), PERIOD);
        context.setValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), EA);
        context.setValue(IN.NON_AGGREGATOR_FORECAST.name(), buildNonAGRForecast());
        context.setValue(IN.D_PROGNOSIS_LIST.name(), buildDPrognosis());
        return context;
    }

    private NonAggregatorForecastDto buildNonAGRForecast()
    {
        NonAggregatorForecastDto nonAggregatorForecastDto = new NonAggregatorForecastDto();

        nonAggregatorForecastDto.getPtus().addAll
        (
            IntStream.rangeClosed(1, PTU_COUNT).mapToObj(idx ->
            {
                PtuNonAggregatorForecastDto ptu = new PtuNonAggregatorForecastDto();
                ptu.setPtuIndex(idx);
                ptu.setPower(0L);
                ptu.setMaxLoad(0L);
                return ptu;
            }).collect(Collectors.toList())
        );
        return nonAggregatorForecastDto;
    }

    private List<PrognosisDto> buildDPrognosis()
    {
        return IntStream.rangeClosed(1, PROGNOSIS_SIZE).mapToObj(index ->
        {
            PrognosisDto prognosisDto = new PrognosisDto();
            
            prognosisDto.setType(PrognosisTypeDto.D_PROGNOSIS);
            prognosisDto.setConnectionGroupEntityAddress(EA);
            prognosisDto.setPeriod(PERIOD);
            
            prognosisDto.getPtus().addAll
            (
                IntStream.rangeClosed(1, PTU_COUNT).mapToObj(ptuIndex ->
                {
                    PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
                    ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
                    
                    if(ptuIndex > 19 && ptuIndex < 31)
                        ptuPrognosisDto.setPower(CONSUMPTION_CONGESTION_VALUE.divide(BigInteger.valueOf(PROGNOSIS_SIZE)));
                    
                    else if(ptuIndex > 49 && ptuIndex < 61)
                        ptuPrognosisDto.setPower(PRODUCTION_CONGESTION_VALUE.divide(BigInteger.valueOf(PROGNOSIS_SIZE)));
                    
                    else if(ptuIndex > 34 && ptuIndex < 46)
                        ptuPrognosisDto.setPower(SAFE_VALUE.negate());

                    else if(ptuIndex == 1)
                        ptuPrognosisDto.setPower(ZERO);

                    else
                        ptuPrognosisDto.setPower(SAFE_VALUE);
                    
                    return ptuPrognosisDto;
                }).collect(Collectors.toList())
            );
            
            return prognosisDto;
        }).collect(Collectors.toList());
    }
}
