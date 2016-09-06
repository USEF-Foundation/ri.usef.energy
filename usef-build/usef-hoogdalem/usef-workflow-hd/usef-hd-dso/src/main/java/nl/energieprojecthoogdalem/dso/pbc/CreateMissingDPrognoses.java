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

import info.usef.core.util.PtuUtil;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.core.workflow.dto.PrognosisTypeDto;
import info.usef.core.workflow.dto.PtuPrognosisDto;
import info.usef.dso.workflow.validate.gridsafetyanalysis.CreateMissingDPrognosisParameter;
import info.usef.dso.workflow.validate.gridsafetyanalysis.CreateMissingDPrognosisParameter.IN;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem implementation for the Workflow 'Grid Safety Analysis workflow'. The step is responsible for missing D-Prognosis
 * creation. This implementation expects to find the following parameters as input:
 * <ul>
 * <li>CONGESTION_POINT_ENTITY_ADDRESS: The entity address of the congestion point ({@link String})</li>
 * <li>AGGREGATOR_DOMAIN: The aggregator domain ({@link String})</li>
 * <li>ANALYSIS_DAY: Analysis day ({@link LocalDate})</li>
 * <li>PTU_DURATION: PTU duration ({@link Integer})</li>
 * <li>AGGREGATOR_CONNECTION_AMMOUNT: Aggregator connection ammount ({@link Integer})</li>
 * </ul>
 * The step provides the following parameters as output:
 * <ul>
 * <li>D_PROGNOSIS: D-Prognosis DTO List ({@link List})</li>
 * </ul>
 * <p>
 * The step uses zero values for missing D-prognosis power values for each PTU
 */
public class CreateMissingDPrognoses implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateMissingDPrognoses.class);

    /**
     * {@inheritDoc}
     */
    public WorkflowContext invoke(WorkflowContext context)
    {
        String entityAddress = (String) context.getValue(IN.CONGESTION_POINT_ENTITY_ADDRESS.name());

        LOGGER.info("Starting workflow step 'CreateMissingPrognosis' for AGR: {}, congestion point: {}."
                , context.getValue(IN.AGGREGATOR_DOMAIN.name())
                , entityAddress);

        LocalDate analysisDay = (LocalDate) context.getValue(IN.ANALYSIS_DAY.name());
        int ptuDuration = (int) context.getValue(IN.PTU_DURATION.name());

        int numberOfPtusPerDay = PtuUtil.getNumberOfPtusPerDay(analysisDay, ptuDuration);

        PrognosisDto prognosisDto = new PrognosisDto();
        prognosisDto.setConnectionGroupEntityAddress(entityAddress);
        prognosisDto.setPeriod(analysisDay);
        prognosisDto.setType(PrognosisTypeDto.D_PROGNOSIS);
        IntStream.rangeClosed(1, numberOfPtusPerDay).mapToObj(ptuIndex -> {
            PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
            ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
            ptuPrognosisDto.setPower(BigInteger.ZERO);
            return ptuPrognosisDto;
        }).forEach(ptuPrognosisDto -> prognosisDto.getPtus().add(ptuPrognosisDto));

        context.setValue(CreateMissingDPrognosisParameter.OUT.D_PROGNOSIS.name(), prognosisDto);
        return context;
    }


}
