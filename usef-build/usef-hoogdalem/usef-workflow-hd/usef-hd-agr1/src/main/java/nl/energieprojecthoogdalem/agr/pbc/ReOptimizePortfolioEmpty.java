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
import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.device.request.DeviceMessageDto;
import info.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter.IN;
import info.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter.OUT;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.FlexOrderDto;
import info.usef.core.workflow.dto.PrognosisDto;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * passes the portfolio to the coordinator
 * <p>
 * The calculation done in this PBC is: 1. Calculate the target power per ptu with target = prognosis + ordered - summed forecast
 * consumption of all udis 2. Calculate the factor per ptu with factor = target / summed potential flex consumption for all udis 3.
 * Calculate the new forecast consumption per udi per ptu/dtu with forecast = forecast + potential flex * factor
 * <p>
 * The PBC receives the following parameters as input : <ul> <li>PTU_DURATION : PTU duration.</li> <li>CURRENT_PTU_INDEX : Current
 * PTU index.</li> <li>PTU_DATE : Period of re-optimization.</li> <li>CONNECTION_PORTFOLIO_IN : List of connection group portfolios
 * {@link ConnectionPortfolioDto}.</li> <li>CONNECTION_GROUPS_TO_CONNECTIONS_MAP : map giving the relationship between each
 * connection group and its connections.</li> <li>RECEIVED_FLEXORDER_LIST : aggregate info and collection of {@link
 * FlexOrderDto}</li> <li>LATEST_A_PLAN_DTO_LIST : contains list of most recent {@link PrognosisDto} (A-plans)</li>
 * <li>LATEST_D_PROGNOSIS_DTO_LIST : contains list of most recent {@link PrognosisDto} (D-Prognoses)</li>
 * <li>RELEVANT_PROGNOSIS_LIST : contains list of prognosis relevant to FlexOrder.</li> </ul>
 * <p>
 * The PBC must output the modified connection portfolio and device messages: <ul> <li>CONNECTION_PORTFOLIO_OUT : re-optimized
 * connection portfolio {@link ConnectionGroupPortfolioDto}.</li> <li>DEVICE_MESSAGES_OUT: A list of {@link DeviceMessageDto}
 * objects containing the device messages.</li> </ul>
 * <p>
 * Note: The device messages created in this PBC do NOT match with the changed forecasts in the re-optimized connection portfolio!
 */
public class ReOptimizePortfolioEmpty implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReOptimizePortfolioEmpty.class);

    /**
     * Invoke step to generate a random between min and max nr of messages and put hem on the WorkflowContext.
     *
     * @param context incoming workflow context
     * @return WorkflowContext containing a new list of deviceMessage
     */
    @Override
    @SuppressWarnings("unchecked") public WorkflowContext invoke(WorkflowContext context)
    {
        List<ConnectionPortfolioDto> connectionPortfolio = context.get(IN.CONNECTION_PORTFOLIO_IN.name(), List.class);

        LOGGER.info("Aggregator empty Re-optimize portfolio Stub passing {} connections in the portfolio and no new messages.", connectionPortfolio.size());

        context.setValue(OUT.CONNECTION_PORTFOLIO_OUT.name(), connectionPortfolio);
        context.setValue(OUT.DEVICE_MESSAGES_OUT.name(), new ArrayList<DeviceMessageDto>());

        return context;
    }
}
