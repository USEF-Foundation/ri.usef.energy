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
import info.usef.dso.workflow.plan.connection.forecast.DsoCreateNonAggregatorForecastParameter.OUT;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem version of create non-Aggregator Forecast, no forecast required
 * */
public class CreateNonAggregatorForecast implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateNonAggregatorForecast.class);

    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.info("create non-Aggregator Forecast executed, no forecast required, returning empty lists");

        context.setValue(OUT.POWER.name(), new ArrayList<>());
        context.setValue(OUT.MAXLOAD.name(), new ArrayList<>());

        return context;
    }
}
