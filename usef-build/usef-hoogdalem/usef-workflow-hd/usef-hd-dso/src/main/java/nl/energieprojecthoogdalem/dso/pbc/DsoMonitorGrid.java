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

import nl.energieprojecthoogdalem.dso.limits.LimitConfiguration;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.dso.workflow.operate.DsoMonitorGridStepParameter.OUT;

/**
 * Hoogdalem implementation for the {@link DsoMonitorGrid}.
 * returns no congestion
 */
public class DsoMonitorGrid implements WorkflowStep {

    //private static final Logger LOGGER = LoggerFactory.getLogger(DsoMonitorGrid.class);

    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        //LOGGER.info("Monitor Grid returning no congestion");

        context.setValue(OUT.CONGESTION.name(), false);

        context.setValue(OUT.ACTUAL_LOAD.name(), 0L);

        context.setValue(OUT.MIN_LOAD.name(), LimitConfiguration.DEFAULT_LOWER.longValue());
        context.setValue(OUT.MAX_LOAD.name(), LimitConfiguration.DEFAULT_UPPER.longValue() );

        return context;
    }

}
