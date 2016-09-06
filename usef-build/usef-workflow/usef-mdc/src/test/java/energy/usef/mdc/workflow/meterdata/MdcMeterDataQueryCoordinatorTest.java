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

package energy.usef.mdc.workflow.meterdata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.MeterData;
import energy.usef.core.data.xml.bean.message.MeterDataQuery;
import energy.usef.core.data.xml.bean.message.Connections;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.mdc.dto.ConnectionMeterDataDto;
import energy.usef.mdc.dto.MeterDataDto;
import energy.usef.mdc.dto.PtuMeterDataDto;
import energy.usef.mdc.model.DistributionSystemOperator;
import energy.usef.mdc.service.business.MdcCoreBusinessService;
import energy.usef.mdc.workflow.MdcWorkflowStep;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class MdcMeterDataQueryCoordinatorTest {

    @Mock
    private JMSHelperService jmsHelperService;

    @Mock
    private Config config;

    @Mock
    private MdcCoreBusinessService mdcCoreBusinessService;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    private MdcMeterDataQueryCoordinator coordinator = new MdcMeterDataQueryCoordinator();

    @Before
    public void init() {
        coordinator = new MdcMeterDataQueryCoordinator();
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, mdcCoreBusinessService);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Mockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        Mockito.when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("mdc.usef-example.com");
    }

    @Test
    public void testHandleEvent() throws Exception {
        final String dsoDomain = "dso.usef-example.com";
        final LocalDate dateRangeStart = new LocalDate("2014-01-01");
        final LocalDate dateRangeEnd = new LocalDate("2014-01-02");

        MeterDataQueryEvent queryEvent = buildMeterDataQueryEvent(dsoDomain, dateRangeStart, dateRangeEnd);

        Mockito.when(mdcCoreBusinessService.findDistributionSystemOperator(dsoDomain))
                .thenReturn(new DistributionSystemOperator(dsoDomain));

        Mockito.when(mdcCoreBusinessService.findConnectionState(Mockito.any(LocalDate.class),
                Mockito.anyListOf(String.class))).thenReturn(IntStream.rangeClosed(0, 3)
                .mapToObj(i -> "ean." + i)
                .collect(Collectors.toMap(Function.identity(), i -> "agr.usef-example.com")));

        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(MdcWorkflowStep.MDC_METER_DATA_QUERY.name()),
                Mockito.any(WorkflowContext.class))).thenReturn(buildMeterDataContext(dateRangeStart));

        // test
        coordinator.handleEvent(queryEvent);

        ArgumentCaptor<WorkflowContext> contextIn = ArgumentCaptor.forClass(WorkflowContext.class);
        Mockito.verify(workflowStepExecuter, Mockito.times(1)).invoke(Mockito.eq(MdcWorkflowStep.MDC_METER_DATA_QUERY.name()),
                contextIn.capture());

        assertEquals(15, contextIn.getValue().getValue(MeterDataQueryStepParameter.IN.PTU_DURATION.name()));
        assertEquals(dateRangeStart, contextIn.getValue().getValue(MeterDataQueryStepParameter.IN.DATE_RANGE_START.name()));
        assertEquals(dateRangeEnd, contextIn.getValue().getValue(MeterDataQueryStepParameter.IN.DATE_RANGE_END.name()));
        assertEquals(queryEvent.getMeterDataQuery().getConnections().get(0).getConnection(),
                contextIn.getValue().getValue(MeterDataQueryStepParameter.IN.CONNECTIONS.name()));

        // check if power is combined into 1 aggregator (4*10)
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(captor.capture());
        assertTrue(captor.getValue().contains("Result=\"Success\""));
        assertTrue(captor.getValue().contains("Power=\"40\""));
        assertTrue(captor.getValue().contains("EntityCount=\"4\""));
    }

    private MeterDataQueryEvent buildMeterDataQueryEvent(String dsoDomain, LocalDate dateRangeStart, LocalDate dateRangeEnd) {
        MeterDataQuery query = new MeterDataQuery();
        query.setDateRangeStart(dateRangeStart);
        query.setDateRangeEnd(dateRangeEnd);

        Connections connectionGroup = new Connections();
        connectionGroup.setParent("ean.111111111111");

        connectionGroup.getConnection().add("ean.12342143");
        query.getConnections().add(connectionGroup);

        MeterDataQueryEvent queryEvent = new MeterDataQueryEvent(query);
        query.setMessageMetadata(MessageMetadataBuilder.buildDefault());
        query.getMessageMetadata().setSenderRole(USEFRole.DSO);
        query.getMessageMetadata().setSenderDomain(dsoDomain);

        return queryEvent;
    }

    @Test
    public void testHandleEventNoData() {
        LocalDate dateRangeStart = new LocalDate("2014-01-01");
        LocalDate dateRangeEnd = new LocalDate("2014-01-02");
        String dsoDomain = "dso.usef-example.com";

        MeterDataQueryEvent queryEvent = buildMeterDataQueryEvent(dsoDomain, dateRangeStart, dateRangeEnd);
        // not configured
        Mockito.when(mdcCoreBusinessService.findDistributionSystemOperator(dsoDomain))
                .thenReturn(new DistributionSystemOperator(dsoDomain));

        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(MdcWorkflowStep.MDC_METER_DATA_QUERY.name()),
                Mockito.any(WorkflowContext.class))).then(obj -> {
            WorkflowContext workflowContext = (WorkflowContext) obj.getArguments()[1];
            workflowContext.setValue(MeterDataQueryStepParameter.OUT.METER_DATA.name(), new ArrayList<MeterData>());
            return workflowContext;
        });

        // test
        coordinator.handleEvent(queryEvent);

        // check if power is combined into 1 aggregator (4*10)
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(captor.capture());

        assertTrue(captor.getValue().contains("Message=\"No data available for any of the days requested.\""));
        assertTrue(captor.getValue().contains("Result=\"Failure\""));
    }

    @Test
    public void testHandleEventWrongSender() {
        LocalDate dateRangeStart = new LocalDate("2014-01-01");
        LocalDate dateRangeEnd = new LocalDate("2014-01-02");
        String dsoDomain = "dso.usef-example.com";
        MeterDataQueryEvent queryEvent = buildMeterDataQueryEvent(dsoDomain, dateRangeStart, dateRangeEnd);
        // not configured
        Mockito.when(mdcCoreBusinessService.findDistributionSystemOperator(dsoDomain)).thenReturn(null);

        // test
        coordinator.handleEvent(queryEvent);

        // check if power is combined into 1 aggregator (4*10)
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(captor.capture());

        assertTrue(captor.getValue().contains("Message=\"Sender is not a configured customer.\""));
        assertTrue(captor.getValue().contains("Result=\"Failure\""));
    }

    private WorkflowContext buildMeterDataContext(LocalDate dateRangeStart) {
        WorkflowContext workflowContext = new DefaultWorkflowContext();
        List<MeterDataDto> meterDataDtos = IntStream.rangeClosed(0, 1).mapToObj(dateRangeStart::plusDays).map(day -> {
            MeterDataDto meterDataDto = new MeterDataDto();
            meterDataDto.setPeriod(day);
            meterDataDto.getConnectionMeterDataDtos().addAll(
                    // get the connection state for that day and map it
                    IntStream.rangeClosed(0, 3).mapToObj(id -> {
                        ConnectionMeterDataDto connectionMeterDataDto = new ConnectionMeterDataDto();
                        connectionMeterDataDto.setEntityAddress("ean." + id);
                        PtuMeterDataDto ptuMeterDataDto = new PtuMeterDataDto();
                        ptuMeterDataDto.setStart(BigInteger.ONE);
                        ptuMeterDataDto.setDuration(BigInteger.ONE);
                        ptuMeterDataDto.setPower(BigInteger.TEN);
                        connectionMeterDataDto.getPtuMeterDataDtos().add(ptuMeterDataDto);
                        return connectionMeterDataDto;
                    }).collect(Collectors.toList()));
            return meterDataDto;
        }).collect(Collectors.toList());
        workflowContext.setValue(MeterDataQueryStepParameter.OUT.METER_DATA.name(), meterDataDtos);
        return workflowContext;
    }
}
