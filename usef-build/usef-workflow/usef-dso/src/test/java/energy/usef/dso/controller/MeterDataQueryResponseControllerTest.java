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

package energy.usef.dso.controller;

import energy.usef.core.data.xml.bean.message.ConnectionMeterData;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.data.xml.bean.message.MeterData;
import energy.usef.core.data.xml.bean.message.MeterDataQueryResponse;
import energy.usef.core.data.xml.bean.message.MeterDataQueryType;
import energy.usef.core.data.xml.bean.message.MeterDataSet;
import energy.usef.core.data.xml.bean.message.PTUMeterData;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.dso.workflow.settlement.collect.FinalizeCollectOrangeRegimeDataEvent;
import energy.usef.dso.workflow.settlement.initiate.FinalizeInitiateSettlementEvent;

import java.math.BigInteger;
import java.util.List;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link MeterDataQueryResponseController}.
 */
@RunWith(PowerMockRunner.class)
public class MeterDataQueryResponseControllerTest {

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private Event<FinalizeInitiateSettlementEvent> finalizeInitiateSettlementEventManager;

    @Mock
    private Event<FinalizeCollectOrangeRegimeDataEvent> finalizeCollectOrangeRegimeDataEventManager;

    private MeterDataQueryResponseController controller;

    @Before
    public void init() {
        controller = new MeterDataQueryResponseController();

        Whitebox.setInternalState(controller, "finalizeInitiateSettlementEventManager", finalizeInitiateSettlementEventManager);
        Whitebox.setInternalState(controller, "finalizeCollectOrangeRegimeDataEventManager",
                finalizeCollectOrangeRegimeDataEventManager);
        Whitebox.setInternalState(controller, corePlanboardBusinessService);
    }

    @Test
    public void testActionOnSuccessWithEventsType() throws BusinessException {
        PowerMockito.when(corePlanboardBusinessService
                .findSinglePlanboardMessage(Matchers.any(LocalDate.class), Matchers.eq(DocumentType.METER_DATA_QUERY_EVENTS),
                        Matchers.eq("mdc.usef-example.com")))
                .thenReturn(
                        new PlanboardMessage(DocumentType.METER_DATA_QUERY_EVENTS, 1L, DocumentStatus.SENT, "mdc.usef-example.com",
                                new LocalDate("2010-10-01"), null, null, null));

        controller.action(buildMeterDataQueryResponse(DispositionSuccessFailure.SUCCESS, MeterDataQueryType.EVENTS), null);

        Mockito.verify(finalizeCollectOrangeRegimeDataEventManager, Mockito.times(1)).fire(
                Matchers.any(FinalizeCollectOrangeRegimeDataEvent.class));
    }

    @Test
    public void testActionOnSuccessWithUsageType() throws BusinessException {
        PowerMockito.when(corePlanboardBusinessService
                .findSinglePlanboardMessage(Matchers.any(LocalDate.class), Matchers.eq(DocumentType.METER_DATA_QUERY_USAGE),
                        Matchers.eq("mdc.usef-example.com")))
                .thenReturn(
                        new PlanboardMessage(DocumentType.METER_DATA_QUERY_USAGE, 1L, DocumentStatus.SENT, "mdc.usef-example.com",
                                new LocalDate("2010-10-01"), null, null, null));

        controller.action(buildMeterDataQueryResponse(DispositionSuccessFailure.SUCCESS, MeterDataQueryType.USAGE), null);

        ArgumentCaptor<FinalizeInitiateSettlementEvent> eventCaptor = ArgumentCaptor
                .forClass(FinalizeInitiateSettlementEvent.class);
        Mockito.verify(finalizeInitiateSettlementEventManager, Mockito.times(1)).fire(eventCaptor.capture());

        Assert.assertNotNull(eventCaptor.getValue().getMeterDataPerCongestionPoint());
        Assert.assertFalse(eventCaptor.getValue().getMeterDataPerCongestionPoint().isEmpty());
    }

    @Test
    public void testActionOnFailureWithUsageType() throws BusinessException {
        PowerMockito.when(corePlanboardBusinessService
                .findSinglePlanboardMessage(Matchers.any(LocalDate.class), Matchers.eq(DocumentType.METER_DATA_QUERY_USAGE),
                        Matchers.eq("mdc.usef-example.com")))
                .thenReturn(
                        new PlanboardMessage(DocumentType.METER_DATA_QUERY_USAGE, 1L, DocumentStatus.SENT, "mdc.usef-example.com",
                                new LocalDate("2010-10-01"), null, null, null));

        controller.action(buildMeterDataQueryResponse(DispositionSuccessFailure.FAILURE, MeterDataQueryType.USAGE), null);

        ArgumentCaptor<FinalizeInitiateSettlementEvent> eventCaptor = ArgumentCaptor
                .forClass(FinalizeInitiateSettlementEvent.class);
        Mockito.verify(finalizeInitiateSettlementEventManager, Mockito.times(1)).fire(eventCaptor.capture());

        Assert.assertNotNull(eventCaptor.getValue().getMeterDataPerCongestionPoint());
        Assert.assertTrue(eventCaptor.getValue().getMeterDataPerCongestionPoint().isEmpty());
    }

    private MeterDataQueryResponse buildMeterDataQueryResponse(DispositionSuccessFailure success, MeterDataQueryType queryType) {
        MeterDataQueryResponse meterDataQueryResponse = new MeterDataQueryResponse();
        meterDataQueryResponse.setMessageMetadata(MessageMetadataBuilder.buildDefault());
        meterDataQueryResponse.getMessageMetadata().setSenderDomain("mdc.usef-example.com");
        meterDataQueryResponse.setResult(success);
        meterDataQueryResponse.setQueryType(queryType);

        List<MeterDataSet> meterDataSets = meterDataQueryResponse
                .getMeterDataSet();
        MeterData meterData = new MeterData();
        meterData.setPeriod(new LocalDate("2010-10-01"));
        meterData.getConnectionMeterData().add(buildConnectionMeterData());

        MeterDataSet meterDataSet = new MeterDataSet();
        meterDataSet.getMeterData().add(meterData);
        meterDataSets.add(meterDataSet);

        return meterDataQueryResponse;
    }

    private ConnectionMeterData buildConnectionMeterData() {
        ConnectionMeterData connectionMeterData = new ConnectionMeterData();
        connectionMeterData.setAGRDomain("agr1.usef-example.com");
        connectionMeterData.setEntityAddress("ean1.11111111");
        connectionMeterData.getPTUMeterData().add(buildPtuMeterData());

        return connectionMeterData;
    }

    private PTUMeterData buildPtuMeterData() {
        PTUMeterData ptuMeterData = new PTUMeterData();

        ptuMeterData.setStart(BigInteger.ONE);
        ptuMeterData.setDuration(BigInteger.ONE);
        ptuMeterData.setPower(BigInteger.TEN);

        return ptuMeterData;
    }
}
