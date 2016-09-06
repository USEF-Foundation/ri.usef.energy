/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.mdc.pbc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.MeterDataQueryTypeDto;
import info.usef.mdc.dto.ConnectionMeterDataDto;
import info.usef.mdc.dto.MeterDataDto;
import info.usef.mdc.dto.PtuMeterDataDto;
import info.usef.mdc.workflow.meterdata.MeterDataQueryStepParameter;

/**
 * Unit tests to test the MdcMeterDataQuery.
 */
@RunWith(PowerMockRunner.class)
public class MdcMeterDataQueryTest {
    private MdcMeterDataQuery pbc = new MdcMeterDataQuery();

    private static final int PTU_DURATION = 15;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR;
    private static final BigInteger PTUS_PER_DAY = new BigInteger(String.valueOf(MINUTES_PER_DAY / PTU_DURATION));


    @Before
    public void init() throws Exception {
        pbc = new MdcMeterDataQuery();
    }

    /**
     * Tests MdcMeterDataQueryStub.invoke method with META_DATA_QUERY_TYPE = USAGE.
     */
    @Test
    public void testInvokeWithUsageType() {
        WorkflowContext context = new DefaultWorkflowContext();

        LocalDate period = new LocalDate("2014-01-01");

        List<String> connections = new ArrayList<>();
        connections.add("ea.1235");
        connections.add("ea.1236");
        connections.add("ea.1237");
        connections.add("ea.1238");
        context.setValue(MeterDataQueryStepParameter.IN.PTU_DURATION.name(), PTU_DURATION);
        context.setValue(MeterDataQueryStepParameter.IN.DATE_RANGE_START.name(), period);
        context.setValue(MeterDataQueryStepParameter.IN.DATE_RANGE_END.name(), period);
        context.setValue(MeterDataQueryStepParameter.IN.CONNECTIONS.name(), connections);
        context.setValue(MeterDataQueryStepParameter.IN.META_DATA_QUERY_TYPE.name(), MeterDataQueryTypeDto.USAGE);

        // test
        WorkflowContext resultContext = pbc.invoke(context);

        @SuppressWarnings("unchecked")
        List<MeterDataDto> meterDataDtos = (List<MeterDataDto>) resultContext.getValue(MeterDataQueryStepParameter.OUT.METER_DATA.name());

        Assert.assertNotNull(meterDataDtos);
        Assert.assertEquals(1, meterDataDtos.size());
        MeterDataDto meterDataDto = meterDataDtos.get(0);
        Assert.assertEquals(4, meterDataDto.getConnectionMeterDataDtos().size());
        Assert.assertEquals(period, meterDataDto.getPeriod());

        for (ConnectionMeterDataDto connectionMeterDataDto : meterDataDto.getConnectionMeterDataDtos()) {
            Assert.assertNotNull(connectionMeterDataDto.getEntityAddress());
            Assert.assertTrue(connections.contains(connectionMeterDataDto.getEntityAddress()));
            Assert.assertEquals(1, connectionMeterDataDto.getPtuMeterDataDtos().size());
            PtuMeterDataDto ptuMeterDataDto = connectionMeterDataDto.getPtuMeterDataDtos().get(0);
            Assert.assertEquals(BigInteger.ONE, ptuMeterDataDto.getStart());
            Assert.assertEquals(BigInteger.ZERO, ptuMeterDataDto.getPower());
            Assert.assertEquals(PTUS_PER_DAY, ptuMeterDataDto.getDuration());
        }
    }
}
