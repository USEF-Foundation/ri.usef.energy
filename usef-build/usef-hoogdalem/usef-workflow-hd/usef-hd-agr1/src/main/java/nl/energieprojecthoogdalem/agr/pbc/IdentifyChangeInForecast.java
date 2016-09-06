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

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastStepParameter.OUT;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem implementation returning no changes in forecast
 * <p>
 * The PBC receives the following parameters as input to take the decision:
 * <ul>
 * <li>CONNECTION_PORTFOLIO : a Map of List {@link ConnectionPortfolioDto} per period.</li>
 * <li>PERIOD: the period for which changes in forecast are being identified.</li>
 * <li>PTU_DURATION : the size of a PTU in minutes.</li>
 * </ul>
 * <p>
 * The PBC must return one flag indicating whether Re-optimize portfolio workflow should be triggered. The output parameter:
 * <ul>
 * <li>FORECAST_CHANGED : boolean value indicating that the Re-optimize portfolio workflow should be triggered</li>
 * </ul>
 *
 */
public class IdentifyChangeInForecast implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifyChangeInForecast.class);

    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.info("IdentifyChangeInForecast returning no changes");

        context.setValue(OUT.FORECAST_CHANGED.name(), false);
        context.setValue(OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name(), new ArrayList<>());

        return context;
    }

}
