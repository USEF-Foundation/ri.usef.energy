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

package energy.usef.dso.workflow.step;

import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.dto.MeterEventTypeDto;
import energy.usef.dso.workflow.dto.ConnectionCapacityLimitationPeriodDto;
import energy.usef.dso.workflow.settlement.determine.DetermineOutagePeriodsStepParameter;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;

public class DsoDetermineOrangeOutagePeriodsStubTest {

    String entityAddress = "EntityAddress";

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithInterruption() {
        DsoDetermineOrangeOutagePeriodsStub stub = new DsoDetermineOrangeOutagePeriodsStub();

        WorkflowContext inContext = new DefaultWorkflowContext();
        List<ConnectionMeterEventDto> meterEventsForPeriodDtos = new ArrayList<>();
        ConnectionMeterEventDto meterEvent = new ConnectionMeterEventDto();
        meterEventsForPeriodDtos.add(meterEvent);
        meterEvent.setEntityAddress(entityAddress);
        meterEvent.setEventType(MeterEventTypeDto.CONNECTION_INTERRUPTION);
        meterEvent.setEventDateTime(new LocalDateTime());

        inContext.setValue(DetermineOutagePeriodsStepParameter.IN.CONNECTION_METER_EVENTS.name(), meterEventsForPeriodDtos);

        WorkflowContext outContext = stub.invoke(inContext);
        checkResults(1, (List<ConnectionCapacityLimitationPeriodDto>) outContext.getValue(
                DetermineOutagePeriodsStepParameter.OUT.CONNECTION_METER_EVENT_PERIODS.name()));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithResumption() {
        DsoDetermineOrangeOutagePeriodsStub stub = new DsoDetermineOrangeOutagePeriodsStub();

        WorkflowContext inContext = new DefaultWorkflowContext();
        List<ConnectionMeterEventDto> meterEventsForPeriodDtos = new ArrayList<>();
        ConnectionMeterEventDto meterEvent = new ConnectionMeterEventDto();
        meterEventsForPeriodDtos.add(meterEvent);
        meterEvent.setEntityAddress(entityAddress);
        meterEvent.setEventType(MeterEventTypeDto.CONNECTION_RESUMPTION);
        meterEvent.setEventDateTime(new LocalDateTime());

        inContext.setValue(DetermineOutagePeriodsStepParameter.IN.CONNECTION_METER_EVENTS.name(), meterEventsForPeriodDtos);

        WorkflowContext outContext = stub.invoke(inContext);
        checkResults(1, (List<ConnectionCapacityLimitationPeriodDto>) outContext.getValue(
                DetermineOutagePeriodsStepParameter.OUT.CONNECTION_METER_EVENT_PERIODS.name()));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeWithAll() {
        DsoDetermineOrangeOutagePeriodsStub stub = new DsoDetermineOrangeOutagePeriodsStub();

        WorkflowContext inContext = new DefaultWorkflowContext();
        List<ConnectionMeterEventDto> meterEventsForPeriodDtos = new ArrayList<>();
        ConnectionMeterEventDto meterEvent = new ConnectionMeterEventDto();
        meterEventsForPeriodDtos.add(meterEvent);
        meterEvent.setEntityAddress(entityAddress);
        meterEvent.setEventType(MeterEventTypeDto.CONNECTION_RESUMPTION);
        meterEvent.setEventDateTime(new LocalDateTime("2015-01-01T10:00:00.000"));

        ConnectionMeterEventDto meterEvent2 = new ConnectionMeterEventDto();
        meterEventsForPeriodDtos.add(meterEvent2);
        meterEvent2.setEntityAddress(entityAddress);
        meterEvent2.setEventType(MeterEventTypeDto.CONNECTION_INTERRUPTION);
        meterEvent2.setEventDateTime(new LocalDateTime("2015-01-01T11:00:00.000"));

        ConnectionMeterEventDto meterEvent3 = new ConnectionMeterEventDto();
        meterEventsForPeriodDtos.add(meterEvent3);
        meterEvent3.setEntityAddress(entityAddress);
        meterEvent3.setEventType(MeterEventTypeDto.CONNECTION_RESUMPTION);
        meterEvent3.setEventDateTime(new LocalDateTime("2015-01-01T12:00:00.000"));

        ConnectionMeterEventDto meterEvent4 = new ConnectionMeterEventDto();
        meterEventsForPeriodDtos.add(meterEvent4);
        meterEvent4.setEntityAddress(entityAddress);
        meterEvent4.setEventType(MeterEventTypeDto.CONNECTION_INTERRUPTION);
        meterEvent4.setEventDateTime(new LocalDateTime("2015-01-01T20:00:00.000"));

        inContext.setValue(DetermineOutagePeriodsStepParameter.IN.CONNECTION_METER_EVENTS.name(), meterEventsForPeriodDtos);

        WorkflowContext outContext = stub.invoke(inContext);
        checkResults(3, (List<ConnectionCapacityLimitationPeriodDto>) outContext.getValue(
                DetermineOutagePeriodsStepParameter.OUT.CONNECTION_METER_EVENT_PERIODS.name()));

    }

    private void checkResults(int expectedSize, List<ConnectionCapacityLimitationPeriodDto> results) {
        Assert.assertNotNull(results);
        Assert.assertEquals(expectedSize, results.size());
        results.stream().forEach(r -> {
            Assert.assertEquals(entityAddress, r.getEntityAddress());
            Assert.assertNotNull(r.getStartDateTime());
            Assert.assertNotNull(r.getEndDateTime());
            Assert.assertNull(r.getCapacityReduction());
        });
    }

}
