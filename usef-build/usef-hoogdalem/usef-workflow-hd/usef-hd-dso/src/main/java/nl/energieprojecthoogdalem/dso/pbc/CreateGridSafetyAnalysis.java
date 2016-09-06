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

import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.DispositionTypeDto;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.core.workflow.dto.PtuPrognosisDto;
import info.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import info.usef.dso.workflow.dto.NonAggregatorForecastDto;
import info.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import info.usef.dso.workflow.dto.PtuNonAggregatorForecastDto;
import info.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter;
import info.usef.dso.workflow.validate.gridsafetyanalysis.CreateGridSafetyAnalysisStepParameter.IN;
import info.usef.pbcfeeder.dto.PbcPowerLimitsDto;

import java.math.BigInteger;
import static java.math.BigInteger.ZERO;
import java.util.function.Function;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import nl.energieprojecthoogdalem.dso.limits.LimitConfiguration;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem Implementation of a workflow step generating the Grid Safety Analysis. The step works as follows: - The step retrieves the
 * Non-Aggregator forecast and D-prognosis forecast - The step goes through the Non-Aggregator forecast, because this forecast
 * contains all possible values for congestion point, PTU date and PTU index - The step combines the forecasted power of the
 * Non-Aggregator forecast with the forecast in the D-Prognosis - Based on the prognosis, the disposition is determined: - A Max
 * load for a congestion point can be set. If the total forecasted power is within this value (+ or -), there is no congestion.
 * Available power is calculated based on the max load and used for the grid safety analysis. - When the forecasted power is not
 * within the max load, the requested power is calculated in order to reduce production or consumption. This value is added to the
 * grid safety analysis.
 */
public class CreateGridSafetyAnalysis implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateGridSafetyAnalysis.class);

    @Inject
    private LimitConfiguration limitConfiguration;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.info("Grid Safety Analysis invoked with context: {}", context);
        String congestionPointEntityAddress = context.get(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), String.class);
        NonAggregatorForecastDto nonAggregatorForecastDto = context.get(IN.NON_AGGREGATOR_FORECAST.name(), NonAggregatorForecastDto.class);
        List<PrognosisDto> dPrognosisInputList = context.get(IN.D_PROGNOSIS_LIST.name(), List.class);
        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);

        PbcPowerLimitsDto powerLimitsDto = fetchPowerLimits(period);
        Map<Integer, BigInteger> totalLoadPerPtu = computeTotalLoad(nonAggregatorForecastDto, dPrognosisInputList);

        GridSafetyAnalysisDto gridSafetyAnalysisDto = buildGridSafetyAnalysis(congestionPointEntityAddress, period, powerLimitsDto, totalLoadPerPtu);

        context.setValue(CreateGridSafetyAnalysisStepParameter.OUT.GRID_SAFETY_ANALYSIS.name(), gridSafetyAnalysisDto);
        return context;
    }

    /**
     * reads the limits from the configuration file and sets them, see {@link LimitConfiguration}
     * @return a {@link PbcPowerLimitsDto} with power limits set
     * */
    private PbcPowerLimitsDto fetchPowerLimits(LocalDate period)
    {
        PbcPowerLimitsDto powerLimitsDto;

        try
        {
            powerLimitsDto = limitConfiguration.getLimits(period);
            LOGGER.info("using upperlimit {}, lowerlimit {}", powerLimitsDto.getUpperLimit(), powerLimitsDto.getLowerLimit());
        }
        catch(NumberFormatException exception)
        {
            LOGGER.error("Unable to parse lower / upper limits to BigDecimals using default values lower {}, upper {}, reason "
                    , LimitConfiguration.DEFAULT_LOWER, LimitConfiguration.DEFAULT_UPPER, exception);
            powerLimitsDto = new PbcPowerLimitsDto( LimitConfiguration.DEFAULT_LOWER, LimitConfiguration.DEFAULT_UPPER);
        }

        return powerLimitsDto;
    }

    /**
     * Computes the total load from the Non-Aggregator forecast and the different available prognoses.
     *
     * @param nonAggregatorForecastDto {@link NonAggregatorForecastDto} the non-aggregator forecast.
     * @param prognoses {@link List} of {@link PrognosisDto}.
     * @return a {@link Map} with the PTU index as key ({@link Integer}) and the total load as value ({@link BigInteger}).
     */
    private Map<Integer, BigInteger> computeTotalLoad(NonAggregatorForecastDto nonAggregatorForecastDto, List<PrognosisDto> prognoses)
    {
        Map<Integer, PtuNonAggregatorForecastDto> nonAggregatorForecastPerPtu = nonAggregatorForecastDto.getPtus()
                .stream()
                .collect(Collectors.toMap(PtuNonAggregatorForecastDto::getPtuIndex, Function.identity()));

        Map<Integer, Optional<BigInteger>> prognosisPowerPerPtu = prognoses.stream()
                .flatMap(prognosis -> prognosis.getPtus().stream())
                .collect(Collectors.groupingBy(ptuPrognosisDto -> ptuPrognosisDto.getPtuIndex().intValue(),
                        Collectors.mapping(PtuPrognosisDto::getPower, Collectors.reducing(BigInteger::add))));

        Map<Integer, BigInteger> totalPowerPerPtu = new HashMap<>();

        for (Integer ptuIndex : prognosisPowerPerPtu.keySet())
        {
            totalPowerPerPtu.put(ptuIndex, prognosisPowerPerPtu.get(ptuIndex)
                                                                .orElse(BigInteger.ZERO)
                                                                .add(BigInteger.valueOf(nonAggregatorForecastPerPtu.get(ptuIndex).getPower())));
        }
        return totalPowerPerPtu;
    }

    private GridSafetyAnalysisDto buildGridSafetyAnalysis(String congestionPointEntityAddress, LocalDate period, PbcPowerLimitsDto powerLimitsDto, Map<Integer, BigInteger> totalLoadPerPtu)
    {
        GridSafetyAnalysisDto gridSafetyAnalysisDto = new GridSafetyAnalysisDto();
        gridSafetyAnalysisDto.setEntityAddress(congestionPointEntityAddress);
        gridSafetyAnalysisDto.setPtuDate(period);
        for (Integer ptuIndex : totalLoadPerPtu.keySet())
        {
            PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto = new PtuGridSafetyAnalysisDto();
            ptuGridSafetyAnalysisDto.setPtuIndex(ptuIndex);
            createGSAPtu(powerLimitsDto ,totalLoadPerPtu.get(ptuIndex), ptuGridSafetyAnalysisDto);
            LOGGER.trace("GridSafetyAnalysis PTU idx: {} Disposition: {} power: {}W", ptuIndex, ptuGridSafetyAnalysisDto.getDisposition(), ptuGridSafetyAnalysisDto.getPower());
            gridSafetyAnalysisDto.getPtus().add(ptuGridSafetyAnalysisDto);
        }
        return gridSafetyAnalysisDto;
    }

    /**
     * Fetches the required disposition for the grid safety analysis.
     *
     * Disposition will be AVAILABLE if the total load is lesser than or equal to the Consumption Limit (Upper Limit) or if the
     * total load is bigger than or equal to the Production limit (Lower Limit).
     *
     * for the Consumption Limit the power value is the difference between the Consumption Limit and Total load (40000 - 44000 = -4000 req or 40000 - 36000 = 4000 av)
     * for the Production Limit  the power value is the difference between Production Limit and Total Load. (-40000 - (-44000) = 4000 req or -40000 - (-36000) = -4000 av)
     *
     * @param powerLimitsDto {@link PbcPowerLimitsDto} the power limits.
     * @param totalLoad {@link BigInteger} the total load for the PTU.rc
     * @param ptuGridSafetyAnalysisDto the PTU to apply the power value and DispositionType.
     * */
    private static void createGSAPtu(PbcPowerLimitsDto powerLimitsDto, BigInteger totalLoad, PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto)
    {
        BigInteger upper = powerLimitsDto.getUpperLimit().toBigInteger()
                ,lower = powerLimitsDto.getLowerLimit().toBigInteger();

        if(totalLoad.compareTo(upper) > 0 || totalLoad.compareTo(lower) < 0)
            ptuGridSafetyAnalysisDto.setDisposition(DispositionTypeDto.REQUESTED);

        else
            ptuGridSafetyAnalysisDto.setDisposition(DispositionTypeDto.AVAILABLE);

        if(totalLoad.compareTo(ZERO) > 0)
            ptuGridSafetyAnalysisDto.setPower(upper.subtract(totalLoad).longValue());

        else if(totalLoad.compareTo(ZERO) < 0)
            ptuGridSafetyAnalysisDto.setPower(lower.subtract(totalLoad).longValue());

        else
            ptuGridSafetyAnalysisDto.setPower(0L);
    }

}
