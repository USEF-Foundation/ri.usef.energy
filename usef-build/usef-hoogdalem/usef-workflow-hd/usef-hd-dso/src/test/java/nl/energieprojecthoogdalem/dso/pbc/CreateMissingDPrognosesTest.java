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
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.dso.workflow.validate.gridsafetyanalysis.CreateMissingDPrognosisParameter;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

/**
 * Unit tests to test the {@link CreateMissingDPrognoses}.
 */
public class CreateMissingDPrognosesTest {
    private static final LocalDate ANALYSIS_DAY = new LocalDate(2014, 5, 20);
    private static final int PTU_DURATION = 15;
    private static final String AGGREGATOR_DOMAIN = "test.com";
    private static final int AGGREGATOR_CONNECTION_NUMBER = 3;

    private CreateMissingDPrognoses CreateMissingDPrognosis;

    @Before
    public void init() throws Exception {
        CreateMissingDPrognosis = new CreateMissingDPrognoses();
    }

    /**
     * Tests DsoCreateMissingPrognosisStub.invoke method.
     */
    @Test
    public void invoke() {
        WorkflowContext inContext = new DefaultWorkflowContext();
        prepateContext(inContext);
        WorkflowContext outContext = CreateMissingDPrognosis.invoke(inContext);

        PrognosisDto prognosisDto = (PrognosisDto) outContext
                .getValue(CreateMissingDPrognosisParameter.OUT.D_PROGNOSIS.name());

        prognosisDto.getPtus().forEach(ptuPrognosisDto ->

                Assert.assertEquals(BigInteger.ZERO, ptuPrognosisDto.getPower())
        );

        Assert.assertEquals(PtuUtil.getNumberOfPtusPerDay(ANALYSIS_DAY, PTU_DURATION), prognosisDto.getPtus().size());
    }

    private void prepateContext(WorkflowContext context) {
        context.setValue(CreateMissingDPrognosisParameter.IN.AGGREGATOR_DOMAIN.name(), AGGREGATOR_DOMAIN);
        context.setValue(CreateMissingDPrognosisParameter.IN.ANALYSIS_DAY.name(), ANALYSIS_DAY);
        context.setValue(CreateMissingDPrognosisParameter.IN.PTU_DURATION.name(), PTU_DURATION);
        context.setValue(CreateMissingDPrognosisParameter.IN.AGGREGATOR_CONNECTION_AMOUNT
                .name(), AGGREGATOR_CONNECTION_NUMBER);
    }

}
