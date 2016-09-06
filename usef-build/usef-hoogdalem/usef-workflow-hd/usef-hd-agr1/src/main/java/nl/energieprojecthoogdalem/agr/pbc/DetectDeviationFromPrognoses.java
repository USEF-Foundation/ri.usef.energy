/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.agr.pbc;

import info.usef.agr.dto.ConnectionGroupPortfolioDto;
import info.usef.agr.workflow.operate.deviation.DetectDeviationFromPrognosisStepParameter.OUT;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.PrognosisDto;

import java.util.ArrayList;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem Implementation of workflow step "DetectDeviationFromPrognosis".
 * <p>
 * returns no deviation
 * <ul>
 * <li>PERIOD</li> : the period the portfolio ({@link LocalDate}).
 * <li>PTU_DURATION</li> : the duration of PTU expressed in minutes ({@link Integer}).
 * <li>CURRENT_PTU_INDEX</li> : current ptu index ({@link Integer}).
 * <li>CONNECTION_PORTFOLIO_DTO</li> : a {@link ConnectionGroupPortfolioDto} containing the current portfolio.
 * <li>LATEST_PROGNOSIS</li> : a {@link PrognosisDto} containing the latest A-plans and/or D-prognoses.
 * </ul>
 * <p>
 * This PBC must output:
 * <ul>
 * <li>DEVIATION_INDEX_LIST</li> : a list of {@link Integer} which contains the ptu indexes with deviation.
 * </ul>
 */
public class DetectDeviationFromPrognoses implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DetectDeviationFromPrognoses.class);

    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.info("DetectDeviationFromPrognosis returning no deviation");

        context.setValue(OUT.DEVIATION_INDEX_LIST.name(), new ArrayList<>());

        return context;
    }

}
