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

package energy.usef.dso.workflow.settlement.collect;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import energy.usef.core.config.Config;
import energy.usef.core.data.xml.bean.message.ConnectionMeterEvent;
import energy.usef.core.data.xml.bean.message.MeterData;
import energy.usef.core.data.xml.bean.message.MeterDataSet;
import energy.usef.core.data.xml.bean.message.MeterEventType;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.RegimeType;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.dto.MeterEventTypeDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.model.MeterDataCompany;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.settlement.determine.DetermineOrangeEvent;

/**
 * Test class in charge of the unit tests related to the {@link DsoCollectOrangeRegimeDataCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class DsoCollectOrangeRegimeDataCoordinatorTest {
    private static final LocalDate PERIOD = new LocalDate();
    private static final String ENTITY_ADDRESS = "test.com";
    private static final String CONGESTION_POINT = "ean.111111111111";
    private static final BigInteger EVENT_DATA = BigInteger.valueOf(123L);
    private static final LocalDateTime EVENT_DATE_TIME = new LocalDateTime();
    private static final MeterEventType EVENT_TYPE_XML = MeterEventType.CAPACITY_MANAGEMENT;
    private static final MeterEventTypeDto EVENT_TYPE_DTO = MeterEventTypeDto.CAPACITY_MANAGEMENT;

    private DsoCollectOrangeRegimeDataCoordinator coordinator;

    @Mock
    private Config config;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Mock
    private JMSHelperService jmsHelperService;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private Event<DetermineOrangeEvent> determineOrangeEventManager;

    @Before
    public void init() throws Exception {
        coordinator = new DsoCollectOrangeRegimeDataCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();

        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, dsoPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);
        Whitebox.setInternalState(coordinator, "determineOrangeEventManager", determineOrangeEventManager);
    }

    private WorkflowContext parseContext(WorkflowContext context) {
        List<ConnectionMeterEventDto> connectionMeterEventDtoList = new ArrayList<>();
        ConnectionMeterEventDto connectionMeterEventDto = new ConnectionMeterEventDto();
        connectionMeterEventDto.setEntityAddress(ENTITY_ADDRESS);
        connectionMeterEventDto.setEventData(EVENT_DATA);
        connectionMeterEventDto.setEventDateTime(EVENT_DATE_TIME);
        connectionMeterEventDto.setEventType(EVENT_TYPE_DTO);
        connectionMeterEventDtoList.add(connectionMeterEventDto);
        context.setValue(MeterDataQueryEventsParameter.OUT.CONNECTION_METER_EVENT_DTO_LIST.name(),
                connectionMeterEventDtoList);
        return context;
    }

    /**
     * Tests DsoCollectOrangeRegimeDataCoordinator.initiateCollectOrangeRegimeData method.
     */
    @Test
    public void testInitiateCollectOrangeRegimeData() {
        List<MeterDataCompany> meterDataCompanyList = new ArrayList<>();
        meterDataCompanyList.add(new MeterDataCompany());
        meterDataCompanyList.get(0).setDomain("mdc.usef-example.com");
        Mockito.when(dsoPlanboardBusinessService.findAllMDCs()).thenReturn(meterDataCompanyList);

        LocalDate dayOneMonthBefore = DateTimeUtil.getCurrentDate().minusMonths(1);
        LocalDate startDay = dayOneMonthBefore.withDayOfMonth(1);
        Mockito.when(
                corePlanboardBusinessService.findConnections(Matchers.eq(startDay), Matchers.eq(startDay),
                        Matchers.eq(RegimeType.ORANGE)))
                .thenReturn(Collections.singletonMap(new CongestionPointConnectionGroup(CONGESTION_POINT),
                        Collections.singletonList(new Connection(ENTITY_ADDRESS))));

        InitiateCollectOrangeRegimeDataEvent event = new InitiateCollectOrangeRegimeDataEvent();
        coordinator.initiateCollectOrangeRegimeData(event);

        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).storePlanboardMessage(Mockito.any(PlanboardMessage.class));
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(Matchers.any());
    }

    /**
     * Tests DsoCollectOrangeRegimeDataCoordinator.finalizeCollectOrangeRegimeData method.
     */
    @Test
    public void testFinalizeCollectOrangeRegimeData() {
        FinalizeCollectOrangeRegimeDataEvent event = new FinalizeCollectOrangeRegimeDataEvent(createMeterDataPerCongestionPoint(),
                PERIOD);
        Connection connection = new Connection();
        connection.setEntityAddress(ENTITY_ADDRESS);

        Mockito.when(
                corePlanboardBusinessService.findConnections(Matchers.eq(PERIOD), Matchers.eq(PERIOD),
                        Matchers.eq(RegimeType.ORANGE)))
                .thenReturn(Collections.singletonMap(new CongestionPointConnectionGroup(CONGESTION_POINT),
                        Collections.singletonList(new Connection(ENTITY_ADDRESS))));

        Mockito.when(
                dsoPlanboardBusinessService.findConnectionsNotRelatedToConnectionMeterEvents(Matchers.eq(PERIOD),
                        Matchers.eq(Collections.singletonList(ENTITY_ADDRESS))))
                .thenReturn(Collections.singletonList(connection));

        Mockito.when(corePlanboardBusinessService.findConnection(Matchers.eq(ENTITY_ADDRESS)))
                .thenReturn(connection);

        Mockito.when(workflowStepExecuter
                .invoke(Mockito.eq(DsoWorkflowStep.DSO_METER_DATA_QUERY_EVENTS.name()), Mockito.any(WorkflowContext.class)))
                .then(call -> parseContext((WorkflowContext) call.getArguments()[1]));

        coordinator.finalizeCollectOrangeRegimeData(event);

        Mockito.verify(
                workflowStepExecuter, Mockito.times(1)).invoke(Mockito.eq(DsoWorkflowStep.DSO_METER_DATA_QUERY_EVENTS.name()),
                Mockito.any(WorkflowContext.class));
        Mockito.verify(dsoPlanboardBusinessService, Mockito.times(2)).storeConnectionMeterEvent(
                Mockito.any(energy.usef.dso.model.ConnectionMeterEvent.class));
        Mockito.verify(determineOrangeEventManager, Mockito.times(1)).fire(
                Mockito.any(DetermineOrangeEvent.class));
    }

    private List<MeterDataSet> createMeterDataPerCongestionPoint() {
        MeterDataSet meterDataSet = new MeterDataSet();

        MeterData meterData = new MeterData();
        meterData.setPeriod(PERIOD);
        meterDataSet.getMeterData().add(meterData);

        List<ConnectionMeterEvent> connectionMeterEventList = new ArrayList<>();
        ConnectionMeterEvent connectionMeterEvent = new ConnectionMeterEvent();
        connectionMeterEvent.setEntityAddress(ENTITY_ADDRESS);
        connectionMeterEvent.setEventData(EVENT_DATA);
        connectionMeterEvent.setEventDateTime(EVENT_DATE_TIME);
        connectionMeterEvent.setEventType(EVENT_TYPE_XML);
        connectionMeterEventList.add(connectionMeterEvent);

        meterData.getConnectionMeterEvent().addAll(connectionMeterEventList);

        return Collections.singletonList(meterDataSet);
    }

}
