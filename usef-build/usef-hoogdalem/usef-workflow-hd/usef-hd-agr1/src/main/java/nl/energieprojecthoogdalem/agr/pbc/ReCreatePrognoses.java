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

import info.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesWorkflowParameter.IN;
import info.usef.agr.workflow.operate.recreate.prognoses.ReCreatePrognosesWorkflowParameter.OUT;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.PrognosisDto;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem implementation of the PBC which will always re-create the A-Plans and D-Prognoses after the re-optimization of the portfolio.
 * <p>
 * The PBC receives the following parameters as input to make the decision:
 * <ul>
 * <li>LATEST_D_PROGNOSES_DTO_LIST : the list of latest {@link info.usef.core.data.xml.bean.message.Prognosis} of type 'D-Progosis'.
 * </li>
 * <li>LATEST_A_PLANS_DTO_LIST : the list of latest {@link info.usef.core.data.xml.bean.message.Prognosis} of type 'A-Plan'.</li>
 * <li>CURRENT_PORTFOLIO : the current (and hence latest) portfolio with connection information.</li>
 * <li>CONNECTION_GROUPS_TO_CONNECTIONS_MAP : a map with a list of connection entity addresses per connection group.</li>
 * </ul>
 * <p>
 * The PBC must output two flags indicating whether A-Plans and/or D-Prognoses must be re-created. These parameters have to be
 * present and have to be named:
 * <ul>
 * <li>REQUIRES_NEW_A_PLAN_SEQUENCES_LIST : {@link java.util.List} of {@link Long} that are the sequence numbers of the
 * A-Plans which will be re-created.</li>
 * <li>REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST : {@link java.util.List} of {@link Long} that are the sequence numbers of the
 * D-Prognoses which will be re-created.</li>
 * </ul>
 */
public class ReCreatePrognoses implements WorkflowStep
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ReCreatePrognoses.class);

    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        List<PrognosisDto> aPlans = (List<PrognosisDto>) context.getValue(IN.LATEST_A_PLANS_DTO_LIST.name());
        List<PrognosisDto> dPrognoses = (List<PrognosisDto>) context.getValue(IN.LATEST_D_PROGNOSES_DTO_LIST.name());

        LOGGER.debug("Received [{}] d-prognoses, [{}] a-plans that needs to be recreated.", dPrognoses.size(), aPlans.size());

        List<Long> recreateAPlanSequences = aPlans.stream().map(PrognosisDto::getSequenceNumber).collect(Collectors.toList());
        List<Long> recreateDPrognosisSequences = dPrognoses.stream().map(PrognosisDto::getSequenceNumber).collect(Collectors.toList());

        context.setValue(OUT.REQUIRES_NEW_A_PLAN_SEQUENCES_LIST.name(), recreateAPlanSequences);
        context.setValue(OUT.REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST.name(), recreateDPrognosisSequences);

        LOGGER.debug("Output values:");
        LOGGER.debug("Re-create A-Plans: [{}]",
                aPlans.stream().map(String::valueOf).collect(Collectors.joining(";")));
        LOGGER.debug("Re-Create D-Prognoses: [{}].",
                dPrognoses.stream().map(String::valueOf).collect(Collectors.joining(";")));

        return context;
    }
}
