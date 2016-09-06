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

import info.usef.core.util.DateTimeUtil;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.FlexRequestDto;
import info.usef.core.workflow.dto.PtuFlexRequestDto;
import info.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import info.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import info.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestStepParameter.IN;
import info.usef.dso.workflow.validate.create.flexrequest.CreateFlexRequestStepParameter.OUT;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem implementation for the Workflow 'Create Flex Requests'. This implementation expects to find the following
 * parameters as input:
 * <ul>
 * <li>CONGESTION_POINT_ENTITY_ADDRESS: the entity address of the congestion point ({@link String})</li>
 * <li>PERIOD: day for which one wants to send flex requests ({@link LocalDate})</li>
 * <li>GRID_SAFETY_ANALYSIS_LIST: Array of {@link info.usef.dso.workflow.dto.GridSafetyAnalysisDto} containing the data of the Grid
 * Safety Analysis for a day.</li>
 * </ul>
 */
public class CreateFlexRequest implements WorkflowStep
{
    private static final int FLEX_REQUEST_EXPIRATION_DAYS = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateFlexRequest.class);

    /**
     * creates one flex request of the Grid Safety Analysis and returns the request
     * see the buildFlexRequest() method for more details
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        String entityAddress = context.get(IN.CONGESTION_POINT_ENTITY_ADDRESS.name(), String.class);
        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);

        GridSafetyAnalysisDto gridSafetyAnalysis = context.get(IN.GRID_SAFETY_ANALYSIS_DTO.name(), GridSafetyAnalysisDto.class);

        List<FlexRequestDto> flexRequestDtos = new ArrayList<>();

        LOGGER.debug("Generating one 100% Flex Request.");
        flexRequestDtos.add(buildFlexRequest(entityAddress, period, gridSafetyAnalysis));

        context.setValue(OUT.FLEX_REQUESTS_DTO_LIST.name(), flexRequestDtos);
        return context;
    }

    /**
     * copies all Grid Safety Analysis values to one Flex request
     * @param  entityAddress the group address to use
     * @param  period the date to use
     * @param  gridSafetyAnalysis the {@link GridSafetyAnalysisDto} to retrieve power values, disposition types from
     * @return a {@link FlexRequestDto} flex request with expiration date of 1 day, all power values and disposition types from the GSA
     * */
    private FlexRequestDto buildFlexRequest(String entityAddress, LocalDate period, GridSafetyAnalysisDto gridSafetyAnalysis)
    {
        FlexRequestDto newFlexRequestDto = new FlexRequestDto();

        newFlexRequestDto.setConnectionGroupEntityAddress(entityAddress);
        newFlexRequestDto.setPeriod(period);
        newFlexRequestDto.setExpirationDateTime(DateTimeUtil.getCurrentDateTime().plusDays(FLEX_REQUEST_EXPIRATION_DAYS));
        // use grid safety analysis to determine flex request
        for (PtuGridSafetyAnalysisDto ptuGridSafetyAnalysisDto : gridSafetyAnalysis.getPtus())
        {
            PtuFlexRequestDto ptuFlexRequestDto = new PtuFlexRequestDto();
            ptuFlexRequestDto.setPtuIndex(BigInteger.valueOf(ptuGridSafetyAnalysisDto.getPtuIndex()));

            ptuFlexRequestDto.setDisposition(ptuGridSafetyAnalysisDto.getDisposition());
            ptuFlexRequestDto.setPower( BigInteger.valueOf( ptuGridSafetyAnalysisDto.getPower() ) );

            newFlexRequestDto.getPtus().add(ptuFlexRequestDto);
        }
        return newFlexRequestDto;
    }
}
