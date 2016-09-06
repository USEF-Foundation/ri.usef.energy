/*
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package energy.usef.mdc.workflow.step;

import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.dto.MeterDataQueryTypeDto;
import energy.usef.mdc.dto.ConnectionMeterDataDto;
import energy.usef.mdc.dto.MeterDataDto;
import energy.usef.mdc.dto.PtuMeterDataDto;
import energy.usef.mdc.pbcfeederimpl.PbcFeederService;
import energy.usef.mdc.workflow.meterdata.MeterDataQueryStepParameter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit tests to test the MdcMeterDataQueryStub.
 */
@RunWith(PowerMockRunner.class)
public class MdcMeterDataQueryStubTest {
    private MdcMeterDataQueryStub stub = new MdcMeterDataQueryStub();

    @Mock
    private PbcFeederService pbcFeederService;

    @Before
    public void init() throws Exception {
        stub = new MdcMeterDataQueryStub();
        Whitebox.setInternalState(stub, pbcFeederService);

        stubPbcFeederService();
    }

    /**
     * Tests MdcMeterDataQueryStub.invoke method with META_DATA_QUERY_TYPE = USAGE.
     */
    @Test
    public void testInvokeWithUsageType() {
        WorkflowContext context = new DefaultWorkflowContext();

        LocalDate period = new LocalDate("2014-01-01");

        List<String> connections = new ArrayList<>();
        String entityAddress = "ea.1235";
        connections.add(entityAddress);
        context.setValue(MeterDataQueryStepParameter.IN.PTU_DURATION.name(), 15);
        context.setValue(MeterDataQueryStepParameter.IN.DATE_RANGE_START.name(), period);
        context.setValue(MeterDataQueryStepParameter.IN.DATE_RANGE_END.name(), period);
        context.setValue(MeterDataQueryStepParameter.IN.CONNECTIONS.name(), connections);
        context.setValue(MeterDataQueryStepParameter.IN.META_DATA_QUERY_TYPE.name(), MeterDataQueryTypeDto.USAGE);

        // test
        WorkflowContext resultContext = stub.invoke(context);

        @SuppressWarnings("unchecked")
        List<MeterDataDto> meterDataDtos = (List<MeterDataDto>) resultContext.getValue(MeterDataQueryStepParameter.OUT.METER_DATA.name());

        Assert.assertNotNull(meterDataDtos);
        Assert.assertEquals(1, meterDataDtos.size());
        MeterDataDto meterDataDto = meterDataDtos.get(0);
        Assert.assertEquals(1, meterDataDto.getConnectionMeterDataDtos().size());
        Assert.assertEquals(period, meterDataDto.getPeriod());

        ConnectionMeterDataDto connectionMeterDataDto = meterDataDto.getConnectionMeterDataDtos().get(0);
        Assert.assertNotNull(connectionMeterDataDto.getEntityAddress());
        Assert.assertEquals(entityAddress, connectionMeterDataDto.getEntityAddress());
        Assert.assertEquals(96, connectionMeterDataDto.getPtuMeterDataDtos().size());
    }

    /**
     * Tests MdcMeterDataQueryStub.invoke method with META_DATA_QUERY_TYPE = EVENTS.
     */
    @Test
    public void testInvokeWithEventsType() {
        WorkflowContext context = new DefaultWorkflowContext();

        LocalDate period = new LocalDate("2014-01-01");

        List<String> connections = new ArrayList<>();
        String entityAddress = "ea.1235";
        connections.add(entityAddress);
        context.setValue(MeterDataQueryStepParameter.IN.PTU_DURATION.name(), 15);
        context.setValue(MeterDataQueryStepParameter.IN.DATE_RANGE_START.name(), period);
        context.setValue(MeterDataQueryStepParameter.IN.DATE_RANGE_END.name(), period);
        context.setValue(MeterDataQueryStepParameter.IN.CONNECTIONS.name(), connections);
        context.setValue(MeterDataQueryStepParameter.IN.META_DATA_QUERY_TYPE.name(), MeterDataQueryTypeDto.EVENTS);

        // test
        WorkflowContext resultContext = stub.invoke(context);

        @SuppressWarnings("unchecked")
        List<MeterDataDto> meterDataDtos = (List<MeterDataDto>) resultContext.getValue(MeterDataQueryStepParameter.OUT.METER_DATA.name());
        if (!meterDataDtos.isEmpty()) {
            MeterDataDto meterDataDto = meterDataDtos.get(0);
            ConnectionMeterEventDto connectionMeterEventDto = meterDataDto.getConnectionMeterEventDtos().get(0);
            Assert.assertNotNull(connectionMeterEventDto.getEntityAddress());
            Assert.assertNotNull(connectionMeterEventDto.getEventDateTime());
        }
    }

    @SuppressWarnings("unchecked")
    private void stubPbcFeederService() {
        PowerMockito.when(pbcFeederService.fetchUncontrolledLoad(Matchers.any(LocalDate.class), Matchers.any(Integer.class),
                Matchers.any(Integer.class), Matchers.anyListOf(String.class)))
                .then(invocation -> ((List<String>) invocation.getArguments()[3]).stream().map(entityAddress -> {
                    ConnectionMeterDataDto connectionMeterDataDto = new ConnectionMeterDataDto();
                    connectionMeterDataDto.setEntityAddress(entityAddress);
                    IntStream.rangeClosed(1, 96).mapToObj(index -> {
                        PtuMeterDataDto ptuMeterDataDto = new PtuMeterDataDto();
                        ptuMeterDataDto.setStart(BigInteger.valueOf(index));
                        ptuMeterDataDto.setDuration(BigInteger.valueOf(1));
                        ptuMeterDataDto.setPower(BigInteger.TEN);
                        return ptuMeterDataDto;
                    }).forEach(ptu -> connectionMeterDataDto.getPtuMeterDataDtos().add(ptu));
                    return connectionMeterDataDto;
                }).collect(Collectors.toList()));

    }
}
